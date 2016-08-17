package de.tuberlin.cit.allocationassistant

class FlinkRunner(options: Options, freamon: Freamon) extends CommandRunner(options, freamon) {
  override val framework: Symbol = 'Flink

  override def buildCmd(scaleOut: Int): String = {
    // TODO set #slots from args
    options.conf.getString("allocation-assistant.flink") +
      s" run -m yarn-cluster" +
      s" -yn $scaleOut" +
      s" -ytm ${options.args.memory()}" +
      s" ${options.jarWithArgs}"
  }

  override def getAppId(line: String): Option[String] = {
    val submitMarker = "Submitted application "
    if (line.contains(submitMarker)) {
      Some(line.substring(line.indexOf(submitMarker))
        .replace(submitMarker, "").trim)
    }
    else None
  }

  override def isStartLine(line: String): Boolean = {
    (line.contains("Job execution switched to status RUNNING")
      || line.contains("All TaskManagers are connected"))
  }

  override def isStopLine(line: String): Boolean = {
    (line.contains("Job execution switched to status FINISHED")
      || line.contains("The following messages were " +
      "created by the YARN cluster while running the Job:"))
  }

}
