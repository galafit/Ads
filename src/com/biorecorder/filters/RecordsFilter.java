package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordListener;
import com.biorecorder.dataformat.RecordSender;

/**
 * A RecordsFilter listen (wrap) some other RecordSender and transforms
 * receiving data records before
 * to send it to its listeners.
 *
 * Thread safe realization only with ONE!!! listener.
 * It is easy and fast. At the moment we do not need more listeners
 * <p>
 * PS How to implement thread safe classical observer pattern with multiple listeners
 * see here: https://www.techyourchance.com/thread-safe-observer-design-pattern-in-java/
 */
public abstract class RecordsFilter implements RecordSender, RecordListener {
    protected final RecordSender in;
    private volatile RecordListener listener;

    public RecordsFilter(RecordSender in) {
        this.in = in;
        listener = new NullRecordListener();
    }

    @Override
    public void addDataListener(RecordListener dataRecordListener) {
        if(dataRecordListener != null) {
            this.listener = dataRecordListener;
            in.addDataListener(this);
        }
    }

    @Override
    public void removeDataListener(RecordListener dataRecordListener) {
        removeDataListener();
    }

    public void removeDataListener() {
        listener = new NullRecordListener();
        in.removeDataListener(this);
    }

    @Override
    public void onDataReceived(int[] dataRecord) {
        filterData(dataRecord);
    }

    protected abstract void filterData(int[] inputRecord);

    protected void sendDataToListeners(int[] dataRecord) {
        listener.onDataReceived(dataRecord);
    }

    class NullRecordListener implements RecordListener {
        @Override
        public void onDataReceived(int[] dataRecord) {
            // do nothing;
        }
    }
}
