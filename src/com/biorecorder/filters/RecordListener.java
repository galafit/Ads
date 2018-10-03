package com.biorecorder.filters;

/**
 * The listener interface for receiving data records.
 * The class that is interested in receiving and processing
 * data records must implement this interface and subscribe to
 * a "data sender"
 */
public interface RecordListener {
    public void writeRecord(int[] dataRecord);
}
