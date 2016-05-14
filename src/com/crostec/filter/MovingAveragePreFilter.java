package com.crostec.filter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class MovingAveragePreFilter {

    private List<Integer> rawData = new ArrayList<Integer>();
    private int bufferSize;
    private static final Log log = LogFactory.getLog(MovingAveragePreFilter.class);

    public MovingAveragePreFilter(int bufferSize) {
            this.bufferSize = bufferSize;
    }

    public int getFilteredValue(int value) {
        int filteredValue = value;
            rawData.add(value);
            if (rawData.size() == bufferSize + 1) {
                rawData.remove(0);
            } else if (rawData.size() > bufferSize + 1) {
                throw new IllegalStateException("bufferSize exceeds maximum value: " + rawData.size());
            }
            long rawDataBufferSum = 0;

            if (rawData.size() < bufferSize) {
                 return  0;
            }
                for (int i = 0; i < bufferSize; i++) {
                     rawDataBufferSum += rawData.get(i);
                }
                filteredValue = (int)(rawDataBufferSum/bufferSize);


        return  filteredValue;
    }
}
