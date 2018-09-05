package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordSender;

/**
 * Created by galafit on 5/9/18.
 */
public class TestFilter extends RecordsFilter {
    public TestFilter(RecordSender in) {
        super(in);
    }

    @Override
    public RecordConfig dataConfig() {
        return in.dataConfig();
    }

    @Override
    protected void filterData(int[] inputRecord) {
        int outRecord[] = new int[inputRecord.length];
        for (int i = 0; i < inputRecord.length; i++) {
            inputRecord[i] = i * 5;
            outRecord[i] = i * 4;
        }
        sendDataToListeners(outRecord);
    }
}
