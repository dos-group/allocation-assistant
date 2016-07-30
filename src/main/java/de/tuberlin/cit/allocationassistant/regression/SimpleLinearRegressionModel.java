package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;

public interface SimpleLinearRegressionModel {
    DenseMatrix map(DenseVector x);
}
