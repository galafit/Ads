package com.biorecorder.ads;

import com.sun.istack.internal.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class BdfHeaderData {

    private String fileNameToSave;
    private AdsConfiguration adsConfiguration;
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long startRecordingTime;
    private double durationOfDataRecord = 1.0;  // duration of EDF data record (in seconds)
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

    public List<String> getAdsChannelNames() {
        List<String> adsChannelNames = new ArrayList<String>();
        List<AdsChannelConfiguration> channelsConfiguration = adsConfiguration.getAdsChannels();
        for (int i = 0; i < adsConfiguration.getDeviceType().getNumberOfAdsChannels(); i++) {
            adsChannelNames.add(channelsConfiguration.get(i).getName());
        }

        return adsChannelNames;
    }

    public List<String> getAccelerometerChannelNames() {
        return accelerometerChannelNames;
    }

    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }

    public void setDurationOfDataRecord(double durationOfDataRecord) {
        this.durationOfDataRecord = durationOfDataRecord;
    }

    public int getNumberOfDataRecords() {
        return numberOfDataRecords;
    }

    public void setNumberOfDataRecords(int numberOfDataRecords) {
        this.numberOfDataRecords = numberOfDataRecords;
    }

    public long getStartRecordingTime() {
        return startRecordingTime;
    }

    public void setStartRecordingTime(long startRecordingTime) {
        this.startRecordingTime = startRecordingTime;
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
