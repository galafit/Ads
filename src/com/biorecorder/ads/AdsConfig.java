package com.biorecorder.ads;

import java.util.ArrayList;

/**
 * Class-structure to store info about Ads configuration
 */
public class AdsConfig {
    private DeviceType deviceType = DeviceType.ADS_8;
    private Sps sps = Sps.S500;     // samples per second (sample rate)
    private Divider accelerometerDivider = Divider.D10;
    private boolean isAccelerometerEnabled = true;
    private boolean isAccelerometerOneChannelMode = true;
    private boolean isBatteryVoltageMeasureEnabled = false;
    private int noiseDivider = 2;

    private ArrayList<AdsChannelConfig> adsChannels = new ArrayList<AdsChannelConfig>();

    public AdsChannelConfig getAdsChannel(int adsChannelNumber) {
        if (adsChannelNumber >= getNumberOfAdsChannels()) {
            throw new IndexOutOfBoundsException("ChannelIndex = " + adsChannelNumber + "; Number of channels = " + getNumberOfAdsChannels());
        }
        while (adsChannels.size() < getNumberOfAdsChannels()) {
            // add new channel
            adsChannels.add(new AdsChannelConfig());
            // set its name with the numbering
            adsChannels.get(adsChannels.size() - 1).setName("Channel "+adsChannels.size());
        }
        return adsChannels.get(adsChannelNumber);
    }

    public int getMaxDiv() {
        return deviceType.getMaxDiv().getValue();
    }

    public Divider[] getChannelsAvailableDividers() {
        return deviceType.getChannelsAvailableDividers();
    }

    public Divider[] getGetAccelerometerAvailableDividers() {
        return deviceType.getGetAccelerometerAvailableDividers();
    }


    public boolean isLoffEnabled() {
        for (int i = 0; i < getNumberOfAdsChannels(); i++) {
            AdsChannelConfig adsChannel = getAdsChannel(i);
            if (adsChannel.isEnabled && adsChannel.isLoffEnable()) {
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

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
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
