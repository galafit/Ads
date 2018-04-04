package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.edflib.base.DefaultEdfConfig;
import com.biorecorder.edflib.base.EdfConfig;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BdfRecorderConfig {
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private boolean isDurationOfDataRecordComputable = true;
    private AdsConfig adsConfig = new AdsConfig();
    private List<Boolean> filter50HzMask = new ArrayList<Boolean>();

    AdsConfig getAdsConfig() {
        return adsConfig;
    }

    void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }

    public boolean isDurationOfDataRecordComputable() {
        return isDurationOfDataRecordComputable;
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

    public int[] getChannelsAvailableDividers() {
        return adsConfig.getAdsChannelsAvailableDividers();
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

    public AdsType getDeviceType() {
        return adsConfig.getAdsType();
    }

    public void setDeviceType(AdsType adsType) {
        adsConfig.setAdsType(adsType);
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

    EdfConfig getAdsDataRecordConfig() {
        DefaultEdfConfig edfConfig = new DefaultEdfConfig();
        edfConfig.setRecordingIdentification(getRecordingIdentification());
        edfConfig.setPatientIdentification(getPatientIdentification());
        edfConfig.setDurationOfDataRecord(adsConfig.getDurationOfDataRecord());
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            if (adsConfig.isAdsChannelEnabled(i)) {
                edfConfig.addSignal();
                int signalNumber = edfConfig.getNumberOfSignals() - 1;
                edfConfig.setTransducer(signalNumber, "Unknown");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAdsChannelsPhysicalDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getAdsChannelPhysicalMin(i), adsConfig.getAdsChannelPhysicalMax(i));
                edfConfig.setDigitalRange(signalNumber, adsConfig.getAdsChannelsDigitalMin(), adsConfig.getAdsChannelsDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAdsChannelSampleRate(i));
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                edfConfig.setLabel(signalNumber, adsConfig.getAdsChannelName(i));
            }
        }

        if (adsConfig.isAccelerometerEnabled()) {
            if (adsConfig.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                edfConfig.addSignal();
                int signalNumber = edfConfig.getNumberOfSignals() - 1;
                edfConfig.setLabel(signalNumber, "Accelerometer");
                edfConfig.setTransducer(signalNumber, "None");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            } else {
                String[] accelerometerChannelNames = {"Accelerometer X", "Accelerometer Y", "Accelerometer Z"};
                for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                    edfConfig.addSignal();
                    int signalNumber = edfConfig.getNumberOfSignals() - 1;
                    edfConfig.setLabel(signalNumber, accelerometerChannelNames[i]);
                    edfConfig.setTransducer(signalNumber, "None");
                    edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                    edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                    edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                    edfConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                    edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                }
            }
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            edfConfig.addSignal();
            int signalNumber = edfConfig.getNumberOfSignals() - 1;
            edfConfig.setLabel(signalNumber, "Battery voltage");
            edfConfig.setTransducer(signalNumber, "None");
            edfConfig.setPhysicalDimension(signalNumber, adsConfig.getBatteryVoltageDimension());
            edfConfig.setPhysicalRange(signalNumber, adsConfig.getBatteryVoltagePhysicalMin(), adsConfig.getBatteryVoltagePhysicalMax());
            edfConfig.setDigitalRange(signalNumber, adsConfig.getBatteryVoltageDigitalMin(), adsConfig.getBatteryVoltageDigitalMax());
            edfConfig.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        if (adsConfig.isLeadOffEnabled()) {
            edfConfig.addSignal();
            int signalNumber = edfConfig.getNumberOfSignals() - 1;
            edfConfig.setLabel(signalNumber, "Loff Status");
            edfConfig.setTransducer(signalNumber, "None");
            edfConfig.setPhysicalDimension(signalNumber, adsConfig.getLeadOffStatusDimension());
            edfConfig.setPhysicalRange(signalNumber, adsConfig.getLeadOffStatusPhysicalMin(), adsConfig.getLeadOffStatusPhysicalMax());
            edfConfig.setDigitalRange(signalNumber, adsConfig.getLeadOffStatusDigitalMin(), adsConfig.getLeadOffStatusDigitalMax());
            edfConfig.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        return edfConfig;
    }


}
