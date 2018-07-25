package com.biorecorder.recorder;

import com.biorecorder.ads.*;

/**
 *
 */
public class RecorderConfig {
    private double durationOfDataRecord = 1; // sec
    private boolean deleteBatteryVoltageChannel = true;
    private int[] channelsExtraDividers;
    private int accelerometerExtraDivider = 1;

    private AdsConfig adsConfig = new AdsConfig();

    public RecorderConfig() {
        channelsExtraDividers = new int[RecorderType.getMaxChannelsCount()];
        for (int i = 0; i < channelsExtraDividers.length; i++) {
            channelsExtraDividers[i] = 1;
        }
    }

    public RecorderConfig(RecorderConfig configToCopy) {
        durationOfDataRecord = configToCopy.durationOfDataRecord;
        adsConfig = new AdsConfig(configToCopy.adsConfig);
    }

    AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public boolean isDeleteBatteryVoltageChannel() {
        return deleteBatteryVoltageChannel;
    }

    public RecorderDivider getChannelDivider(int channelNumber) {
        return RecorderDivider.valueOf(adsConfig.getAdsChannelDivider(channelNumber), accelerometerExtraDivider);
    }

    public void setChannelDivider(int channelNumber, RecorderDivider recorderDivider) {
        adsConfig.setAdsChannelDivider(channelNumber, recorderDivider.getAdsDivider());
        channelsExtraDividers[channelNumber] = recorderDivider.getExtraDivider();
    }

    public RecorderDivider[] getAccelerometerAvailableDividers() {
        return getDeviceType().getAccelerometerAvailableDividers();
    }

    public void setAccelerometerDivider(RecorderDivider recorderDivider) {
        accelerometerExtraDivider = recorderDivider.getExtraDivider();
    }

    public RecorderDivider getAccelerometerDivider() {
        return RecorderDivider.valueOf(adsConfig.getAccelerometerDivider(), accelerometerExtraDivider);
    }

    public boolean isLeadOffEnabled() {
        return adsConfig.isLeadOffEnabled();
    }

    public int getChannelsCount() {
        return adsConfig.getAdsChannelsCount();
    }

    public int getSampleRate() {
        return adsConfig.getSampleRate().getValue();
    }

    public void setSampleRate(RecorderSampleRate sampleRate) {
        adsConfig.setSampleRate(sampleRate.getAdsSps());
    }

    public boolean isAccelerometerEnabled() {
        return adsConfig.isAccelerometerEnabled();
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        adsConfig.setAccelerometerEnabled(accelerometerEnabled);
    }

    public boolean isBatteryVoltageMeasureEnabled() {
        return adsConfig.isBatteryVoltageMeasureEnabled();
    }

    public void setBatteryVoltageMeasureEnabled(boolean batteryVoltageMeasureEnabled) {
        adsConfig.setBatteryVoltageMeasureEnabled(batteryVoltageMeasureEnabled);
    }

    public RecorderType getDeviceType() {
        return RecorderType.valueOf(adsConfig.getAdsType());
    }

    public void setDeviceType(RecorderType recorderType) {
        adsConfig.setAdsType(recorderType.getAdsType());
    }

    public boolean isAccelerometerOneChannelMode() {
        return adsConfig.isAccelerometerOneChannelMode();
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        adsConfig.setAccelerometerOneChannelMode(accelerometerOneChannelMode);
    }

    public String getChannelName(int channelNumber) {
        return adsConfig.getAdsChannelName(channelNumber);
    }

    public void setChannelName(int channelNumber, String name) {
        adsConfig.setAdsChannelName(channelNumber, name);
    }

    public void setChannelLeadOffEnable(int channelNumber, boolean leadOffEnable) {
        adsConfig.setAdsChannelLeadOffEnable(channelNumber, leadOffEnable);
    }

    public void setChannelRldSenseEnabled(int channelNumber, boolean rldSenseEnabled) {
        adsConfig.setAdsChannelRldSenseEnabled(channelNumber, rldSenseEnabled);
    }

    public boolean isChannelLeadOffEnable(int channelNumber) {
        return adsConfig.isAdsChannelLeadOffEnable(channelNumber);
    }

    public boolean isChannelRldSenseEnabled(int channelNumber) {
        return adsConfig.isAdsChannelRldSenseEnabled(channelNumber);
    }

    public RecorderGain getChannelGain(int channelNumber) {
        return RecorderGain.valueOf(adsConfig.getAdsChannelGain(channelNumber));
    }

    public void setChannelGain(int channelNumber, RecorderGain recorderGain) {
        adsConfig.setAdsChannelGain(channelNumber, recorderGain.getAdsGain());
    }

    public RecorderCommutator getChannelCommutator(int channelNumber) {
        return RecorderCommutator.valueOf(adsConfig.getAdsChannelCommutatorState(channelNumber));
    }

    public void setChannelCommutator(int channelNumber, RecorderCommutator recorderCommutator) {
        adsConfig.setAdsChannelCommutatorState(channelNumber, recorderCommutator.getAdsCommutator());
    }

    public boolean isChannelEnabled(int channelNumber) {
        return adsConfig.isAdsChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        adsConfig.setAdsChannelEnabled(channelNumber, enabled);
    }

    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }

    public void setDurationOfDataRecord(double durationOfDataRecord) {
        this.durationOfDataRecord = durationOfDataRecord;
    }

    public int getChannelSampleRate(int channelNumber) {
        return getSampleRate() / getChannelDivider(channelNumber).getValue();
    }

    public int getAccelerometerSampleRate() {
        return getSampleRate() / getAccelerometerDivider().getValue();
    }
}
