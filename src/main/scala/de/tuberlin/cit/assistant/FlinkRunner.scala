package de.tuberlin.cit.assistant

class FlinkRunner(options: Options, freamon: Freamon) extends CommandRunner(options, freamon) {
  override val framework: Symbol = 'Flink

  override def buildCmd(scaleOut: Int): String = {
    var s = options.conf.getString("allocation-assistant.flink") +
      s" run -m yarn-cluster -yn $scaleOut "
    if (options.args.masterMemory.isDefined) {
      s += s"-yjm ${options.args.masterMemory()} "
    }
    if (options.args.memory.isDefined) {
      s += s"-ytm ${options.args.memory()} "
    }
    if (options.args.slots.isDefined) {
      s += s"-ys ${options.args.slots()} "
    }
    s + options.args.jarWithArgs().mkString(" ")
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
