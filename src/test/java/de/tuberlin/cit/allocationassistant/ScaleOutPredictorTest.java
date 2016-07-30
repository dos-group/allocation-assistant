package de.tuberlin.cit.allocationassistant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ScaleOutPredictorTest {
    @Test
    public void testComputeScaleOut() throws Exception {
        ScaleOutPredictor predictor = new ScaleOutPredictor();
        int[] scaleOuts = new int[] {1, 2, 3, 4};
        double[] runtimes = new double[] {4, 4, 3, 1};

        int scaleOutPrediction;

        try {
            scaleOutPrediction = predictor.computeScaleOut(scaleOuts, runtimes, 1.4);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        scaleOutPrediction = predictor.computeScaleOut(scaleOuts, runtimes, 2);
        assertEquals(6, scaleOutPrediction);
    }
}
