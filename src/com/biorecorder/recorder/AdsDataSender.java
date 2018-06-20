package com.biorecorder.recorder;

import com.biorecorder.ads.Ads;
import com.biorecorder.ads.AdsConfig;
import com.biorecorder.ads.Commutator;
import com.biorecorder.ads.NumberedDataListener;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;
import com.biorecorder.dataformat.NullDataListener;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * Helper class that convert Ads to DataSender for further data records
 * filtering and transformation
 * In future this class will realise detach (separate)
 * the receiving and sending of data records in different threads
 * through the concurrent queue
 */
class AdsDataSender implements DataSender {
    private final LinkedBlockingQueue<NumberedDataRecord> dataQueue = new LinkedBlockingQueue<>();
    private Thread dataHandlingThread;

    private final Ads ads;
    private final AdsConfig adsConfig;
    private int lastDataRecordNumber = -1;
    private volatile DataListener dataListener = new NullDataListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();

    private volatile long firstRecordTime;
    private volatile long lastRecordTime;
    private volatile double durationOfDataRecord; // sec


    public AdsDataSender(Ads ads, AdsConfig adsConfig) {
        this.ads = ads;
        this.adsConfig = adsConfig;
        durationOfDataRecord = adsConfig.getDurationOfDataRecord();
        dataHandlingThread = new Thread("«Data Records handling» thread") {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        // block until a request arrives
                        NumberedDataRecord numberedDataRecord = dataQueue.take();

                        // send to listener
                        notifyDataListeners(numberedDataRecord.getRecord());
                        int numberOfLostFrames = numberedDataRecord.getRecordNumber() - lastDataRecordNumber - 1;
                        for (int i = 0; i < numberOfLostFrames; i++) {
                            notifyDataListeners(numberedDataRecord.getRecord());
                        }
                        lastDataRecordNumber = numberedDataRecord.getRecordNumber();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // stop
                        break;
                    }
                }
            }
        };
        dataHandlingThread.start();
    }

    /**
     * Gets the start measuring time (time of starting measuring the fist data record) =
     * time of the first received data record - duration of data record
     * @return start measuring time
     */
    public long getStartMeasuringTime() {
        return firstRecordTime - (long)(durationOfDataRecord * 1000);
    }

    /**
     * Gets the calculated duration of data records = (lastDataRecordTime - firstDataRecordTime) / number of received data records
     * @return calculated duration of data records
     */
    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }

    /**
     * this method MUST be called to finalize data handling thread
     */
    public void finalize() {
       dataHandlingThread.interrupt();
    }

    @Override
    public DataConfig dataConfig() {
        return ads.getDataConfig(adsConfig);
    }

    @Override
    public void addDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
        ads.addDataListener(new NumberedDataListener() {
            @Override
            public void onDataReceived(int[] dataRecord, int recordNumber) {
                try {
                    if(recordNumber == 0) {
                        firstRecordTime = System.currentTimeMillis();
                        lastRecordTime = firstRecordTime;
                    } else {
                        lastRecordTime = System.currentTimeMillis();
                    }
                    if (recordNumber > 0) {
                        durationOfDataRecord = (lastRecordTime - firstRecordTime)  /  (recordNumber * 1000.0);
                    }
                    dataQueue.put(new NumberedDataRecord(dataRecord, recordNumber));
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
                if(adsConfig.isBatteryVoltageMeasureEnabled()) {
                    notifyBatteryLevelListener(Ads.batteryIntToPercentage(batteryCharge));
                }
            }
        });
    }

    @Override
    public void removeDataListener(DataListener dataListener) {
        this.dataListener = new NullDataListener();
        ads.removeDataListener();
    }


    public void addLeadOffListener(LeadOffListener listener) {
        leadOffListener = listener;
    }

    public void addButteryLevelListener(BatteryLevelListener listener) {
        batteryListener = listener;
    }

    private void notifyDataListeners(int[] dataRecord) {
        dataListener.onDataReceived(dataRecord);
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
