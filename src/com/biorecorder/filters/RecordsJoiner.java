package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordSender;
import com.biorecorder.dataformat.DefaultRecordConfig;

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
public class RecordsJoiner extends RecordsFilter {
    private int numberOfRecordsToJoin;
    private int[] outDataRecord;
    private int joinedRecordsCounter;
    private int inRecordSize;
    private int outRecordSize;

    public RecordsJoiner(RecordSender in, int numberOfRecordsToJoin) {
        super(in);
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        for (int i = 0; i < inConfig.signalsCount(); i++) {
            inRecordSize += inConfig.getNumberOfSamplesInEachDataRecord(i);
         }
        outRecordSize = inRecordSize * numberOfRecordsToJoin;
        outDataRecord = new int[outRecordSize];
    }



    @Override
    public RecordConfig dataConfig() {
        DefaultRecordConfig resultantConfig = new DefaultRecordConfig(inConfig);
        resultantConfig.setDurationOfDataRecord(inConfig.getDurationOfDataRecord() * numberOfRecordsToJoin);
        for (int i = 0; i < resultantConfig.signalsCount(); i++) {
            resultantConfig.setNumberOfSamplesInEachDataRecord(i, inConfig.getNumberOfSamplesInEachDataRecord(i) * numberOfRecordsToJoin);
        }
        return resultantConfig;
    }


    /**
     * Accumulate and join the specified number of incoming samples into one resultant
     * DataRecord and when it is ready send it to the dataListener
     */
    @Override
    protected void filterData(int[] inputRecord)  {
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
            sendDataToListeners(outDataRecord);
            outDataRecord = new int[outRecordSize];
            joinedRecordsCounter = 0;
        }
    }
}
