package de.tuberlin.cit.allocationassistant

import java.io.File
import java.time.Instant

import de.tuberlin.cit.freamon.api.{ApplicationMetadata, ApplicationStart, ApplicationStop}

import scala.sys.process._

abstract class CommandRunner(options: Options, freamon: Freamon) {

  val framework: Symbol

  def buildCmd(scaleOut: Int): String

  def getAppId(line: String): Option[String]

  def isStartLine(line: String): Boolean

  def isStopLine(line: String): Boolean

  /** Executes the application and triggers Freamon to start/stop recording.
    *
    * @param scaleOut number of containers to run the application on
    */
  def run(scaleOut: Int): Int = {
    // TODO set #slots from args
    val cmd = buildCmd(scaleOut)
    println(s"Executing command $cmd")

    val logPath = options.cmdLogPath + File.separator + Instant.now() + ".log"
    println(s"Saving command output to $logPath")

    new File(options.cmdLogPath).mkdirs()
    val commandWrapper = new CommandWrapper(new File(logPath))
    val envHadoop = "HADOOP_CONF_DIR" -> options.hadoopConfDir

    val result = Process(cmd, Option.empty, envHadoop) ! commandWrapper

    if (commandWrapper.canFinish) {
      println("Job did not finish, stopping Freamon manually")
      freamon.freamonMaster ! ApplicationStop(commandWrapper.appId, System.currentTimeMillis())
    }

    commandWrapper.close()

    result
  }

  def sendMetadata(appId: String): Unit = {
    freamon.freamonMaster ! ApplicationMetadata(appId,
      framework=framework,
      signature=options.jarSignature,
      datasetSize=options.datasetSize,
      coresPerContainer=options.args.slots(),
      memoryPerContainer=options.args.memory()
    )
  }

  class CommandWrapper(file: File) extends FileProcessLogger(file) {
    var appId = "no appId"
    var startTime = 0L

    // the marker text could appear several times, we only trigger the first time
    var canStart = true
    var canFinish = false

    override def out(line: => String) {
      handleLine(line)
      super[FileProcessLogger].out(line)
    }

    override def err(lineFn: => String) {
      val line = lineFn + ""
      handleLine(line)
      super[FileProcessLogger].err(line)
    }

    def handleLine(line: String): Unit = {
      getAppId(line).map { id =>
        appId = id
        println("Submitted application " + appId) // both informative and for Yarn-Workload-Runner
        Unit
      }

      if (canStart && isStartLine(line)) {
        startTime = System.currentTimeMillis()
        freamon.freamonMaster ! ApplicationStart(appId, startTime)
        sendMetadata(appId)

        println("Job started as " + appId)

        canStart = false
        canFinish = true
      }

      if (canFinish && isStopLine(line)) {
        freamon.freamonMaster ! ApplicationStop(appId, System.currentTimeMillis())

        val duration = (System.currentTimeMillis() - startTime) / 1000f
        println(s"Job finished, took $duration seconds")

        canFinish = false
      }
    }
  }

}
