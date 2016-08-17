package de.tuberlin.cit.allocationassistant

class SparkRunner(options: Options, freamon: Freamon) extends CommandRunner(options, freamon) {
  override val framework: Symbol = 'Spark

  override def buildCmd(scaleOut: Int): String = {
    // TODO set #slots from args
    options.conf.getString("allocation-assistant.spark") +
      s" --master yarn --deploy-mode cluster" +
      s" --num-executors $scaleOut" +
      s" --executor-memory ${options.args.memory()} " +
      s"${options.args.jarWithArgs().mkString(" ")}"
  }

  override def getAppId(line: String): Option[String] = {
    val submitMarker = "Submitting application "
    val toMarker = " to ResourceManager"
    if (line.contains(submitMarker)) {
      Some(line.substring(line.indexOf(submitMarker))
        .replace(submitMarker, "")
        .replace(toMarker, "")
        .trim)
    }
    else None
  }

  override def isStartLine(line: String): Boolean = {
    line.contains("Application report for application_") &&
      line.contains(" (state: RUNNING)")
  }

  override def isStopLine(line: String): Boolean = {
    line.contains("Application report for application_") &&
      line.contains(" (state: FINISHED)")
  }

}
