package de.tuberlin.cit.adjustments

import java.util.Date

import breeze.linalg._
import de.tuberlin.cit.prediction.{Bell, Ernest, UnivariatePredictor}
import org.apache.spark.SparkContext
import org.apache.spark.scheduler._
import scalikejdbc._

import scala.language.postfixOps

class StageScaleOutPredictor(
                              sparkContext: SparkContext,
                              appSignature: String,
                              dbPath: String,
                              minExecutors: Int,
                              maxExecutors: Int,
                              targetRuntimeMs: Int,
                              isAdaptive: Boolean
                            ) extends SparkListener {

  private var appEventId: Long = _
  private var appStartTime: Long = _
  private var jobStartTime: Long = _

  private var scaleOut: Int = _
  private var nextScaleOut: Int = _

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton(s"jdbc:h2:$dbPath", "sa", "")

  scaleOut = computeInitialScaleOut()
  nextScaleOut = scaleOut
  println(s"Using initial scale-out of $scaleOut.")

  sparkContext.requestTotalExecutors(scaleOut, 0, Map[String,Int]())

  def computeInitialScaleOut(): Int = {
    val (scaleOuts, runtimes) = getNonAdaptiveRuns(appSignature)

    val halfExecutors = (minExecutors + maxExecutors) / 2

    scaleOuts.length match {
      case 0 => maxExecutors
      case 1 => halfExecutors
      case 2 =>

        if (runtimes.sorted.last < targetRuntimeMs) {
          (minExecutors + halfExecutors) / 2
        } else {
          (halfExecutors + maxExecutors) / 2
        }

      case _ =>

        val predictedScaleOuts = (minExecutors to maxExecutors).toArray
        val predictedRuntimes = computePredictions(scaleOuts, runtimes, predictedScaleOuts)

        val candidateScaleOuts = (predictedScaleOuts zip predictedRuntimes)
          .filter(_._2 < targetRuntimeMs)
          .map(_._1)

        if (candidateScaleOuts.isEmpty) {
          maxExecutors
        } else {
          candidateScaleOuts.min
        }

    }


  }

  def getNonAdaptiveRuns(appSignature: String): (Array[Int], Array[Int]) = {
    val result = DB readOnly { implicit session =>
      sql"""
      SELECT STARTED_AT, SCALE_OUT, DURATION_MS
      FROM APP_EVENT JOIN JOB_EVENT ON APP_EVENT.ID = JOB_EVENT.APP_EVENT_ID
      WHERE APP_ID = $appSignature;
      """.map({ rs =>
        val startedAt = rs.timestamp("started_at")
        val scaleOut = rs.int("scale_out")
        val durationMs = rs.int("duration_ms")
        (startedAt, scaleOut, durationMs)
      }).list().apply()
    }

    val (scaleOuts, runtimes) = result
      .groupBy(_._1)
      .toArray
      .flatMap(t => {
        val jobStages = t._2
        val scaleOuts = jobStages.map(_._2)
        val scaleOut = scaleOuts.head
        val nonAdaptive = scaleOuts.forall(scaleOut == _)
        if (nonAdaptive) {
          val runtime = jobStages.map(_._3).sum
          List((scaleOut, runtime))
        } else {
          List()
        }
      })
      .unzip

    (scaleOuts, runtimes)
  }

  def computePredictions(scaleOuts: Array[Int], runtimes: Array[Int], predictedScaleOuts: Array[Int]): Array[Int] = {
    val x = convert(DenseVector(scaleOuts), Double)
    val y = convert(DenseVector(runtimes), Double)

    // calculate the range over which the runtimes must be predicted
    val xPredict = DenseVector(predictedScaleOuts)

    // subdivide the scaleout range into interpolation and extrapolation
    val interpolationMask: BitVector = (xPredict :>= min(scaleOuts)) :& (xPredict :<= max(scaleOuts))
    val xPredictInterpolation = xPredict(interpolationMask).toDenseVector
    val xPredictExtrapolation = xPredict(!interpolationMask).toDenseVector

    // predict with respective model
    val yPredict = DenseVector.zeros[Double](xPredict.length)

    // fit ernest
    val ernest: UnivariatePredictor = new Ernest()
    ernest.fit(x, y)

    val uniqueScaleOuts = unique(x).length
    if (uniqueScaleOuts <= 2) {
      // for very few data, just take the mean
      yPredict := sum(y) / y.length
    } else if (uniqueScaleOuts <= 5) {
      // if too few data use ernest model
      yPredict := ernest.predict(convert(xPredict, Double))
    } else {
      // fit data using bell (for interpolation)
      val bell: UnivariatePredictor = new Bell()
      bell.fit(x, y)
      yPredict(interpolationMask) := bell.predict(convert(xPredictInterpolation, Double))
      yPredict(!interpolationMask) := ernest.predict(convert(xPredictExtrapolation, Double))
    }

    yPredict.map(_.toInt).toArray
  }

  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {

    DB localTx { implicit session =>
      sql"""
      UPDATE app_event
      SET finished_at = CURRENT_TIMESTAMP()
      WHERE id = ${appEventId};
      """.update().apply()
    }

  }

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    println(s"Job ${jobStart.jobId} started.")
    jobStartTime = System.currentTimeMillis()

    // https://stackoverflow.com/questions/29169981/why-is-sparklistenerapplicationstart-never-fired
    // onApplicationStart workaround
    if (appStartTime == 0) {
      appStartTime = jobStartTime

      DB localTx { implicit session =>
        appEventId =
          sql"""
        INSERT INTO app_event (
          app_id,
          started_at
        )
        VALUES (
          ${appSignature},
          ${new Date(appStartTime)}
        );
        """.updateAndReturnGeneratedKey("id").apply()
      }
    }

  }

  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    val jobEndTime = System.currentTimeMillis()
    val jobDuration = jobEndTime - jobStartTime
    println(s"Job ${jobEnd.jobId} finished in $jobDuration ms with $scaleOut nodes.")

    DB localTx { implicit session =>
      sql"""
      INSERT INTO job_event (
        app_event_id,
        job_id,
        started_at,
        finished_at,
        duration_ms,
        scale_out
      )
      VALUES (
        ${appEventId},
        ${jobEnd.jobId},
        ${new Date(jobStartTime)},
        ${new Date(jobEndTime)},
        ${jobDuration},
        ${scaleOut}
      );
      """.update().apply()
    }

    if (nextScaleOut != scaleOut) {
      scaleOut = nextScaleOut
    }

    if (isAdaptive) {
      val (scaleOuts, runtimes) = getNonAdaptiveRuns(appSignature)
      if (scaleOuts.length > 3) { // do not scale adaptively for the bootstrap runs
        updateScaleOut(jobEnd.jobId)
      }
    }

  }

  def updateScaleOut(jobId: Int): Unit = {
    val result = DB readOnly { implicit session =>
      sql"""
      SELECT JOB_ID, SCALE_OUT, DURATION_MS
      FROM APP_EVENT JOIN JOB_EVENT ON APP_EVENT.ID = JOB_EVENT.APP_EVENT_ID
      WHERE APP_ID = ${appSignature}
      ORDER BY JOB_ID;
      """.map({ rs =>
        val jobId = rs.int("job_id")
        val scaleOut = rs.int("scale_out")
        val durationMs = rs.int("duration_ms")
        (jobId, scaleOut, durationMs)
      }).list().apply()
    }
    val jobRuntimeData: Map[Int, List[(Int, Int, Int)]] = result.groupBy(_._1)

    // calculate the prediction for the remaining runtime depending on scale-out
    val predictedScaleOuts = (minExecutors to maxExecutors).toArray
    val remainingRuntimes: Array[DenseVector[Int]] = jobRuntimeData.keys
      .toArray
      .filter(_ > jobId)
      .sorted
      .map(jobId => {
        val (x, y) = jobRuntimeData(jobId).map(t => (t._2, t._3)).toArray.unzip
        val predictedRuntimes: Array[Int] = computePredictions(x, y, predictedScaleOuts)
        DenseVector(predictedRuntimes)
      })

    if (remainingRuntimes.length <= 1) {
      return
    }
    val nextJobId = jobRuntimeData.keys.toArray.sorted.head

    // predicted runtimes of the next job
    val nextJobRuntimes = remainingRuntimes.head
    // predicted runtimes sum of the jobs *after* the next job
    val futureJobsRuntimes = remainingRuntimes.drop(1).fold(DenseVector.zeros[Int](predictedScaleOuts.length))(_ + _)

    val currentRuntime = System.currentTimeMillis() - appStartTime
    println(s"Current runtime: $currentRuntime")
    val nextJobRuntime = nextJobRuntimes.valueAt(scaleOut - minExecutors)
    println(s"Next job runtime prediction: $nextJobRuntime")
    val remainingTargetRuntime = targetRuntimeMs - currentRuntime - nextJobRuntime
    println(s"Remaining runtime: $remainingTargetRuntime")

    // check if current scale-out can fulfill the target runtime constraint
    // TODO currently, the rescaling only happens if the remaining runtime prediction *exceeds* the constraint
    val relativeSlack = 1.05
    val absoluteSlack = 0
    if (futureJobsRuntimes(scaleOut - minExecutors) > remainingTargetRuntime * relativeSlack + absoluteSlack) {
      val nextScaleOutIndex = futureJobsRuntimes.findAll(_ < remainingTargetRuntime * .9)
        .sorted
        .headOption
        .getOrElse(argmin(futureJobsRuntimes))
      val nextScaleOut = predictedScaleOuts(nextScaleOutIndex)

      if (nextScaleOut != scaleOut) {
        println(s"Adjusting scale-out to $nextScaleOut after job $nextJobId.")
        sparkContext.requestTotalExecutors(nextScaleOut, 0, Map[String,Int]())
        this.nextScaleOut = nextScaleOut
      }
    }

  }
}
