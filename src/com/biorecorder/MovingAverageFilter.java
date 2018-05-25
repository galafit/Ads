package com.biorecorder;

import com.biorecorder.bdfrecorder.filters.DigitalFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by galafit on 30/3/18.
 */
public class MovingAverageFilter implements DigitalFilter {
    private List<Double> buffer = new ArrayList();
    private int bufferSize;

    public MovingAverageFilter(int numberOfAveragingPoints) {
        this.bufferSize = numberOfAveragingPoints;
    }

    public double filteredValue(double value) {
        this.buffer.add(Double.valueOf(value));
        if(this.buffer.size() < this.bufferSize) {
            return value;
        } else {
            if(this.buffer.size() == this.bufferSize + 1) {
                this.buffer.remove(0);
            }

            long bufferSum = 0L;

            for(int i = 0; i < this.bufferSize; ++i) {
                bufferSum = (long)((double)bufferSum + ((Double)this.buffer.get(i)).doubleValue());
            }

            return (double)(bufferSum / (long)this.bufferSize);
        }
    }
}
