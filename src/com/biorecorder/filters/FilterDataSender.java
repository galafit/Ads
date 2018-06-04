package com.biorecorder.filters;

import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;

/**
 * A FilterDataSender listen (wrap) some other DataSender and transforms
 * receiving data records before
 * to send it to its listeners.
 *
 * Thread safe realization only with ONE!!! listener.
 * It is easy and fast. At the moment we do not need more listeners
 * <p>
 * PS How to implement thread safe classical observer pattern with multiple listeners
 * see here: https://www.techyourchance.com/thread-safe-observer-design-pattern-in-java/
 */
public abstract class FilterDataSender implements DataSender, DataListener {
    protected final DataSender in;
    private volatile DataListener listener;

    public FilterDataSender(DataSender in) {
        this.in = in;
        listener = new NullDataListener();
    }

    @Override
    public void addDataListener(DataListener dataListener) {
        if(dataListener != null) {
            this.listener = dataListener;
            in.addDataListener(this);
        }
    }

    @Override
    public void removeDataListener(DataListener dataListener) {
        removeDataListener();
    }

    public void removeDataListener() {
        listener = new NullDataListener();
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

    class NullDataListener implements DataListener {
        @Override
        public void onDataReceived(int[] dataRecord) {
            // do nothing;
        }
    }
}
