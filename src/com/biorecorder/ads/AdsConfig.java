package com.biorecorder.ads;

import java.util.ArrayList;

/**
 * Class-structure to store info about Ads configuration
 */
public class AdsConfig {

    private AdsType adsType = AdsType.ADS_2;
    private Sps sps = Sps.S500;     // samples per second (sample rate)

    private boolean isBatteryVoltageMeasureEnabled = true;
    private int noiseDivider = 2;

    private boolean isAccelerometerEnabled = true;
    private boolean isAccelerometerOneChannelMode = true;
    private Divider accelerometerDivider = AdsType.ACCELEROMETER_DIVIDER;


    private ArrayList<AdsChannelConfig> adsChannels = new ArrayList<AdsChannelConfig>(8);

    public AdsConfig() {
        for(int i = 0; i < AdsType.getMaxChannelsCount() ; i++) {
            AdsChannelConfig channel = new AdsChannelConfig();
            channel.setName("Channel "+(i+1));
            adsChannels.add(channel);
        }
    }


    public byte[] getAdsConfigurationCommand() {
        return getAdsType().getAdsConfigurationCommand(this);
    }

    public boolean isLeadOffEnabled() {
        for (int i = 0; i < getAdsChannelsCount(); i++) {
            if (adsChannels.get(i).isEnabled() && adsChannels.get(i).isLoffEnable()) {
                return true;
            }
        }
        return false;
    }

    public int getAdsChannelsCount() {
        return adsType.getAdsChannelsCount();
    }

    public Sps getSampleRate() {
        return sps;
    }

    public void setSampleRate(Sps sps) {
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

    public int getAccelerometerDivider() {
        return accelerometerDivider.getValue();
    }

    public AdsType getAdsType() {
        return adsType;
    }

    public void setAdsType(AdsType adsType) {
        this.adsType = adsType;
    }

    public int getNoiseDivider() {
        return noiseDivider;
    }

    public boolean isAccelerometerOneChannelMode() {
        return isAccelerometerOneChannelMode;
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        isAccelerometerOneChannelMode = accelerometerOneChannelMode;
    }


    public String getAdsChannelName(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).getName();
    }

    public void setAdsChannelName(int adsChannelNumber, String name) {
        adsChannels.get(adsChannelNumber).setName(name);
    }

    public void setAdsChannelLeadOffEnable(int adsChannelNumber, boolean leadOffEnable) {
        adsChannels.get(adsChannelNumber).setLoffEnable(leadOffEnable);
    }


    public void setAdsChannelRldSenseEnabled(int adsChannelNumber, boolean rldSenseEnabled) {
        adsChannels.get(adsChannelNumber).setRldSenseEnabled(rldSenseEnabled);
    }

    public boolean isAdsChannelLeadOffEnable(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).isLoffEnable();
    }

    public boolean isAdsChannelRldSenseEnabled(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).isRldSenseEnabled();
    }

    public Gain getAdsChannelGain(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).getGain();
    }

    public void setAdsChannelGain(int adsChannelNumber, Gain gain) {
        adsChannels.get(adsChannelNumber).setGain(gain);
    }

    public Commutator getAdsChannelCommutatorState(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).getCommutator();
    }

    public void setAdsChannelCommutatorState(int adsChannelNumber, Commutator commutator) {
        adsChannels.get(adsChannelNumber).setCommutator(commutator);
    }

    public int getAdsChannelDivider(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).getDivider().getValue();
    }

    public void setAdsChannelDivider(int adsChannelNumber, Divider divider) {
        adsChannels.get(adsChannelNumber).setDivider(divider);
    }

    public boolean isAdsChannelEnabled(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).isEnabled();
    }

    public void setAdsChannelEnabled(int adsChannelNumber, boolean enabled) {
        adsChannels.get(adsChannelNumber).setEnabled(enabled);
    }

    public double getDurationOfDataRecord() {
        return (1.0 * Divider.getMaxDivider().getValue())/getSampleRate().getValue();
    }

}
