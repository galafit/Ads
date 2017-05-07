package com.biorecorder.ads;

import java.util.ArrayList;


public class AdsConfig {
    private DeviceType deviceType = DeviceType.ADS_2channel;
    private Sps sps = Sps.S500;     // samples per second (sample rate)
    private String comPortName = "";
    private Divider accelerometerDivider = Divider.D10;
    private boolean isAccelerometerEnabled = true;
    private boolean isAccelerometerOneChannelMode = true;
    private boolean isBatteryVoltageMeasureEnabled = false;
    private boolean isHighResolutionMode = true;
    private int noiseDivider = 2;

    private ArrayList<AdsChannelConfig> adsChannels = new ArrayList<AdsChannelConfig>();

    public AdsChannelConfig getAdsChannel(int adsChannelNumber) {
        while(adsChannels.size() < getNumberOfAdsChannels()) {
           adsChannels.add(new AdsChannelConfig());
        }
        return adsChannels.get(adsChannelNumber);
    }

    public boolean isLoffEnabled() {
        for (int i = 0; i < getNumberOfAdsChannels(); i++) {
            AdsChannelConfig adsChannel = getAdsChannel(i);
            if(adsChannel.isEnabled && adsChannel.isLoffEnable()){
                return true;
            }
        }
        return false;
    }

    public int getNumberOfAdsChannels() {
        return deviceType.getNumberOfAdsChannels();
    }

    public Sps getSps() {
        return sps;
    }

    public void setSps(Sps sps) {
        this.sps = sps;
    }

    public boolean isAccelerometerEnabled() {
        return isAccelerometerEnabled;
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        isAccelerometerEnabled = accelerometerEnabled;
    }

    public boolean isBatteryVoltageMeasureEnabled() {
        return isBatteryVoltageMeasureEnabled;
    }

    public void setBatteryVoltageMeasureEnabled(boolean batteryVoltageMeasureEnabled) {
        isBatteryVoltageMeasureEnabled = batteryVoltageMeasureEnabled;
    }

    public Divider getAccelerometerDivider() {
        return accelerometerDivider;
    }

    public void setAccelerometerDivider(Divider accelerometerDivider) {
        this.accelerometerDivider = accelerometerDivider;
    }

    public String getComPortName() {
        return comPortName;
    }

    public void setComPortName(String comPortName) {
        this.comPortName = comPortName;
    }

    public boolean isHighResolutionMode() {
        return isHighResolutionMode;
    }

    public void setHighResolutionMode(boolean highResolutionMode) {
        isHighResolutionMode = highResolutionMode;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public int getNoiseDivider() {
        return noiseDivider;
    }

    public void setNoiseDivider(int noiseDivider) {
        this.noiseDivider = noiseDivider;
    }

    public boolean isAccelerometerOneChannelMode() {
        return isAccelerometerOneChannelMode;
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        isAccelerometerOneChannelMode = accelerometerOneChannelMode;
    }
}
