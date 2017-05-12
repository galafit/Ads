package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsConfig;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.HeaderInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.istack.internal.Nullable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class BdfRecorderConfig {
    private AdsConfig adsConfig = new AdsConfig();
    private List<Boolean> filter50HzMask = new ArrayList<Boolean>();
    private String dirToSave = new File(System.getProperty("user.dir"), "records").getAbsolutePath();
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    @JsonIgnore
    private String fileNameToSave;

    public void setFileNameToSave(String fileNameToSave) {
        this.fileNameToSave = fileNameToSave;
    }

    public void setDirectoryToSave(File directory) {
        dirToSave = directory.getAbsolutePath();
    }

    public File getDirToSave() {
        return new File(dirToSave);
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

    public File getFileToSave() {
        String filename = normalizeFilename(fileNameToSave);
        return new File(dirToSave, filename);
    }


    private double getDurationOfDataRecord() {
        return 1.0 * adsConfig.getMaxDiv()/
                adsConfig.getSps().getValue();
    }



    public AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfiguration) {
        this.adsConfig = adsConfiguration;
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

    public HeaderInfo getHeaderConfig() {
        HeaderInfo headerInfo = new HeaderInfo(FileType.BDF_24BIT);
        headerInfo.setPatientIdentification(getPatientIdentification());
        headerInfo.setRecordingIdentification(getRecordingIdentification());
        headerInfo.setDurationOfDataRecord(getDurationOfDataRecord());
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            if (adsConfig.getAdsChannel(i).isEnabled()) {
                headerInfo.addSignal();
                int signalNumber = headerInfo.getNumberOfSignals() - 1;
                headerInfo.setTransducer(signalNumber,"Unknown");
                headerInfo.setPhysicalDimension(signalNumber, "uV");
                int physicalMaximum = 2400000 / adsConfig.getAdsChannel(i).getGain().getValue();
                headerInfo.setPhysicalRange(signalNumber, -physicalMaximum, physicalMaximum);
                int digitalMaximum = Math.round(8388607 / getAdsConfig().getNoiseDivider());
                int digitalMinimum = Math.round(-8388608 / getAdsConfig().getNoiseDivider());
                headerInfo.setDigitalRange(signalNumber, digitalMinimum, digitalMaximum);
                headerInfo.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() *
                        adsConfig.getSps().getValue() /
                        adsConfig.getAdsChannel(i).getDivider().getValue());
                headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                headerInfo.setLabel(i, adsConfig.getAdsChannel(i).getName());
            }
        }

        if (adsConfig.isAccelerometerEnabled()) {
            if (adsConfig.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                headerInfo.addSignal();
                int signalNumber = headerInfo.getNumberOfSignals() - 1;
                headerInfo.setLabel(signalNumber, "Accelerometer");
                headerInfo.setTransducer(signalNumber, "None");
                headerInfo.setPhysicalDimension(signalNumber, "m/sec^3");
                headerInfo.setPhysicalRange(signalNumber, -1000, 1000);
                headerInfo.setDigitalRange(signalNumber, -2000, 2000 );
                headerInfo.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfig.getSps().getValue() /
                        adsConfig.getAccelerometerDivider().getValue());
                headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            } else {
                int accelerometerDigitalMaximum = 9610;
                int accelerometerDigitalMinimum = 4190;
                int accelerometerPhysicalMaximum = 1000;
                int accelerometerPhysicalMinimum = -1000;

                String[] accelerometerChannelNames = {"Accelerometer X", "Accelerometer Y", "Accelerometer Z"};
                for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                    headerInfo.addSignal();
                    int signalNumber = headerInfo.getNumberOfSignals() - 1;
                    headerInfo.setLabel(signalNumber, accelerometerChannelNames[i]);
                    headerInfo.setTransducer(signalNumber,"None");
                    headerInfo.setPhysicalDimension(signalNumber,"mg");
                    headerInfo.setPhysicalRange(signalNumber, accelerometerPhysicalMinimum, accelerometerPhysicalMaximum);
                    headerInfo.setDigitalRange(signalNumber, accelerometerDigitalMinimum, accelerometerDigitalMaximum);
                    headerInfo.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfig.getSps().getValue() /
                            adsConfig.getAccelerometerDivider().getValue());
                    headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                }
            }
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            headerInfo.addSignal();
            int signalNumber = headerInfo.getNumberOfSignals() - 1;
            headerInfo.setLabel(signalNumber, "Battery voltage");
            headerInfo.setTransducer(signalNumber, "None");
            headerInfo.setPhysicalDimension(signalNumber, "V");
            headerInfo.setPhysicalRange(signalNumber, 0, 50);
            headerInfo.setDigitalRange(signalNumber, 0, 10240);
            headerInfo.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        if (adsConfig.isLoffEnabled()) {
            headerInfo.addSignal();
            int signalNumber = headerInfo.getNumberOfSignals() - 1;
            headerInfo.setLabel(signalNumber, "Loff Status");
            headerInfo.setTransducer(signalNumber, "None");
            headerInfo.setPhysicalDimension(signalNumber, "Bit mask");
            headerInfo.setPhysicalRange(signalNumber, 0, 65536);
            headerInfo.setDigitalRange(signalNumber, 0, 65536);
            headerInfo.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        return headerInfo;
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


}
