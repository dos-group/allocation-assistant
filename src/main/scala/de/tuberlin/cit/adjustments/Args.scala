package de.tuberlin.cit.adjustments

import org.rogach.scallop.exceptions.ScallopException
import org.rogach.scallop.{ScallopConf, ScallopOption}

class Args(a: Seq[String]) extends ScallopConf(a) {
  override def onError(e: Throwable): Unit = e match {
    case ScallopException(message) =>
      println(message)
//      println(s"Usage: allocation-assistant -c <config> -r <max runtime> -m <memory> -s <slots> " +
//        s"-i <fallback containers> -N <max containers> " +
//        s"[more args ...] <Jar> [Jar args ...]")
      println()
      printHelp()
      System.exit(1)
    case other => super.onError(e)
  }

//  val config = opt[String](required = true,
//    descr = "Path to the .conf file")
//
//  val engine = opt[String](required = true,
//    descr = "engine to use (Flink or Spark)").map(_.toLowerCase)
//
  val maxRuntime: ScallopOption[Double] = opt[Double](required = true, short = 'r',
    descr = "Maximum runtime in seconds")

//  val memory = opt[Int](
//    descr = "Memory per container, in MB")
//  val masterMemory = opt[Int](short = 'M',
//    descr = "Master memory, in MB (Flink JobManager or Spark driver)")
//  val slots = opt[Int](
//    descr = "Number of slots per TaskManager")

//  val fallbackContainers: ScallopOption[Int] = opt[Int](required = true, short = 'i',
//    descr = "Amount of containers to assign, if no previous runs are available or " +
//      "if the runtime constraint cannot be fulfilled")
  val minContainers: ScallopOption[Int] = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers: ScallopOption[Int] = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  val adaptive: ScallopOption[Boolean] = opt[Boolean](default = Option(false), noshort = true,
    descr = "Enables runtime adjustments by scaling between jobs")


  //  val jarWithArgs = trailArg[List[String]](required = true, name = "xy.jar [arg1 arg2 ...]",
//    descr = "Jar to run and its arguments")

  verify()
}
