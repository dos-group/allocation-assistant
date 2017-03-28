package de.tuberlin.cit.prediction

import org.scalatest.FlatSpec

class BellSpec extends FlatSpec {

  "Bell" should "throw an IllegalStateException when predict is called without previous call to fit" in {

    assertThrows[IllegalStateException] {
      val bell = new Bell()
      val x = (1 to 5).map(_.toDouble).toArray
      bell.predict(x)
    }

  }

  it should "throw an IllegalArgumentException when fit is called with vectors of different length" in {

    assertThrows[IllegalArgumentException] {
      val bell = new Bell()
      val x = (1 to 5).map(_.toDouble).toArray
      val y = (1 to 4).map(_.toDouble).toArray
      bell.fit(x, y)
    }

  }

}
