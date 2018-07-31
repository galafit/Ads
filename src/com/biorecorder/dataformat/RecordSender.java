package com.biorecorder.dataformat;


/**
 *  Data sender sends data records to all its subscribers
 *  and provides RecordConfig object describing the structure of
 *  sending data records. This interface actually just an example
 *  of possible "realization"
 */
public interface RecordSender {
    public RecordConfig dataConfig();

    public void addDataListener(RecordListener dataRecordListener);

    public void removeDataListener(RecordListener dataRecordListener);

}
