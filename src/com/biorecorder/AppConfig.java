package com.biorecorder;

import com.biorecorder.ads.*;
import com.biorecorder.bdfrecorder.BdfRecorderConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.internal.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by galafit on 2/6/17.
 */
public class AppConfig {
    private String comportName = "";
    private String dirToSave = new File(System.getProperty("user.dir"), "records").getAbsolutePath();
    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    @JsonIgnore
    private String fileName;

    BdfRecorderConfig getBdfRecorderConfig() {
        return bdfRecorderConfig;
    }

    void setBdfRecorderConfig(BdfRecorderConfig bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
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

    public void setFileName(String fileName) {
        this.fileName = normalizeFilename(fileName);
    }

    public String getFilename() {
        return fileName;
    }

    public static String normalizeFilename(@Nullable String filename) {
        String FILE_EXTENSION = "bdf";
        String defaultFilename = new SimpleDateFormat("dd-MM-yyyy_HH-mm").format(new Date(System.currentTimeMillis()));

        if (filename == null || filename.isEmpty()) {
            return defaultFilename.concat(".").concat(FILE_EXTENSION);
        }
        filename = filename.trim();

        // if filename has no extension
        if (filename.lastIndexOf('.') == -1) {
            filename = filename.concat(".").concat(FILE_EXTENSION);
            return defaultFilename + filename;
        }
        // if  extension  match with given FILE_EXTENSIONS
        // (?i) makes it case insensitive (catch BDF as well as bdf)
        if (filename.matches("(?i).*\\." + FILE_EXTENSION)) {
            return defaultFilename +filename;
        }
        // If the extension do not match with  FILE_EXTENSION We need to replace it
        filename = filename.substring(0, filename.lastIndexOf(".") + 1).concat(FILE_EXTENSION);
        return defaultFilename + "_" + filename;
    }


    public Boolean is50HzFilterEnabled(int adsChannelNumber) {
        return bdfRecorderConfig.is50HzFilterEnabled(adsChannelNumber);
    }

    public void setIs50HzFilterEnabled(int adsChannelNumber, boolean is50HzFilterEnabled) {
        bdfRecorderConfig.setIs50HzFilterEnabled(adsChannelNumber, is50HzFilterEnabled);
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

    public Integer[] getAdsChannelsAvailableFrequencies(int sampleRate) {
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

    public int getAdsChannelFrequency(int adsChannelNumber) {
        return bdfRecorderConfig.getAdsChannelFrequency(adsChannelNumber);
    }

    public int getAccelerometerFrequency() {
        return bdfRecorderConfig.getAccelerometerFrequency();
    }

    public void setFrequencies(int sampleRate, int accelerometerFrequency, int[] adsChannelsFrequencies) {
        bdfRecorderConfig.setSampleRate(Sps.valueOf(sampleRate));
        bdfRecorderConfig.setAccelerometerDivider(Divider.valueOf(sampleRate / accelerometerFrequency));
        for (int i = 0; i < adsChannelsFrequencies.length; i++) {
            bdfRecorderConfig.setAdsChannelDivider(i, Divider.valueOf(sampleRate / adsChannelsFrequencies[i]));
        }
    }

    public boolean isLeadOffEnabled() {
        return bdfRecorderConfig.isLeadOffEnabled();
    }

    public int getNumberOfAdsChannels() {
        return bdfRecorderConfig.getNumberOfAdsChannels();
    }

    public int getSampleRate() {
        return bdfRecorderConfig.getSampleRate();
    }

    public Integer[] getAvailableSampleRates() {
        Sps[] sps = Sps.values();
        Integer[] spsValues = new Integer[sps.length];
        for (int i = 0; i < sps.length; i++) {
            spsValues[i] = sps[i].getValue();
        }
        return spsValues;
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

    public void setDeviceType(String deviceType) {
        bdfRecorderConfig.setDeviceType(DeviceType.valueOf(deviceType));
    }

    public String[] getAvailableDeviceTypes() {
        DeviceType[] deviceTypes = DeviceType.values();
        String[] typesNames = new String[deviceTypes.length];
        for (int i = 0; i < deviceTypes.length; i++) {
            typesNames[i] = deviceTypes[i].name();
        }
        return typesNames;
    }

    public boolean isAccelerometerOneChannelMode() {
        return bdfRecorderConfig.isAccelerometerOneChannelMode();
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        bdfRecorderConfig.setAccelerometerOneChannelMode(accelerometerOneChannelMode);
    }

    public String getAdsChannelName(int adsChannelNumber) {
        return bdfRecorderConfig.getAdsChannelName(adsChannelNumber);
    }

    public void setAdsChannelName(int adsChannelNumber, String name) {
        bdfRecorderConfig.setAdsChannelName(adsChannelNumber, name);
    }

    public void setAdsChannelLeadOffEnable(int adsChannelNumber, boolean leadOffEnable) {
        bdfRecorderConfig.setAdsChannelLeadOffEnable(adsChannelNumber, leadOffEnable);
    }

    public void setAdsChannelRldSenseEnabled(int adsChannelNumber, boolean rldSenseEnabled) {
        bdfRecorderConfig.setAdsChannelRldSenseEnabled(adsChannelNumber, rldSenseEnabled);
    }

    public boolean isAdsChannelLeadOffEnable(int adsChannelNumber) {
        return bdfRecorderConfig.isAdsChannelLeadOffEnable(adsChannelNumber);
    }

    public boolean isAdsChannelRldSenseEnabled(int adsChannelNumber) {
        return bdfRecorderConfig.isAdsChannelRldSenseEnabled(adsChannelNumber);
    }

    public int getAdsChannelGain(int adsChannelNumber) {
        return bdfRecorderConfig.getAdsChannelGain(adsChannelNumber);
    }

    public void setAdsChannelGain(int adsChannelNumber, int gain) {
        bdfRecorderConfig.setAdsChannelGain(adsChannelNumber, Gain.valueOf(gain));
    }

    public Integer[] getAvailableGaines() {
        Gain[] gains = Gain.values();
        Integer[] gainsValues = new Integer[gains.length];
        for (int i = 0; i < gains.length; i++) {
            gainsValues[i] = gains[i].getValue();
        }
        return gainsValues;
    }

    public String getAdsChannelCommutatorState(int adsChannelNumber) {
        return bdfRecorderConfig.getAdsChannelCommutatorState(adsChannelNumber).name();
    }

    public void setAdsChannelCommutatorState(int adsChannelNumber, String commutatorState) {
        bdfRecorderConfig.setAdsChannelCommutatorState(adsChannelNumber, CommutatorState.valueOf(commutatorState));
    }

    public String[] getAvailableCommutatorStates() {
        CommutatorState[] commutators = CommutatorState.values();
        String[] commutatorsNames = new String[commutators.length];
        for (int i = 0; i < commutators.length; i++) {
            commutatorsNames[i] = commutators[i].name();
        }
        return commutatorsNames;
    }

    public boolean isAdsChannelEnabled(int adsChannelNumber) {
        return bdfRecorderConfig.isAdsChannelEnabled(adsChannelNumber);
    }

    public void setAdsChannelEnabled(int adsChannelNumber, boolean enabled) {
        bdfRecorderConfig.setAdsChannelEnabled(adsChannelNumber, enabled);
    }
}
