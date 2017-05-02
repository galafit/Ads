package com.biorecorder.ads;

import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.HeaderInfo;
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

    public HeaderInfo getHeaderConfig() {
        HeaderInfo headerInfo = new HeaderInfo(FileType.BDF_24BIT);
        headerInfo.setPatientIdentification(getPatientIdentification());
        headerInfo.setRecordingIdentification(getRecordingIdentification());
        headerInfo.setDurationOfDataRecord(getDurationOfDataRecord());
        List<AdsChannelConfiguration> channelConfigurations = adsConfiguration.getAdsChannels();
        for (int i = 0; i < channelConfigurations.size(); i++) {
            if (channelConfigurations.get(i).isEnabled) {
                headerInfo.addSignal();
                int signalNumber = headerInfo.getNumberOfSignals() - 1;
                headerInfo.setTransducer(signalNumber,"Unknown");
                headerInfo.setPhysicalDimension(signalNumber, "uV");
                int physicalMaximum = 2400000 / channelConfigurations.get(i).getGain().getValue();
                headerInfo.setPhysicalRange(signalNumber, -physicalMaximum, physicalMaximum);
                int digitalMaximum = Math.round(8388607 / getAdsConfiguration().getNoiseDivider());
                int digitalMinimum = Math.round(-8388608 / getAdsConfiguration().getNoiseDivider());
                headerInfo.setDigitalRange(signalNumber, digitalMinimum, digitalMaximum);
                headerInfo.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() *
                        adsConfiguration.getSps().getValue() /
                        channelConfigurations.get(i).getDivider().getValue());
                headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                headerInfo.setLabel(i, channelConfigurations.get(signalNumber).getName());
            }
        }

        if (adsConfiguration.isAccelerometerEnabled()) {
            if (adsConfiguration.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                headerInfo.addSignal();
                int signalNumber = headerInfo.getNumberOfSignals() - 1;
                headerInfo.setLabel(signalNumber, "Accelerometer");
                headerInfo.setTransducer(signalNumber, "None");
                headerInfo.setPhysicalDimension(signalNumber, "m/sec^3");
                headerInfo.setPhysicalRange(signalNumber, -1000, 1000);
                headerInfo.setDigitalRange(signalNumber, -2000, 2000 );
                headerInfo.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                        adsConfiguration.getAccelerometerDivider().getValue());
                headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            } else {
                int accelerometerDigitalMaximum = 9610;
                int accelerometerDigitalMinimum = 4190;
                int accelerometerPhysicalMaximum = 1000;
                int accelerometerPhysicalMinimum = -1000;

                for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                    headerInfo.addSignal();
                    int signalNumber = headerInfo.getNumberOfSignals() - 1;
                    headerInfo.setLabel(signalNumber, getAccelerometerChannelNames().get(i));
                    headerInfo.setTransducer(signalNumber,"None");
                    headerInfo.setPhysicalDimension(signalNumber,"mg");
                    headerInfo.setPhysicalRange(signalNumber, accelerometerPhysicalMinimum, accelerometerPhysicalMaximum);
                    headerInfo.setDigitalRange(signalNumber, accelerometerDigitalMinimum, accelerometerDigitalMaximum);
                    headerInfo.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                            adsConfiguration.getAccelerometerDivider().getValue());
                    headerInfo.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                }
            }
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
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
        if (adsConfiguration.isLoffEnabled()) {
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
