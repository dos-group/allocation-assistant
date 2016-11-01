package de.tuberlin.cit.allocationassistant.prediction

import org.scalatest.FlatSpec

import scala.math._
import scala.util.Random

class KernelRegressionSpec extends FlatSpec {

  "KernelRegression" should "throw an IllegalStateException when predict is called without previous call to fit" in {

    assertThrows[IllegalStateException] {
      val kernelRegression = new KernelRegression()
      val x = (1 to 5).map(_.toDouble).toArray
      kernelRegression.predict(x)
    }

  }

  it should "throw an IllegalArgumentException when fit is called with vectors of different length" in {

    assertThrows[IllegalArgumentException] {
      val kernelRegression = new KernelRegression()
      val x = (1 to 5).map(_.toDouble).toArray
      val y = (1 to 4).map(_.toDouble).toArray
      kernelRegression.fit(x, y)
    }

  }

  it should "calculate the correct prediction" in {

    assert {
      val kernelRegression = new KernelRegression(bandwidth = 1.8)
      val x = Array[Double](1, 2, 3, 4, 5)
      val y = Array[Double](1, 4, 6, 4, 1)
      kernelRegression.fit(x, y)

      val xPredict = Array[Double](1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5)
      val yTrue = Array[Double](2.0008, 2.7264, 3.2511, 3.5680, 3.6740, 3.5680, 3.2511, 2.7264, 2.0008)
      val yPredict = kernelRegression.predict(xPredict)

      yTrue zip yPredict forall { case (a, b) => abs(a - b) < 10e-4 }
    }

  }
}
