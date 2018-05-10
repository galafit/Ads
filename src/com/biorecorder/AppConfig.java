package com.biorecorder;

import com.biorecorder.bdfrecorder.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;

/**
 * Created by galafit on 30/3/18.
 */
public class AppConfig {
    private RecorderConfig recorderConfig = new RecorderConfig();
    private boolean isDurationOfDataRecordComputable = true;

    private String comportName;
    private String dirToSave;
    @JsonIgnore
    private String fileName;

    RecorderConfig getRecorderConfig() {
        return recorderConfig;
    }

    public void setRecorderConfig(RecorderConfig recorderConfig) {
        this.recorderConfig = recorderConfig;
    }

    public String getPatientIdentification() {
        return recorderConfig.getPatientIdentification();
    }

    public void setPatientIdentification(String patientIdentification) {
        recorderConfig.setPatientIdentification(patientIdentification);
    }

    public String getRecordingIdentification() {
        return recorderConfig.getRecordingIdentification();
    }

    public void setRecordingIdentification(String recordingIdentification) {
        recorderConfig.setRecordingIdentification(recordingIdentification);
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
        // first we try to return «dirToSave» if it is specified
        if(dirToSave != null) {
            return dirToSave;
        }
        // then we try return «projectDir/records»

        String projectDir = System.getProperty("user.dir");
        String dirName = "records";

        File dir = new File (projectDir, dirName);
        return dir.toString();
       /*
        // finally we return «homeDir/records»
        String userHomeDir = System.getProperty("user.home");
        dir = new File (userHomeDir, dirName);
        return dir.toString();*/
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
        return recorderConfig.is50HzFilterEnabled(channelNumber);
    }

    public void setIs50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
        recorderConfig.setIs50HzFilterEnabled(channelNumber, is50HzFilterEnabled);
    }

    public int[] getChannelsAvailableDividers() {
        return recorderConfig.getChannelsAvailableDividers();
    }

    public int[] getAccelerometerAvailableDividers() {
        return recorderConfig.getAccelerometerAvailableDividers();
    }

    public int getChannelFrequency(int channelNumber) {
        return recorderConfig.getChannelFrequency(channelNumber);
    }

    public int getAccelerometerFrequency() {
        return recorderConfig.getAccelerometerFrequency();
    }

    /**
     * for channels possible values are dividers of 10: {1, 2, 5, 10}
     * @param channelNumber
     * @param divider
     */
    public void setChannelDivider(int channelNumber, int divider) {
        recorderConfig.setChannelDivider(channelNumber, divider);
    }

    /**
     * for accelerometer possible values: 10
     * @param divider = 10
     */
    public void setAccelerometerDivider(int divider) {
        recorderConfig.setAccelerometerDivider(divider);
    }

    public boolean isLeadOffEnabled() {
        return recorderConfig.isLeadOffEnabled();
    }

    public int getNumberOfChannels() {
        return recorderConfig.getNumberOfChannels();
    }

    public int getSampleRate() {
        return recorderConfig.getRecorderSampleRate().getValue();
    }

    public boolean isAccelerometerEnabled() {
        return recorderConfig.isAccelerometerEnabled();
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        recorderConfig.setAccelerometerEnabled(accelerometerEnabled);
    }

    public boolean isBatteryVoltageMeasureEnabled() {
        return recorderConfig.isBatteryVoltageMeasureEnabled();
    }

    public void setBatteryVoltageMeasureEnabled(boolean batteryVoltageMeasureEnabled) {
        recorderConfig.setBatteryVoltageMeasureEnabled(batteryVoltageMeasureEnabled);
    }

    public String getDeviceType() {
        return recorderConfig.getDeviceType().name();
    }

    public void setDeviceType(String recorderTypeName) {
        recorderConfig.setDeviceType(RecorderType.valueOf(recorderTypeName));
    }

    public boolean isAccelerometerOneChannelMode() {
        return recorderConfig.isAccelerometerOneChannelMode();
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        recorderConfig.setAccelerometerOneChannelMode(accelerometerOneChannelMode);
    }

    public String getChannelName(int channelNumber) {
        return recorderConfig.getChannelName(channelNumber);
    }

    public void setChannelName(int channelNumber, String name) {
        recorderConfig.setChannelName(channelNumber, name);
    }

    public void setChannelLeadOffEnable(int channelNumber, boolean leadOffEnable) {
        recorderConfig.setChannelLeadOffEnable(channelNumber, leadOffEnable);
    }

    public void setChannelRldSenseEnabled(int channelNumber, boolean rldSenseEnabled) {
        recorderConfig.setChannelRldSenseEnabled(channelNumber, rldSenseEnabled);
    }

    public boolean isChannelLeadOffEnable(int channelNumber) {
        return recorderConfig.isChannelLeadOffEnable(channelNumber);
    }

    public boolean isChannelRldSenseEnabled(int channelNumber) {
        return recorderConfig.isChannelRldSenseEnabled(channelNumber);
    }

    public int getChannelGain(int channelNumber) {
        return recorderConfig.getChannelGain(channelNumber).getValue();
    }

    public void setChannelGain(int channelNumber, int gainValue) {
        recorderConfig.setChannelGain(channelNumber, RecorderGain.valueOf(gainValue));
    }


    public String getChannelRecordingMode(int channelNumber) {
        return recorderConfig.getChannelRecordingMode(channelNumber).name();
    }

    public void setChannelRecordinMode(int channelNumber, String recordingModeName) {
        recorderConfig.setChannelRecordingMode(channelNumber, RecordingMode.valueOf(recordingModeName));
    }

    public boolean isChannelEnabled(int channelNumber) {
        return recorderConfig.isChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        recorderConfig.setChannelEnabled(channelNumber, enabled);
    }

    public Integer[] getChannelsAvailableFrequencies(int sampleRate) {
        int[] dividers = recorderConfig.getChannelsAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate / dividers[i];
        }
        return frequencies;
    }

    public Integer[] getAccelerometerAvailableFrequencies(int sampleRate) {
        int[] dividers = recorderConfig.getAccelerometerAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate / dividers[i];
        }
        return frequencies;
    }


    public void setFrequencies(int sampleRate, int accelerometerFrequency, int[] adsChannelsFrequencies) {
        recorderConfig.setRecorderSampleRate(RecorderSampleRate.valueOf(sampleRate));
        recorderConfig.setAccelerometerDivider((sampleRate / accelerometerFrequency));
        for (int i = 0; i < adsChannelsFrequencies.length; i++) {
            recorderConfig.setChannelDivider(i, (sampleRate / adsChannelsFrequencies[i]));
        }
    }

    public static Integer[] getAvailableGains() {
        RecorderGain[] gains = RecorderGain.values();
        Integer[] values = new Integer[gains.length];
        for (int i = 0; i < gains.length; i++) {
            values[i] = gains[i].getValue();
        }
        return values;
    }

    public static Integer[] getAvailableFrequencies() {
        RecorderSampleRate[] sampleRates = RecorderSampleRate.values();
        Integer[] values = new Integer[sampleRates.length];
        for (int i = 0; i < sampleRates.length; i++) {
            values[i] = sampleRates[i].getValue();
        }
        return values;
    }

    public static String[] getAvailableRecordingModes() {
        RecordingMode[] modes = RecordingMode.values();
        String[] names = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            names[i] = modes[i].name();
        }
        return names;
    }

    public static String[] getAvailableDeviseTypes() {
        RecorderType[] devises = RecorderType.values();
        String[] names = new String[devises.length];
        for (int i = 0; i < devises.length; i++) {
            names[i] = devises[i].name();
        }
        return names;
    }
}
