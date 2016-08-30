package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegression;
import de.tuberlin.cit.allocationassistant.regression.SimpleLinearRegressionModel;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;

import java.util.ArrayList;
import java.util.Collection;

public class ScaleOutPredictor {
    private SimpleLinearRegression regression;

    public ScaleOutPredictor() {
        // construct model
        SimpleLinearRegressionModel model = x -> {
            DenseMatrix X = new DenseMatrix(x.size(), 2);
            for (int i = 0; i < x.size(); i++) {
                X.set(i, 0, 1);
                X.set(i, 1, 1./x.get(i));
            }
            return X;
        };
        regression = new SimpleLinearRegression(model);
    }

    public int computeScaleOut(int[] scaleOuts, double[] runtimes, double maxRuntime) {
        // put input into DenseVector
        DenseVector x = new DenseVector(scaleOuts.length);
        for (int i = 0; i < scaleOuts.length; i++) {
            x.set(i, scaleOuts[i]);
        }
        DenseVector y = new DenseVector(runtimes);

        // train model
        regression.fit(x, y);

        // y = a + b/x
        DenseVector coeffs = regression.getCoefficients();
        double a = coeffs.get(0);
        double b = coeffs.get(1);

        // calculate scale-out
        if (maxRuntime < a) {
            throw new IllegalArgumentException(String.format(
                    "impossible to fulfill runtime constraint %s, need at least %s", maxRuntime, a));
        }
        return (int) Math.ceil(b / (maxRuntime - a));
    }

    public int computeScaleOut(PredictorInput runs, Double maxRuntime) {
        int[] scaleOutsUnboxed = new int[runs.scaleOuts.size()];
        int i = 0;
        for (Integer scaleOut: runs.scaleOuts) {
            scaleOutsUnboxed[i] = scaleOut;
            ++i;
        }

        double[] runtimesUnboxed = new double[runs.runtimes.size()];
        i = 0;
        for (Double runtime : runs.runtimes) {
            runtimesUnboxed[i] = runtime;
            ++i;
        }

        return computeScaleOut(scaleOutsUnboxed, runtimesUnboxed, maxRuntime);
    }

    public static class PredictorInput {
        public Collection<Integer> scaleOuts = new ArrayList<>();
        public Collection<Double> runtimes = new ArrayList<>();
    }
}
