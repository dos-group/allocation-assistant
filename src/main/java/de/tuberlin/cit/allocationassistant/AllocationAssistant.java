package de.tuberlin.cit.allocationassistant;

import java.io.IOException;

public class AllocationAssistant {

	public static void main(String[] args) throws IOException {
		Options options = new Options(args);
		Freamon freamon = new Freamon(options.akka());

		PreviousRuns previousRuns = freamon.getPreviousRuns(options.jarWithArgs());
		Integer[] scaleOuts = previousRuns.scaleOuts();
		Double[] runtimes = previousRuns.runtimes();

		// then (if multiple previous runs are available) build model (Ilya's code using JBLAS),
		// then use model to find scale-out (user target if available and between min-max resource constraints)
		int scaleOut = computeScaleOut(scaleOuts, runtimes, options.maxRuntime());

		new FlinkRunner(options, freamon).runFlink(scaleOut);
	}

	private static int computeScaleOut(Integer[] scaleOuts, Double[] runtimes, int maxRuntime) {
		return 0; // TODO stub
	}

}
