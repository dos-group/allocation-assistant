package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;

public class Utils {
    public static void checkVector(DoubleMatrix x) {
        if (!x.isColumnVector()) {
            throw new IllegalArgumentException("x must be a column vector!");
        }
    }

    public static boolean allClose(DoubleMatrix A, DoubleMatrix B, double tol) {
        DoubleMatrix diff = A.sub(B);

        for (int i = 0; i < A.length; i++) {
            double abs = Math.abs(diff.get(i));
            if (abs > tol) {
                return false;
            }
        }
        return true;
    }

    public static boolean allClose(DoubleMatrix A, DoubleMatrix B) {
        return allClose(A, B, 1e-8);
    }
}
