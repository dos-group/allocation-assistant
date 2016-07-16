package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

public class MultipleLinearRegression implements Predictor {
    private DoubleMatrix coeffs;

    public MultipleLinearRegression() {
        coeffs = DoubleMatrix.EMPTY;
    }

    public void fit(DoubleMatrix X, DoubleMatrix y) {
        coeffs = Solve.solveLeastSquares(X, y);
    }

    public DoubleMatrix predict(DoubleMatrix X) {
        return X.mmul(coeffs);
    }

    public DoubleMatrix getCoefficients() {
        return coeffs;
    }
}
