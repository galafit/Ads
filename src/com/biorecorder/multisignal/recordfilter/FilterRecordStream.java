package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.multisignal.recordformat.RecordsHeader;
import com.biorecorder.multisignal.recordformat.RecordsStream;

/**
 * FilterRecordStream is just a wrapper of an already existing
 * RecordStream (the underlying stream)
 * which do some transforms with input data records before
 * to write them to the underlying stream.
 */
public class FilterRecordStream implements RecordsStream {
    protected RecordsHeader inConfig;
    protected int inRecordSize;
    protected RecordsStream outStream;

    public FilterRecordStream(RecordsStream outStream) {
        this.outStream = outStream;
    }


    public RecordsHeader getResultantConfig(){
        if(outStream instanceof FilterRecordStream) {
            return ((FilterRecordStream) outStream).getResultantConfig();
        } else {
            return getOutConfig();
        }
    }

    @Override
    public void setHeader(RecordsHeader header) {
        this.inConfig = header;
        inRecordSize = 0;
        for (int i = 0; i < header.numberOfSignals(); i++) {
            inRecordSize += header.getNumberOfSamplesInEachDataRecord(i);
        }
        outStream.setHeader(getOutConfig());
    }

    @Override
    public void writeRecord(int[] dataRecord) {
        outStream.writeRecord(dataRecord);
    }

    @Override
    public void close() {
        outStream.close();
    }

    protected RecordsHeader getOutConfig() {
        return inConfig;
    }
}
