package com.biorecorder.filters;

import com.biorecorder.dataformat.DataRecordConfig;
import com.biorecorder.dataformat.DataRecordSender;
import com.biorecorder.dataformat.DefaultDataRecordConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Permit to omit samples from some channels (delete signals)
 */
public class  SignalsRemover extends RecordsFilter {
    private List<Integer> signalsToRemove = new ArrayList<Integer>();
    private int inRecordSize;
    private int resultantRecordSize;

    public SignalsRemover(DataRecordSender input) {
        super(input);
        for (int i = 0; i < in.dataConfig().signalsCount(); i++) {
            inRecordSize += in.dataConfig().getNumberOfSamplesInEachDataRecord(i);
        }
        resultantRecordSize = calculateResultantRecordSize();
    }

    /**
     * Indicates that the samples from the given signal should be omitted in
     * resultant data records. This method can be called only
     * before adding a listener!
     *
     * @param signalNumber number of the signal
     *                     whose samples should be omitted. Numbering starts from 0.
     */
    public void removeSignal(int signalNumber) {
        signalsToRemove.add(signalNumber);
        resultantRecordSize = calculateResultantRecordSize();
    }

    @Override
    public DataRecordConfig dataConfig() {
        DefaultDataRecordConfig resultantConfig = new DefaultDataRecordConfig(in.dataConfig());

        for (int i = in.dataConfig().signalsCount() - 1; i >= 0 ; i--) {
            if(signalsToRemove.contains(i)) {
                resultantConfig.removeSignal(i);
            }
        }
        return resultantConfig;
    }

    private int calculateResultantRecordSize() {
        int size = inRecordSize;
        for (Integer removedSignal : signalsToRemove) {
            size -= in.dataConfig().getNumberOfSamplesInEachDataRecord(removedSignal);
        }
        return size;
    }


    /**
     * Omits data from the "deleted" channels and
     * create resultant array of samples
     */
    @Override
    protected void filterData(int[] inputRecord) {
        int[] resultantRecord = new int[resultantRecordSize];

        int inputSignalNumber = 0;
        int inputSignalStartSampleNumber = 0;
        int resultantSampleCount = 0;
        for (int i = 0; i < inRecordSize; i++) {
            if(i >= inputSignalStartSampleNumber + in.dataConfig().getNumberOfSamplesInEachDataRecord(inputSignalNumber)) {
               inputSignalStartSampleNumber += in.dataConfig().getNumberOfSamplesInEachDataRecord(inputSignalNumber);
               inputSignalNumber++;
            }
            if(!signalsToRemove.contains(inputSignalNumber)) {
                resultantRecord[resultantSampleCount] = inputRecord[i];
                resultantSampleCount++;
            }
        }
        sendDataToListeners(resultantRecord);
    }
}
