package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BdfRecorderConfig {
    private AdsConfig adsConfig = new AdsConfig();
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";

    private List<Boolean> filter50HzMask = new ArrayList<Boolean>();

    AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }

    public String getPatientIdentification() {
        return patientIdentification;
    }

    public void setPatientIdentification(String patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    public String getRecordingIdentification() {
        return recordingIdentification;
    }

    public void setRecordingIdentification(String recordingIdentification) {
        this.recordingIdentification = recordingIdentification;
    }

    public Boolean is50HzFilterEnabled(int channelNumber) {
        while(filter50HzMask.size() < adsConfig.getAdsChannelsCount()) {
            filter50HzMask.add(true);
        }
        return filter50HzMask.get(channelNumber);
    }

    public void setIs50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
        while(filter50HzMask.size() < adsConfig.getAdsChannelsCount()) {
            filter50HzMask.add(true);
        }
        filter50HzMask.set(channelNumber, is50HzFilterEnabled);
    }

    public int[] getChannelsAvailableDividers() {
        return adsConfig.getAdsChannelsAvailableDividers();
    }

    public int[] getAccelerometerAvailableDividers() {
        return adsConfig.getAccelerometerAvailableDividers();
    }

    public int getChannelFrequency(int channelNumber){
        return adsConfig.getAdsChannelSampleRate(channelNumber);
    }

    public int getAccelerometerFrequency(){
        return adsConfig.getAccelerometerSampleRate();
    }

    /**
     * for channels possible values are dividers of 10: {1, 2, 5, 10}
     * @param channelNumber
     * @param divider
     */
    public void setChannelDivider(int channelNumber, int divider) {
        adsConfig.setAdsChannelDivider(channelNumber, Divider.valueOf(divider));
    }

    /**
     * for accelerometer possible values: 10
     * @param divider = 10
     */
    public void setAccelerometerDivider(int divider) {
        adsConfig.setAccelerometerDivider(Divider.valueOf(divider));
    }


    public boolean isLeadOffEnabled() {
        return adsConfig.isLeadOffEnabled();
    }

    public int getNumberOfChannels() {
        return adsConfig.getAdsChannelsCount();
    }

    public RecorderSampleRate getRecorderSampleRate() {
        return RecorderSampleRate.valueOf(adsConfig.getSampleRate());
    }

    public void setRecorderSampleRate(RecorderSampleRate sampleRate) {
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

    public RecordingMode getChannelRecordingMode(int channelNumber) {
        return RecordingMode.valueOf(adsConfig.getAdsChannelCommutatorState(channelNumber));
    }

    public void setChannelRecordingMode(int channelNumber, RecordingMode recordingMode) {
        adsConfig.setAdsChannelCommutatorState(channelNumber, recordingMode.getAdsCommutatorState());
    }

    public boolean isChannelEnabled(int channelNumber) {
        return adsConfig.isAdsChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        adsConfig.setAdsChannelEnabled(channelNumber, enabled);
    }
}
