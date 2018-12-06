package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.DataHeader;
import com.biorecorder.multisignal.recordformat.DataRecordStream;
import com.biorecorder.multisignal.recordformat.FormatVersion;

/**
 * Permits to join (piece together) given number of incoming DataRecords.
 * out  data records (that will be send to the listener)
 * have the following structure:
 * <br>  number of samples from channel_0 in original DataRecord * numberOfRecordsToJoin ,
 * <br>  number of samples from channel_1 in original DataRecord * numberOfRecordsToJoin,
 * <br>  ...
 * <br>  number of samples from channel_i in original DataRecord * numberOfRecordsToJoin
 * <p>
 *
 * <br>duration of resulting DataRecord = duration of original DataRecord * numberOfRecordsToJoin
 */
public class RecordsJoiner extends FilterRecordStream {
    private int numberOfRecordsToJoin;
    private int[] outDataRecord;
    private int joinedRecordsCounter;
    private int outRecordSize;

    public RecordsJoiner(DataRecordStream outStream, int numberOfRecordsToJoin) {
        super(outStream);
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
    }


    @Override
    public void setHeader(DataHeader header) {
        super.setHeader(header);
        outRecordSize = inRecordSize * numberOfRecordsToJoin;
        outDataRecord = new int[outRecordSize];
    }

    @Override
    public DataHeader getOutConfig() {
        DataHeader outConfig = new DataHeader(inConfig);
        outConfig.setDurationOfDataRecord(inConfig.getDurationOfDataRecord() * numberOfRecordsToJoin);
        for (int i = 0; i < outConfig.numberOfSignals(); i++) {
            outConfig.setNumberOfSamplesInEachDataRecord(i, inConfig.getNumberOfSamplesInEachDataRecord(i) * numberOfRecordsToJoin);
        }
        return outConfig;
    }


    /**
     * Accumulate and join the specified number of incoming samples into one out
     * DataRecord and when it is ready send it to the dataListener
     */
    @Override
    public void writeDataRecord(int[] inputRecord)  {
        int signalNumber = 0;
        int signalStart = 0;
        int signalSamples = inConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
        for (int inSamplePosition = 0; inSamplePosition < inRecordSize; inSamplePosition++) {

            if(inSamplePosition >= signalStart + signalSamples) {
                signalStart += signalSamples;
                signalNumber++;
                signalSamples = inConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
            }

            int outSamplePosition = signalStart * numberOfRecordsToJoin;
            outSamplePosition += joinedRecordsCounter * signalSamples;
            outSamplePosition += inSamplePosition - signalStart;

            outDataRecord[outSamplePosition] = inputRecord[inSamplePosition];
        }

        joinedRecordsCounter++;

        if(joinedRecordsCounter == numberOfRecordsToJoin) {
            outStream.writeDataRecord(outDataRecord);
            outDataRecord = new int[outRecordSize];
            joinedRecordsCounter = 0;
        }
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 3 samples, 1 channel 2 samples, 3 channel 4 samples
        int[] dataRecord = {1,3,8,  2,4,  7,6,8,6};

        DataHeader dataConfig = new DataHeader(FormatVersion.BDF_24BIT, 3);
        dataConfig.setNumberOfSamplesInEachDataRecord(0, 3);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 2);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 4);

        // join 2 records
        int numberOfRecordsToJoin = 2;
        // expected dataRecord
        int[] expectedDataRecord = {1,3,8,1,3,8,  2,4,2,4,  7,6,8,6,7,6,8,6};

        RecordsJoiner recordFilter = new RecordsJoiner(new TestStream(expectedDataRecord), numberOfRecordsToJoin);

        recordFilter.setHeader(dataConfig);

        // send 4 records and get as result 2 joined records
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
    }
}
