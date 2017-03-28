package de.tuberlin.cit.adjustments

import java.util.concurrent.ThreadLocalRandom

import breeze.numerics.pow
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.mllib.optimization.SquaredL2Updater
import org.apache.spark.mllib.regression.{LabeledPoint, LinearRegressionWithSGD}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SparkTest {

  def main(args: Array[String]): Unit = {

    val conf = new Args(args)

    val appSignature = "Spark Test"

    val sparkConf = new SparkConf()
      .setAppName(appSignature)
      .setMaster("local[4]")
      .set("spark.shuffle.service.enabled", "true")
      .set("spark.dynamicAllocation.enabled", "true")
//      .set("spark.extraListeners", "de.tuberlin.cit.spark.StageScaleOutPredictor") // this makes sure the 'app start' event is called

    val sparkContext = new SparkContext(sparkConf)
    val listener = new StageScaleOutPredictor(
      sparkContext,
      appSignature,
      conf.minContainers(),
      conf.maxContainers(),
      conf.maxRuntime().toInt,
      conf.adaptive())
    sparkContext.addSparkListener(listener)

    val trainingSet = getTrainingSet(sparkContext, 2000000, 20).cache()

    val numIterations = 100
    val stepSize = 1.0
    val regParam = 0.01

    val algorithm = new LinearRegressionWithSGD()
    algorithm.optimizer
      .setNumIterations(numIterations)
      .setStepSize(stepSize)
      .setUpdater(new SquaredL2Updater())
      .setRegParam(regParam)

    val model = algorithm.run(trainingSet)
    println(model.weights)

    sparkContext.stop()
  }

  def getTrainingSet(sc: SparkContext, m: Int, n: Int): RDD[LabeledPoint] = {
    sc
      .range(1, m)
      .map(_ => {
        val x = ThreadLocalRandom.current().nextDouble()
        val noise = ThreadLocalRandom.current().nextGaussian()

        // generate the function value with added gaussian noise
        val label = function(x) + noise

        // generate a vandermatrix from x
        val vector = polyvander(x, n - 1)

        LabeledPoint(label, new DenseVector(vector))
      })
  }

  def polyvander(x: Double, order: Int): Array[Double] = {
    (0 to order).map(pow(x, _)).toArray
  }

  def function(x: Double): Double = {
    2 * x + 10
  }

}
