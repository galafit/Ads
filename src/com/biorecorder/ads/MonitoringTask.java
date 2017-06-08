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
            adsState.setDeviceType(DeviceType.ADS_2);
        }
        if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
            adsState.setDeviceType(DeviceType.ADS_8);
        }
    }

    @Override
    public void onDataReceived(int[] dataFrame) {
        if(!isDataReceived) {
            isDataReceived = true;
            adsState.setActive(true);
            adsState.setDataComing(true);
            if(adsConfig.isLeadOffEnabled()) {
                if(adsConfig.getDeviceType().getNumberOfAdsChannels() == 2) {
                    adsState.setLoffMask(intToBitMask(dataFrame[dataFrame.length - 1], adsConfig.getDeviceType().getNumberOfAdsChannels() * 2));
                } else if (adsConfig.getDeviceType().getNumberOfAdsChannels() == 8) {
                    adsState.setLoffMask(intToBitMask(dataFrame[dataFrame.length - 2], adsConfig.getDeviceType().getNumberOfAdsChannels() * 2));
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

}

