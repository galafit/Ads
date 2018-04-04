package com.biorecorder;

import com.biorecorder.bdfrecorder.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by galafit on 30/3/18.
 */
public class AppConfig1 {
    private BdfRecorderConfig1 bdfRecorderConfig = new BdfRecorderConfig1();
    private boolean isDurationOfDataRecordComputable = true;

    private String comportName;
    private String dirToSave;
    @JsonIgnore
    private String fileName;

    BdfRecorderConfig1 getBdfRecorderConfig() {
        return bdfRecorderConfig;
    }

    public void setBdfRecorderConfig(BdfRecorderConfig1 bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
    }

    public String getPatientIdentification() {
        return bdfRecorderConfig.getPatientIdentification();
    }

    public void setPatientIdentification(String patientIdentification) {
        bdfRecorderConfig.setPatientIdentification(patientIdentification);
    }

    public String getRecordingIdentification() {
        return bdfRecorderConfig.getRecordingIdentification();
    }

    public void setRecordingIdentification(String recordingIdentification) {
        bdfRecorderConfig.setRecordingIdentification(recordingIdentification);
    }

    public boolean isDurationOfDataRecordComputable() {
        return isDurationOfDataRecordComputable;
    }

    public void setDurationOfDataRecordComputable(boolean durationOfDataRecordComputable) {
        isDurationOfDataRecordComputable = durationOfDataRecordComputable;
    }

    public String getComportName() {
        return comportName;
    }

    public void setComportName(String comportName) {
        this.comportName = comportName;
    }

    public String getDirToSave() {
        return dirToSave;
    }

    public void setDirToSave(String dirToSave) {
        this.dirToSave = dirToSave;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public Boolean is50HzFilterEnabled(int channelNumber) {
        return bdfRecorderConfig.is50HzFilterEnabled(channelNumber);
    }

    public void setIs50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
        bdfRecorderConfig.setIs50HzFilterEnabled(channelNumber, is50HzFilterEnabled);
    }

    public int[] getChannelsAvailableDividers() {
        return bdfRecorderConfig.getChannelsAvailableDividers();
    }

    public int[] getAccelerometerAvailableDividers() {
        return bdfRecorderConfig.getAccelerometerAvailableDividers();
    }

    public int getChannelFrequency(int channelNumber) {
        return bdfRecorderConfig.getChannelFrequency(channelNumber);
    }

    public int getAccelerometerFrequency() {
        return bdfRecorderConfig.getAccelerometerFrequency();
    }

    /**
     * for channels possible values are dividers of 10: {1, 2, 5, 10}
     * @param channelNumber
     * @param divider
     */
    public void setChannelDivider(int channelNumber, int divider) {
        bdfRecorderConfig.setChannelDivider(channelNumber, divider);
    }

    /**
     * for accelerometer possible values: 10
     * @param divider = 10
     */
    public void setAccelerometerDivider(int divider) {
        bdfRecorderConfig.setAccelerometerDivider(divider);
    }

    public boolean isLeadOffEnabled() {
        return bdfRecorderConfig.isLeadOffEnabled();
    }

    public int getNumberOfChannels() {
        return bdfRecorderConfig.getNumberOfChannels();
    }

    public RecorderSampleRate getSampleRate() {
        return bdfRecorderConfig.getRecorderSampleRate();
    }

    public void setSampleRate(RecorderSampleRate sampleRate) {
        bdfRecorderConfig.setRecorderSampleRate(sampleRate);
    }

    public boolean isAccelerometerEnabled() {
        return bdfRecorderConfig.isAccelerometerEnabled();
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        bdfRecorderConfig.setAccelerometerEnabled(accelerometerEnabled);
    }

    public boolean isBatteryVoltageMeasureEnabled() {
        return bdfRecorderConfig.isBatteryVoltageMeasureEnabled();
    }

    public void setBatteryVoltageMeasureEnabled(boolean batteryVoltageMeasureEnabled) {
        bdfRecorderConfig.setBatteryVoltageMeasureEnabled(batteryVoltageMeasureEnabled);
    }

    public RecorderType getDeviceType() {
        return bdfRecorderConfig.getDeviceType();
    }

    public void setDeviceType(RecorderType recorderType) {
        bdfRecorderConfig.setDeviceType(recorderType);
    }

    public boolean isAccelerometerOneChannelMode() {
        return bdfRecorderConfig.isAccelerometerOneChannelMode();
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        bdfRecorderConfig.setAccelerometerOneChannelMode(accelerometerOneChannelMode);
    }

    public String getChannelName(int channelNumber) {
        return bdfRecorderConfig.getChannelName(channelNumber);
    }

    public void setChannelName(int channelNumber, String name) {
        bdfRecorderConfig.setChannelName(channelNumber, name);
    }

    public void setChannelLeadOffEnable(int channelNumber, boolean leadOffEnable) {
        bdfRecorderConfig.setChannelLeadOffEnable(channelNumber, leadOffEnable);
    }

    public void setChannelRldSenseEnabled(int channelNumber, boolean rldSenseEnabled) {
        bdfRecorderConfig.setChannelRldSenseEnabled(channelNumber, rldSenseEnabled);
    }

    public boolean isChannelLeadOffEnable(int channelNumber) {
        return bdfRecorderConfig.isChannelLeadOffEnable(channelNumber);
    }

    public boolean isChannelRldSenseEnabled(int channelNumber) {
        return bdfRecorderConfig.isChannelRldSenseEnabled(channelNumber);
    }

    public RecorderGain getChannelGain(int channelNumber) {
        return bdfRecorderConfig.getChannelGain(channelNumber);
    }

    public void setChannelGain(int channelNumber, RecorderGain gain) {
        bdfRecorderConfig.setChannelGain(channelNumber, gain);
    }


    public RecordingMode getChannelRecordingMode(int channelNumber) {
        return bdfRecorderConfig.getChannelRecordingMode(channelNumber);
    }

    public void setChannelRecordinMode(int channelNumber, RecordingMode recordingMode) {
        bdfRecorderConfig.setChannelRecordingMode(channelNumber, recordingMode);
    }

    public boolean isChannelEnabled(int channelNumber) {
        return bdfRecorderConfig.isChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        bdfRecorderConfig.setChannelEnabled(channelNumber, enabled);
    }

    public Integer[] getChannelsAvailableFrequencies(RecorderSampleRate sampleRate) {
        int[] dividers = bdfRecorderConfig.getChannelsAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate.getValue() / dividers[i];
        }
        return frequencies;
    }

    public Integer[] getAccelerometerAvailableFrequencies(RecorderSampleRate sampleRate) {
        int[] dividers = bdfRecorderConfig.getAccelerometerAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate.getValue() / dividers[i];
        }
        return frequencies;
    }


    public void setFrequencies(int sampleRate, int accelerometerFrequency, int[] adsChannelsFrequencies) {
        bdfRecorderConfig.setRecorderSampleRate(RecorderSampleRate.valueOf(sampleRate));
        bdfRecorderConfig.setAccelerometerDivider((sampleRate / accelerometerFrequency));
        for (int i = 0; i < adsChannelsFrequencies.length; i++) {
            bdfRecorderConfig.setChannelDivider(i, (sampleRate / adsChannelsFrequencies[i]));
        }
    }
}
