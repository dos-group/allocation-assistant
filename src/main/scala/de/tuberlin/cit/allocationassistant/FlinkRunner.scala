package de.tuberlin.cit.allocationassistant

import java.io.File
import java.time.Instant

import de.tuberlin.cit.freamon.api.{ApplicationStart, ApplicationStop}

import scala.sys.process._

class FlinkRunner(options: Options, freamon: Freamon) {

  /** Executes the application on Flink and triggers Freamon to start/stop recording.
    *
    * @param scaleOut number of containers to run the application on,
    *                 limited by the minContainers and maxContainers arguments
    */
  def runFlink(scaleOut: Int): Int = {
    // TODO set #slots from args
    val cmd = s"${options.flink} run -m yarn-cluster" +
      s" -yn $scaleOut" +
      s" -ytm ${options.args.memory()}" +
      s" ${options.jarWithArgs}"
    println(s"Executing command $cmd")

    val logPath = options.cmdLogPath + File.separator + Instant.now()
    println(s"Saving command output to $logPath")

    new File(options.cmdLogPath).mkdirs()
    val fileOutput = new FlinkLogger(new File(logPath))
    val envHadoop = "HADOOP_CONF_DIR" -> options.hadoopConfDir

    val result = Process(cmd, Option.empty, envHadoop) ! fileOutput

    if (fileOutput.canFinish) {
      println("Job did not finish, stopping Freamon manually")
      freamon.freamonMaster ! ApplicationStop(fileOutput.appId, System.currentTimeMillis())
    }

    result
  }

  class FlinkLogger(file: File) extends FileProcessLogger(file) {
    val submitMarker = "Submitted application"

    var appId = "no appId"
    var startTime = 0L

    // the marker text could appear several times, we only trigger the first time
    var canStart = true
    var canFinish = false

    override def out(line: => String) {
      if (line.contains(submitMarker)) {
        appId = line.substring(line.indexOf(submitMarker))
          .replace(submitMarker, "").trim
        println(line) // both informative and for Yarn-Workload-Runner
      }

      if (canStart &&
        (line.contains("Job execution switched to status RUNNING")
          || line.contains("All TaskManagers are connected"))) {

        startTime = System.currentTimeMillis()
        freamon.freamonMaster ! ApplicationStart(
          appId, startTime, options.jarWithArgs, options.args.slots(), options.args.memory())

        println("Job started as " + appId)

        canStart = false
        canFinish = true
      }

      if (canFinish &&
        (line.contains("Job execution switched to status FINISHED")
          || line.contains("The following messages were " +
          "created by the YARN cluster while running the Job:"))) {

        freamon.freamonMaster ! ApplicationStop(appId, System.currentTimeMillis())

        val duration = (System.currentTimeMillis() - startTime) / 1000f
        println(s"Job finished, took $duration seconds")

        canFinish = false
      }

      super.out(line)
    }
  }

}
