package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;

public interface Predictor {
    void fit(DenseMatrix x, DenseVector y);
    DenseVector predict(DenseMatrix x);
}
