package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordSender;
import com.biorecorder.dataformat.DefaultRecordConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by galafit on 25/7/18.
 */
public class RecordSignalsFrequencyReducer extends RecordsFilter {
    private Map<Integer, Integer> dividers = new HashMap<>();
    private int resultantRecordSize;
    
    public RecordSignalsFrequencyReducer(RecordSender input) {
        super(input);
        resultantRecordSize = calculateResultantRecordSize();
    }

    /**
     *
     * @throws IllegalArgumentException if signal number of samples in DataRecord is
     * not a multiple of divider
     */
    public void addDivider(int signalNumber, int divider) throws IllegalArgumentException {
        if(in.dataConfig().getNumberOfSamplesInEachDataRecord(signalNumber) % divider != 0 ) {
           String errMsg = "Number of samples in DataRecord must be a multiple of divider. Number of samples = "
                   + in.dataConfig().getNumberOfSamplesInEachDataRecord(signalNumber)
                   + " Divider = " + divider;
           throw new IllegalArgumentException(errMsg);
        }
        dividers.put(signalNumber, divider);
        calculateResultantRecordSize();
    }

    private int calculateResultantRecordSize() {
        int size = 0;

        for (int i = 0; i < in.dataConfig().signalsCount(); i++) {
            Integer divider = dividers.get(i);
            if(divider != null) {
                size += in.dataConfig().getNumberOfSamplesInEachDataRecord(i) / divider;
            } else {
                size += in.dataConfig().getNumberOfSamplesInEachDataRecord(i);
            }
          }
        return size;
    }

    @Override
    public RecordConfig dataConfig() {
        DefaultRecordConfig resultantConfig = new DefaultRecordConfig(in.dataConfig());
        for (int i = 0; i < resultantConfig.signalsCount(); i++) {
            Integer divider = dividers.get(i);
            if(divider != null) {
                int numberOfSamples = resultantConfig.getNumberOfSamplesInEachDataRecord(i) / divider;
                resultantConfig.setNumberOfSamplesInEachDataRecord(i, numberOfSamples);
            }
        }
        return resultantConfig;
    }

    @Override
    protected void filterData(int[] inputRecord) {
        int[] resultantRecord = new int[resultantRecordSize];

        int signalCount = 0;
        int signalSampleCount = 0;

        int count = 0;
        long sum = 0;
        Integer divider = 1;

        int resultantIndex = 0;

        for (int i = 0; i < inputRecord.length; i++) {
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
                    resultantRecord[resultantIndex] = (int)(sum / divider);
                } else {
                    resultantRecord[resultantIndex] = inputRecord[i];
                }
                resultantIndex++;
                count = 0;
                sum = 0;
            }
            if(signalSampleCount == in.dataConfig().getNumberOfSamplesInEachDataRecord(signalCount)) {
                signalCount++;
                signalSampleCount = 0;
            }
        }
        sendDataToListeners(resultantRecord);
    }
}
