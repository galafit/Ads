package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.DataHeader;
import com.biorecorder.multisignal.recordformat.DataRecordStream;

/**
 * FilterRecordStream is just a wrapper of an already existing
 * RecordStream (the underlying stream)
 * which do some transforms with input data records before
 * to write them to the underlying stream.
 */
public class FilterRecordStream implements DataRecordStream {
    protected DataHeader inConfig;
    protected int inRecordSize;
    protected DataRecordStream outStream;

    public FilterRecordStream(DataRecordStream outStream) {
        this.outStream = outStream;
    }


    public DataHeader getResultantConfig(){
        if(outStream instanceof FilterRecordStream) {
            return ((FilterRecordStream) outStream).getResultantConfig();
        } else {
            return getOutConfig();
        }
    }

    @Override
    public void setHeader(DataHeader header) {
        this.inConfig = header;
        inRecordSize = 0;
        for (int i = 0; i < header.numberOfSignals(); i++) {
            inRecordSize += header.getNumberOfSamplesInEachDataRecord(i);
        }
        outStream.setHeader(getOutConfig());
    }

    @Override
    public void writeDataRecord(int[] dataRecord) {
        outStream.writeDataRecord(dataRecord);
    }

    @Override
    public void close() {
        outStream.close();
    }

    protected DataHeader getOutConfig() {
        return inConfig;
    }
}
