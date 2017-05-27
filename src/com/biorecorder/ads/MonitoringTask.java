package com.biorecorder.ads;

import java.util.TimerTask;

/**
 * Monitors if ads active
 */
class MonitoringTask extends TimerTask implements MessageListener, AdsDataListener {
    private boolean isMessageReceived = false;
    private boolean isDataReceived = false;
    private final AdsState adsState;

    public MonitoringTask(AdsState adsState) {
        this.adsState = adsState;
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
        }
    }

    @Override
    public void run() {
        if(!isDataReceived) {
            adsState.setDataComing(false);
        }
        if(!isDataReceived && !isMessageReceived) {
            adsState.setActive(false);
        }
        isMessageReceived = false;
        isDataReceived = false;
    }
}

