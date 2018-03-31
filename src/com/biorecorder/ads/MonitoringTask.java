package com.biorecorder.ads;

import com.sun.istack.internal.Nullable;

import java.util.TimerTask;

/**
 * Monitors if ads active
 */
class MonitoringTask extends TimerTask implements MessageListener, AdsDataListener {
    private boolean isMessageReceived = false;
    private boolean isDataReceived = false;
    private final AdsState adsState;
    private AdsConfig adsConfig;

    public MonitoringTask(AdsState adsState, @Nullable AdsConfig adsConfig) {
        this.adsState = adsState;
        this.adsConfig = adsConfig;
    }

    @Override
    public void onMessageReceived(AdsMessage adsMessage, String additionalInfo) {
        if(!isMessageReceived) {
            isMessageReceived = true;
            adsState.setActive(true);
        }
        if (adsMessage == AdsMessage.STOP_RECORDING) {
            adsState.setStoped(true);
        }
        if (adsMessage == AdsMessage.ADS_2_CHANNELS) {
            adsState.setAdsType(AdsType.ADS_2);
        }
        if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
            adsState.setAdsType(AdsType.ADS_8);
        }
    }

    @Override
    public void onDataReceived(int[] dataFrame) {
        if(!isDataReceived) {
            isDataReceived = true;
            adsState.setActive(true);
            adsState.setDataComing(true);
            adsState.setStoped(false);
            if(adsConfig.isLeadOffEnabled()) {
                if(adsConfig.getAdsType().getAdsChannelsCount() == 2) {
                    adsState.setLoffMask(intToBitMask(dataFrame[dataFrame.length - 1], adsConfig.getAdsType().getAdsChannelsCount() * 2));
                } else if (adsConfig.getAdsType().getAdsChannelsCount() == 8) {
                    adsState.setLoffMask(bytesToBitMask(dataFrame[dataFrame.length - 2], adsConfig.getAdsType().getAdsChannelsCount() * 2));
                }
            }
        }
    }

    @Override
    public void run() {
        if(!isDataReceived) {
            adsState.setDataComing(false);
            adsState.setLoffMask(null);
        }
        if(!isDataReceived && !isMessageReceived) {
            adsState.setActive(false);
        }
        isMessageReceived = false;
        isDataReceived = false;
    }

    static boolean[] intToBitMask(int num, int maskLength) {
        boolean[] bm = new boolean[maskLength];
        for (int k = 0; k < bm.length; k++) {
            bm[k] = false;
            if (((num >> k) & 1) == 1) {
                bm[k] = true;
            }
        }
        return bm;
    }

    /**
     * ads_8channel send lead-off status in different manner:
     * first byte - states of all negative electrodes from 8 channels
     * second byte - states of all positive electrodes from 8 channels
     * @param num integer with lead-off info
     * @param maskLength 2*2 or 8*2 (2 electrodes for every channel)
     * @return
     */
    static boolean[] bytesToBitMask(int num, int maskLength) {
        boolean[] bm = new boolean[maskLength];
        for (int k = 0; k < bm.length; k++) {
            bm[k] = false;
            if(k < 8) { // first byte
                if (((num >> k) & 1) == 1) {
                    bm[2 * k + 1] = true;
                }
            } else { // second byte
                if (((num >> k) & 1) == 1) {
                    bm[2 * (k - 8)] = true;
                }
            }

        }
        return bm;
    }
}

