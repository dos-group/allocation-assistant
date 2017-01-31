package de.tuberlin.cit.allocationassistant

import de.tuberlin.cit.allocationassistant.prediction.ScaleOutPredictor
import de.tuberlin.cit.freamon.api.PreviousRuns

object AllocationAssistant {

  def computeScaleOut(scaleOuts: Array[Int], runtimes: Array[Double], constraint: (Int, Int), target: Double): Int = {


    // argsort scale-outs
    val idxs = scaleOuts.zipWithIndex.sortBy(_._1).map(_._2)

    // sort scale-outs and runtimes array
    val scaleOutsSorted = idxs.map(scaleOuts(_))
    val runtimesSorted = idxs.map(runtimes(_))

    val (minScaleOut, maxScaleOut) = constraint

    // for less than 3 scale-outs use the following heuristic

    if (scaleOutsSorted.length == 0) {
      return maxScaleOut
    }

    if (scaleOutsSorted.length == 1) {
      return (minScaleOut + scaleOutsSorted(0)) / 2
    }

    if (scaleOutsSorted.length == 2) {
      if (runtimesSorted(0) < target) {
        return (minScaleOut + scaleOutsSorted(0)) / 2
      } else {
        return (scaleOutsSorted(0) + scaleOutsSorted(1)) / 2
      }
    }

    // if there are at least 3 scale-outs use our model
    val predictor = new ScaleOutPredictor
    val result = predictor.computeScaleOut(scaleOutsSorted, runtimesSorted, constraint, target)

    if (result.isDefined) {
      val (scaleOutPrediction, runtimePrediction) = result.get
      return scaleOutPrediction
    }

    // if target cannot be fulfilled use the max amount of nodes
    maxScaleOut
  }

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

    val (scaleOuts, runtimes) = filterPreviousRuns(freamon.getPreviousRuns(options.jarSignature), options.inputSize)
    val numPrevRuns = scaleOuts.length
    println(s"Found $numPrevRuns runs with signature ${options.jarSignature} and dataset size within 10% of ${options.inputSize}")

    val maxRuntime: Double = options.args.maxRuntime()
    val scaleOutConstraint = (options.args.minContainers(), options.args.maxContainers())
    val scaleOut = computeScaleOut(scaleOuts, runtimes, scaleOutConstraint, maxRuntime)
    println(s"Using scale-out of $scaleOut")

    if (options.args.dryRun.isSupplied) {
      System.exit(0)
    }

    runner.run(scaleOut)

    // terminate the application, or else akka will keep it alive
    System.exit(0)
  }

}
