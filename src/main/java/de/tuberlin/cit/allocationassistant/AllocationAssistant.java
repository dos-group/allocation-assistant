package de.tuberlin.cit.allocationassistant;

import java.io.IOException;

public class AllocationAssistant {

	public static void main(String[] args) throws IOException {
		// parse arguments: jarFile, jarArguments, initial resource allocation, user constraints (min/max resources, runtime-target)
		ConfigUtil conf = new ConfigUtil(args);
		Freamon freamon = new Freamon(conf.akka());

		// TODO: compute resources:

		// first get previous runtimes from Freamon,
		Object prevRuntimes = freamon.findSimilarApps(conf.args());

		// then (if multiple previous runs are available) build model (Ilya's code using JBLAS),
		// then use model to find scale-out (user target if available and between min-max resource constraints)
		Object resourceAlloc = null;

		// execute Flink job via Flink's YARN client
		new FlinkRunner(conf, freamon).runFlink(resourceAlloc);
	}

}
