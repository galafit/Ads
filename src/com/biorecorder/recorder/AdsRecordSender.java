package com.biorecorder.recorder;

import com.biorecorder.ads.Ads;
import com.biorecorder.ads.AdsConfig;
import com.biorecorder.ads.Commutator;
import com.biorecorder.ads.NumberedDataListener;
import com.biorecorder.dataformat.*;
import com.biorecorder.dataformat.RecordListener;
import com.biorecorder.dataformat.NullRecordListener;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This class:
 * <br>1) implement {@link RecordSender} interface for further data records filtering and transformation
 * <br>2) convert numbered data records to simple data records (supplementing the lost ones) and
 * send them to the listener in separated thread
 * <br>3) extract lead off info and battery charge info from data records and send it to the
 * corresponding listeners
 */
class AdsRecordSender implements RecordSender {
    private final LinkedBlockingQueue<NumberedDataRecord> dataQueue = new LinkedBlockingQueue<>();
    private Thread dataHandlingThread;

    private final Ads ads;
    private final AdsConfig adsConfig;
    private int lastDataRecordNumber = -1;
    private volatile RecordListener dataRecordListener = new NullRecordListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();

    private volatile long firstRecordTime;
    private volatile long lastRecordTime;
    private volatile double calculatedDurationOfDataRecord; // sec


    public AdsRecordSender(Ads ads, AdsConfig adsConfig) {
        this.ads = ads;
        this.adsConfig = adsConfig;
        calculatedDurationOfDataRecord = adsConfig.getDurationOfDataRecord();
    }

    public Future<Boolean> startRecording() throws IllegalStateException, IllegalArgumentException {
        dataHandlingThread = new Thread("«Data Records handling» thread") {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        // block until a request arrives
                        handleData(dataQueue.take());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // stop
                        break;
                    }
                }
            }
        };
         dataHandlingThread.start();
        return ads.startRecording(adsConfig);
    }

    public boolean stop() throws IllegalStateException {
        if(dataHandlingThread != null) {
            dataHandlingThread.interrupt();
        }
        ads.removeDataListener();
        return ads.stop();
    }

    /**
     * Gets the start measuring time (time of starting measuring the fist data record) =
     * time of the first received data record - duration of data record
     *
     * @return start measuring time
     */
    public long getStartMeasuringTime() {
        if(firstRecordTime == 0) {
            return 0;
        }
        return firstRecordTime - (long) (calculatedDurationOfDataRecord * 1000);
    }

    /**
     * Gets the calculated duration of data records = (lastDataRecordTime - firstDataRecordTime) / number of received data records
     *
     * @return calculated duration of data records
     */
    public double calculateDurationOfDataRecord() {
        return calculatedDurationOfDataRecord;
    }

    @Override
    public RecordConfig dataConfig() {
        return ads.getDataConfig(adsConfig);
    }

    @Override
    public void addDataListener(RecordListener dataRecordListener) {
        this.dataRecordListener = dataRecordListener;
        ads.addDataListener(new NumberedDataListener() {
            @Override
            public void onDataReceived(int[] dataRecord, int recordNumber) {
                try {
                    if (recordNumber == 0) {
                        firstRecordTime = System.currentTimeMillis();
                        lastRecordTime = firstRecordTime;
                    } else {
                        lastRecordTime = System.currentTimeMillis();
                    }
                    if (recordNumber > 0) {
                        calculatedDurationOfDataRecord = (lastRecordTime - firstRecordTime) / (recordNumber * 1000.0);
                    }
                    putData(new NumberedDataRecord(dataRecord, recordNumber));
                    //handleData(new NumberedDataRecord(dataRecord, recordNumber));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                int batteryCharge = dataRecord[dataRecord.length - 1];

                if (adsConfig.isLeadOffEnabled()) {
                    batteryCharge = dataRecord[dataRecord.length - 2];

                    boolean[] loffMask = Ads.leadOffIntToBitMask(dataRecord[dataRecord.length - 1], adsConfig.getAdsChannelsCount());
                    Boolean[] resultantLoffMask = new Boolean[loffMask.length];
                    for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                        if (adsConfig.isAdsChannelEnabled(i) && adsConfig.isAdsChannelLeadOffEnable(i)
                                && adsConfig.getAdsChannelCommutatorState(i).equals(Commutator.INPUT)) {
                            resultantLoffMask[2 * i] = loffMask[2 * i];
                            resultantLoffMask[2 * i + 1] = loffMask[2 * i + 1];
                        }

                    }
                    notifyLeadOffListeners(resultantLoffMask);
                }
                if (adsConfig.isBatteryVoltageMeasureEnabled()) {
                    notifyBatteryLevelListener(Ads.lithiumBatteryIntToPercentage(batteryCharge));
                }
            }
        });
    }


    private void putData(NumberedDataRecord numberedDataRecord) throws InterruptedException {
        dataQueue.put(numberedDataRecord);
    }

    private void handleData(NumberedDataRecord numberedDataRecord) throws InterruptedException{
        // send to listener
        notifyDataListeners(numberedDataRecord.getRecord());
        int numberOfLostFrames = numberedDataRecord.getRecordNumber() - lastDataRecordNumber - 1;
        for (int i = 0; i < numberOfLostFrames; i++) {
            notifyDataListeners(numberedDataRecord.getRecord());
        }
        lastDataRecordNumber = numberedDataRecord.getRecordNumber();
    }

    @Override
    public void removeDataListener(RecordListener dataRecordListener) {
        this.dataRecordListener = new NullRecordListener();
        ads.removeDataListener();
    }


    public void addLeadOffListener(LeadOffListener listener) {
        leadOffListener = listener;
    }

    public void addButteryLevelListener(BatteryLevelListener listener) {
        batteryListener = listener;
    }

    private void notifyDataListeners(int[] dataRecord) {
        dataRecordListener.onDataReceived(dataRecord);
    }

    private void notifyBatteryLevelListener(int batteryVoltage) {
        batteryListener.onBatteryLevelReceived(batteryVoltage);
    }

    private void notifyLeadOffListeners(Boolean[] leadOffMask) {
        leadOffListener.onLeadOffMaskReceived(leadOffMask);
    }


    class NullLeadOffListener implements LeadOffListener {
        @Override
        public void onLeadOffMaskReceived(Boolean[] leadOffMask) {
            // do nothing
        }
    }

    class NullBatteryLevelListener implements BatteryLevelListener {
        @Override
        public void onBatteryLevelReceived(int batteryLevel) {
            // do nothing;
        }
    }

    class NumberedDataRecord {
        int[] record;
        int recordNumber;

        public NumberedDataRecord(int[] record, int recordNumber) {
            this.record = record;
            this.recordNumber = recordNumber;
        }

        public int[] getRecord() {
            return record;
        }

        public int getRecordNumber() {
            return recordNumber;
        }
    }
}
