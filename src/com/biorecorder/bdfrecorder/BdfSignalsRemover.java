package com.biorecorder.bdfrecorder;

import java.util.ArrayList;
import java.util.List;

/**
 * Permit to omit samples from some channels (delete signals)
 */
public class BdfSignalsRemover extends BdfFilter{
    private List<Integer> signalsToRemove = new ArrayList<Integer>();

    public BdfSignalsRemover(BdfDataProducer input) {
        super(input);
    }

    /**
     * Indicate that the samples from the given signal should be omitted in resultant DataRecords
     *
     * @param signalNumber number of the signal
     *                     whose samples should be omitted. Numbering starts from 0.
     */
    public void removeSignal(int signalNumber) {
        signalsToRemove.add(signalNumber);
    }

    @Override
    public int getSignalsCount() {
        return input.getSignalsCount() - signalsToRemove.size();
    }

    @Override
    public double getDurationOfDataRecord() {
        return input.getDurationOfDataRecord();
    }

    @Override
    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        int removedSignalsCount = 0;
        for (Integer inputSignalNumber : signalsToRemove) {
            if(inputSignalNumber <= signalNumber) {
                removedSignalsCount++;
            }

        }
        return input.getNumberOfSamplesInEachDataRecord(signalNumber - removedSignalsCount);
    }


    /**
     * Omits data from the "deleted" channels and
     * create resultant array of samples
     */
    @Override
    protected void filterData(int[] inputRecord) {
        int inputRecordLength = recordSize(input);
        int[] resultantRecord = new int[recordSize(this)];

        int inputSignalNumber = 0;
        int inputSignalStartSampleNumber = 0;
        int resultantSampleCount = 0;
        for (int i = 0; i < inputRecordLength; i++) {
            if(i >= inputSignalStartSampleNumber + input.getNumberOfSamplesInEachDataRecord(inputSignalNumber)) {
               inputSignalStartSampleNumber += input.getNumberOfSamplesInEachDataRecord(inputSignalNumber);
               inputSignalNumber++;
            }
            if(!signalsToRemove.contains(inputSignalNumber)) {
                resultantRecord[resultantSampleCount] = inputRecord[i];
                resultantSampleCount++;
            }
        }
        dataListener.onDataRecordReceived(resultantRecord);
    }
}
