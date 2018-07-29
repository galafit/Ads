package com.biorecorder.recorder;

/**
 * Created by galafit on 28/7/18.
 */
public interface RecordStream {
    void writeRecord(int[] dataRecord);
    void close();
}
