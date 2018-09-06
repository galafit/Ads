package com.biorecorder.filters;

import com.biorecorder.dataformat.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Permit to omit samples from some channels (delete signals)
 */
public class SignalRemover extends RecordFilter {
    private List<Integer> signalsToRemove = new ArrayList<Integer>();
    private int outRecordSize;

    public SignalRemover(RecordSender in) {
        super(in);
        outRecordSize = calculateoutRecordSize();
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
        outRecordSize = calculateoutRecordSize();
    }

    @Override
    public RecordConfig dataConfig() {
        DefaultRecordConfig outConfig = new DefaultRecordConfig(inConfig);

        for (int i = inConfig.signalsCount() - 1; i >= 0 ; i--) {
            if(signalsToRemove.contains(i)) {
                outConfig.removeSignal(i);
            }
        }
        return outConfig;
    }

    private int calculateoutRecordSize() {
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
    protected void filterData(int[] inputRecord) {
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
        sendDataToListeners(outRecord);
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 1 sample, 1 channel 2 samples, 2 channel 3 samples, 3 channel 4 samples
        int[] dataRecord = {1,  2,3,  4,5,6,  7,8,9,0};

        DefaultRecordConfig dataConfig = new DefaultRecordConfig(4);
        dataConfig.setNumberOfSamplesInEachDataRecord(0, 1);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 2);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 3);
        dataConfig.setNumberOfSamplesInEachDataRecord(3, 4);


        TestRecordSender recordSender = new TestRecordSender(dataConfig);


        // remove signals 0 and 2
        SignalRemover recordFilter = new SignalRemover(recordSender);
        recordFilter.removeSignal(0);
        recordFilter.removeSignal(2);

        // expected dataRecord
        int[] expectedDataRecord = {2,3,   7,8,9,0};

        recordFilter.addDataListener(new RecordListener() {
            @Override
            public void onDataReceived(int[] dataRecord1) {
                boolean isTestOk = true;
                if(expectedDataRecord.length != dataRecord1.length) {
                    System.out.println("Error!!! Resultant record length: "+dataRecord1.length+ " Expected record length : "+expectedDataRecord.length);
                    isTestOk = false;
                }
                if(recordFilter.dataConfig().signalsCount() != 2) {
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
        });
        recordSender.sendRecord(dataRecord);
    }
}
