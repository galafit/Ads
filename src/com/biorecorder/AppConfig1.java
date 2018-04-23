package com.biorecorder;

import com.biorecorder.bdfrecorder.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;

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
        // first we try to return «dirToSave» if it is specified and exists
        if(dirToSave != null) {
            File dir = new File(dirToSave);
            if(dir.exists() && dir.isDirectory()) {
                return dirToSave;
            }
        }
        // then we try return «projectDir/records» if it is exist or can be created
        String projectDir = System.getProperty("user.dir");
        String dirName = "records";

        File dir = new File (projectDir, dirName);
        if(dir.exists()) {
            return dir.toString();
        } else {
            try {
                dir.mkdir();
                return dir.toString();
            } catch (Exception ex) {
                // do nothing!
            }
        }
        // finally we return «homeDir/records»
        String userHomeDir = System.getProperty("user.home");
        dir = new File (userHomeDir, dirName);
        return dir.toString();
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

    public int getSampleRate() {
        return bdfRecorderConfig.getRecorderSampleRate().getValue();
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

    public String getDeviceType() {
        return bdfRecorderConfig.getDeviceType().name();
    }

    public void setDeviceType(String recorderTypeName) {
        bdfRecorderConfig.setDeviceType(RecorderType.valueOf(recorderTypeName));
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

    public int getChannelGain(int channelNumber) {
        return bdfRecorderConfig.getChannelGain(channelNumber).getValue();
    }

    public void setChannelGain(int channelNumber, int gainValue) {
        bdfRecorderConfig.setChannelGain(channelNumber, RecorderGain.valueOf(gainValue));
    }


    public String getChannelRecordingMode(int channelNumber) {
        return bdfRecorderConfig.getChannelRecordingMode(channelNumber).name();
    }

    public void setChannelRecordinMode(int channelNumber, String recordingModeName) {
        bdfRecorderConfig.setChannelRecordingMode(channelNumber, RecordingMode.valueOf(recordingModeName));
    }

    public boolean isChannelEnabled(int channelNumber) {
        return bdfRecorderConfig.isChannelEnabled(channelNumber);
    }

    public void setChannelEnabled(int channelNumber, boolean enabled) {
        bdfRecorderConfig.setChannelEnabled(channelNumber, enabled);
    }

    public Integer[] getChannelsAvailableFrequencies(int sampleRate) {
        int[] dividers = bdfRecorderConfig.getChannelsAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate / dividers[i];
        }
        return frequencies;
    }

    public Integer[] getAccelerometerAvailableFrequencies(int sampleRate) {
        int[] dividers = bdfRecorderConfig.getAccelerometerAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = sampleRate / dividers[i];
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
