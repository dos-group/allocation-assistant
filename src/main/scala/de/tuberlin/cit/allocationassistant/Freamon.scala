package de.tuberlin.cit.allocationassistant

import akka.actor.{ActorSystem, Address}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api._

import scala.concurrent.Await
import scala.concurrent.duration._

/** Utilities for communicating with Freamon */
class Freamon(config: Config) {
  val freamonMaster = {
    val actorSystemName = config.getString("allocation-assistant.actors.system")
    val actorSystem = ActorSystem(actorSystemName, config)
    val freamonSystemPath = new Address("akka.tcp",
      config.getString("freamon.actors.systems.master.name"),
      config.getString("freamon.hosts.master.hostname"),
      config.getInt("freamon.hosts.master.port"))
    val freamonActorPath = freamonSystemPath.toString + "/user/" + config.getString("freamon.actors.systems.master.actor")

    actorSystem.actorSelection(freamonActorPath)
  }

  def getPreviousRuns(jarWithArgs: String): PreviousRuns = {
    implicit val timeout = Timeout(5 seconds)
    val future = freamonMaster ? FindPreviousRuns(jarWithArgs)
    try {
      Await.result(future, timeout.duration).asInstanceOf[PreviousRuns]
    } catch {
      case e: java.util.concurrent.TimeoutException =>
        println(s"Freamon did not respond with PreviousRuns after $timeout")
        throw e
    }
  }
}
