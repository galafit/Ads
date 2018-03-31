package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.edflib.base.DefaultEdfConfig;
import com.biorecorder.edflib.base.EdfConfig;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BdfRecorderConfig1 {
    public static final int GAIN_1 = 1;
    public static final int GAIN_2 = 2;
    public static final int GAIN_3 = 3;
    public static final int GAIN_4 = 4;
    public static final int GAIN_6 = 6;
    public static final int GAIN_8 = 8;
    public static final int GAIN_12 = 12;

    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private boolean isDurationOfDataRecordComputable = true;
    private AdsConfig adsConfig = new AdsConfig();
    private List<Boolean> filter50HzMask = new ArrayList<Boolean>();

    AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public boolean isDurationOfDataRecordComputable() {
        return isDurationOfDataRecordComputable;
    }

    public Boolean is50HzFilterEnabled(int channelNumber) {
        while(filter50HzMask.size() < adsConfig.getNumberOfAdsChannels()) {
            filter50HzMask.add(true);
        }
        return filter50HzMask.get(channelNumber);
    }

    public void setIs50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
        while(filter50HzMask.size() < adsConfig.getNumberOfAdsChannels()) {
            filter50HzMask.add(true);
        }
        filter50HzMask.set(channelNumber, is50HzFilterEnabled);
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

    public int[] getChannelsAvailableDividers() {
        return adsConfig.getChannelsAvailableDividers();
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

    public void setChannelDivider(int channelNumber, Divider divider) {
        adsConfig.setAdsChannelDivider(channelNumber, divider);
    }

    public void setAccelerometerDivider(Divider divider) {
        adsConfig.setAccelerometerDivider(divider);
    }


    public boolean isLeadOffEnabled() {
        return adsConfig.isLeadOffEnabled();
    }

    public int getNumberOfChannels() {
        return adsConfig.getNumberOfAdsChannels();
    }

    public int getSampleRate() {
        return adsConfig.getSampleRate().getValue();
    }

    public void setSampleRate(Sps sampleRate) {
        adsConfig.setSampleRate(sampleRate);
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
        return RecorderType.valueOf(adsConfig.getAdsType().getAdsChannelsCount());
    }

    public void setDeviceType(RecorderType recorderType) {
        adsConfig.setAdsType(AdsType.valueOf(recorderType.getChannelsCount()));
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

    public int getChannelGain(int channelNumber) {
        return adsConfig.getAdsChannelGain(channelNumber).getValue();
    }

    public void setChannelGain(int channelNumber, int gain) {
        adsConfig.setAdsChannelGain(channelNumber, Gain.valueOf(gain));
    }

    public Integer[] getAvailableGaines() {
        Gain[] gains = Gain.values();
        Integer[] gainsValues = new Integer[gains.length];
        for (int i = 0; i < gains.length; i++) {
            gainsValues[i] = gains[i].getValue();
        }
        return gainsValues;
    }

    public String getChannelRecordingMode(int channelNumber) {
        return adsConfig.getAdsChannelCommutatorState(channelNumber).name();
    }

    public void setChannelRecordinMode(int channelNumber, RecordingMode recordingMode) {
        adsConfig.setAdsChannelCommutatorState(channelNumber, CommutatorState.valueOf(recordingMode.name()));
    }

    public boolean isChannelEnabled(int channelNumber) {
        return adsConfig.isAdsChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        adsConfig.setAdsChannelEnabled(channelNumber, enabled);
    }
}
