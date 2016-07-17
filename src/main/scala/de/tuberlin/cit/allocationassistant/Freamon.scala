package de.tuberlin.cit.allocationassistant

import java.lang.Double

import akka.actor.{Actor, ActorSystem, Address, Props}
import akka.event.Logging
import akka.pattern.ask
import scala.concurrent.duration._
import com.typesafe.config.Config

import scala.concurrent.Await

case class PreviousRuns(scaleOuts: Array[Integer], runtimes: Array[Double])

/** Utilities for communicating with Freamon */
class Freamon(config: Config) {
  private val actorSystemName = config.getString("allocation-assistant.actors.system")
  private val actorSystem = ActorSystem(actorSystemName, config)
  private val actorName = config.getString("allocation-assistant.actors.freamon")
  private val freamonTunnel = actorSystem.actorOf(Props[FreamonTunnel], name = actorName)

  def getPreviousRuns(jarWithArgs: String): PreviousRuns = {
    val future = freamonTunnel ? Array("findPreviousRuns", jarWithArgs)
    val response = Await.result(future, 5 seconds).asInstanceOf[Array[Any]]
    PreviousRuns(response(1).asInstanceOf, response(2).asInstanceOf)
  }

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
    config.getString("freamon.actors.systems.master.name"),
    config.getString("allocation-assistant.freamon-master.hostname"),
    config.getInt("allocation-assistant.freamon-master.port"))

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
