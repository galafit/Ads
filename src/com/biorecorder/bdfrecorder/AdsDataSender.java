package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.Ads;
import com.biorecorder.ads.AdsConfig;
import com.biorecorder.ads.Commutator;
import com.biorecorder.ads.NumberedDataListener;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;
import com.biorecorder.dataformat.NullDataListener;

import java.util.concurrent.Future;


/**
 * Helper class that convert Ads to DataSender for further data records
 * filtering and transformation
 * In future this class will realise detach (separate)
 * the receiving and sending of data records in different threads
 * through the concurrent queue
 */
class AdsDataSender implements DataSender {
    private final Ads ads;
    private final AdsConfig adsConfig;
    private int lastDataRecordNumber = -1;
    private volatile DataListener dataListener = new NullDataListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();
    private volatile Long startRecordingTime;


    public AdsDataSender(Ads ads, AdsConfig adsConfig) {
        this.ads = ads;
        this.adsConfig = adsConfig;
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
                if(recordNumber == 0) {
                    startRecordingTime = System.currentTimeMillis() - (long)(adsConfig.getDurationOfDataRecord() * 1000);
                }
                //long time_start = System.currentTimeMillis();
                notifyDataListeners(dataRecord);
               /* long time_end = System.currentTimeMillis();
                long delta = (time_end - time_start);
                if(delta > 0) {
                    System.out.println("time "+ delta);
                }*/

                int numberOfLostFrames = recordNumber - lastDataRecordNumber - 1;
                for (int i = 0; i < numberOfLostFrames; i++) {
                    notifyDataListeners(dataRecord);
                }
                lastDataRecordNumber++;

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

    /**
     * Gets the time of starting measuring the fist data record
     * or null if no data record was received
     * @return time  = time of the first received data record - time of measuring
     */
    public Long getStartRecordingTime() {
        return startRecordingTime;
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

}
