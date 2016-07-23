package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegression;
import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegressionModel;
import de.tuberlin.cit.freamon.api.PreviousRuns;
import org.jblas.DoubleMatrix;

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
			// TODO apply limit here instead of when building command
			System.out.println("Computed scaleOut of " + scaleOut);
		}

		new FlinkRunner(options, freamon).runFlink(scaleOut);

		// terminate the application, or else akka will keep it alive
		System.exit(0);
	}

	private static int computeScaleOut(Integer[] scaleOuts, Double[] runtimes, Options options) {
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
		DoubleMatrix y = new DoubleMatrix(scaleOuts.length);
		for (int i = 0; i < runtimes.length; i++) {
			y.put(i, runtimes[i]);
		}

		// train model
		regression.fit(x, y);

		// y = a + b/x
		DoubleMatrix coeffs = regression.getCoefficients();
		double a = coeffs.get(0);
		double b = coeffs.get(1);

		// calculate scale-out
		double maxRuntime = ((Double) options.args().maxRuntime().apply());
		if (maxRuntime < a) {
			throw new IllegalArgumentException(String.format(
					"impossible to fulfill runtime constraint %s, need at least %s", maxRuntime, a));
		}
		return (int) Math.ceil(b / (maxRuntime - a));
	}

}
