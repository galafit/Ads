package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.ads.exceptions.PortBusyRuntimeException;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import com.biorecorder.bdfrecorder.exceptions.*;
import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.DefaultEdfConfig;
import com.biorecorder.edflib.base.EdfConfig;
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

    private boolean isRecording = false;
    private double resultantDataRecordDuration = 1; // sec
    private EdfFilter edfWriter;
    private File bdfFile;

    public BdfRecorder() {
        ads = new Ads();
        ads.addAdsEventsListener(this);
    }

    public void addBdfDataListener(BdfDataListener listener) {
        dataListeners.add(listener);
    }

    public void addLowButteryEventListener(LowButteryEventListener listener) {
        butteryEventListeners.add(listener);
    }

    /**
     * Get the info about edf
     * @return
     */
    public EdfConfig getRecordingInfo(BdfRecorderConfig bdfRecorderConfig) {
        if(edfWriter == null) {
            edfWriter = createEdfWriter(null, bdfRecorderConfig , null, null);
        }
        return edfWriter.getResultantConfig();
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


    public  String[] getAvailableComportNames() {
        return ads.getAvailableComportNames();
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
    public void startRecording(BdfRecorderConfig bdfRecorderConfig, @Nullable File file) throws IllegalStateException, BdfFileNotFoundRuntimeException {
        if(! ads.isConnected()) {
            String errMsg = "BdfRecorder must be connected to some serial port first!";
            throw new IllegalStateException(errMsg);
        }
        try {
            edfWriter = createEdfWriter(file, bdfRecorderConfig, bdfRecorderConfig.getPatientIdentification(), bdfRecorderConfig.getRecordingIdentification());
            ads.addAdsDataListener(new AdsDataListener() {
                @Override
                public void onDataReceived(int[] dataFrame) {
                    edfWriter.writeDigitalRecord(dataFrame);
                }
            });
            ads.sendStartCommand(bdfRecorderConfig.getAdsConfig());
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

    /**
     * "Lead-Off" detection serves to alert/notify when an electrode is making poor electrical
     * contact or disconnecting. Therefore in Lead-Off detection mask TRUE means DISCONNECTED and
     * FALSE means CONNECTED.
     * <p>
     * Every ads-channel has 2 electrodes (Positive and Negative) so in leadOff detection mask:
     * <br>
     * element-0 and element-1 correspond to Positive and Negative electrodes of ads channel 0,
     * element-2 and element-3 correspond to Positive and Negative electrodes of ads channel 1,
     * ...
     * element-14 and element-15 correspond to Positive and Negative electrodes of ads channel 8.
     * <p>
     * @return leadOff detection mask or null if ads is stopped or
     * leadOff detection is disabled
     */
    public boolean[] getLeadOfDetectionMask() {
        return ads.getLeadOfDetectionMask();
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

    private EdfFilter createEdfWriter(@Nullable File file, BdfRecorderConfig bdfRecorderConfig, @Nullable String patientId, @Nullable String recordingId) throws FileNotFoundRuntimeException {
        EdfDataReceiver edfDataReceiver = new EdfDataReceiver(file);

        EdfConfig adsDataRecordConfig = createAdsDataRecordConfig(bdfRecorderConfig.getAdsConfig(), patientId, recordingId);

        System.out.print(adsDataRecordConfig);
        // join DataRecords to have data records length = resultantDataRecordDuration;
        int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
        EdfJoiner edfJoiner = new EdfJoiner(numberOfFramesToJoin, edfDataReceiver);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        EdfSignalsFilter edfSignalsFilter = new EdfSignalsFilter(edfJoiner);
        //int sps = bdfRecorderConfig.getSampleRate();
        int enableSignalsCounter = 0;
        for (int i = 0; i < bdfRecorderConfig.getNumberOfAdsChannels(); i++) {
            if (bdfRecorderConfig.isAdsChannelEnabled(i)) {
                if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                    edfSignalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(bdfRecorderConfig.getAdsChannelFrequency(i) / 50));
                }
                enableSignalsCounter++;
            }
        }
        // delete helper Loff channel
        EdfSignalsRemover edfSignalsRemover = new EdfSignalsRemover(edfSignalsFilter);
        if (bdfRecorderConfig.isLeadOffEnabled()) {
            edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
        }

        edfSignalsRemover.setConfig(adsDataRecordConfig);
        return edfSignalsRemover;
    }

    EdfConfig createAdsDataRecordConfig(AdsConfig adsConfig, @Nullable String patientId, @Nullable String recordingId) {
        DefaultEdfConfig edfConfig = new DefaultEdfConfig();
        if (patientId != null) {
            edfConfig.setPatientIdentification(patientId);
        }
        if (patientId != null) {
            edfConfig.setRecordingIdentification(recordingId);
        }
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


    class EdfDataReceiver extends EdfWriter {
        private EdfFileWriter edfFileWriter = null;

        public EdfDataReceiver(@Nullable File file) {
            if(file != null) {
                bdfFile = file;
                edfFileWriter = new EdfFileWriter(file, FileType.BDF_24BIT);
            }
        }

        @Override
        public void setConfig(EdfConfig recordingInfo) {
            super.setConfig(recordingInfo);
            if(edfFileWriter != null) {
                edfFileWriter.setConfig(recordingInfo);
            }
        }

        @Override
        public void writeDigitalSamples(int[] samples, int offset, int length) {
            sampleCounter += samples.length;
            for (BdfDataListener listener : dataListeners) {
                listener.onDataRecordReceived(samples);
            }
            if(edfFileWriter != null) {
                edfFileWriter.writeDigitalSamples(samples, offset, length);
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
