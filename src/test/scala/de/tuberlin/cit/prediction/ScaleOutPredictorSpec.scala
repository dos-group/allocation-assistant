package de.tuberlin.cit.prediction

import org.scalatest.FlatSpec

import scala.io.Source
import scala.math.abs

class ScaleOutPredictorSpec extends FlatSpec {

  "ScaleOutPredictor" should "predict the correct scale-out and runtime" in {
    val resourceUrl = getClass.getResource("/tpchq10.flink.csv")
    val bufferedSource = Source.fromURL(resourceUrl)
    val (scaleOuts, runtimes): (Array[Int], Array[Double]) = bufferedSource.getLines.drop(1).map(line => {
      val cols = line.split(",").map(_.trim)
      val (scaleOut, runtime) = (cols(0), cols(1))
      (scaleOut.toInt, runtime.toDouble)
    }).toArray.unzip

    val predictor = new ScaleOutPredictor
    val result = predictor.computeScaleOut(scaleOuts, runtimes, (4, 60), 8 * 60 * 1000)

    assert(result.isDefined)

    val (scaleOut, runtime) = result.get

    assert(scaleOut == 33)
    assert(abs(runtime - 467973) < 1)
  }

}
