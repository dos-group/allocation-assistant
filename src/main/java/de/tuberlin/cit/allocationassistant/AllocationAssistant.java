package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegression;
import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegressionModel;
import org.jblas.DoubleMatrix;

import java.io.IOException;

public class AllocationAssistant {

	public static void main(String[] args) throws IOException {
		// parse arguments: jarFile, jarArguments, initial resource allocation, user constraints (min/max resources, runtime-target)
		ConfigUtil conf = new ConfigUtil(args);
		Freamon freamon = new Freamon(conf.akka());

		// TODO: compute resources:

		// first get previous runtimes from Freamon,
		Object prevRuntimes = freamon.findSimilarApps(conf.args());

		// runtime data
		//int[] scaleOuts = null;
		//double[] runtimes = null;

		// constraint
		//double maxRuntime = 0;

		//int scaleOut = computeScaleOut(scaleOuts, runtimes, maxRuntime);


		// then (if multiple previous runs are available) build model (Ilya's code using JBLAS),
		// then use model to find scale-out (user target if available and between min-max resource constraints)
		Object resourceAlloc = null;

		// execute Flink job via Flink's YARN client
		new FlinkRunner(conf, freamon).runFlink(resourceAlloc);
	}

	private static int computeScaleOut(int[] scaleOuts, double[] runtimes, double maxRuntime) {
		// construct model
		SimpleLinearRegressionModel model = x -> {
			DoubleMatrix X = DoubleMatrix.ones(x.rows, 2);
			X.putColumn(1, x.rdiv(1.));
			return X;
		};
		SimpleLinearRegression regression = new SimpleLinearRegression(model);

		// put input into DoubleMatrix
		DoubleMatrix x = new DoubleMatrix(scaleOuts.length);
		for (int i = 0; i < scaleOuts.length; i++) {
			x.put(i, scaleOuts[i]);
		}
		DoubleMatrix y = new DoubleMatrix(runtimes);

		// train model
		regression.fit(x, y);

		// y = a + b/x
		DoubleMatrix coeffs = regression.getCoefficients();
		double a = coeffs.get(0);
		double b = coeffs.get(1);

		// calculate scale-out
		if (maxRuntime < a) {
			return -1; // impossible to fulfill constraint
		}
		return (int) Math.ceil(b / (maxRuntime - a));
	}

}
