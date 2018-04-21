package com.biorecorder;

import com.biorecorder.bdfrecorder.BdfRecorder;
import com.biorecorder.bdfrecorder.RecorderEventsListener;
import com.biorecorder.bdfrecorder.exceptions.*;

import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderApp implements RecorderEventsListener {
    private static final Log log = LogFactory.getLog(BdfRecorderApp.class);

    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    private static final String FILE_NOT_ACCESSIBLE_ERR = "File: {0} could not be created or accessed.";
    private static final String COMPORT_BUSY_ERR = "ComPort: {0} is busy.";
    private static final String COMPORT_NOT_FOUND_ERR = "ComPort: {0} is not found.";
    private static final String APP_ERR = "Error: {0}";
    private static final String LOW_BUTTERY_MSG = "The buttery is low. BdfRecorder was stopped.";
    private static final String START_CANCELLED_MSG = "Start cancelled.";
    private static final String START_FAILED_MSG = "Start failed. Check whether the device is connected" +
            "\nand selected ComPort is correct and try again.";
    private static final String DIR_CREATION_CONFIRMATION_MSG = "Directory: {0}\ndoes not exist. Do you want to create it?";


    private Preferences preferences;
    private BdfRecorder bdfRecorder = new BdfRecorder();

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    javax.swing.Timer notificationTimer;
    java.util.Timer connectionTimer;
    private volatile String selectedComportName;
    private AppConfig recordingSettings;

    private Future startFuture;

    private NotificationListener notificationListener;
    private MessageListener messageListener;

    public BdfRecorderApp(Preferences preferences, String selectedComportName) {
        this.preferences = preferences;
        this.selectedComportName = selectedComportName;
        bdfRecorder.addLowButteryEventListener(this);

        notificationTimer = new javax.swing.Timer(NOTIFICATION_PERIOD_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkStartFuture();
                if(notificationListener != null) {
                    notificationListener.update();
                }
            }
        });
        notificationTimer.start();

        connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
               daemonConnect();
            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    @Override
    public void handleStartCanceled() {
        // TO DO
    }

    private void checkStartFuture() {
        if(startFuture != null) {
            if (startFuture.isDone()) {
                try {
                    startFuture.get();
                } catch (ExecutionException ex) {
                    if(ex.getCause() instanceof InvalidDeviceTypeRuntimeException) {
                        sendMessage(ex.getCause().getMessage());

                    } else {
                        sendMessage(START_FAILED_MSG);
                    }
                } catch (CancellationException ex) {
                    sendMessage(START_CANCELLED_MSG);
                } catch (InterruptedException ie) {
                    sendMessage(START_FAILED_MSG);
                } finally {
                    startFuture = null;
                }
            }
        }
    }


    /**
     * If the comportName is not equal to any available port we add it to the list.
     * <p>
     * String full comparison is very "heavy" operation.
     * So instead we will compare only lengths and 2 last symbols...
     * That will be quick and good enough for our purpose
     * @return available comports list with selected port included
     */
    public synchronized String[] getComportNames() {
        String[] availablePorts = bdfRecorder.getAvailableComportNames();
        if(selectedComportName == null || selectedComportName.isEmpty()) {
            return availablePorts;
        }
        if(availablePorts.length == 0) {
            String[] resultantPorts = new String[1];
            resultantPorts[0] = selectedComportName;
            return resultantPorts;
        }

        boolean isSelectedPortAvailable = false;
        for (String port : availablePorts) {
            if(port.length() == selectedComportName.length()
                    && port.charAt(port.length() - 1) == selectedComportName.charAt(selectedComportName.length() - 1)
                    && port.charAt(port.length() - 2) == selectedComportName.charAt(selectedComportName.length() - 2)) {
                isSelectedPortAvailable = true;
                break;
            }
        }
        if(isSelectedPortAvailable) {
            return availablePorts;
        } else {
            String[] resultantPorts = new String[availablePorts.length + 1];
            resultantPorts[0] = selectedComportName;
            System.arraycopy(availablePorts, 0, resultantPorts, 1, availablePorts.length);
            return resultantPorts;
        }
    }

    public void changeRecorder() {
        stopRecording();
        recordingSettings = null;
      //  bdfRecorder.disconnect();
      //  bdfRecorder = new BdfRecorder();
    }


    @Override
    public void handleLowButtery() {
        stopRecording();
        sendMessage(LOW_BUTTERY_MSG);
    }


    /**
     * "Lead-Off" detection serves to alert/notify when an electrode is making poor electrical
     * contact or disconnecting. Therefore in Lead-Off detection mask has:
     * <ul>
     *     <li>TRUE if electrode DISCONNECTED</li>
     *     <li>FALSE if electrode CONNECTED</li>
     *     <li>NULL if the channel is disabled or its lead-off detection disabled or
     *     its commutator state != "INPUT"</li>
     * </ul>
     * Every ads-channel has 2 electrodes (Positive and Negative) so in leadOff detection mask:
     * <br>element-0 and element-1 correspond to Positive and Negative electrodes of ads channel 0,
     * <br>element-2 and element-3 correspond to Positive and Negative electrodes of ads channel 1,
     * <br>...
     * <br>element-14 and element-15 correspond to Positive and Negative electrodes of ads channel 8.
     *
     * @return leadOff detection mask
     */
    public Boolean[] getLeadOfDetectionMask() {
        if(recordingSettings == null) {
            return new Boolean[8 * 2];
        }
        Boolean[] mask = new Boolean[recordingSettings.getNumberOfAdsChannels() * 2];
        boolean[] adsMask = bdfRecorder.getLeadOfDetectionMask();
        if(adsMask != null) {
            for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
                if(recordingSettings.isAdsChannelEnabled(i) && recordingSettings.isAdsChannelLeadOffEnable(i)
                        && recordingSettings.getAdsChannelCommutatorState(i).equals("INPUT")) {
                    mask[2*i] = adsMask[2*i];
                    mask[2*i + 1] = adsMask[2*i + 1];
                }
            }
        }
        return mask;
    }

    public void setNotificationListener(NotificationListener l) {
        notificationListener = l;
    }

    public void setMessageListener(MessageListener l) {
        messageListener = l;
    }

    private void sendMessage(String message) {
        if(messageListener != null) {
            messageListener.showMessage(message);
        }
    }

    private synchronized void daemonConnect() {
        if (!bdfRecorder.isConnected() && selectedComportName != null && !selectedComportName.isEmpty()) {
            try {
                bdfRecorder.connect(selectedComportName);
            } catch (Exception ex) {
                // DO NOTHING!
            }
        }

    }

    public synchronized void connect(String comportName) {
        selectedComportName = comportName;
        try {
            if(comportName != null && !comportName.isEmpty()) {
                bdfRecorder.connect(comportName);
            }
        } catch (ComportBusyRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(COMPORT_BUSY_ERR, comportName);
            sendMessage(errMSg);
        } catch (ComportNotFoundRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(COMPORT_NOT_FOUND_ERR, comportName);
            sendMessage(errMSg);
        } catch (Exception ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
           // closeApplication(ERROR_STATUS);
        }
    }

    public synchronized void startRecording(AppConfig recordingSettings)  {
        this.recordingSettings = recordingSettings;
        String dirToSave = recordingSettings.getDirToSave();
        File dir = new File(dirToSave);
        if(!dir.exists()) {
            String msg = MessageFormat.format(DIR_CREATION_CONFIRMATION_MSG, dirToSave);
            boolean isConfirmed = messageListener.askConfirmation(msg);
            if(isConfirmed) {
                dir.mkdir();
            }
            else {
                return;
            }
        }
        String filename =  normalizeFilename(recordingSettings.getFilename());
        File fileToWrite = new File(dirToSave, filename);
        String comportName = recordingSettings.getComportName();
        selectedComportName = comportName;
        try {
            bdfRecorder.connect(comportName);
            startFuture = bdfRecorder.startRecording(recordingSettings.getBdfRecorderConfig(), fileToWrite);
        } catch (BdfFileNotFoundRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(FILE_NOT_ACCESSIBLE_ERR, fileToWrite);
            sendMessage(errMSg);
        } catch (ComportBusyRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(COMPORT_BUSY_ERR, comportName);
            sendMessage(errMSg);
        } catch (ComportNotFoundRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(COMPORT_NOT_FOUND_ERR, comportName);
            sendMessage(errMSg);
        } catch (Exception ex) {
            log.error(ex);
            ex.printStackTrace();
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
            closeApplication(ERROR_STATUS);
        }
    }

    public synchronized void stopRecording() throws BdfRecorderRuntimeException {
        try {
            bdfRecorder.stopRecording();
        } catch (Exception ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
            closeApplication(ERROR_STATUS);
        }
    }

    public boolean isRecording() {
        return bdfRecorder.isRecording();
    }

    public boolean isActive() {
        return bdfRecorder.isActive();
    }

    public String getStateReport() {
        String stateString = "Disconnected";
        if(bdfRecorder.isActive()) {
            stateString = "Connected";
        }

        int recordsNumber = getNumberOfWrittenDataRecords();
        if(bdfRecorder.isRecording()) {
            if(recordsNumber == 0) {
                stateString = "Starting...";
            } else {
                stateString = "Recording... " + recordsNumber + " data records";
            }
        } else {
            if(recordsNumber > 0) {
                stateString = "Saved to file: " + bdfRecorder.getSavedFile();
            }
        }
        return stateString;
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




    private int getNumberOfWrittenDataRecords() {
        try {
            return bdfRecorder.getNumberOfSentDataRecords();
        } catch (Exception ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
            closeApplication(ERROR_STATUS);
        }
        return 0;
    }


    private void closeApplication(int status) {
        try {
            if(recordingSettings != null) {
                try{
                    preferences.saveConfig(recordingSettings);
                } catch (Exception ex) {
                    String errMsg = "Error during saving preferences";
                    log.error(errMsg, ex);
                }
            }
            bdfRecorder.stopRecording();
            notificationTimer.stop();
            connectionTimer.cancel();
            bdfRecorder.disconnect();
            System.exit(status);
        } catch (Exception e) {
            String errMsg = "Error during closing application";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public synchronized void closeApplication(AppConfig recordingSettings) {
        this.recordingSettings = recordingSettings;
        closeApplication(SUCCESS_STATUS);
    }
}
