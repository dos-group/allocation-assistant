package de.tuberlin.cit.allocationassistant

import akka.actor.{Actor, ActorSystem, Address, Props}
import akka.event.Logging
import com.typesafe.config.Config

/** Utilities for communicating with Freamon */
class Freamon(config: Config) {
  private val actorSystemName = config.getString("allocation-assistant.systems.master.name")
  private val actorSystem = ActorSystem(actorSystemName, config)
  private val actorName = config.getString("allocation-assistant.systems.master.actor")
  private val freamonTunnel = actorSystem.actorOf(Props[FreamonTunnel], name = actorName)

  def findSimilarApps(params: Object): Object = {
    // TODO retrieve similar apps for previous runtimes
    // freamonTunnel ! FindSimilarApps(params)
    null
  }

  // TODO start/stop messages can be case classes again, now that we depend on Freamon (YarnWorkloadRunner could not)

  def sendStart(applicationId: String) {
    freamonTunnel ! Array("jobStarted", applicationId, System.currentTimeMillis())
  }

  def sendStop(applicationId: String) {
    freamonTunnel ! Array("jobStopped", applicationId, System.currentTimeMillis())
  }
}

/** Actor that forwards all messages to the Freamon master */
class FreamonTunnel extends Actor {
  val log = Logging(context.system, this)
  val config = context.system.settings.config

  val masterSystemPath = new Address("akka.tcp",
    config.getString("allocation-assistant.freamon.actors.systems.master.name"),
    config.getString("allocation-assistant.freamon-master.hostname"),
    config.getInt("allocation-assistant.freamon-master.port"))

  val masterActorPath = masterSystemPath.toString + "/user/" + config.getString("allocation-assistant.freamon.actors.systems.master.actor")

  val freamonMaster = context.actorSelection(masterActorPath)

  override def preStart() {
    log.info("Freamon connection started")
  }

  def receive = {
    case msg =>
      freamonMaster.forward(msg)
      log.debug(s"sent $msg")

    // TODO handle responses to our requests (e.g. similar apps for previous runtimes)
  }
}
