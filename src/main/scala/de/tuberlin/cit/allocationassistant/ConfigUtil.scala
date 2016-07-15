package de.tuberlin.cit.allocationassistant

import java.io.File

import com.typesafe.config.ConfigFactory
import org.rogach.scallop.Scallop

class ConfigUtil(rawArgs: Array[String]) {
  val args = new Scallop(rawArgs)

    // optional

    .opt[Int]("initialAllocation", default = () => Option(1),
    descr = "if no previous runs are available, this allocation is used")

    .opt[Int]("runtimeTarget", default = () => Option(1),
    descr = "preferred runtime in seconds")

    .opt[Int]("max", default = () => Option(1),
    descr = "preferred runtime in seconds")

    // required

    .opt[String]("config", required = true,
    descr = "path to the .conf file")

    .opt[String]("jarFile", required = true,
    descr = "path to the Jar file that will be run")

    .opt[String]("jarArgs", required = true, short = 'a',
    descr = "arguments for the Jar")

    // TODO add all trailing options as jarArgs?

    .verify

  println("Loading configuration at " + args("config"))

  // configuration from the file passed as argument, with defaults from application.conf
  val conf = ConfigFactory.parseFile(new File(args[String]("config")))
    .withFallback(ConfigFactory.load()).resolve()

  // configuration prepared for use with akka
  val akka = ConfigFactory.parseString(
    "akka.remote.netty.tcp.hostname=" + conf.getString("allocation-assistant.freamon.hostname")
      + "\nakka.remote.netty.tcp.port=" + conf.getInt("allocation-assistant.freamon.port")
  ).withFallback(conf)

  val cmdLogPath = "logs/" // TODO
  val hadoopConfDir: String = System.getenv("HADOOP_PREFIX") + "/etc/hadoop/"

}
