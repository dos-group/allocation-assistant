package de.tuberlin.cit.allocationassistant

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

object ConfigHelper {
  // configuration from the file passed as argument, with defaults from application.conf
  def loadFromFile(path: String): Config = {
    ConfigFactory.parseFile(new File(path))
      .withFallback(ConfigFactory.load()).resolve()
  }

  // configuration prepared for use with akka
  def prepareForAkka(conf: Config): Config = {
    ConfigFactory.parseString(
      "akka.remote.netty.tcp.hostname=" + conf.getString("allocation-assistant.actors.hostname")
        + "\nakka.remote.netty.tcp.port=" + conf.getInt("allocation-assistant.actors.port")
    ).withFallback(conf)
  }
}
