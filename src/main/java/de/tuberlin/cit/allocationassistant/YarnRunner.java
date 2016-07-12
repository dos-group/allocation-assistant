package de.tuberlin.cit.allocationassistant;

import org.apache.log4j.Logger;

import java.io.*;

public class YarnRunner {

    public static void main(String[] args) {

		// TODO: parse arguments: jarFile, jarArguments, initial resource allocation, user constraints (min/max resources, runtime-target)

		private String[] environment = new String[]{"HADOOP_CONF_DIR=" /* TODO: hadoop dir */};

		String flinkHome = Config.getInstance().getFlinkHome();
		String jobCommand = flinkHome + "/bin/flink run " + concatRunnerArguments(runnerArguments) + jarFile + concatJarArguments(jarArguments);

		// TODO: compute resources:
		// first get previous runtimes from Freamon,
		// then (if multiple previous runs are available) build model (Ilya's code using JBLAS),
		// then use model to find scale-out (user target if available and between min-max resource constraints)

		// TODO: notify Freamon - job started

		// TODO: execute Flink job via Flink's YARN client
		Process process = Runtime.getRuntime().exec(jobCommand, environment);

		// TODO: notify Freamon - job stopped

    }

}
