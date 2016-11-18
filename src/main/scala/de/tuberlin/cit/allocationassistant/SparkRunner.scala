package de.tuberlin.cit.allocationassistant

class SparkRunner(options: Options, freamon: Freamon) extends CommandRunner(options, freamon) {
  override val framework: Symbol = 'Spark

  override def buildCmd(scaleOut: Int): String = {
    var s = options.conf.getString("allocation-assistant.spark") +
      s" --master yarn --deploy-mode cluster --num-executors $scaleOut "
    if (options.args.memory.isDefined) {
      s += s"--executor-memory ${options.args.memory()} "
    }
    if (options.args.slots.isDefined) {
      s += s"--executor-cores ${options.args.slots()} "
    }
    s + options.args.jarWithArgs().mkString(" ")
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
