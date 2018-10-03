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
    protected final RecordConfig inConfig;
    protected final int inRecordSize;
    protected volatile RecordStream outStream;

    public RecordFilter(RecordConfig inConfig) {
        this.inConfig = inConfig;
        int recordSize = 0;
        for (int i = 0; i < inConfig.signalsCount(); i++) {
            recordSize += inConfig.getNumberOfSamplesInEachDataRecord(i);
        }
        inRecordSize = recordSize;
        outStream = new NullStream();
    }


    public void setOutStream(RecordStream outStream) {
        if(outStream != null) {
            this.outStream = outStream;
        }
    }

    public void removeOutStream() {
        outStream = new NullStream();
    }

    @Override
    public void writeRecord(int[] dataRecord) {
        filterData(dataRecord);
    }

    @Override
    public void close() {
        outStream.close();
    }

    public RecordConfig dataConfig() {
        return inConfig;
    }

    protected void filterData(int[] inputRecord) {
       outStream.writeRecord(inputRecord);
    }

    class NullStream implements RecordStream {
        @Override
        public void writeRecord(int[] dataRecord) {
            // do nothing;
        }

        @Override
        public void close() {
            // do nothing
        }
    }
}
