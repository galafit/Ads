package com.biorecorder.filters;

import com.biorecorder.dataformat.DataRecordConfig;
import com.biorecorder.dataformat.DataRecordSender;
import com.biorecorder.dataformat.DefaultDataRecordConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by galafit on 25/7/18.
 */
public class SignalsFrequencyDivider extends RecordsFilter {
    private Map<Integer, Integer> dividers = new HashMap<>();
    private int resultantRecordSize;
    
    public SignalsFrequencyDivider(DataRecordSender input) {
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
    public DataRecordConfig dataConfig() {
        DefaultDataRecordConfig resultantConfig = new DefaultDataRecordConfig(in.dataConfig());
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
        int sampleCount = 0;
        for (int i = 0; i < inputRecord.length; i++) {
            Integer divider = dividers.get(signalCount);
            if(divider != null) {
                for (int j = 0; j < divider; j++) {

                }
            }

        }

    }
}
