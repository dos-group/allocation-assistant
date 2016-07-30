package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;

public class MultipleLinearRegression implements Predictor {
    private DenseVector coeffs;

    public void fit(DenseMatrix X, DenseVector y) {
        coeffs = new DenseVector(X.numColumns());
        X.solve(y, coeffs);
    }

    public DenseVector predict(DenseMatrix X) {
        if (coeffs == null) {
            throw new IllegalStateException("Cannot predict before no data is fitted.");
        }
        DenseVector result = new DenseVector(X.numRows());
        X.mult(coeffs, result);
        return result;
    }

    public DenseVector getCoefficients() {
        return coeffs;
    }
}
