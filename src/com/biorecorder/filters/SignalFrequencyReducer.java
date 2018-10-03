package com.biorecorder.filters;

import com.biorecorder.dataformat.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by galafit on 25/7/18.
 */
public class SignalFrequencyReducer extends RecordFilter {
    private Map<Integer, Integer> dividers = new HashMap<>();
    private int outRecordSize;
    
    public SignalFrequencyReducer(RecordConfig inConfig) {
        super(inConfig);
        outRecordSize = calculateOutRecordSize();
    }

    /**
     *
     * @throws IllegalArgumentException if signal number of samples in DataRecord is
     * not a multiple of divider
     */
    public void addDivider(int signalNumber, int divider) throws IllegalArgumentException {
        if(inConfig.getNumberOfSamplesInEachDataRecord(signalNumber) % divider != 0 ) {
           String errMsg = "Number of samples in DataRecord must be a multiple of divider. Number of samples = "
                   + inConfig.getNumberOfSamplesInEachDataRecord(signalNumber)
                   + " Divider = " + divider;
           throw new IllegalArgumentException(errMsg);
        }
        dividers.put(signalNumber, divider);
        calculateOutRecordSize();
    }

    private int calculateOutRecordSize() {
        outRecordSize = 0;

        for (int i = 0; i < inConfig.signalsCount(); i++) {
            Integer divider = dividers.get(i);
            if(divider != null) {
                outRecordSize += inConfig.getNumberOfSamplesInEachDataRecord(i) / divider;
            } else {
                outRecordSize += inConfig.getNumberOfSamplesInEachDataRecord(i);
            }
        }
        return outRecordSize;
    }

    @Override
    public RecordConfig dataConfig() {
        DefaultRecordConfig outConfig = new DefaultRecordConfig(inConfig);
        for (int i = 0; i < outConfig.signalsCount(); i++) {
            Integer divider = dividers.get(i);
            if(divider != null) {
                int numberOfSamples = outConfig.getNumberOfSamplesInEachDataRecord(i) / divider;
                outConfig.setNumberOfSamplesInEachDataRecord(i, numberOfSamples);
            }
        }
        return outConfig;
    }

    @Override
    protected void filterData(int[] inputRecord) {
        int[] outRecord = new int[outRecordSize];

        int signalCount = 0;
        int signalSampleCount = 0;

        int count = 0;
        long sum = 0;
        Integer divider = 1;

        int outIndex = 0;

        for (int i = 0; i < inRecordSize; i++) {
            if(signalSampleCount == 0) {
                divider = dividers.get(signalCount);
                if(divider == null) {
                    divider = 1;
                }
            }
            sum += inputRecord[i];
            count++;
            signalSampleCount++;
            if(count == divider) {
                if(divider > 1) {
                    outRecord[outIndex] = (int)(sum / divider);
                } else {
                    outRecord[outIndex] = inputRecord[i];
                }
                outIndex++;
                count = 0;
                sum = 0;
            }
            if(signalSampleCount == inConfig.getNumberOfSamplesInEachDataRecord(signalCount)) {
                signalCount++;
                signalSampleCount = 0;
            }
        }
        outStream.writeRecord(outRecord);
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 4 samples, 1 channel 2 samples, 2 channel 6 samples
        int[] dataRecord = {1,3,8,4,  2,4,  5,7,6,8,6,0};

        DefaultRecordConfig dataConfig = new DefaultRecordConfig(3);
        dataConfig.setNumberOfSamplesInEachDataRecord(0, 4);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 2);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 6);


        // reduce signals frequencies by 4, 2, 2
        SignalFrequencyReducer recordFilter = new SignalFrequencyReducer(dataConfig);
        recordFilter.addDivider(0, 4);
        recordFilter.addDivider(1, 2);
        recordFilter.addDivider(2, 2);

        // expected dataRecord
        int[] expectedDataRecord = {4,  3,  6,7,3};

        recordFilter.setOutStream(new RecordStream() {
            @Override
            public void writeRecord(int[] dataRecord1) {
                boolean isTestOk = true;
                if(expectedDataRecord.length != dataRecord1.length) {
                    System.out.println("Error!!! Resultant record length: "+dataRecord1.length+ " Expected record length : "+expectedDataRecord.length);
                    isTestOk = false;
                }

                for (int i = 0; i < dataRecord1.length; i++) {
                    if(dataRecord1[i] != expectedDataRecord[i]) {
                        System.out.println(i + " resultant data: "+dataRecord1[i]+ " expected data: "+expectedDataRecord[i]);
                        isTestOk = false;
                        break;
                    }
                }

                System.out.println("Is test ok: "+isTestOk);
            }

            @Override
            public void close() {

            }
        });

        recordFilter.writeRecord(dataRecord);
    }
}
