package de.tuberlin.cit.allocationassistant

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api.{FindPreviousStages, PreviousRuns, StageDuration}
import org.apache.spark.SparkContext
import org.apache.spark.scheduler.{SparkListener, SparkListenerStageCompleted, SparkListenerStageSubmitted}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ScaleoutPredictor(
                         sparkContext: SparkContext,
                         appSignature: String,
                         inputSize: Double,
                         clusterConfig: Config
                       ) extends SparkListener {

  private val akkaConfig = clusterConfig.getConfig("ScaleoutPredictor.akka")

  private val scaleoutConstraint = (
    clusterConfig.getInt("ScaleoutPredictor.cluster.minExecutors"),
    clusterConfig.getInt("ScaleoutPredictor.cluster.maxExecutors"))

  private val freamon = new Freamon(akkaConfig)

  private var currentStageNr = 1
  private var stageStartTime: Long = 0
  private var currentScaleout: Int = 0

  private var futurePrevStages: Future[PreviousRuns] = _

  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    // TODO make this work for multiple stages running in parallel
    currentStageNr += 1
    stageStartTime = System.currentTimeMillis()
  }

  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    val stageStopTime = System.currentTimeMillis()

    val newScaleout = predictNewScaleout(currentStageNr + 1)
    val ok = sparkContext.requestTotalExecutors(newScaleout, 0, null)

    recordStageTimes(stageStartTime, stageStopTime, currentScaleout)
    currentScaleout = newScaleout

    val request = FindPreviousStages(appSignature, currentStageNr + 1)
    implicit val timeout = Timeout(999 seconds)
    futurePrevStages = (freamon.freamonMaster ? request).asInstanceOf
  }

  def predictNewScaleout(stage: Int): Int = {
    val prevStages = Await.result(futurePrevStages, 5 seconds)
    val (scaleOuts, runtimes) = AllocationAssistant.filterPreviousRuns(prevStages, inputSize)
    val targetRuntime = ???
    val newScaleout = AllocationAssistant.computeScaleOut(scaleOuts, runtimes, scaleoutConstraint, targetRuntime)
    newScaleout
  }

  def recordStageTimes(stageStart: Long, stageStop: Long, numExecutors: Int): Unit = {
    freamon.freamonMaster ! StageDuration(appSignature, currentStageNr, inputSize, numExecutors, stageStart, stageStop)
  }

}
