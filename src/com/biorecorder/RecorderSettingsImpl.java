package com.biorecorder;

import com.biorecorder.gui.RecorderSettings;
import com.biorecorder.recorder.*;

public class RecorderSettingsImpl implements RecorderSettings {
    private static final String[] ACCELEROMETER_COMMUTATORS = {"1 channel", "3 channels"};
    private final AppConfig appConfig;
    private final String[] availableComports;

    public RecorderSettingsImpl(AppConfig appConfig, String[] availableComports) {
        this.appConfig = appConfig;
        this.availableComports = availableComports;
    }

    AppConfig getAppConfig() {
        return appConfig;
    }

    @Override
    public String getDeviceType() {
        return appConfig.getRecorderConfig().getDeviceType().name();
    }

    @Override
    public void setDeviceType(String recorderType) {
        appConfig.getRecorderConfig().setDeviceType(RecorderType.valueOf(recorderType));
    }
    
    @Override
    public int getChannelsCount() {
        return appConfig.getRecorderConfig().getChannelsCount();
    }

    @Override
    public int getMaxFrequency() {
        return appConfig.getRecorderConfig().getSampleRate();
    }

    @Override
    public void setMaxFrequency(int frequency) {
        appConfig.getRecorderConfig().setSampleRate(RecorderSampleRate.valueOf(frequency));
    }

    @Override
    public String getPatientIdentification() {
        return appConfig.getPatientIdentification();
    }

    @Override
    public void setPatientIdentification(String patientIdentification) {
        appConfig.setPatientIdentification(patientIdentification);
    }

    @Override
    public String getRecordingIdentification() {
        return appConfig.getRecordingIdentification();
    }

    @Override
    public void setRecordingIdentification(String recordingIdentification) {
        appConfig.setRecordingIdentification(recordingIdentification);
    }

    @Override
    public boolean is50HzFilterEnabled(int channelNumber) {
        return appConfig.is50HzFilterEnabled(channelNumber);
    }

