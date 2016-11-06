package de.tuberlin.cit.allocationassistant

import de.tuberlin.cit.allocationassistant.prediction.ScaleOutPredictor
import de.tuberlin.cit.freamon.api.PreviousRuns

object AllocationAssistant {

  def filterPreviousRuns(previousRuns: PreviousRuns, targetDatasetSize: Double): (Array[Int], Array[Double]) = {

    (previousRuns.scaleOuts zip previousRuns.runtimes zip previousRuns.datasetSizes)
      .filter({
        case ((_, _), datasetSize) => .9 * targetDatasetSize < datasetSize && datasetSize < 1.1 * targetDatasetSize
      })
      .map({
        case ((scaleOut, runtime), _) => (scaleOut.toInt, runtime.toDouble)
      })
      .unzip

  }

  def main(args: Array[String]) {
    val options: Options = new Options(args)
    val freamon: Freamon = new Freamon(options.akka)

    val engine: String = options.args.engine()
    var runner: CommandRunner = null
    engine match {
      case "flink" => runner = new FlinkRunner(options, freamon)
      case "spark" => runner = new SparkRunner(options, freamon)
      case _ =>
        Console.err.println(s"""Unknown engine "$engine", use "flink" or "spark"""")
        System.exit(1)
    }

    val (scaleOuts, runtimes) = filterPreviousRuns(freamon.getPreviousRuns(options.jarSignature), options.datasetSize)
    val numPrevRuns = scaleOuts.length
    println(s"Found $numPrevRuns runs with signature ${options.jarSignature} and dataset size within 10% of ${options.datasetSize}")

    var scaleOut = options.args.fallbackContainers()
    if (numPrevRuns > 2) {
      val maxRuntime: Double = options.args.maxRuntime()
      val scaleOutConstraint = (options.args.minContainers(), options.args.maxContainers())
      val predictor = new ScaleOutPredictor
      val result = predictor.computeScaleOut(scaleOuts, runtimes, scaleOutConstraint, maxRuntime)

      if (result.isDefined) {
        val (scaleOutPrediction, runtimePrediction) = result.get
        scaleOut = scaleOutPrediction
      }
    }
    println(s"Using scale-out of $scaleOut")

    runner.run(scaleOut)

    // terminate the application, or else akka will keep it alive
    System.exit(0)
  }

}
