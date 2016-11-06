package de.tuberlin.cit.allocationassistant.prediction

import breeze.linalg._
import breeze.numerics._

class KernelRegression(degree: Int = 1, bandwidth: Double = 1, tolerance: Double = scala.Double.MinPositiveValue)
  extends UnivariatePredictor {

  var x: DenseVector[Double] = _
  var y: DenseVector[Double] = _

  override def fit(x: DenseVector[Double], y: DenseVector[Double]): UnivariatePredictor = {
    if (x.length != y.length) {
      throw new IllegalArgumentException("Vectors x and y must have the same length!")
    }

    this.x = x
    this.y = y

    this
  }

  override def predict(xPredict: DenseVector[Double]): DenseVector[Double] = {
    if (x == null || y == null) {
      throw new IllegalStateException("Model has not been fitted, yet!")
    }

    val weightMatrix: DenseMatrix[Double] = DenseMatrix.zeros[Double](xPredict.length, x.length)

    for (i <- 0 until xPredict.length) {
      for (j <- 0 until x.length) {
        val diff = xPredict(i) - x(j)
        weightMatrix(i, j) = exp(- diff * diff / (2 * bandwidth * bandwidth))
      }
    }

    val X = featureMap(x)
    val XPredict = featureMap(xPredict)

    predict(X, y, XPredict, weightMatrix)
  }

  private def featureMap(x: DenseVector[Double]): DenseMatrix[Double] = {
    val X: DenseMatrix[Double] = DenseMatrix.zeros[Double](x.length, degree + 1)
    for (i <- 0 to degree) {
      X(::, i) := pow(x, i)
    }

    X
  }

  private def predict(X: DenseMatrix[Double], y: DenseVector[Double],
                      XPredict: DenseMatrix[Double], W: DenseMatrix[Double]): DenseVector[Double] = {
    val yPredict = DenseVector.zeros[Double](XPredict.rows)
    for (i <- 0 until yPredict.length) {
      yPredict(i) = predictSingle(X, y, XPredict(i, ::).t, W(i, ::).t)
    }
    yPredict
  }

  private def predictSingle(X: DenseMatrix[Double], y: DenseVector[Double],
                            x: DenseVector[Double], w: DenseVector[Double]): Double = {

    val Xw: DenseMatrix[Double] = X(::, *) :* w
    val c: DenseVector[Double] = (Xw.t * X + tolerance * DenseMatrix.eye[Double](X.cols)) \ (Xw.t * y)

    x.t * c
  }

}
