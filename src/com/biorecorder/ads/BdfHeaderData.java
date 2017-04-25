package com.biorecorder.ads;

import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.HeaderConfig;
import com.sun.istack.internal.Nullable;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class  BdfHeaderData {

    private String fileNameToSave;
    private AdsConfiguration adsConfiguration;

    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long startRecordingTime;
    private int numberOfDataRecords = -1;
    List<String> accelerometerChannelNames = new ArrayList<String>();

    public BdfHeaderData(AdsConfiguration adsConfiguration) {
        this.adsConfiguration = adsConfiguration;
        accelerometerChannelNames.add("Accelerometer X");
        accelerometerChannelNames.add("Accelerometer Y");
        accelerometerChannelNames.add("Accelerometer Z");
    }

    public void setFileNameToSave(String fileNameToSave) {
        this.fileNameToSave = fileNameToSave;
    }

    public void setDirectoryToSave(String directory) {
        adsConfiguration.setDirectoryToSave(directory);
    }

    public File getFileToSave() {
        String directory = adsConfiguration.getDirectoryToSave();
        String filename = normalizeFilename(fileNameToSave);
        return new File(directory, filename);
    }

    public List<String> getAccelerometerChannelNames() {
        return accelerometerChannelNames;
    }

    private double getDurationOfDataRecord() {
        return 1.0 * adsConfiguration.getDeviceType().getMaxDiv().getValue()/
                adsConfiguration.getSps().getValue();
    }

    public long getStartRecordingTime() {
        return startRecordingTime;
    }

    public AdsConfiguration getAdsConfiguration() {
        return adsConfiguration;
    }

    public void setAdsConfiguration(AdsConfiguration adsConfiguration) {
        this.adsConfiguration = adsConfiguration;
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



    public HeaderConfig getHeaderConfig() {
        HeaderConfig headerConfig = new HeaderConfig(FileType.BDF_24BIT);
        headerConfig.setPatientIdentification(getPatientIdentification());
        headerConfig.setRecordingIdentification(getRecordingIdentification());
        headerConfig.setRecordingStartTime(getStartRecordingTime());
        headerConfig.setDurationOfDataRecord(getDurationOfDataRecord());
        List<AdsChannelConfiguration> channelConfigurations = adsConfiguration.getAdsChannels();
        for (int i = 0; i < channelConfigurations.size(); i++) {
            if (channelConfigurations.get(i).isEnabled) {
                headerConfig.addSignal();
                int signalNumber = headerConfig.getNumberOfSignals() - 1;
                headerConfig.setTransducer(signalNumber,"Unknown");
                headerConfig.setPhysicalDimension(signalNumber, "uV");
                int physicalMaximum = 2400000 / channelConfigurations.get(i).getGain().getValue();
                headerConfig.setPhysicalMin(signalNumber, -physicalMaximum);
                headerConfig.setPhysicalMax(signalNumber, physicalMaximum);
                int digitalMaximum = Math.round(8388607 / getAdsConfiguration().getNoiseDivider());
                int digitalMinimum = Math.round(-8388608 / getAdsConfiguration().getNoiseDivider());
                headerConfig.setDigitalMin(signalNumber, digitalMinimum);
                headerConfig.setDigitalMax(signalNumber,digitalMaximum);
                //headerConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() *
                        adsConfiguration.getSps().getValue() /
                        channelConfigurations.get(i).getDivider().getValue());
                headerConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                headerConfig.setLabel(i, channelConfigurations.get(signalNumber).getName());
            }
        }

        if (adsConfiguration.isAccelerometerEnabled()) {
            if (adsConfiguration.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                int signalNumber = headerConfig.getNumberOfSignals() - 1;
                headerConfig.addSignal();
                headerConfig.setLabel(signalNumber, "Accelerometer");
                headerConfig.setTransducer(signalNumber, "None");
                headerConfig.setPhysicalDimension(signalNumber, "m/sec^3");
                headerConfig.setPhysicalMin(signalNumber,-1000);
                headerConfig.setPhysicalMax(signalNumber,1000);
                headerConfig.setDigitalMax(signalNumber,2000);
                headerConfig.setDigitalMin(signalNumber,-2000);
                headerConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                        adsConfiguration.getAccelerometerDivider().getValue());
                headerConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            } else {
                int accelerometerDigitalMaximum = 9610;
                int accelerometerDigitalMinimum = 4190;
                int accelerometerPhysicalMaximum = 1000;
                int accelerometerPhysicalMinimum = -1000;

                for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                    headerConfig.addSignal();
                    int signalNumber = headerConfig.getNumberOfSignals() - 1;
                    headerConfig.setLabel(signalNumber, getAccelerometerChannelNames().get(i));
                    headerConfig.setTransducer(signalNumber,"None");
                    headerConfig.setPhysicalDimension(signalNumber,"mg");
                    headerConfig.setPhysicalMin(signalNumber, accelerometerPhysicalMinimum);
                    headerConfig.setPhysicalMax(signalNumber, accelerometerPhysicalMaximum);
                    headerConfig.setDigitalMax(signalNumber, accelerometerDigitalMaximum);
                    headerConfig.setDigitalMin(signalNumber, accelerometerDigitalMinimum);
                    headerConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                            adsConfiguration.getAccelerometerDivider().getValue());
                    headerConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                }
            }
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
            headerConfig.addSignal();
            int signalNumber = headerConfig.getNumberOfSignals() - 1;
            headerConfig.setLabel(signalNumber, "Battery voltage");
            headerConfig.setTransducer(signalNumber, "None");
            headerConfig.setPhysicalDimension(signalNumber, "V");
            headerConfig.setPhysicalMin(signalNumber, 0);
            headerConfig.setPhysicalMax(signalNumber, 50);
            headerConfig.setDigitalMax(signalNumber, 10240);
            headerConfig.setDigitalMin(signalNumber, 0);
            headerConfig.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            headerConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        if (adsConfiguration.isLoffEnabled()) {
            headerConfig.addSignal();
            int signalNumber = headerConfig.getNumberOfSignals() - 1;
            headerConfig.setLabel(signalNumber, "Loff Status");
            headerConfig.setTransducer(signalNumber, "None");
            headerConfig.setPhysicalDimension(signalNumber, "Bit mask");
            headerConfig.setPhysicalMin(signalNumber, 0);
            headerConfig.setPhysicalMax(signalNumber, 65536);
            headerConfig.setDigitalMax(signalNumber, 65536);
            headerConfig.setDigitalMin(signalNumber, 0);
            headerConfig.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            headerConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        return headerConfig;
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
