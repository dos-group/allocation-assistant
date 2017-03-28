package de.tuberlin.cit.prediction

import breeze.linalg.{DenseVector, unique}

class InterpolationSplits(x: DenseVector[Double], y: DenseVector[Double])
  extends Iterator[(DenseVector[Double], DenseVector[Double], DenseVector[Double], DenseVector[Double])] {

  private val xUnique = unique(x)
  private val xInterpolation = xUnique(1 to -2)

  private val n = xInterpolation.length
  private var i = 0

  override def hasNext: Boolean = i < n

  override def next(): (DenseVector[Double], DenseVector[Double], DenseVector[Double], DenseVector[Double]) = {
    val mask = x :== xInterpolation(i)

    val xTrain = x(!mask)
    val yTrain = y(!mask)

    val xTest = x(mask)
    val yTest = y(mask)

    i = i + 1

    (xTrain.toDenseVector, yTrain.toDenseVector, xTest.toDenseVector, yTest.toDenseVector)
  }
}
