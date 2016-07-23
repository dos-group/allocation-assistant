package de.tuberlin.cit.allocationassistant

import java.io.File

import com.typesafe.config.ConfigFactory
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.ScallopException

class Args(a: Seq[String]) extends ScallopConf(a) {
  override def onError(e: Throwable): Unit = e match {
    case ScallopException(message) =>
      println(message)
      println(s"Usage: allocation-assistant -c <config> -r <max runtime> -m <memory> -s <slots>" +
        s" [more args ...] <Jar> [Jar args ...]\n")
      printHelp()
      System.exit(1)
    case other => throw other
  }

  val config = opt[String](required = true,
    descr = "Path to the .conf file")

  val maxRuntime = opt[Int](required = true, short = 'r',
    descr = "Maximum runtime in seconds")

  val memory = opt[Int](required = true,
    descr = "Memory per container, in MB")
  val slots = opt[Int](required = true,
    descr = "Number of slots per TaskManager")

  val initialContainers = opt[Int](
    descr = "If no previous runs are available, this many containers are assigned")
  val minContainers = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers = opt[Int](short = 'N', default = Option(Int.MaxValue),
    descr = "Maximum number of containers to assign")

  val rawJarWithArgs = trailArg[List[String]](required = true, name = "jarWithArgs",
    descr = "Jar to run and its arguments")

  verify()
}

class Options(rawArgs: Array[String]) {
  val args = new Args(rawArgs)

  val jarWithArgs = args.rawJarWithArgs().mkString(" ")

  // easier to access from java
  val maxRuntime = args.maxRuntime()

  println(s"Loading configuration at ${args.config()}")
  // configuration from the file passed as argument, with defaults from application.conf
  val conf = ConfigFactory.parseFile(new File(args.config()))
    .withFallback(ConfigFactory.load()).resolve()

  // configuration prepared for use with akka
  val akka = ConfigFactory.parseString(
    "akka.remote.netty.tcp.hostname=" + conf.getString("allocation-assistant.actors.hostname")
      + "\nakka.remote.netty.tcp.port=" + conf.getInt("allocation-assistant.actors.port")
  ).withFallback(conf)

  val flink = conf.getString("allocation-assistant.flink")
  val cmdLogPath = conf.getString("allocation-assistant.flink-logs")
  val hadoopConfDir: String = System.getenv("HADOOP_PREFIX") + "/etc/hadoop/"

}
