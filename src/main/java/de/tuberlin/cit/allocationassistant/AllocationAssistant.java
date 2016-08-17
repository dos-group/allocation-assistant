package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.freamon.api.PreviousRuns;

import java.io.IOException;

public class AllocationAssistant {

	public static void main(String[] args) throws IOException {
		Options options = new Options(args);
		Freamon freamon = new Freamon(options.akka());

		PreviousRuns previousRuns = freamon.getPreviousRuns(options.jarSignature());
		Integer[] scaleOuts = previousRuns.scaleOuts();
		Double[] runtimes = previousRuns.runtimes();

		System.out.println("found " + scaleOuts.length + " runs with signature " + options.jarSignature());

		int scaleOut;
		if (scaleOuts.length < 2) {
			// throws if this arg was not supplied
			scaleOut = ((Integer) options.args().initialContainers().apply());
		} else {
			// build and use model to find scale-out (user target if available and between min-max resource constraints)
			ScaleOutPredictor predictor = new ScaleOutPredictor();
			scaleOut = predictor.computeScaleOut(scaleOuts, runtimes, (Double) options.args().maxRuntime().apply());
		}
		scaleOut = options.applyScaleOutLimits(scaleOut);
		System.out.println("Using scaleOut of " + scaleOut);

		new FlinkRunner(options, freamon).run(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}
}
