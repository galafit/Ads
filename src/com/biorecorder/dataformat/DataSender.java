package com.biorecorder.dataformat;


/**
 *  Data sender sends data records to all its subscribers
 *  and provides DataConfig object describing the structure of
 *  sending data records. This interface actually just an example
 *  of possible "realization"
 */
public interface DataSender {
    public DataConfig dataConfig();

    public void addDataListener(DataListener dataListener);

    public void removeDataListener(DataListener dataListener);

}
