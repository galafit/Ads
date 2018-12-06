package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.DataHeader;
import com.biorecorder.multisignal.recordformat.DataRecordStream;
import com.biorecorder.multisignal.recordformat.FormatVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by galafit on 25/7/18.
 */
public class SignalFrequencyReducer extends FilterRecordStream {
    private Map<Integer, Integer> dividers = new HashMap<>();
    private int outRecordSize;

    public SignalFrequencyReducer(DataRecordStream outStream) {
        super(outStream);
    }

    @Override
    public void setHeader(DataHeader header) {
        super.setHeader(header);
        outRecordSize = calculateOutRecordSize();
    }

    /**
     *
     * @throws IllegalArgumentException if signal number of samples in DataRecord is
     * not a multiple of divider
     */
    public void addDivider(int signalNumber, int divider) throws IllegalArgumentException {
        if(inConfig != null && inConfig.getNumberOfSamplesInEachDataRecord(signalNumber) % divider != 0 ) {
           String errMsg = "Number of samples in DataRecord must be a multiple of divider. Number of samples = "
                   + inConfig.getNumberOfSamplesInEachDataRecord(signalNumber)
                   + " Divider = " + divider;
           throw new IllegalArgumentException(errMsg);
        }
        dividers.put(signalNumber, divider);
        if(inConfig != null) {
            outRecordSize = calculateOutRecordSize();
            outStream.setHeader(getOutConfig());
        }
    }

    private int calculateOutRecordSize() {
        int outRecordSize = 0;

        for (int i = 0; i < inConfig.numberOfSignals(); i++) {
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
    public DataHeader getOutConfig() {
        DataHeader outConfig = new DataHeader(inConfig);
        for (int i = 0; i < outConfig.numberOfSignals(); i++) {
            Integer divider = dividers.get(i);
            if(divider != null) {
                int numberOfSamples = outConfig.getNumberOfSamplesInEachDataRecord(i) / divider;
                outConfig.setNumberOfSamplesInEachDataRecord(i, numberOfSamples);
            }
        }
        return outConfig;
    }

    @Override
    public void writeDataRecord(int[] inputRecord) {
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
        outStream.writeDataRecord(outRecord);
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 4 samples, 1 channel 2 samples, 2 channel 6 samples
        int[] dataRecord = {1,3,8,4,  2,4,  5,7,6,8,6,0};

        DataHeader dataConfig = new DataHeader(FormatVersion.BDF_24BIT, 3);
        dataConfig.setNumberOfSamplesInEachDataRecord(0, 4);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 2);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 6);


        // reduce signals frequencies by 4, 2, 2

        // expected dataRecord
        int[] expectedDataRecord = {4,  3,  6,7,3};

        SignalFrequencyReducer recordFilter = new SignalFrequencyReducer(new TestStream(expectedDataRecord));
        recordFilter.addDivider(0, 4);
        recordFilter.addDivider(1, 2);
        recordFilter.addDivider(2, 2);

        recordFilter.setHeader(dataConfig);

        recordFilter.writeDataRecord(dataRecord);
    }
}
