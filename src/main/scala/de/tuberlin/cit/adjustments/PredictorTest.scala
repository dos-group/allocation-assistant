package de.tuberlin.cit.adjustments

import breeze.linalg.{BitVector, DenseVector, convert, max, min, sum, unique}
import de.tuberlin.cit.prediction.{Bell, Ernest, UnivariatePredictor}

object PredictorTest {
  val targetRuntimeMs = 500000
  val (minExecutors, maxExecutors) = (4, 40)

  def main(args: Array[String]): Unit = {
    val (scaleOuts, runtimes) = (Array(40, 22, 13), Array(309145, 300002, 312929))

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
        val predictedRuntimes = computePredictions(scaleOuts, runtimes)

        println(scaleOuts.mkString(","))
        println(runtimes.mkString(","))

        println(predictedScaleOuts.mkString(","))
        println(predictedRuntimes.mkString(","))

        val candidateScaleOuts = (predictedScaleOuts zip predictedRuntimes)
          .filter(_._2 < targetRuntimeMs)
          .map(_._1)

        println(candidateScaleOuts.mkString(","))

        if (candidateScaleOuts.isEmpty) {
          maxExecutors
        } else {
          println(candidateScaleOuts.min)
          candidateScaleOuts.min
        }

    }
  }

  def computePredictions(scaleOuts: Array[Int], runtimes: Array[Int]): Array[Int] = {
    val x = convert(DenseVector(scaleOuts), Double)
    val y = convert(DenseVector(runtimes), Double)

    // calculate the range over which the runtimes must be predicted
    val xPredict = DenseVector.range(minExecutors, maxExecutors + 1)

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
}
