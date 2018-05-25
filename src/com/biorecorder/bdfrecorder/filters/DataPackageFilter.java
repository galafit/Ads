package com.biorecorder.bdfrecorder.filters;

import com.biorecorder.bdfrecorder.dataformat.DataListener;
import com.biorecorder.bdfrecorder.dataformat.DataProducer;

/**
 * Created by galafit on 9/5/18.
 */
public abstract class DataPackageFilter implements DataProducer {
    protected DataProducer input;
    protected DataListener dataListener;

    public DataPackageFilter(DataProducer input) {
        this.input = input;
        dataListener = new NullçDataListener();
    }

    @Override
    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
        input.setDataListener(new DataListener() {
            @Override
            public void onDataReceived(int[] dataRecord) {
                filterData(dataRecord);
            }
        });
    }

    @Override
    public void removeDataListener() {
        input.removeDataListener();
        dataListener = new NullçDataListener();
    }

    protected abstract void filterData(int[] inputRecord);


    class NullçDataListener implements DataListener {
        @Override
        public void onDataReceived(int[] dataRecord) {
            // do nothing;
        }
    }
}
