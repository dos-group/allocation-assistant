package de.tuberlin.cit.allocationassistant.prediction

import breeze.linalg.{DenseMatrix, DenseVector}

object CrossValidation {

  def crossValidationScore(models: Seq[UnivariatePredictor],
                           splits: Iterator[(DenseVector[Double], DenseVector[Double], DenseVector[Double], DenseVector[Double])],
                           lossFunction: (DenseVector[Double], DenseVector[Double]) => Double): DenseMatrix[Double] = {

    val scores = splits.flatMap({ case (xTrain, yTrain, xTest, yTest) =>
      models.map({ model =>
        val yPredict = model.fit(xTrain, yTrain).predict(xTest)
        lossFunction(yPredict, yTest)
      })
    }).toArray

    new DenseMatrix(models.length, scores.length / models.length, scores)

  }

}
