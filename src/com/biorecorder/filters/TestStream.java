package com.biorecorder.filters;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordStream;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by galafit on 3/10/18.
 */
public class TestStream implements RecordStream {
    List<int[]> expectedRecords;
    int count;

    public TestStream(List<int[]> expectedRecords) {
        this.expectedRecords = expectedRecords;
    }

    public TestStream(int[] expectedRecord) {
        expectedRecords = new ArrayList<>(1);
        expectedRecords.add(expectedRecord);
    }

    @Override
    public void writeRecord(int[] dataRecord) {
        boolean isTestOk = true;
        int[] expectedRecord = expectedRecords.get(count);
        if(expectedRecord.length != dataRecord.length) {
            System.out.println("Error!!! Resultant record length: "+dataRecord.length+ " Expected record length : "+expectedRecord.length);
            isTestOk = false;
        }
        for (int i = 0; i < dataRecord.length; i++) {
            if(dataRecord[i] != expectedRecord[i]) {
                System.out.println(i + " resultant data: "+dataRecord[i]+ " expected data: "+expectedRecord[i]);
                isTestOk = false;
                break;
            }
        }
        System.out.println("Is test ok: "+isTestOk);
        if(count < expectedRecords.size() - 1) {
            count++;
        }
    }

    @Override
    public void setRecordConfig(RecordConfig recordConfig) {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }
}
