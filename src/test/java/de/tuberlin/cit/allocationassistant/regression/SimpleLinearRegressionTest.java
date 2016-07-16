package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SimpleLinearRegressionTest {
    private SimpleLinearRegression regression;

    @Before
    public void setUp() throws Exception {
        SimpleLinearRegressionModel model = x -> {
            DoubleMatrix X = DoubleMatrix.ones(x.rows, 2);
            X.putColumn(1, x.rdiv(1.));
            return X;
        };
        regression = new SimpleLinearRegression(model);
    }

    @Test
    public void testFit() throws Exception {
        DoubleMatrix x = new DoubleMatrix(new double[] {1, 2, 3, 4});
        DoubleMatrix X = DoubleMatrix.concatHorizontally(DoubleMatrix.ones(x.length), x);
        DoubleMatrix y = new DoubleMatrix(new double[] {4, 4, 3, 1});

        try {
            regression.fit(X, y);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testPredict() throws Exception {
        DoubleMatrix x = new DoubleMatrix(new double[] {1, 2, 3, 4});
        DoubleMatrix y = new DoubleMatrix(new double[] {4, 4, 3, 1});

        regression.fit(x, y);

        DoubleMatrix yPred = regression.predict(x);
        DoubleMatrix yPredTrue = new DoubleMatrix(new double[] {
                4.415384615384612,
                2.9384615384615365,
                2.4461538461538446,
                2.1999999999999984,
        });

        assertTrue(Utils.allClose(yPred, yPredTrue));
    }

    @Test
    public void testGetCoefficients() throws Exception {
        DoubleMatrix x = new DoubleMatrix(new double[] {1, 2, 3, 4});
        DoubleMatrix y = new DoubleMatrix(new double[] {4, 4, 3, 1});

        regression.fit(x, y);

        DoubleMatrix w = regression.getCoefficients();
        DoubleMatrix wTrue = new DoubleMatrix(new double[] {1.4615384615384603, 2.9538461538461522});

        assertTrue(Utils.allClose(w, wTrue));
    }
}
