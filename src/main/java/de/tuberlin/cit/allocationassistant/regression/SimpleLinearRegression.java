package de.tuberlin.cit.allocationassistant.regression;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;

/**
 * This class fits a function to a one-dimensional data set.
 * The function class is defined by supplying the desired model.
 * <p>
 * Using this class consists of fitting a function model to the data and then predicting new data.
 * The following example illustrates the usage by fitting a quadratic function.
 * <pre>
 *     // define quadratic model, i.e. y = a + b x + c x^2
 *     SimpleLinearRegressionModel model = x -> {
 *         DenseMatrix X = new DenseMatrix(x.size(), 2);
 *         for (int i = 0; i < x.size(); i++) {
 *             X.set(i, 0, 1);
 *             X.set(i, 1, 1./x.get(i));
 *         }
 *         return X;
 *     };
 *
 *     SimpleLinearRegression regression = new SimpleLinearRegression(model);
 *
 *     DenseVector x = new DenseVector(new double[] {-2, -1, 1, 2});
 *     DenseVector y = new DenseVector(new double[] {4.3, 1.8, 2.1, 3.7});
 *
 *     regression.fit(x, y);
 *     DenseVector coeffs = regression.getCoefficients();
 *     // coeffs = [1.266667; -0.090000; 0.683333]
 *     // i.e. y = 1.266667 - 0.090000 x + 0.683333 x^2
 *
 *     DenseVector yPred = regression.predict(x);
 *     // yPred = [4.180000; 2.040000; 1.860000; 3.820000]
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
    public void fit(DenseVector x, DenseVector y) {
        DenseMatrix X = model.map(x);
        regression.fit(X, y);
    }

    /**
     * Fits a function to the training data.
     *
     * @param x shape (n,1), a column vector containing the one-dimensional data samples.
     * @param y shape (n,1), a column vector containing the target values.
     */
    public void fit(DenseMatrix x, DenseVector y) {
        Utils.checkColumnVector(x);
        DenseVector xvec = Matrices.getColumn(x, 0);
        fit(xvec, y);
    }

    /**
     * Predicts the value at the given point.
     *
     * @param x the value for which to predict the target.
     * @return the prediction.
     */
    public double predict(double x) {
        DenseVector xvec = new DenseVector(new double[]{x});
        DenseVector result = predict(xvec);
        return result.get(0);
    }

    /**
     * Predicts the values for the given set of values.
     *
     * @param x shape (n,1), a column vector containing the one-dimensional data sample
     *          for which the target value is predicted
     * @return shape (n,1), a column vector containing the prediction values.
     */
    public DenseVector predict(DenseVector x) {
        DenseMatrix X = model.map(x);
        return regression.predict(X);
    }

    /**
     * Predicts the values for the given set of values.
     *
     * @param x shape (n,1), a column vector containing the one-dimensional data sample
     *          for which the target value is predicted
     * @return shape (n,1), a column vector containing the prediction values.
     */
    public DenseVector predict(DenseMatrix x) {
        Utils.checkColumnVector(x);
        DenseVector xvec = Matrices.getColumn(x, 0);
        return predict(xvec);
    }

    /**
     * Returns the coefficients of the defined function model.
     *
     * @return shape (d,1), a column vector containing the coefficients.
     */
    public DenseVector getCoefficients() {
        return regression.getCoefficients();
    }
}
