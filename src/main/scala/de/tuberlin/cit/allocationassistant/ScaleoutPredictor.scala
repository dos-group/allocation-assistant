package de.tuberlin.cit.allocationassistant

import java.lang.Double

import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api.{FindPreviousRunsOfStage, PreviousRuns, StageDuration}
import org.apache.spark.SparkContext

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ScaleoutPredictor(
                         sparkContext: SparkContext,
                         appSignature: String,
                         numStages: Int,
                         inputSize: Double,
                         targetRuntime: Int,
                         clusterConfig: Config
                       ) extends SparkListener {


  private val minExecutors = clusterConfig.getInt("allocation-assistant.minExecutors")
  private val maxExecutors = clusterConfig.getInt("allocation-assistant.maxExecutors")

  private val freamon = new Freamon(clusterConfig)

  private var jobStart: Long = 0
  private var currentStageNr = 1
  private var stageStartTime: Long = 0
  private var currentScaleout: Int = 0

  private var futureStageRuntimes: Map[Int, Future[PreviousRuns]] = _

  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    // we assume that at most one stage runs at any given time
    currentStageNr += 1
    stageStartTime = System.currentTimeMillis()
    if (jobStart == 0)
      jobStart = stageStartTime
  }

  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    val stageStopTime = System.currentTimeMillis()
    val stageDuration = stageStopTime - stageStartTime

    recordStageTimes(stageStartTime, stageStopTime, currentScaleout)

    val nextStages = currentStageNr + 1 until numStages
    val stageRuntimes = futureStageRuntimes.mapValues(getStageRuntimes)

    // check if the job will finish in time, rescale if it won't
    val runtimeLeft = jobStart + targetRuntime - System.currentTimeMillis()
    val totalDurationNeededForCurrentScaleout = nextStages
      .map(stage => predictStageDurationForScaleout(currentScaleout, stageRuntimes(stage)))
      .sum
    if (totalDurationNeededForCurrentScaleout > runtimeLeft * 1.05) {
      // exceeding target runtime by over 5%, rescaling

      val newScaleout = (minExecutors to maxExecutors)
        .find { scaleout =>
          val totalDurationNeeded = nextStages
            .map(stage => predictStageDurationForScaleout(scaleout, stageRuntimes(stage)))
            .sum
          totalDurationNeeded < runtimeLeft * .90 // 10% slack
        }
        .getOrElse(maxExecutors)

      val ok = sparkContext.requestTotalExecutors(newScaleout, 0, null)
      currentScaleout = newScaleout
    }

    implicit val timeout = Timeout(999 seconds)
    futureStageRuntimes = nextStages.map { n =>
      (n, freamon.freamonMaster ? FindPreviousRunsOfStage(appSignature, n))
    }.toMap.asInstanceOf
  }

  private def getStageRuntimes(response: Future[PreviousRuns]): Array[java.lang.Double] = {
    val runsOfSameStage = Await.result(response, 0 seconds)
    runsOfSameStage.datasetSizes.zip(runsOfSameStage.runtimes)
      .filter { case (size, runtime) =>
        .9 * inputSize < size && size < 1.1 * inputSize
      }.map(_._2)
  }

  def predictStageDurationForScaleout(scaleout: Int, stageRuntimes: Array[java.lang.Double]): Int = {
    // TODO queries the model in a different way than the allocation assistant
    ???
  }

  def recordStageTimes(stageStart: Long, stageStop: Long, numExecutors: Int): Unit = {
    freamon.freamonMaster ! StageDuration(appSignature, currentStageNr, inputSize, numExecutors, stageStart, stageStop)
  }

}
