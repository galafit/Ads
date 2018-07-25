package com.biorecorder.filters;

import com.biorecorder.dataformat.DataRecordListener;
import com.biorecorder.dataformat.DataRecordSender;

/**
 * A RecordsFilter listen (wrap) some other DataRecordSender and transforms
 * receiving data records before
 * to send it to its listeners.
 *
 * Thread safe realization only with ONE!!! listener.
 * It is easy and fast. At the moment we do not need more listeners
 * <p>
 * PS How to implement thread safe classical observer pattern with multiple listeners
 * see here: https://www.techyourchance.com/thread-safe-observer-design-pattern-in-java/
 */
public abstract class RecordsFilter implements DataRecordSender, DataRecordListener {
    protected final DataRecordSender in;
    private volatile DataRecordListener listener;

    public RecordsFilter(DataRecordSender in) {
        this.in = in;
        listener = new NullDataRecordListener();
    }

    @Override
    public void addDataListener(DataRecordListener dataRecordListener) {
        if(dataRecordListener != null) {
            this.listener = dataRecordListener;
            in.addDataListener(this);
        }
    }

    @Override
    public void removeDataListener(DataRecordListener dataRecordListener) {
        removeDataListener();
    }

    public void removeDataListener() {
        listener = new NullDataRecordListener();
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

    class NullDataRecordListener implements DataRecordListener {
        @Override
        public void onDataReceived(int[] dataRecord) {
            // do nothing;
        }
    }
}
