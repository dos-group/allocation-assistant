package de.tuberlin.cit.allocationassistant

import akka.actor.{Actor, ActorSystem, Address, Props}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api._

import scala.concurrent.Await
import scala.concurrent.duration._

/** Utilities for communicating with Freamon */
class Freamon(config: Config) {
  private val actorSystemName = config.getString("allocation-assistant.actors.system")
  private val actorSystem = ActorSystem(actorSystemName, config)
  private val actorName = config.getString("allocation-assistant.actors.freamon")
  private val freamonTunnel = actorSystem.actorOf(Props[FreamonTunnel], name = actorName)

  def getPreviousRuns(jarWithArgs: String): PreviousRuns = {
    implicit val timeout = Timeout(5 seconds)
    val future = freamonTunnel ? FindPreviousRuns(jarWithArgs)
    Await.result(future, timeout.duration).asInstanceOf[PreviousRuns]
  }

  def sendStart(applicationId: String, signature: String, cores: Int, mem: Int) {
    freamonTunnel ! ApplicationStart(applicationId, System.currentTimeMillis(), signature, cores, mem)
  }

  def sendStop(applicationId: String) {
    freamonTunnel ! ApplicationStop(applicationId, System.currentTimeMillis())
  }
}

/** Actor that forwards all messages to the Freamon master */
class FreamonTunnel extends Actor {
  val log = Logging(context.system, this)
  val config = context.system.settings.config

  val masterSystemPath = new Address("akka.tcp",
    config.getString("freamon.actors.systems.master.name"),
    config.getString("freamon.hosts.master.hostname"),
    config.getInt("freamon.hosts.master.port"))

  val masterActorPath = masterSystemPath.toString + "/user/" + config.getString("freamon.actors.systems.master.actor")

  val freamonMaster = context.actorSelection(masterActorPath)

  override def preStart() {
    log.info("Freamon connection started")
  }

  def receive = {
    case msg =>
      freamonMaster.forward(msg)
      log.debug(s"sent $msg")
  }
}
