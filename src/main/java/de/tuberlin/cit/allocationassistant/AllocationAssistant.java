package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.freamon.api.PreviousRuns;

import java.io.IOException;

public class AllocationAssistant {

	public static void main(String[] args) throws IOException {
		Options options = new Options(args);
		Freamon freamon = new Freamon(options.akka());

		PreviousRuns previousRuns = freamon.getPreviousRuns(options.jarWithArgs());
		Integer[] scaleOuts = previousRuns.scaleOuts();
		Double[] runtimes = previousRuns.runtimes();

		System.out.println("found " + scaleOuts.length + " runs with signature " + options.jarWithArgs());

		// then (if multiple previous runs are available) build model (Ilya's code using JBLAS),
		// then use model to find scale-out (user target if available and between min-max resource constraints)
		int scaleOut = computeScaleOut(scaleOuts, runtimes, options.maxRuntime());

		new FlinkRunner(options, freamon).runFlink(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}

	private static int computeScaleOut(Integer[] scaleOuts, Double[] runtimes, int maxRuntime) {
		return 0; // TODO stub
	}

}
