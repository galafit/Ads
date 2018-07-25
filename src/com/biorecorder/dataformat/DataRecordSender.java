package com.biorecorder.dataformat;


/**
 *  Data sender sends data records to all its subscribers
 *  and provides DataRecordConfig object describing the structure of
 *  sending data records. This interface actually just an example
 *  of possible "realization"
 */
public interface DataRecordSender {
    public DataRecordConfig dataConfig();

    public void addDataListener(DataRecordListener dataRecordListener);

    public void removeDataListener(DataRecordListener dataRecordListener);

}
