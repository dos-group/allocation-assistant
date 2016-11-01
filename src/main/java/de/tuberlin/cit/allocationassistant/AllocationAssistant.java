package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.allocationassistant.prediction.ScaleOutPredictor;
import de.tuberlin.cit.freamon.api.PreviousRuns;
import scala.Tuple2;

public class AllocationAssistant {

	public static void main(String[] args) {
		Options options = new Options(args);
		Freamon freamon = new Freamon(options.akka());

		String engine = options.args().engine().apply();
		CommandRunner runner;
		if ("flink".equals(engine)) {
			runner = new FlinkRunner(options, freamon);
		} else if ("spark".equals(engine)) {
			runner = new SparkRunner(options, freamon);
		} else {
			System.err.println("Unknown engine " + engine + ", use \"flink\" or \"spark\"");
			System.exit(1);
			return;
		}

		PreviousRuns previousRuns = freamon.getPreviousRuns(options.jarSignature());
		PredictorInput runs = new DatasetSizeFilter(options.datasetSize()).filterPreviousRuns(previousRuns);
		int numPrevRuns = runs.scaleOuts.size();

        System.out.printf("found %d runs with signature %s and dataset size within 10%% of %s%n",
                numPrevRuns, options.jarSignature(), options.datasetSize());

		int scaleOut;
		if (numPrevRuns < 2) {
			if (options.args().initialContainers().isEmpty()) {
				System.err.println("Not enough (" + numPrevRuns + ") previous runs available, please specify the initial number of containers (-i)");
				System.exit(1);
			}
			scaleOut = (Integer) options.args().initialContainers().apply();
		} else {
			// build and use model to find scale-out (user target if available and between min-max resource constraints)
			Double maxRuntime = (Double) options.args().maxRuntime().apply();
			Tuple2<Object, Object> scaleOutConstraint = new Tuple2<>(1,100); // TODO
			ScaleOutPredictor predictor = new ScaleOutPredictor();
			scaleOut = (int) predictor.computeScaleOut(
					runs.unboxScaleOuts(),
					runs.unboxRuntimes(),
					scaleOutConstraint,
					maxRuntime)._1;
		}
		scaleOut = options.applyScaleOutLimits(scaleOut);
		System.out.println("Using scale-out of " + scaleOut);

		runner.run(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}
}
