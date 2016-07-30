package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;

public class Utils {
    public static void checkColumnVector(DenseMatrix x) {
        if (x.numColumns() != 1) {
            throw new IllegalArgumentException("x must be a column vector!");
        }
    }

    public static boolean allClose(DenseVector a, DenseVector b, double tol) {
        DenseVector diff = a.copy();
        diff.add(-1., b);

        for (int i = 0; i < diff.size(); i++) {
            double abs = Math.abs(diff.get(i));
            if (abs > tol) {
                return false;
            }
        }
        return true;
    }

    public static boolean allClose(DenseVector A, DenseVector B) {
        return allClose(A, B, 1e-8);
    }
}
