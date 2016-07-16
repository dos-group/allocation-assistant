package de.tuberlin.cit.allocationassistant

import java.io.File

import scala.sys.process._

class FlinkRunner(conf: ConfigUtil, freamon: Freamon) {

  def buildCommand(): String = {
    "" // TODO build command
  }

  def runFlink(resourceAlloc: Any): Int = {
    val fileOutput = new FilteringLogger(new File(conf.cmdLogPath))
    val envHadoop = "HADOOP_CONF_DIR" -> conf.hadoopConfDir // TODO would HADOOP_PREFIX work too?
    Process("cmd", Option.empty, envHadoop) ! fileOutput
  }

  class FilteringLogger(file: File) extends FileProcessLogger(file) {
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
      }

      if (canStart &&
        (line.contains("Job execution switched to status RUNNING")
          || line.contains("All TaskManagers are connected"))) {
        startTime = System.currentTimeMillis()
        freamon.sendStart(appId)
        println(s"$appId started")
        canStart = false
        canFinish = true
      }

      if (canFinish &&
        (line.contains("Job execution switched to status FINISHED")
          || line.contains("The following messages were " +
          "created by the YARN cluster while running the Job:"))) {
        freamon.sendStop(appId)
        val duration = (System.currentTimeMillis() - startTime) / 1000
        println(s"$appId finished, took $duration seconds")
        canFinish = false
      }

      super.out(line)
    }
  }

}
