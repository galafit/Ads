package com.biorecorder.bdfrecorder;

/**
 * Permits to join (piece together) given number of incoming DataRecords.
 * Resultant  DataRecords (that will be send to the listener)
 * have the following structure:
 * <br>  number of samples from channel_0 in original DataRecord * numberOfRecordsToJoin ,
 * <br>  number of samples from channel_1 in original DataRecord * numberOfRecordsToJoin,
 * <br>  ...
 * <br>  number of samples from channel_i in original DataRecord * numberOfRecordsToJoin
 * <p>
 *
 * <br>duration of resulting DataRecord = duration of original DataRecord * numberOfRecordsToJoin
 */
public class BdfJoiner extends BdfFilter {
    private int numberOfRecordsToJoin;
    private int[] resultantDataRecord;
    private int joinedRecordsCounter;

    public BdfJoiner(BdfDataProducer input, int numberOfRecordsToJoin) {
        super(input);
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        resultantDataRecord = new int[recordLength(input) * numberOfRecordsToJoin];
    }

    @Override
    public int getSignalsCount() {
        return input.getSignalsCount();
    }

    @Override
    public double getDurationOfDataRecord() {
        return input.getDurationOfDataRecord() * numberOfRecordsToJoin;
    }

    @Override
    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return input.getNumberOfSamplesInEachDataRecord(signalNumber) * numberOfRecordsToJoin;
    }

    /**
     * Accumulate and join the specified number of incoming samples into one resultant
     * DataRecord and when it is ready send it to the dataListener
     */
    @Override
    protected void filterData(int[] inputRecord)  {
        int inputRecordLength = recordLength(input);
        for (int inSamplePosition = 0; inSamplePosition < inputRecordLength; inSamplePosition++) {
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
            dataListener.onDataRecordReceived(resultantDataRecord);
            resultantDataRecord = new int[inputRecordLength * numberOfRecordsToJoin];
            joinedRecordsCounter = 0;
        }
    }
}
