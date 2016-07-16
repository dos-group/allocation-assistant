package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;

public interface Predictor {
    void fit(DoubleMatrix x, DoubleMatrix y);
    DoubleMatrix predict(DoubleMatrix x);
}
