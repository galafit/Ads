package com.biorecorder;

import com.biorecorder.ads.*;
import com.biorecorder.bdfrecorder.BdfRecorderConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;

/**
 * Created by galafit on 2/6/17.
 */
public class AppConfig {
    private String comportName;
    private String dirToSave;
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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilename() {
        return fileName;
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
        bdfRecorderConfig.setDeviceType(AdsType.valueOf(deviceType));
    }

    public String[] getAvailableDeviceTypes() {
        AdsType[] adsTypes = AdsType.values();
        String[] typesNames = new String[adsTypes.length];
        for (int i = 0; i < adsTypes.length; i++) {
            typesNames[i] = adsTypes[i].name();
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
