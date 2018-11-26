package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.RecordConfig;
import com.biorecorder.multisignal.recordformat.RecordStream;

/**
 * FilterRecordStream is just a wrapper of an already existing
 * RecordStream (the underlying stream)
 * which do some transforms with input data records before
 * to write them to the underlying stream.
 */
public class FilterRecordStream implements RecordStream {
    protected RecordConfig inConfig;
    protected int inRecordSize;
    protected RecordStream outStream;

    public FilterRecordStream(RecordStream outStream) {
        this.outStream = outStream;
    }


    public RecordConfig getResultantConfig(){
        if(outStream instanceof FilterRecordStream) {
            return ((FilterRecordStream) outStream).getResultantConfig();
        } else {
            return getOutConfig();
        }
    }

    @Override
    public void setRecordConfig(RecordConfig inConfig) {
        this.inConfig = inConfig;
        inRecordSize = 0;
        for (int i = 0; i < inConfig.signalsCount(); i++) {
            inRecordSize += inConfig.getNumberOfSamplesInEachDataRecord(i);
        }
        outStream.setRecordConfig(getOutConfig());
    }

    @Override
    public void writeRecord(int[] dataRecord) {
        outStream.writeRecord(dataRecord);
    }

    @Override
    public void close() {
        outStream.close();
    }

    protected RecordConfig getOutConfig() {
        return inConfig;
    }
}
