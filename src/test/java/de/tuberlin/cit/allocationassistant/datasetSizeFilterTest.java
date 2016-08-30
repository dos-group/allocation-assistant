package de.tuberlin.cit.allocationassistant;

import de.tuberlin.cit.freamon.api.PreviousRuns;
import org.junit.Before;
import org.junit.Test;
import scala.Array;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class datasetSizeFilterTest {
    DatasetSizeFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = new DatasetSizeFilter(123d);
    }

    @Test
    public void testEmpty() throws Exception {
        Integer[] s = {};
        Double[] r = {};
        Double[] d = {};
        ScaleOutPredictor.PredictorInput pin = filter.filterPreviousRuns(new PreviousRuns(s, r, d));
        assertEquals(0, pin.scaleOuts.size());
        assertEquals(0, pin.runtimes.size());
    }

    @Test
    public void testNone() throws Exception {
        Integer[] s = {1, 2, 3};
        Double[] r = {123d, 124d, 122d};
        Double[] d = {1d, 2d, 3d};
        ScaleOutPredictor.PredictorInput pin = filter.filterPreviousRuns(new PreviousRuns(s, r, d));
        assertEquals(0, pin.scaleOuts.size());
        assertEquals(0, pin.runtimes.size());
    }

    @Test
    public void testAll() throws Exception {
        Integer[] s = {1, 2, 3};
        Double[] r = {1d, 2d, 3d};
        Double[] d = {123d, 124d, 122d};
        ScaleOutPredictor.PredictorInput pin = filter.filterPreviousRuns(new PreviousRuns(s, r, d));
        assertArrayEquals(new Integer[] {1, 2, 3}, pin.scaleOuts.toArray());
        assertArrayEquals(new Double[] {1d, 2d, 3d}, pin.runtimes.toArray());
    }

    @Test
    public void testSome() throws Exception {
        Integer[] s = {1, 4, 2, 3};
        Double[] r = {1d, 42d, 2d, 3d};
        Double[] d = {123d, 1d, 999d, 122d};
        ScaleOutPredictor.PredictorInput pin = filter.filterPreviousRuns(new PreviousRuns(s, r, d));
        assertArrayEquals(new Integer[] {1, 3}, pin.scaleOuts.toArray());
        assertArrayEquals(new Double[] {1d, 3d}, pin.runtimes.toArray());
    }
}
