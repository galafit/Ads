package com.biorecorder.dataformat;

/**
 * Created by galafit on 5/9/18.
 */
public class DefaultRecordSender implements RecordSender {
    RecordListener listener = new NullRecordListener();
    RecordConfig config;

    public DefaultRecordSender(RecordConfig config) {
        this.config = config;
    }

    public void sendRecord(int[] dataRecord) {
        listener.onDataReceived(dataRecord);
    }

    @Override
    public RecordConfig dataConfig() {
        return config;
    }

    @Override
    public void addDataListener(RecordListener dataRecordListener) {
        listener = dataRecordListener;
    }

    @Override
    public void removeDataListener(RecordListener dataRecordListener) {
        removeDataListener();
    }

    public void removeDataListener() {
        listener = new NullRecordListener();
    }
}
