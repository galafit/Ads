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
    private AdsListenerBdfWriter adsListenerBdfWriter;

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
     * Get the info describing the structure of dataRecords
     * that BdfRecorder sends to its listeners and write to the Edf/Bdf file
     * @return object with info about recording process and dataRecords structure
     */
    public EdfConfig getRecordingInfo(BdfRecorderConfig bdfRecorderConfig) {
        if(adsListenerBdfWriter == null) {
            adsListenerBdfWriter = new AdsListenerBdfWriter(bdfRecorderConfig , null, resultantDataRecordDuration,null);
        }
        return adsListenerBdfWriter.getResultantEdfConfig();
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
            adsListenerBdfWriter = new AdsListenerBdfWriter(bdfRecorderConfig, file, resultantDataRecordDuration, dataListeners);
            ads.addAdsDataListener(adsListenerBdfWriter);
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
            ads.removeAdsDataListener(adsListenerBdfWriter);
            if(adsListenerBdfWriter != null) {
                adsListenerBdfWriter.close();
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
        if(adsListenerBdfWriter != null) {
            return adsListenerBdfWriter.getFile();
        }
        return null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isActive() {
        return ads.isActive();
    }
    
    public int getNumberOfSentDataRecords() {
        if (adsListenerBdfWriter != null) {
            return adsListenerBdfWriter.getNumberOfWrittenDataRecords();

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

}
