package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SimpleLinearRegressionTest {
    private SimpleLinearRegressionModel model;
    private SimpleLinearRegression regression;

    @Before
    public void setUp() throws Exception {
        model = x -> {
            DenseMatrix X = new DenseMatrix(x.size(), 2);
            for (int i = 0; i < x.size(); i++) {
                X.set(i, 0, 1);
                X.set(i, 1, 1./x.get(i));
            }
            return X;
        };
        regression = new SimpleLinearRegression(model);
    }

    @Test
    public void testFit() throws Exception {
        DenseVector x = new DenseVector(new double[] {1, 2, 3, 4});
        DenseMatrix X = model.map(x);
        DenseVector y = new DenseVector(new double[] {4, 4, 3, 1});

        try {
            regression.fit(X, y);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testPredict() throws Exception {
        DenseVector x = new DenseVector(new double[] {1, 2, 3, 4});
        DenseVector y = new DenseVector(new double[] {4, 4, 3, 1});

        regression.fit(x, y);

        DenseVector yPred = regression.predict(x);
        DenseVector yPredTrue = new DenseVector(new double[] {
                4.415384615384612,
                2.9384615384615365,
                2.4461538461538446,
                2.1999999999999984,
        });

        assertTrue(Utils.allClose(yPred, yPredTrue));
    }

    @Test
    public void testGetCoefficients() throws Exception {
        DenseVector x = new DenseVector(new double[] {1, 2, 3, 4});
        DenseVector y = new DenseVector(new double[] {4, 4, 3, 1});

        regression.fit(x, y);

        DenseVector w = regression.getCoefficients();
        DenseVector wTrue = new DenseVector(new double[] {1.4615384615384603, 2.9538461538461522});

        assertTrue(Utils.allClose(w, wTrue));
    }
}
