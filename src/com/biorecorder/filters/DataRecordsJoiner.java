package com.biorecorder.filters;

import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataSender;

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
public class DataRecordsJoiner extends FilterDataSender {
    private int numberOfRecordsToJoin;
    private int[] resultantDataRecord;
    private int joinedRecordsCounter;
    private int inRecordSize;
    private int resultantRecordSize;

    public DataRecordsJoiner(DataSender in, int numberOfRecordsToJoin) {
        super(in);
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        for (int i = 0; i < in.dataConfig().signalsCount(); i++) {
            inRecordSize += in.dataConfig().getNumberOfSamplesInEachDataRecord(i);
        }
        resultantRecordSize = inRecordSize * numberOfRecordsToJoin;
        resultantDataRecord = new int[resultantRecordSize];
    }



    @Override
    public DataConfig dataConfig() {
        return new DataConfigWrapper(in.dataConfig()) {
            @Override
            public double getDurationOfDataRecord() {
                return inConfig.getDurationOfDataRecord() * numberOfRecordsToJoin;
            }

            @Override
            public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
                return inConfig.getNumberOfSamplesInEachDataRecord(signalNumber) * numberOfRecordsToJoin;
            }
        };
    }


    /**
     * Accumulate and join the specified number of incoming samples into one resultant
     * DataRecord and when it is ready send it to the dataListener
     */
    @Override
    protected void filterData(int[] inputRecord)  {
        for (int inSamplePosition = 0; inSamplePosition < inRecordSize; inSamplePosition++) {
            int counter = 0;
            int signalNumber = 0;
            int numberOfSamples = in.dataConfig().getNumberOfSamplesInEachDataRecord(0);
            while (inSamplePosition >= counter + numberOfSamples) {
                counter += numberOfSamples;
                signalNumber++;
                numberOfSamples = in.dataConfig().getNumberOfSamplesInEachDataRecord(signalNumber);
            }

            int outSamplePosition = counter * numberOfRecordsToJoin;
            outSamplePosition += joinedRecordsCounter * numberOfSamples;
            outSamplePosition += inSamplePosition - counter;

            resultantDataRecord[outSamplePosition] = inputRecord[inSamplePosition];
        }

        joinedRecordsCounter++;

        if(joinedRecordsCounter == numberOfRecordsToJoin) {
            sendDataToListeners(resultantDataRecord);
            resultantDataRecord = new int[resultantRecordSize];
            joinedRecordsCounter = 0;
        }
    }
}
