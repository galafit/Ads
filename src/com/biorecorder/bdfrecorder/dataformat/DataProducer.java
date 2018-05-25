package com.biorecorder.bdfrecorder.dataformat;

/**
 * Created by galafit on 9/5/18.
 */
public interface DataProducer {
    public DataConfig edfConfig();

    public void setDataListener(DataListener dataListener);

    public void removeDataListener();
}
