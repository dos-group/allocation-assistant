package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;

public interface SimpleLinearRegressionModel {
    DoubleMatrix map(DoubleMatrix x);
}
