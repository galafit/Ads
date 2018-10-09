package com.biorecorder.recorder;

/**
 * Created by galafit on 9/10/18.
 */
public class RecordingInfo {
    private final long startRecordingTime;
    private final double durationOfDataRecord;

    public RecordingInfo(long startRecordingTime, double durationOfDataRecord) {
        this.startRecordingTime = startRecordingTime;
        this.durationOfDataRecord = durationOfDataRecord;
    }

    public long getStartRecordingTime() {
        return startRecordingTime;
    }

    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }
}
