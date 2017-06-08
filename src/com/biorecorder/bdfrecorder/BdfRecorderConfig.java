package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BdfRecorderConfig {
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private AdsConfig adsConfig = new AdsConfig();
    private List<Boolean> filter50HzMask = new ArrayList<Boolean>();

    AdsConfig getAdsConfig() {
        return adsConfig;
    }

    void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }

    public Boolean is50HzFilterEnabled(int adsChannelNumber) {
        while(filter50HzMask.size() < adsConfig.getNumberOfAdsChannels()) {
            filter50HzMask.add(true);
        }
        return filter50HzMask.get(adsChannelNumber);
    }

    public void setIs50HzFilterEnabled(int adsChannelNumber, boolean is50HzFilterEnabled) {
        while(filter50HzMask.size() < adsConfig.getNumberOfAdsChannels()) {
            filter50HzMask.add(true);
        }
        filter50HzMask.set(adsChannelNumber, is50HzFilterEnabled);
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

    public int getMaxDiv() {
        return adsConfig.getMaxDiv();
    }


    public int[] getChannelsAvailableDividers() {
        return adsConfig.getChannelsAvailableDividers();
    }

    public int[] getAccelerometerAvailableDividers() {
        return adsConfig.getAccelerometerAvailableDividers();
    }

    public int getAdsChannelFrequency(int adsChannelNumber){
        return adsConfig.getAdsChannelSampleRate(adsChannelNumber);
    }

    public int getAccelerometerFrequency(){
        return adsConfig.getAccelerometerSampleRate();
    }

    public void setAdsChannelDivider(int adsChannelNumber, Divider divider) {
        adsConfig.setAdsChannelDivider(adsChannelNumber, divider);
    }

    public void setAccelerometerDivider(Divider divider) {
        adsConfig.setAccelerometerDivider(divider);
    }


    public boolean isLeadOffEnabled() {
        return adsConfig.isLeadOffEnabled();
    }

    public int getNumberOfAdsChannels() {
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

    public DeviceType getDeviceType() {
        return adsConfig.getDeviceType();
    }

    public void setDeviceType(DeviceType deviceType) {
        adsConfig.setDeviceType(deviceType);
    }

    public boolean isAccelerometerOneChannelMode() {
        return adsConfig.isAccelerometerOneChannelMode();
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        adsConfig.setAccelerometerOneChannelMode(accelerometerOneChannelMode);
    }

    public String getAdsChannelName(int adsChannelNumber) {
        return adsConfig.getAdsChannelName(adsChannelNumber);
    }

    public void setAdsChannelName(int adsChannelNumber, String name) {
        adsConfig.setAdsChannelName(adsChannelNumber, name);
    }

    public void setAdsChannelLeadOffEnable(int adsChannelNumber, boolean leadOffEnable) {
        adsConfig.setAdsChannelLeadOffEnable(adsChannelNumber, leadOffEnable);
    }

    public void setAdsChannelRldSenseEnabled(int adsChannelNumber, boolean rldSenseEnabled) {
        adsConfig.setAdsChannelRldSenseEnabled(adsChannelNumber, rldSenseEnabled);
    }

    public boolean isAdsChannelLeadOffEnable(int adsChannelNumber) {
        return adsConfig.isAdsChannelLeadOffEnable(adsChannelNumber);
    }

    public boolean isAdsChannelRldSenseEnabled(int adsChannelNumber) {
        return adsConfig.isAdsChannelRldSenseEnabled(adsChannelNumber);
    }

    public int getAdsChannelGain(int adsChannelNumber) {
        return adsConfig.getAdsChannelGain(adsChannelNumber).getValue();
    }

    public void setAdsChannelGain(int adsChannelNumber, Gain gain) {
        adsConfig.setAdsChannelGain(adsChannelNumber, gain);
    }

    public CommutatorState getAdsChannelCommutatorState(int adsChannelNumber) {
        return adsConfig.getAdsChannelCommutatorState(adsChannelNumber);
    }

    public void setAdsChannelCommutatorState(int adsChannelNumber, CommutatorState commutatorState) {
        adsConfig.setAdsChannelCommutatorState(adsChannelNumber, commutatorState);
    }


    public boolean isAdsChannelEnabled(int adsChannelNumber) {
        return adsConfig.isAdsChannelEnabled(adsChannelNumber);
    }

    public void setAdsChannelEnabled(int adsChannelNumber, boolean enabled) {
        adsConfig.setAdsChannelEnabled(adsChannelNumber, enabled);
    }

}
