package com.biorecorder.ads;

import java.util.ArrayList;

/**
 * Class-structure to store info about Ads configuration
 */
public class AdsConfig {
    private AdsType adsType = AdsType.ADS_2;
    private Sps sps = Sps.S500;     // samples per second (sample rate)
    private Divider accelerometerDivider = Divider.D10;
    private boolean isAccelerometerEnabled = true;
    private boolean isAccelerometerOneChannelMode = true;
    private boolean isBatteryVoltageMeasureEnabled = false;
    private int noiseDivider = 2;

    private ArrayList<AdsChannelConfig> adsChannels = new ArrayList<AdsChannelConfig>(8);

    public AdsConfig() {
        for(int i = 0; i < 8; i++) {
            AdsChannelConfig channel = new AdsChannelConfig();
            channel.setName("Channel "+(i+1));
            adsChannels.add(channel);
        }
    }

    public byte[] getAdsConfigurationCommand() {
        return getAdsType().getAdsConfigurationCommand(this);
    }

    public int getMaxDiv() {
        return adsType.getMaxDiv().getValue();
    }

    public int[] getChannelsAvailableDividers() {
        Divider[] dividers = adsType.getChannelsAvailableDividers();
        int[] values = new int[dividers.length];
        for (int i = 0; i < values.length; i++) {
           values[i] = dividers[i].getValue();
        }
        return values;
    }

    public int[] getAccelerometerAvailableDividers() {
        Divider[] dividers = adsType.getGetAccelerometerAvailableDividers();
        int[] values = new int[dividers.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = dividers[i].getValue();
        }
        return values;
    }

    public boolean isLeadOffEnabled() {
        for (int i = 0; i < getNumberOfAdsChannels(); i++) {
            if (adsChannels.get(i).isEnabled() && adsChannels.get(i).isLoffEnable()) {
                return true;
            }
        }
        return false;
    }

    public int getNumberOfAdsChannels() {
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

    public void setAccelerometerDivider(Divider divider) {
        accelerometerDivider = divider;
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

    public CommutatorState getAdsChannelCommutatorState(int adsChannelNumber) {
        return adsChannels.get(adsChannelNumber).getCommutatorState();
    }

    public void setAdsChannelCommutatorState(int adsChannelNumber, CommutatorState commutatorState) {
        adsChannels.get(adsChannelNumber).setCommutatorState(commutatorState);
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
        return (1.0 * getMaxDiv())/getSampleRate().getValue();
    }

    public double getAdsChannelPhysicalMax(int adsChannelNumber) {
        return 2400000 / getAdsChannelGain(adsChannelNumber).getValue();
    }

    public double getAdsChannelPhysicalMin(int adsChannelNumber) {
        return - getAdsChannelPhysicalMax(adsChannelNumber);
    }

    public int getAdsChannelsDigitalMax() {
        return Math.round(8388607 / getNoiseDivider());
    }

    public int getAdsChannelsDigitalMin() {
        return Math.round(-8388608 / getNoiseDivider());
    }

    public int getAdsChannelSampleRate(int adsChannelNumber){
        return getSampleRate().getValue() / getAdsChannelDivider(adsChannelNumber);
    }

    public String getAdsChannelsPhysicalDimension() {
        return "uV";
    }

    public double getAccelerometerPhysicalMax() {
        return 1000;
    }

    public double getAccelerometerPhysicalMin() {
        return - getAccelerometerPhysicalMax();
    }

    public int getAccelerometerDigitalMax() {
        if(isAccelerometerOneChannelMode) {
            return 2000;
        }
        return 9610;
    }

    public int getAccelerometerDigitalMin() {
        if(isAccelerometerOneChannelMode) {
            return -2000;
        }
        return 4190;
    }

    public int getAccelerometerSampleRate(){
        return getSampleRate().getValue() / getAccelerometerDivider();
    }

    public String getAccelerometerPhysicalDimension() {
        if(isAccelerometerOneChannelMode()) {
            return "m/sec^3";
        }
        return "mg";
    }


    public double getBatteryVoltagePhysicalMax() {
        return 50;
    }

    public double getBatteryVoltagePhysicalMin() {
        return 0;
    }

    public int getBatteryVoltageDigitalMax() {
        return 10240;
    }

    public int getBatteryVoltageDigitalMin() {
        return 0;
    }

    public String getBatteryVoltageDimension() {
        return "V";
    }

    public double getLeadOffStatusPhysicalMax() {
        return 65536;
    }

    public double getLeadOffStatusPhysicalMin() {
        return 0;
    }

    public int getLeadOffStatusDigitalMax() {
        return 65536;
    }

    public int getLeadOffStatusDigitalMin() {
        return 0;
    }

    public String getLeadOffStatusDimension() {
        return "Bit mask";
    }


}
