package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.freamon.api.PreviousRuns;

public class DatasetSizeFilter {
    private final double datasetSize;

    public DatasetSizeFilter(double datasetSize) {
        this.datasetSize = datasetSize;
    }

    public PredictorInput filterPreviousRuns(PreviousRuns previousRuns) {
        double minSize = datasetSize * 0.9;
        double maxSize = datasetSize * 1.1;

        PredictorInput predictorInput = new PredictorInput();

        Integer[] scaleOuts = previousRuns.scaleOuts();
        Double[] runtimes = previousRuns.runtimes();
        Double[] datasetSizes = previousRuns.datasetSizes();

        for (int i = 0; i < datasetSizes.length; i++) {
            Double dsSize = datasetSizes[i];
            if (minSize <= dsSize && dsSize <= maxSize) {
                predictorInput.scaleOuts.add(scaleOuts[i]);
                predictorInput.runtimes.add(runtimes[i]);
            }
        }

        return predictorInput;
    }
}
