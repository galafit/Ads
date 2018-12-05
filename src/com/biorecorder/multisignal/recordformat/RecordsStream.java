package com.biorecorder.multisignal.recordformat;

/**
 * Created by galafit on 28/7/18.
 */
public interface RecordsStream {
    void setHeader(RecordsHeader header);
    void writeRecord(int[] dataRecord);
    void close();
}

