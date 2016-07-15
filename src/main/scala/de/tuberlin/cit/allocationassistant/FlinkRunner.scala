package de.tuberlin.cit.allocationassistant

import java.io.File

import scala.sys.process._

class FlinkRunner(conf: ConfigUtil, freamon: Freamon) {

  def buildCommand(): String = {
    "" // TODO build command
  }

  def runFlink(resourceAlloc: Any): Int = {
    val fileOutput = new FilteringLogger(new File(conf.cmdLogPath))
    val envHadoop = "HADOOP_CONF_DIR" -> conf.hadoopConfDir
    Process("cmd", Option.empty, envHadoop) ! fileOutput
  }

  class FilteringLogger(file: File) extends FileProcessLogger(file) {
    val submitMarker = "Submitted application"

    var appId: String = null
    var running = false

    override def out(line: => String) {
      if (line.contains(submitMarker)) {
        appId = line.substring(line.indexOf(submitMarker))
          .replace(submitMarker, "").trim
      }

      if (!running &&
        (line.contains("Job execution switched to status RUNNING")
          || line.contains("All TaskManagers are connected"))) {
        freamon.sendStart(appId)
        println(s"$appId started")
      }

      super.out(line)
    }
  }

}
