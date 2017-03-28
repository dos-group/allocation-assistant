package de.tuberlin.cit.prediction

import breeze.linalg.{*, DenseVector, argmin, linspace, sum}
import breeze.numerics.abs

class Bell extends UnivariatePredictor {
  var bestModel: UnivariatePredictor = _

  override def fit(x: DenseVector[Double], y: DenseVector[Double]): UnivariatePredictor = {
    if (x.length != y.length) {
      throw new IllegalArgumentException("Vectors x and y must have the same length!")
    }

    // create candidate models for interpolation
    val bandwidths = linspace(1, 100, 100).toArray
    val models = bandwidths.map(bandwidth => new KernelRegression(bandwidth = bandwidth, tolerance = 1e-12)) :+ new Ernest()

    // compute the cv score using interpolation splits
    val scores = CrossValidation.crossValidationScore(models, new InterpolationSplits(x, y),
      (yPredict, y) => {
        sum(abs(yPredict - y) :/ y) / y.length
      })

    // compute mean score for each model and select the best
    val meanScores = sum(scores(*, ::)) / scores.rows.toDouble
    val idx = argmin(meanScores)

    // train the selected model
    bestModel = models(idx)
    bestModel.fit(x, y)
  }

  override def predict(x: DenseVector[Double]): DenseVector[Double] = {
    if (bestModel == null) {
      throw new IllegalStateException("Model has not been fitted, yet!")
    }

    bestModel.predict(x)
  }
}
