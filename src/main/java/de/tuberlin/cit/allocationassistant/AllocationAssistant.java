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

		int scaleOut;
		if (scaleOuts.length < 2) {
			// throws if this arg was not supplied
			scaleOut = ((Integer) options.args().initialContainers().apply());
			System.out.println("Using initial scaleOut of " + scaleOut + " from -i argument");
		} else {
			// build and use model to find scale-out (user target if available and between min-max resource constraints)
			scaleOut = computeScaleOut(scaleOuts, runtimes, options);
			System.out.println("Using computed scaleOut of " + scaleOut);
		}

		new FlinkRunner(options, freamon).runFlink(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}

	private static int computeScaleOut(Integer[] scaleOuts, Double[] runtimes, Options options) {
		return 0; // TODO stub
	}

}
