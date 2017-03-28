package de.tuberlin.cit.prediction

import org.scalatest.FlatSpec

import scala.math.{abs, log}

class ErnestSpec extends FlatSpec {

  "Ernest" should "throw an IllegalStateException when predict is called without previous call to fit" in {

    assertThrows[IllegalStateException] {
      val ernest = new Ernest()
      val x = (1 to 5).map(_.toDouble).toArray
      ernest.predict(x)
    }

  }

  it should "throw an IllegalArgumentException when fit is called with vectors of different length" in {

    assertThrows[IllegalArgumentException] {
      val ernest = new Ernest()
      val x = (1 to 5).map(_.toDouble).toArray
      val y = (1 to 4).map(_.toDouble).toArray
      ernest.fit(x, y)
    }

  }

  it should "calculate the correct prediction" in {

    assert {
      val ernest = new Ernest()
      val x = (1 to 10).map(_.toDouble).toArray
      val y = x.map(xi => 5 + 4 * log(xi) + 3 / xi + 2 * xi) // y follows the ernest model

      ernest.fit(x, y)
      val yPredict = ernest.predict(x)

      y zip yPredict forall { case (a, b) => abs(a - b) < 10e-8 }
    }

    assert {
      val ernest = new Ernest()
      val x = (1 to 10).map(_.toDouble).toArray
      val y = x.map(xi => 5 - 2 * xi) // check non-negativity constraint

      ernest.fit(x, y)
      val yPredict = ernest.predict(x)

      yPredict forall { abs(_) < 10e-8 }
    }

  }

}
