package de.tuberlin.cit.allocationassistant.prediction

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.numerics.log
import breeze.optimize.linear.NNLS

class Ernest extends UnivariatePredictor {
  var coefficients: DenseVector[Double] = _

  override def fit(x: DenseVector[Double], y: DenseVector[Double]): UnivariatePredictor = {
    if (x.length != y.length) {
      throw new IllegalArgumentException("Vectors x and y must have the same length!")
    }

    val X = featureMap(x)
    val nnls = new NNLS()
    coefficients = nnls.minimize(X.t * X, X.t * y)

    this
  }

  override def predict(x: DenseVector[Double]): DenseVector[Double] = {
    if (coefficients == null) {
      throw new IllegalStateException("Model has not been fitted, yet!")
    }

    featureMap(x) * coefficients
  }

  private def featureMap(x: DenseVector[Double]): DenseMatrix[Double] = {
    DenseMatrix.vertcat(
      DenseVector.ones[Double](x.length).asDenseMatrix,
      (DenseVector.ones[Double](x.length) :/ x).asDenseMatrix,
      log(x).asDenseMatrix,
      x.asDenseMatrix
    ).t
  }

}
