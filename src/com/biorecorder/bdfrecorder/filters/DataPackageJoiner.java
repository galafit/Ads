package com.biorecorder.bdfrecorder.filters;

import com.biorecorder.bdfrecorder.dataformat.DataConfig;
import com.biorecorder.bdfrecorder.dataformat.DataProducer;

/**
 * Permits to join (piece together) given number of incoming DataRecords.
 * Resultant  data records (that will be send to the listener)
 * have the following structure:
 * <br>  number of samples from channel_0 in original DataRecord * numberOfRecordsToJoin ,
 * <br>  number of samples from channel_1 in original DataRecord * numberOfRecordsToJoin,
 * <br>  ...
 * <br>  number of samples from channel_i in original DataRecord * numberOfRecordsToJoin
 * <p>
 *
 * <br>duration of resulting DataRecord = duration of original DataRecord * numberOfRecordsToJoin
 */
public class DataPackageJoiner extends DataPackageFilter {
    private int numberOfRecordsToJoin;
    private int[] resultantDataRecord;
    private int joinedRecordsCounter;
    private int inputRecordSize;
    private int resultantRecordSize;

    public DataPackageJoiner(DataProducer input, int numberOfRecordsToJoin) {
        super(input);
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        for (int i = 0; i < input.edfConfig().signalsCount(); i++) {
            inputRecordSize += input.edfConfig().numberOfSamplesInEachDataRecord(i);
        }
        resultantRecordSize = inputRecordSize * numberOfRecordsToJoin;
        resultantDataRecord = new int[resultantRecordSize];
    }



    @Override
    public DataConfig edfConfig() {
        return new DataConfigWrapper(input.edfConfig()) {
            @Override
            public int signalsCount() {
                return innerConfig.signalsCount();
            }

            @Override
            public double durationOfDataRecord() {
                return innerConfig.durationOfDataRecord() * numberOfRecordsToJoin;
            }

            @Override
            public int numberOfSamplesInEachDataRecord(int signalNumber) {
                return innerConfig.numberOfSamplesInEachDataRecord(signalNumber) * numberOfRecordsToJoin;
            }
        };
    }

    private int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return input.edfConfig().numberOfSamplesInEachDataRecord(signalNumber) * numberOfRecordsToJoin;
    }

    /**
     * Accumulate and join the specified number of incoming samples into one resultant
     * DataRecord and when it is ready send it to the dataListener
     */
    @Override
    protected void filterData(int[] inputRecord)  {
        for (int inSamplePosition = 0; inSamplePosition < inputRecordSize; inSamplePosition++) {
            int counter = 0;
            int channelNumber = 0;
            while (inSamplePosition >= counter + getNumberOfSamplesInEachDataRecord(channelNumber)) {
                counter += getNumberOfSamplesInEachDataRecord(channelNumber);
                channelNumber++;
            }

            int outSamplePosition = counter * numberOfRecordsToJoin;
            outSamplePosition += joinedRecordsCounter * getNumberOfSamplesInEachDataRecord(channelNumber);
            outSamplePosition += inSamplePosition - counter;

            resultantDataRecord[outSamplePosition] = inputRecord[inSamplePosition];
        }

        joinedRecordsCounter++;

        if(joinedRecordsCounter == numberOfRecordsToJoin) {
            dataListener.onDataReceived(resultantDataRecord);
            resultantDataRecord = new int[resultantRecordSize];
            joinedRecordsCounter = 0;
        }
    }
}
