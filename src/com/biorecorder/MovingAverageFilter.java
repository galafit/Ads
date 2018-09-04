package com.biorecorder;

import com.biorecorder.filters.DigitalFilter;

/**
 * Created by galafit on 30/3/18.
 */
public class MovingAverageFilter implements DigitalFilter {
    private final CircularFifoBuffer buffer;
    private final int bufferSize;
    private double sum;

    public MovingAverageFilter(int numberOfAveragingPoints) {
        buffer = new CircularFifoBuffer(numberOfAveragingPoints);
        bufferSize = numberOfAveragingPoints;
    }

    public double filteredValue(double value) {
        buffer.add(value);
        sum += value;

        if(buffer.size() < bufferSize) {
            return value;
        } else {
            double avg = sum / bufferSize;
            sum -= buffer.get();
            return avg;
        }
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        int numberOfAveragingPoints = 3;
        MovingAverageFilter filter = new MovingAverageFilter(numberOfAveragingPoints);
        for (int i = 0; i < arr.length; i++) {
            double filteredValue = filter.filteredValue(arr[i]);
            double expectedValue;
            if(i < numberOfAveragingPoints - 1) {
                expectedValue = arr[i];
            } else {
                expectedValue = 0;
                for (int j = 0; j < numberOfAveragingPoints; j++) {
                    expectedValue += arr[i - j];
                }
                expectedValue = expectedValue / numberOfAveragingPoints;
            }
            System.out.println(i + " filtered value: " + filteredValue + " Expected value " + expectedValue + "  Is equal: " + (filteredValue == expectedValue));
        }
    }
}
