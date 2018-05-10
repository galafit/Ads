package com.biorecorder.bdfrecorder;

/**
 * Created by galafit on 9/5/18.
 */
public abstract class BdfFilter implements BdfDataProducer {
    protected BdfDataProducer input;
    protected BdfDataListener dataListener;

    public BdfFilter(BdfDataProducer input) {
        this.input = input;
        dataListener = new NullBdfDataListener();
    }

    @Override
    public void setDataListener(BdfDataListener dataListener) {
        this.dataListener = dataListener;
        input.setDataListener(new BdfDataListener() {
            @Override
            public void onDataRecordReceived(int[] dataRecord) {
                filterData(dataRecord);
            }
        });
    }

    @Override
    public void removeDataListener() {
        input.removeDataListener();
        dataListener = new NullBdfDataListener();
    }

    protected abstract void filterData(int[] inputRecord);

    public static int recordLength(BdfDataProducer dataProducer) {
        int totalNumberOfSamplesInRecord = 0;
        for (int i = 0; i < dataProducer.getSignalsCount(); i++) {
            totalNumberOfSamplesInRecord += dataProducer.getNumberOfSamplesInEachDataRecord(i);
        }
        return totalNumberOfSamplesInRecord;
    }
}
