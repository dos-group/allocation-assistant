package de.tuberlin.cit.allocationassistant;

import java.util.ArrayList;
import java.util.Collection;

public class PredictorInput {
    public Collection<Integer> scaleOuts = new ArrayList<>();
    public Collection<Double> runtimes = new ArrayList<>();

    public int[] unboxScaleOuts() {
        int[] scaleOutsUnboxed = new int[scaleOuts.size()];
        int i = 0;
        for (Integer scaleOut: scaleOuts) {
            scaleOutsUnboxed[i] = scaleOut;
            ++i;
        }
        return scaleOutsUnboxed;
    }

    public double[] unboxRuntimes() {
        double[] runtimesUnboxed = new double[runtimes.size()];
        int i = 0;
        for (Double runtime : runtimes) {
            runtimesUnboxed[i] = runtime;
            ++i;
        }
        return runtimesUnboxed;
    }
}
