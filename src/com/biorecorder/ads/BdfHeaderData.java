package com.biorecorder.ads;


import com.biorecorder.edflib.HeaderConfig;
import com.biorecorder.edflib.SignalConfig;
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

    public double getDurationOfDataRecord() {
        return 1.0 * adsConfiguration.getDeviceType().getMaxDiv().getValue()/
                adsConfiguration.getSps().getValue();
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



    public HeaderConfig getHeaderConfig() {
        HeaderConfig headerConfig = new HeaderConfig();
        headerConfig.setPatientId(getPatientIdentification());
        headerConfig.setRecordingId(getRecordingIdentification());
        headerConfig.setStartTime(getStartRecordingTime());
        headerConfig.setDurationOfDataRecord(getDurationOfDataRecord());

        List<AdsChannelConfiguration> channelConfigurations = adsConfiguration.getAdsChannels();
        for (int i = 0; i < channelConfigurations.size(); i++) {
            if (channelConfigurations.get(i).isEnabled) {
                SignalConfig signal = new SignalConfig();
                signal.setTransducerType("Unknown");
                signal.setPhysicalDimension("uV");
                int physicalMaximum = 2400000 / channelConfigurations.get(i).getGain().getValue();
                signal.setPhysicalMin(-physicalMaximum);
                signal.setPhysicalMax(physicalMaximum);
//        String channelsDigitalMaximum = "8388607";
//        String channelsDigitalMinimum = "-8388608";
                int digitalMaximum = Math.round(8388607 / getAdsConfiguration().getNoiseDivider());
                int digitalMinimum = Math.round(-8388608 / getAdsConfiguration().getNoiseDivider());
                signal.setDigitalMin(digitalMinimum);
                signal.setDigitalMax(digitalMaximum);
                signal.setPrefiltering("None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() *
                        adsConfiguration.getSps().getValue() /
                        channelConfigurations.get(i).getDivider().getValue());
                signal.setNumberOfSamplesInEachDataRecord(nrOfSamplesInEachDataRecord);
                signal.setLabel(channelConfigurations.get(i).getName());
                headerConfig.addSignalConfig(signal);
            }
        }


        if (adsConfiguration.isAccelerometerEnabled()) {
            if (adsConfiguration.isAccelerometerOneChannelMode()) {
                SignalConfig signal = new SignalConfig();
                signal.setLabel("Accelerometer");
                signal.setTransducerType("None");
                signal.setPhysicalDimension("m/sec^3");
                signal.setPhysicalMin(-1000);
                signal.setPhysicalMax(1000);
                signal.setDigitalMax(2000);
                signal.setDigitalMin(-2000);
                signal.setPrefiltering("None");
                int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                        adsConfiguration.getAccelerometerDivider().getValue());
                signal.setNumberOfSamplesInEachDataRecord(nrOfSamplesInEachDataRecord);
                headerConfig.addSignalConfig(signal);
            } else {
                int accelerometerDigitalMaximum = 9610;
                int accelerometerDigitalMinimum = 4190;
                int accelerometerPhysicalMaximum = 1000;
                int accelerometerPhysicalMinimum = -1000;

                for (int i = 0; i < 3; i++) {     //3 accelerometer chanels
                    SignalConfig signal = new SignalConfig();
                    signal.setLabel(getAccelerometerChannelNames().get(i));
                    signal.setTransducerType("None");
                    signal.setPhysicalDimension("mg");
                    signal.setPhysicalMin(accelerometerPhysicalMinimum);
                    signal.setPhysicalMax(accelerometerPhysicalMaximum);
                    signal.setDigitalMax(accelerometerDigitalMaximum);
                    signal.setDigitalMin(accelerometerDigitalMinimum);
                    signal.setPrefiltering("None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(getDurationOfDataRecord() * adsConfiguration.getSps().getValue() /
                            adsConfiguration.getAccelerometerDivider().getValue());
                    signal.setNumberOfSamplesInEachDataRecord(nrOfSamplesInEachDataRecord);
                    headerConfig.addSignalConfig(signal);
                }
            }
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
            SignalConfig signal = new SignalConfig();
            signal.setLabel("Battery voltage");
            signal.setTransducerType("None");
            signal.setPhysicalDimension("V");
            signal.setPhysicalMin(0);
            signal.setPhysicalMax(50);
            signal.setDigitalMax(10240);
            signal.setDigitalMin(0);
            signal.setPrefiltering("None");
            int nrOfSamplesInEachDataRecord = 1;
            signal.setNumberOfSamplesInEachDataRecord(nrOfSamplesInEachDataRecord);
            headerConfig.addSignalConfig(signal);
        }
        if (adsConfiguration.isLoffEnabled()) {
            SignalConfig signal = new SignalConfig();
            signal.setLabel("Loff Status");
            signal.setTransducerType("None");
            signal.setPhysicalDimension("Bit mask");
            signal.setPhysicalMin(0);
            signal.setPhysicalMax(65536);
            signal.setDigitalMax(65536);
            signal.setDigitalMin(0);
            signal.setPrefiltering("None");
            int nrOfSamplesInEachDataRecord = 1;
            signal.setNumberOfSamplesInEachDataRecord(nrOfSamplesInEachDataRecord);
            headerConfig.addSignalConfig(signal);
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
