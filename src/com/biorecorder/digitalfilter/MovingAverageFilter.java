package com.biorecorder.digitalfilter;

import com.biorecorder.CircularFifoBuffer;

/**
 * Created by galafit on 30/3/18.
 */
public class MovingAverageFilter implements DigitalFilter {
    private final CircularFifoBuffer fifoBuffer;
    private final int bufferSize;
    private double sum;

    public MovingAverageFilter(int numberOfAveragingPoints) {
        fifoBuffer = new CircularFifoBuffer(numberOfAveragingPoints);
        bufferSize = numberOfAveragingPoints;
    }

    public double filteredValue(double value) {
        fifoBuffer.add(value);
        sum += value;
        double avg = sum / fifoBuffer.size();
        if(fifoBuffer.size() == bufferSize) {
            sum -= fifoBuffer.get();
        }
        return avg;
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        int numberOfAveragingPoints = 3;
        MovingAverageFilter filter = new MovingAverageFilter(numberOfAveragingPoints);
        boolean isTestOk = true;
        for (int i = 0; i < arr.length; i++) {
            double filteredValue = filter.filteredValue(arr[i]);
            double expectedValue = 0;
            int n = Math.min(i, numberOfAveragingPoints - 1) + 1;
            for (int j = 0; j < n; j++) {
                expectedValue += arr[i - j];
            }
            expectedValue = expectedValue / n;

            if(filteredValue != expectedValue) {
                System.out.println(i + " Error! filtered value: " + filteredValue + " Expected value " + expectedValue);
                isTestOk = false;
                break;
            }
        }
        System.out.println("Is test ok: "+isTestOk);
    }
}
