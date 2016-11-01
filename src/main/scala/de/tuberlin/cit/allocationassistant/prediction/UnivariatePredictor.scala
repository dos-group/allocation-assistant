package de.tuberlin.cit.allocationassistant.prediction

import breeze.linalg.DenseVector

/**
  * This trait provides a basic interface for univariate predictors.
  * The following examples shows a general usage of such predictors:
  *
  * {{{
  * // create a concrete predictor
  * val predictor: UnivariatePredictor = new ConcretePredictor()
  *
  * // train the model using training data
  * val (xTrain, yTrain) = getTrainingData()
  * predictor.fit(xTrain, yTrain)
  *
  * // now the model can be used for prediction
  * val (xTest, yTest) = getTestData()
  * val yPredict = predictor.predict(xTest)
  * }}}
  */
trait UnivariatePredictor {

  /**
    * Fits the model to the training data.
    *
    * @param x a vector containing the one-dimensional (univariate) data samples.
    * @param y a vector containing the target values.
    * @return a reference to itself
    */
  def fit(x: DenseVector[Double], y: DenseVector[Double]): UnivariatePredictor


  /**
    * Fits the model to the training data.
    *
    * @param x an array containing the one-dimensional (univariate) data samples.
    * @param y an array containing the target values.
    * @return a reference to itself
    */
  def fit(x: Array[Double], y: Array[Double]): UnivariatePredictor = {
    fit(new DenseVector(x), new DenseVector(y))
  }

  /**
    * Predicts the values for the given set of values.
    *
    * @param x a vector containing the one-dimensional (univariate) data sample for which the target value is predicted
    * @return a column vector containing the predicted values.
    */
  def predict(x: DenseVector[Double]): DenseVector[Double]

  /**
    * Predicts the values for the given set of values.
    *
    * @param x an array containing the one-dimensional (univariate) data sample for which the target value is predicted
    * @return a column vector containing the predicted values.
    */
  def predict(x: Array[Double]): Array[Double] = {
    predict(new DenseVector(x)).toArray
  }
}
