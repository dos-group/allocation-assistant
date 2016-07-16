package de.tuberlin.cit.allocationassistant.regression;

import org.jblas.DoubleMatrix;

/**
 * This class fits a function to a one-dimensional data set.
 * The function class is defined by supplying the desired model.
 * <p>
 * Using this class consists of fitting a function model to the data and then predicting new data.
 * The following example illustrates the usage by fitting a quadratic function.
 * <pre>
 *     // define quadratic model, i.e. y = a + b x + c x^2
 *     SimpleLinearRegressionModel model = x -> {
 *         DoubleMatrix X = DoubleMatrix.ones(x.rows, 3);
 *         X.putColumn(1, x);
 *         X.putColumn(2, x.mul(x));
 *         return X;
 *     };
 *     SimpleLinearRegression regression = new SimpleLinearRegression(model);
 *
 *     DoubleMatrix x = new DoubleMatrix(new double[] {-2, -1, 1, 2});
 *     DoubleMatrix y = new DoubleMatrix(new double[] {4.3, 1.8, 2.1, 3.7});
 *
 *     regression.fit(x, y);
 *     DoubleMatrix coeffs = regression.getCoefficients();
 *     coeffs.print(); // [1.266667; -0.090000; 0.683333]
 *                     // i.e. y = 1.266667 - 0.090000 x + 0.683333 x^2
 *
 *     DoubleMatrix yPred = regression.predict(x);
 *     yPred.print(); // [4.180000; 2.040000; 1.860000; 3.820000]
 * </pre>
 */
public class SimpleLinearRegression implements Predictor {
    private MultipleLinearRegression regression;
    private SimpleLinearRegressionModel model;

    /**
     * Constructs a new regression object with the given function model.
     *
     * @param model the function model.
     */
    public SimpleLinearRegression(SimpleLinearRegressionModel model) {
        this.regression = new MultipleLinearRegression();
        this.model = model;
    }

    /**
     * Fits a function to the training data.
     *
     * @param x shape (n,1), a column vector containing the one-dimensional data samples.
     * @param y shape (n,1), a column vector containing the target values.
     */
    public void fit(DoubleMatrix x, DoubleMatrix y) {
        Utils.checkVector(x);
        DoubleMatrix X = model.map(x);
        regression.fit(X, y);
    }

    /**
     * Predicts the value at the given point.
     *
     * @param x the value for which to predict the target.
     * @return the prediction.
     */
    public double predict(double x) {
        return predict(DoubleMatrix.scalar(x)).scalar();
    }

    /**
     * Predicts the values for the given set of values.
     *
     * @param x shape (n,1), a column vector containing the one-dimensional data sample
     *          for which the target value is predicted
     * @return shape (n,1), a column vector containing the prediction values.
     */
    public DoubleMatrix predict(DoubleMatrix x) {
        Utils.checkVector(x);
        DoubleMatrix X = model.map(x);
        return regression.predict(X);
    }

    /**
     * Returns the coefficients of the defined function model.
     *
     * @return shape (d,1), a column vector containing the coefficients.
     */
    public DoubleMatrix getCoefficients() {
        return regression.getCoefficients();
    }
}
