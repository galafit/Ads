package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordSender;
import com.biorecorder.dataformat.DefaultRecordConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Permit to omit samples from some channels (delete signals)
 */
public class RecordSignalsRemover extends RecordsFilter {
    private List<Integer> signalsToRemove = new ArrayList<Integer>();
    private int inRecordSize;
    private int resultantRecordSize;

    public RecordSignalsRemover(RecordSender in) {
        super(in);
        for (int i = 0; i < this.inConfig.signalsCount(); i++) {
            inRecordSize += this.inConfig.getNumberOfSamplesInEachDataRecord(i);
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
    public RecordConfig dataConfig() {
        DefaultRecordConfig resultantConfig = new DefaultRecordConfig(inConfig);

        for (int i = inConfig.signalsCount() - 1; i >= 0 ; i--) {
            if(signalsToRemove.contains(i)) {
                resultantConfig.removeSignal(i);
            }
        }
        return resultantConfig;
    }

    private int calculateResultantRecordSize() {
        int size = inRecordSize;
        for (Integer removedSignal : signalsToRemove) {
            size -= inConfig.getNumberOfSamplesInEachDataRecord(removedSignal);
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
            if(i >= inputSignalStartSampleNumber + inConfig.getNumberOfSamplesInEachDataRecord(inputSignalNumber)) {
               inputSignalStartSampleNumber += inConfig.getNumberOfSamplesInEachDataRecord(inputSignalNumber);
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
