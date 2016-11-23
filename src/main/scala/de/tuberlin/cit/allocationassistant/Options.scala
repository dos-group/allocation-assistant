package de.tuberlin.cit.allocationassistant

import java.io.File
import java.nio.file.{Files, Paths}
import java.security.MessageDigest

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.ScallopException

import scala.util.Try

class Args(a: Seq[String]) extends ScallopConf(a) {
  override def onError(e: Throwable): Unit = e match {
    case ScallopException(message) =>
      println(message)
      println(s"Usage: allocation-assistant -c <config> -r <max runtime> -m <memory> -s <slots> " +
        s"-i <fallback containers> -N <max containers> " +
        s"[more args ...] <Jar> [Jar args ...]")
      println()
      printHelp()
      System.exit(1)
    case other => super.onError(e)
  }

  val config = opt[String](required = true,
    descr = "Path to the .conf file")

  val engine = opt[String](required = true,
    descr = "engine to use (Flink or Spark)").map(_.toLowerCase)

  val maxRuntime = opt[Double](required = true, short = 'r',
    descr = "Maximum runtime in seconds")

  val memory = opt[Int](
    descr = "Memory per container, in MB")
  val slots = opt[Int](
    descr = "Number of slots per TaskManager")

  val fallbackContainers = opt[Int](required = true, short = 'i',
    descr = "Amount of containers to assign, if no previous runs are available or " +
      "if the runtime constraint cannot be fulfilled")
  val minContainers = opt[Int](short = 'n', default = Option(1),
    descr = "Minimum number of containers to assign")
  val maxContainers = opt[Int](required = true, short = 'N',
    descr = "Maximum number of containers to assign")

  val jarWithArgs = trailArg[List[String]](required = true, name = "xy.jar [arg1 arg2 ...]",
    descr = "Jar to run and its arguments")

  verify()
}

class Options(rawArgs: Array[String]) {
  val args = new Args(rawArgs)

  println(s"Loading configuration at ${args.config()}")
  // configuration from the file passed as argument, with defaults from application.conf
  val conf = ConfigFactory.parseFile(new File(args.config()))
    .withFallback(ConfigFactory.load()).resolve()

  // configuration prepared for use with akka
  val akka = ConfigFactory.parseString(
    "akka.remote.netty.tcp.hostname=" + conf.getString("allocation-assistant.actors.hostname")
      + "\nakka.remote.netty.tcp.port=" + conf.getInt("allocation-assistant.actors.port")
  ).withFallback(conf)

  val cmdLogPath = conf.getString("allocation-assistant.flink-logs")
  val hadoopConfDir: String = System.getenv("HADOOP_PREFIX") + "/etc/hadoop/"

  // hash of first jar file in trailing args
  val jarSignature = {
    val jarPath = args.jarWithArgs().find(_.endsWith(".jar")).head
    val jarBytes = Files.readAllBytes(Paths.get(jarPath))
    val hashBytes = MessageDigest.getInstance("MD5").digest(jarBytes)
    hashBytes.map("%02x".format(_)).mkString
  }

  /** size of the input dataset (all `hdfs://` URI args but the last) in bytes */
  val inputSize: Double =
  Try {
    val datasetPaths = args.jarWithArgs().filter(_.startsWith("hdfs://"))
    datasetPaths.takeRight(1) // all except last
    val conf = new Configuration()
    conf.addResource(new Path(hadoopConfDir, "core-site.xml"))
    conf.addResource(new Path(hadoopConfDir, "hdfs-site.xml"))
    conf.setIfUnset("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
    val fs = FileSystem.get(conf)
    var sizeSum: Double = 0
    for (inputPath <- datasetPaths) {
      val filesIter = fs.listFiles(new Path(inputPath), true)
      while (filesIter.hasNext) {
        sizeSum += filesIter.next.getLen
      }
    }
    sizeSum
  }.getOrElse(0)
}
