package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.RecordConfig;
import com.biorecorder.multisignal.recordformat.RecordStream;
import com.biorecorder.multisignal.recordformat.FormatVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Permit to omit samples from some channels (delete signals)
 */
public class SignalRemover extends FilterRecordStream {
    private List<Integer> signalsToRemove = new ArrayList<Integer>();
    private int outRecordSize;

    public SignalRemover(RecordStream outStream) {
        super(outStream);
    }

    @Override
    public void setRecordConfig(RecordConfig inConfig) {
        super.setRecordConfig(inConfig);
        outRecordSize = calculateOutRecordSize();
    }

    /**
     * Indicates that the samples from the given signal should be omitted in
     * out data records. This method can be called only
     * before adding a listener!
     *
     * @param signalNumber number of the signal
     *                     whose samples should be omitted. Numbering starts from 0.
     */
    public void removeSignal(int signalNumber) {
        signalsToRemove.add(signalNumber);
        if(inConfig != null) {
            outRecordSize = calculateOutRecordSize();
            outStream.setRecordConfig(getOutConfig());
        }
    }

    @Override
    public RecordConfig getOutConfig() {
        RecordConfig outConfig = new RecordConfig(inConfig);

        for (int i = inConfig.signalsCount() - 1; i >= 0 ; i--) {
            if(signalsToRemove.contains(i)) {
                outConfig.removeSignal(i);
            }
        }
        return outConfig;
    }

    private int calculateOutRecordSize() {
        int size = inRecordSize;
        for (Integer removedSignal : signalsToRemove) {
            size -= inConfig.getNumberOfSamplesInEachDataRecord(removedSignal);
        }
        return size;
    }


    /**
     * Omits data from the "deleted" channels and
     * create out array of samples
     */
    @Override
    public void writeRecord(int[] inputRecord) {
        int[] outRecord = new int[outRecordSize];

        int signalNumber = 0;
        int signalStart = 0;
        int outSamples = 0;
        for (int i = 0; i < inRecordSize; i++) {
            if(i >= signalStart + inConfig.getNumberOfSamplesInEachDataRecord(signalNumber)) {
               signalStart += inConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
               signalNumber++;
            }
            if(!signalsToRemove.contains(signalNumber)) {
                outRecord[outSamples] = inputRecord[i];
                outSamples++;
            }
        }
        outStream.writeRecord(outRecord);
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 1 sample, 1 channel 2 samples, 2 channel 3 samples, 3 channel 4 samples
        int[] dataRecord = {1,  2,3,  4,5,6,  7,8,9,0};

        RecordConfig dataConfig = new RecordConfig(FormatVersion.BDF_24BIT, 4);
        dataConfig.setNumberOfSamplesInEachDataRecord(0, 1);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 2);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 3);
        dataConfig.setNumberOfSamplesInEachDataRecord(3, 4);

        // remove signals 0 and 2

        // expected dataRecord
        int[] expectedDataRecord = {2,3,   7,8,9,0};

        SignalRemover recordFilter = new SignalRemover(new TestStream(expectedDataRecord));
        recordFilter.removeSignal(0);
        recordFilter.removeSignal(2);
        recordFilter.setRecordConfig(dataConfig);

        recordFilter.writeRecord(dataRecord);
    }
}
