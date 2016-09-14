package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.freamon.api.PreviousRuns;

public class AllocationAssistant {

	public static void main(String[] args) {
		Options options = new Options(args);
		Freamon freamon = new Freamon(options.akka());

		PreviousRuns previousRuns = freamon.getPreviousRuns(options.jarSignature());
		PredictorInput runs = new DatasetSizeFilter(options.datasetSize()).filterPreviousRuns(previousRuns);
		int numPrevRuns = runs.scaleOuts.size();

		System.out.println("found " + numPrevRuns + " runs with signature " + options.jarSignature());

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
			ScaleOutPredictor predictor = new ScaleOutPredictor();
			scaleOut = predictor.computeScaleOut(runs.unboxScaleOuts(), runs.unboxRuntimes(), maxRuntime);
		}
		scaleOut = options.applyScaleOutLimits(scaleOut);
		System.out.println("Using scale-out of " + scaleOut);

		new FlinkRunner(options, freamon).run(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}
}
