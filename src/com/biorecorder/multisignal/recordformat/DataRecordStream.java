package com.biorecorder.multisignal.recordformat;

/**
 * Created by galafit on 28/7/18.
 */
public interface DataRecordStream {
    void setHeader(DataHeader header);
    void writeDataRecord(int[] dataRecord);
    void close();
}

