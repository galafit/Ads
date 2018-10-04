package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordStream;

/**
 * A RecordsFilter listen (wrap) some other RecordSender and transforms
 * receiving data records before
 * to send it to its listeners.
 *
 * Thread safe realization only with ONE!!! listener.
 * It is easy and fast. At the moment we do not need more listeners
 * <p>
 * PS How to implement thread safe classical observer pattern with multiple listeners
 * see here: https://www.techyourchance.com/thread-safe-observer-design-pattern-in-java/
 */
public class RecordFilter implements RecordStream {
    protected RecordConfig inConfig;
    protected int inRecordSize;
    protected RecordStream outStream;

    public RecordFilter(RecordStream outStream) {
        this.outStream = outStream;
    }


    public RecordConfig getResultantConfig(){
        if(outStream instanceof RecordFilter) {
            return ((RecordFilter) outStream).getResultantConfig();
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
