package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.ads.exceptions.PortBusyRuntimeException;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import com.biorecorder.bdfrecorder.exceptions.*;
import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.DefaultEdfRecordingInfo;
import com.biorecorder.edflib.base.EdfRecordingInfo;
import com.biorecorder.edflib.base.EdfWriter;
import com.biorecorder.edflib.exceptions.FileNotFoundRuntimeException;
import com.biorecorder.edflib.filters.EdfFilter;
import com.biorecorder.edflib.filters.EdfJoiner;
import com.biorecorder.edflib.filters.EdfSignalsFilter;
import com.biorecorder.edflib.filters.EdfSignalsRemover;
import com.biorecorder.edflib.filters.signalfilters.MovingAverageFilter;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class BdfRecorder implements AdsEventsListener {
    private static final Log log = LogFactory.getLog(BdfRecorder.class);
    private Ads ads = new Ads();
    private List<BdfDataListener> dataListeners = new ArrayList<BdfDataListener>();
    private List<LowButteryEventListener> butteryEventListeners = new ArrayList<LowButteryEventListener>();

    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    private boolean isRecording = false;
    private double resultantDataRecordDuration = 1; // sec
    private EdfFilter edfWriter;
    private File bdfFile;

    private EdfWriter receiver;

    public BdfRecorder() {
        ads = new Ads();
        ads.setAdsEventsListener(this);
        bdfRecorderConfig.setAdsConfig(ads.getConfig());
    }

    public void setConfig(BdfRecorderConfig bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
        ads.setConfig(bdfRecorderConfig.getAdsConfig());
    }

    public BdfRecorderConfig getConfig() {
        return bdfRecorderConfig;
    }


    public void addBdfDataListener(BdfDataListener listener) {
        dataListeners.add(listener);
    }

    public void addLowButteryEventListener(LowButteryEventListener listener) {
        butteryEventListeners.add(listener);
    }

    public EdfRecordingInfo getRecordingInfo() {
        if(edfWriter == null) {
            edfWriter = createEdfWriter(null, ads.getConfig());
        }
        return edfWriter.getResultantRecordingInfo();
    }

    /**
     *
     * @param comportName
     * @throws ComportNotFoundRuntimeException
     * @throws ComportBusyRuntimeException
     * @throws ConnectionRuntimeException
     */
    public void connect(String comportName) throws  ComportNotFoundRuntimeException, ComportBusyRuntimeException, ConnectionRuntimeException {
        try {
            ads.connect(comportName);
        } catch (PortNotFoundRuntimeException ex) {
            throw new ComportNotFoundRuntimeException(ex);
        } catch (PortBusyRuntimeException ex) {
            throw new ComportBusyRuntimeException(ex);
        } catch (Exception ex) {
            String errMsg = "Error during connecting to serial port: "+comportName;
            throw new ConnectionRuntimeException(errMsg, ex);
        }

    }

    /**
     *
     * @throws ConnectionRuntimeException
     */
    public void disconnect()  throws ConnectionRuntimeException {
        try {
            ads.disconnect();
        } catch (Exception ex) {
            String errMsg = "Error during disconnecting";
            throw new ConnectionRuntimeException(errMsg, ex);
        }
    }

    public boolean isConnected() {
        return ads.isConnected();
    }

    public static boolean isComportAvailable(String comportName) {
        return Ads.isComportAvailable(comportName);
    }

    public static String[] getAvailableComportNames() {
        return Ads.getAvailableComportNames();
    }


    /**
     * Start BdfRecorder. The data records are send to all BdfDataListeners
     * If file is not null the data are also written to the file.
     *
     * @param file file the data will be saved
     * @throws IllegalStateException if BdfRecorder is not connected
     * @throws BdfFileNotFoundRuntimeException If file to write data could not be created or
     * does not have permission to write
     */
    public void startRecording(@Nullable File file) throws IllegalStateException, BdfFileNotFoundRuntimeException {
        if(! ads.isConnected()) {
            String errMsg = "BdfRecorder must be connected to some serial port first!";
            throw new IllegalStateException(errMsg);
        }
        try {
            AdsConfig adsConfig = bdfRecorderConfig.getAdsConfig();
            ads.setConfig(adsConfig);
            edfWriter = createEdfWriter(file, adsConfig);
            ads.setAdsDataListener(new AdsDataListener() {
                @Override
                public void onDataReceived(int[] dataFrame) {
                    edfWriter.writeDigitalSamples(dataFrame);
                }
            });
            ads.sendStartCommand();
            isRecording = true;
        } catch (FileNotFoundRuntimeException ex) {
            String errMsg = MessageFormat.format("File: \"{0}\" could not be created or accessed", file);
            new BdfFileNotFoundRuntimeException(errMsg, ex);
        }

    }

    /**
     *
     * @throws BdfRecorderRuntimeException if some kind of error occurs during
     * stopping recording
     */
    public void stopRecording() throws BdfRecorderRuntimeException {
        if(!isRecording) {
            return;
        }
        try {
            ads.sendStopRecordingCommand();
            if(edfWriter != null) {
                edfWriter.close();
            }
            isRecording = false;
        } catch (Exception e) {
            String errMsg = "Error during BdfRecorder stop recording: "+e.getMessage();
            throw new BdfRecorderRuntimeException(errMsg, e);

        }
    }

    public File getSavedFile() {
        return bdfFile;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isActive() {
        return ads.isActive();
    }
    
    public int getNumberOfSentDataRecords() {
        if (edfWriter != null) {
            return edfWriter.getNumberOfWrittenDataRecords();

        }
        return 0;
    }

    @Override
    public void handleAdsLowButtery() {
        for (LowButteryEventListener listener : butteryEventListeners) {
            listener.handleLowButteryEvent();
        }
    }

    @Override
    public void handleAdsFrameBroken(String eventInfo) {
        log.info(eventInfo);
    }

    private EdfFilter createEdfWriter(@Nullable File file, AdsConfig adsConfig) throws FileNotFoundRuntimeException {
        EdfDataReceiver edfDataReceiver = new EdfDataReceiver(file);

        EdfRecordingInfo adsDataRecordConfig = createAdsDataRecordConfig(adsConfig);

        // join DataRecords to have data records length = resultantDataRecordDuration;
        int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
        EdfJoiner dataRecordsJoiner = new EdfJoiner(numberOfFramesToJoin, edfDataReceiver);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        EdfSignalsFilter signalsFilter = new EdfSignalsFilter(dataRecordsJoiner);
        //int sps = bdfRecorderConfig.getSampleRate();
        int enableSignalsCounter = 0;
        for (int i = 0; i < bdfRecorderConfig.getNumberOfAdsChannels(); i++) {
            if (bdfRecorderConfig.isAdsChannelEnabled(i)) {
                if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                    signalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(bdfRecorderConfig.getAdsChannelFrequency(i) / 50));
                }
                enableSignalsCounter++;
            }
        }
        // delete helper Loff channel
        EdfSignalsRemover signalsRemover = new EdfSignalsRemover(signalsFilter);
        if (bdfRecorderConfig.isLoffEnabled()) {
            signalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
        }

        signalsRemover.setRecordingInfo(adsDataRecordConfig);
        receiver = edfDataReceiver;
        return signalsRemover;
    }

    EdfRecordingInfo createAdsDataRecordConfig(AdsConfig adsConfig) {
        DefaultEdfRecordingInfo edfConfig = new DefaultEdfRecordingInfo();
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
                edfConfig.setLabel(i, adsConfig.getAdsChannelName(i));
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
        if (adsConfig.isLoffEnabled()) {
            edfConfig.addSignal();
            int signalNumber = edfConfig.getNumberOfSignals() - 1;
            edfConfig.setLabel(signalNumber, "Loff Status");
            edfConfig.setTransducer(signalNumber, "None");
            edfConfig.setPhysicalDimension(signalNumber, adsConfig.getLoffStatusDimension());
            edfConfig.setPhysicalRange(signalNumber, adsConfig.getLoffStatusPhysicalMin(), adsConfig.getLoffStatusPhysicalMax());
            edfConfig.setDigitalRange(signalNumber, adsConfig.getLoffStatusDigitalMin(), adsConfig.getLoffStatusDigitalMax());
            edfConfig.setPrefiltering(signalNumber, "None");
            int nrOfSamplesInEachDataRecord = 1;
            edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
        }
        return edfConfig;
    }


    class EdfDataReceiver extends EdfWriter {
        private EdfFileWriter edfFileWriter = null;

        public EdfDataReceiver(@Nullable File file) {
            if(file != null) {
                bdfFile = file;
                edfFileWriter = new EdfFileWriter(file, FileType.BDF_24BIT);
            }
        }

        @Override
        public void setRecordingInfo(EdfRecordingInfo recordingInfo) {
            super.setRecordingInfo(recordingInfo);
            if(edfFileWriter != null) {
                edfFileWriter.setRecordingInfo(recordingInfo);
            }
        }

        @Override
        public void writeDigitalSamples(int[] samples) {
            sampleCounter += samples.length;
            for (BdfDataListener listener : dataListeners) {
                listener.onDataRecordReceived(samples);
            }
            if(edfFileWriter != null) {
                edfFileWriter.writeDigitalSamples(samples);
            }
        }

        @Override
        public void close() {
            if(edfFileWriter != null) {
                edfFileWriter.close();
                if(getNumberOfReceivedDataRecords() == 0) {
                    bdfFile.delete();
                    bdfFile = null;
                }
            }
        }
    }
}
