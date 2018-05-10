package com.biorecorder.bdfrecorder;

/**
 * Created by galafit on 9/5/18.
 */
public interface BdfDataProducer {

    public int getSignalsCount();

    public double getDurationOfDataRecord();

    /**
     * Gets the number of samples belonging to the signal
     * in each DataRecord (data package).
     * When duration of DataRecords = 1 sec (default):
     * NumberOfSamplesInEachDataRecord = sampleFrequency
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return number of samples belonging to the signal with the given sampleNumberToSignalNumber
     * in each DataRecord (data package)
     */
    public int getNumberOfSamplesInEachDataRecord(int signalNumber);

    public void setDataListener(BdfDataListener bdfDataListener);

    public void removeDataListener();
}