    @Override
    public void set50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
        appConfig.set50HzFilterEnabled(channelNumber, is50HzFilterEnabled);
    }

    @Override
    public String getComportName() {
        return appConfig.getComportName();
    }

    @Override
    public void setComportName(String comportName) {
        appConfig.setComportName(comportName);
    }

    @Override
    public String getDirToSave() {
        return appConfig.getDirToSave();
    }

    @Override
    public void setDirToSave(String dirToSave) {
        appConfig.setDirToSave(dirToSave);
    }

    @Override
    public String getFileName() {
        return appConfig.getFileName();
    }

    @Override
    public void setFileName(String fileName) {
        appConfig.setFileName(fileName);
    }
    

    @Override
    public boolean isChannelEnabled(int channelNumber) {
        return appConfig.getRecorderConfig().isChannelEnabled(channelNumber);
    }

    @Override
    public void setChannelEnabled(int channelNumber, boolean enabled) {
        appConfig.getRecorderConfig().setChannelEnabled(channelNumber, enabled);
    }

    @Override
    public String getChannelName(int channelNumber) {
        return appConfig.getRecorderConfig().getChannelName(channelNumber);
    }

    @Override
    public void setChannelName(int channelNumber, String name) {
        appConfig.getRecorderConfig().setChannelName(channelNumber, name);
    }
    
    @Override
    public int getChannelGain(int channelNumber) {
        return appConfig.getRecorderConfig().getChannelGain(channelNumber).getValue();
    }

    @Override
    public void setChannelGain(int channelNumber, int gainValue) {
        appConfig.getRecorderConfig().setChannelGain(channelNumber, RecorderGain.valueOf(gainValue));
    }


    @Override
    public String getChannelMode(int channelNumber) {
        return appConfig.getRecorderConfig().getChannelCommutator(channelNumber).name();
    }

    @Override
    public void setChannelMode(int channelNumber, String mode) {
        appConfig.getRecorderConfig().setChannelCommutator(channelNumber, RecorderCommutator.valueOf(mode));
    }


    @Override
    public int getChannelSampleRate(int channelNumber) {
        return appConfig.getRecorderConfig().getChannelSampleRate(channelNumber);
    }


    @Override
    public void setChannelFrequency(int channelNumber, int frequency) {
        int dividerValue = getMaxFrequency() / frequency;
        RecorderDivider divider;
        try {
           divider = RecorderDivider.valueOf(dividerValue);
        } catch (Exception ex) {
            divider = RecorderDivider.D1;
        }
        appConfig.getRecorderConfig().setChannelDivider(channelNumber, divider);
    }


    @Override
    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        appConfig.getRecorderConfig().setAccelerometerEnabled(accelerometerEnabled);
    }

    @Override
    public boolean isAccelerometerEnabled() {
        return appConfig.getRecorderConfig().isAccelerometerEnabled();
    }

    @Override
    public String getAccelerometerName() {
        return "Accelerometer";
    }

    @Override
    public int getAccelerometerFrequency() {
        return appConfig.getRecorderConfig().getAccelerometerSampleRate();
    }

    @Override
    public void setAccelerometerFrequency(int frequency) {
        int dividerValue = getMaxFrequency() / frequency;
        RecorderDivider divider;
        try {
            divider = RecorderDivider.valueOf(dividerValue);
        } catch (Exception ex) {
            divider = RecorderDivider.D10;
        }
        appConfig.getRecorderConfig().setAccelerometerDivider(divider);
    }

    @Override
    public String getAccelerometerMode() {
        if(appConfig.getRecorderConfig().isAccelerometerOneChannelMode()) {
            return ACCELEROMETER_COMMUTATORS[0];
        }
        return ACCELEROMETER_COMMUTATORS[1];
    }

    @Override
    public void setAccelerometerMode(String mode) {
        if(mode != null && mode.equals(ACCELEROMETER_COMMUTATORS[0])) {
            appConfig.getRecorderConfig().setAccelerometerOneChannelMode(true);
        } else {
            appConfig.getRecorderConfig().setAccelerometerOneChannelMode(false);
        }

    }

    @Override
    public String[] getAccelerometerAvailableModes() {
        return ACCELEROMETER_COMMUTATORS;
    }

    @Override
    public String[] getAvailableDeviseTypes() {
        RecorderType[] devises = RecorderType.values();
        String[] names = new String[devises.length];
        for (int i = 0; i < devises.length; i++) {
            names[i] = devises[i].name();
        }
        return names;
    }

    @Override
    public  Integer[] getAvailableMaxFrequencies() {
        RecorderSampleRate[] sampleRates = RecorderSampleRate.values();
        Integer[] values = new Integer[sampleRates.length];
        for (int i = 0; i < sampleRates.length; i++) {
            values[i] = sampleRates[i].getValue();
        }
        return values;
    }

    @Override
    public Integer[] getChannelsAvailableFrequencies() {
        RecorderDivider[] dividers = RecorderDivider.values();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = getMaxFrequency() / dividers[i].getValue();
        }
        return frequencies;
    }

    @Override
    public Integer[] getAccelerometerAvailableFrequencies() {
        RecorderDivider[] dividers = appConfig.getRecorderConfig().getAccelerometerAvailableDividers();
        Integer[] frequencies = new Integer[dividers.length];
        for (int i = 0; i < dividers.length; i++) {
            frequencies[i] = getMaxFrequency() / dividers[i].getValue();
        }
        return frequencies;
    }

    @Override
    public  Integer[] getChannelsAvailableGains() {
        RecorderGain[] gains = RecorderGain.values();
        Integer[] values = new Integer[gains.length];
        for (int i = 0; i < gains.length; i++) {
            values[i] = gains[i].getValue();
        }
        return values;
    }

    @Override
    public String[] getChannelsAvailableModes() {
        RecorderCommutator[] modes = RecorderCommutator.values();
        String[] names = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            names[i] = modes[i].name();
        }
        return names;
    }

    @Override
    public String[] getAvailableComports() {
        return availableComports;
    }
}
