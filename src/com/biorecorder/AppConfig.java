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
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private boolean[] filter50HzMask = new boolean[recorderConfig.getChannelsCount()];

    private String comportName;
    private String dirToSave;
    @JsonIgnore
    private String fileName;

    public AppConfig() {
        for (int i = 0; i < filter50HzMask.length; i++) {
            filter50HzMask[i] = true;
        }
    }

    RecorderConfig getRecorderConfig() {
        return recorderConfig;
    }

    public String getPatientIdentification() {
        return patientIdentification;
    }

    public void setRecorderConfig(RecorderConfig recorderConfig) {
        this.recorderConfig = recorderConfig;
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

    public boolean is50HzFilterEnabled(int channelNumber) {
        return filter50HzMask[channelNumber];
    }

    public void set50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
         filter50HzMask[channelNumber] = is50HzFilterEnabled;
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


    public int getChannelSampleRate(int channelNumber) {
        return recorderConfig.getSampleRate() / recorderConfig.getChannelDivider(channelNumber);
    }

    public int getAccelerometerSampleRate() {
        return recorderConfig.getSampleRate() / recorderConfig.getAccelerometerDivider();
    }

    public void setSampleRates(int sampleRate, int[] adsChannelsFrequencies) {
        recorderConfig.setSampleRate(RecorderSampleRate.valueOf(sampleRate));
         for (int i = 0; i < adsChannelsFrequencies.length; i++) {
            recorderConfig.setChannelDivider(i, RecorderDivider.valueOf(sampleRate / adsChannelsFrequencies[i]));
        }
    }

    public boolean isLeadOffEnabled() {
        return recorderConfig.isLeadOffEnabled();
    }

    public int getChannelsCount() {
        return recorderConfig.getChannelsCount();
    }

    public int getSampleRate() {
        return recorderConfig.getSampleRate();
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

    public void setDeviceType(String recorderType) {
        recorderConfig.setDeviceType(RecorderType.valueOf(recorderType));
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


    public String getChannelCommutator(int channelNumber) {
        return recorderConfig.getChannelCommutator(channelNumber).name();
    }

    public void setChannelCommutator(int channelNumber, String commutator) {
        recorderConfig.setChannelCommutator(channelNumber, RecorderCommutator.valueOf(commutator));
    }

    public boolean isChannelEnabled(int channelNumber) {
        return recorderConfig.isChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        recorderConfig.setChannelEnabled(channelNumber, enabled);
    }


    public Integer[] getChannelsAvailableSampleRates() {
        RecorderDivider[] dividers = RecorderDivider.values();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = getSampleRate() / dividers[i].getValue();
        }
        return frequencies;
    }

    public static Integer[] getAvailableSampleRates() {
        RecorderSampleRate[] sampleRates = RecorderSampleRate.values();
        Integer[] values = new Integer[sampleRates.length];
        for (int i = 0; i < sampleRates.length; i++) {
            values[i] = sampleRates[i].getValue();
        }
        return values;
    }


    public static Integer[] getAvailableGains() {
        RecorderGain[] gains = RecorderGain.values();
        Integer[] values = new Integer[gains.length];
        for (int i = 0; i < gains.length; i++) {
            values[i] = gains[i].getValue();
        }
        return values;
    }


    public static String[] getAvailableRecordingModes() {
        RecorderCommutator[] modes = RecorderCommutator.values();
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
