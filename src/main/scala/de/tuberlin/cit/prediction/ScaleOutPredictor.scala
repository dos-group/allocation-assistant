package de.tuberlin.cit.prediction

import breeze.linalg.{DenseVector, convert}

/**
  * Computes the scale-out according to runtime targets as presented in 2016 Thamsen et al.
  *
  * Example usage:
  * {{{
  * val predictor = new ScaleOutPredictor()
  * val (scaleOut, predictedRuntime) = predictor.computeScaleOut(scaleOuts, runtimes, (minScaleOut, maxScaleOut), maxRuntime)
  * }}}
  */
class ScaleOutPredictor {

  def computeScaleOut(scaleOuts: Array[Int], runtimes: Array[Double],
                      scaleOutConstraint: (Int, Int), runtime: Double): Option[(Int, Double)] = {

    val x = convert(new DenseVector(scaleOuts), Double)
    val y = new DenseVector(runtimes)

    // and ernest (for extrapolation)
    val ernest: UnivariatePredictor = new Ernest()
    ernest.fit(x, y)

    // calculate the range over which the runtimes must be predicted
    val (minScaleOut, maxScaleOut) = scaleOutConstraint
    val xPredict = DenseVector.range(minScaleOut, maxScaleOut+1)

    // subdivide the scaleout range into interpolation and extrapolation
    // FIXME wrong interpolation mask
    val interpolationMask = (xPredict :>= minScaleOut) :& (xPredict :<= maxScaleOut)
    val xPredictInterpolation = xPredict(interpolationMask).toDenseVector
    val xPredictExtrapolation = xPredict(!interpolationMask).toDenseVector

    // predict with respective model
    val yPredict = DenseVector.zeros[Double](xPredict.length)

    if (x.length <= 5) {
      // if too few data use ernest model
      yPredict(interpolationMask) := ernest.predict(convert(xPredictInterpolation, Double))
    } else {
      // fit data using bell (for interpolation)
      val bell: UnivariatePredictor = new Bell()
      bell.fit(x, y)
      yPredict(interpolationMask) := bell.predict(convert(xPredictInterpolation, Double))
    }

    yPredict(!interpolationMask) := ernest.predict(convert(xPredictExtrapolation, Double))

    // get the prediction over the constrained scaleout range
    val targetMask = yPredict :< runtime
    val xTarget = xPredict(targetMask)
    val yTarget = yPredict(targetMask)

    // select smallest scaleout satisfying the runtime constraint (greedy), if possible
    if (xTarget.length == 0) {
      return None
    }

    val predictedScaleOut = xTarget(0)
    val predictedRuntime = yTarget(0)

    Some((predictedScaleOut, predictedRuntime))
  }

}
