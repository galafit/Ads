package com.biorecorder;

import com.biorecorder.bdfrecorder.*;
import com.biorecorder.bdfrecorder.exceptions.*;

import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.EdfConfig;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderApp1  {
    private static final Log log = LogFactory.getLog(BdfRecorderApp1.class);

    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;


    private static final String FILE_NOT_ACCESSIBLE_MSG = "File: {0} could not be created or accessed.";
    private static final String COMPORT_BUSY_MSG = "ComPort: {0} is busy.";
    private static final String COMPORT_NOT_FOUND_MSG = "ComPort: {0} is not found.";
    private static final String COMPORT_NULL_MSG = "Comport name can not be null or empty";

    private static final String WRONG_DEVICE_TYPE_MSG = "Specified device type is invalid: {0}. Connected device: {1}";
    private static final String ALREADY_RECORDING_MSG = "Device is already recording. Stop it first";
    private static final String FAILED_TO_START_MSG = "Failed to start recording";

    private static final String APP_ERR_MSG = "Error: {0}";
    private static final String LOW_BUTTERY_MSG = "The buttery is low. BdfRecorder was stopped.";
    private static final String START_CANCELLED_MSG = "Start failed. Check whether the device is connected" +
            "\nand selected ComPort is correct and try again.";
    private static final String DIR_CREATION_CONFIRMATION_MSG = "Directory: {0}\ndoes not exist. Do you want to create it?";
    private static final String FAILED_TO_CREATE_DIR_MSG = "Directory: {0} can not be created.";
    private static final String FAILED_CLOSE_FILE_MSG = "Failed to save the file: {0}";
    private static final String FAILED_TO_WRITE_DATA_MSG = "Failed to write data record {0}\nto the file: {1}";

    private static final String FAILED_TO_STOP_MSG = "Failed to stop recorder.";
    private static final String FAILED_TO_CLOSE_FILE_MSG = "File: {0} was not correctly saved";
    private static final String FAILED_TO_DISCONNECT_MSG = "Failed to disconnect from comport {0}";

    private Preferences1 preferences;
    private BdfRecorder1 bdfRecorder;
    private volatile EdfFileWriter edfFileWriter;
    private volatile File edfFile;
    private volatile Boolean[] leadOffBitMask;

    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    private final Timer notificationTimer = new Timer("Notification timer");
    private final Timer connectionTimer = new Timer("ComPort connection timer");
    private volatile TimerTask connectionTask = new NullTimerTask();

    private NotificationListener notificationListener;
    private MessageListener messageListener;


    public BdfRecorderApp1(Preferences1 preferences, String comportName) {
        this.preferences = preferences;
        notificationListener = createNotificationNullListener();
        messageListener = createMessageNullListener();

        notificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                notificationListener.update();
            }
        }, NOTIFICATION_PERIOD_MS, NOTIFICATION_PERIOD_MS);

        if(comportName != null && !comportName.isEmpty()) {
            connectionTask = new ConnectionTask(comportName);
            connectionTimer.schedule(connectionTask, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
        }
    }


    class ConnectionTask extends TimerTask {
        private final String comportName;

        public ConnectionTask(String comportName) {
            this.comportName = comportName;
        }

        @Override
        public void run() {
            try {
                createRecorder(comportName);
                cancel();
            } catch (Exception e) {
                // do nothing;
            }
        }
    }

    private synchronized void createRecorder(String comportName) throws ConnectionRuntimeException {
        if (bdfRecorder == null) {
            bdfRecorder = new BdfRecorder1(comportName);
            bdfRecorder.setEventsListener(new RecorderEventsListener() {
                @Override
                public void handleLowButtery() {
                    stop();
                    sendMessage(LOW_BUTTERY_MSG);
                }

                @Override
                public void handleStartCanceled() {
                    bdfRecorder.removeDataListener();
                    bdfRecorder.removeLeadOffListener();
                    leadOffBitMask = null;

                    try {
                        edfFileWriter.close();
                        edfFile.delete();
                    } catch (Exception ex) {
                        log.error(ex);
                    }
                    edfFileWriter = null;
                    edfFile = null;
                    sendMessage(START_CANCELLED_MSG);
                }
            });
        }
    }



    // will be called only from GUI
    public void startRecording(AppConfig1 appConfig) {
        String comportName = appConfig.getComportName();
        BdfRecorderConfig1 bdfRecorderConfig = appConfig.getBdfRecorderConfig();

        if(comportName == null || comportName.isEmpty()) {
            sendMessage(COMPORT_NULL_MSG);
            return;
        }

        if(bdfRecorder != null && bdfRecorder.getComportName().equals(comportName)) {
            if(isRecording()) {
                return;
            }
        }

        if(bdfRecorder != null && !bdfRecorder.getComportName().equals(comportName)) {
            disconnect();
        }

        if(bdfRecorder == null) {
            try {
                createRecorder(comportName);
            } catch (ConnectionRuntimeException ex) {
                if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_BUSY) {
                    log.error(ex);
                    String errMSg = MessageFormat.format(COMPORT_BUSY_MSG, comportName);
                    sendMessage(errMSg);
                    return;
                } else if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_NOT_FOUND) {
                    log.error(ex);
                    String errMSg = MessageFormat.format(COMPORT_NOT_FOUND_MSG, comportName);
                    sendMessage(errMSg);
                    return;
                } else {
                    log.error(ex);
                    String errMSg = MessageFormat.format(APP_ERR_MSG, ex.getMessage());
                    sendMessage(errMSg);
                }
            }
        }

        // remove all previously added filters
        bdfRecorder.removeAllChannelsFilters();

        // Apply MovingAverage filters to to ads channels to reduce 50Hz noise
        int enableSignalsCounter = 0;
        for (int i = 0; i < bdfRecorderConfig.getNumberOfChannels(); i++) {
            if (bdfRecorderConfig.isChannelEnabled(i)) {
                if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                    int numberOfAveragingPoints = bdfRecorderConfig.getChannelFrequency(i) / 50;
                    bdfRecorder.addChannelFilter(enableSignalsCounter, new MovingAverageFilter(numberOfAveragingPoints), "MovAvg:"+ numberOfAveragingPoints);
                }
                enableSignalsCounter++;
            }
        }

        boolean isDurationOfDataRecordComputable = appConfig.isDurationOfDataRecordComputable();

        String dirToSave = appConfig.getDirToSave();
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
        String filename =  normalizeFilename(appConfig.getFileName());
        File fileToWrite = new File(dirToSave, filename);

        EdfConfig edfConfig = bdfRecorder.getResultantRecordingInfo(bdfRecorderConfig);

        RecorderStartResult startResult = bdfRecorder.startRecording(bdfRecorderConfig);
        if(!startResult.isSuccess()) {
            if(startResult == RecorderStartResult.ALREADY_RECORDING) {
                sendMessage(ALREADY_RECORDING_MSG);
            }
            if(startResult == RecorderStartResult.WRONG_DEVICE_TYPE) {
                String errMSg = MessageFormat.format(WRONG_DEVICE_TYPE_MSG, bdfRecorderConfig.getDeviceType(), bdfRecorder.getDeviceType());
                sendMessage(errMSg);
            }
            if(startResult == RecorderStartResult.FAILED_TO_SEND_COMMAND) {
                log.error("Failed to send start command");
                sendMessage(FAILED_TO_START_MSG);
            }
            return;
        }
        numberOfWrittenDataRecords.set(0);
        try {
            edfFileWriter = new EdfFileWriter(fileToWrite, FileType.BDF_24BIT);
            edfFileWriter.setDurationOfDataRecordsComputable(isDurationOfDataRecordComputable);
            edfFileWriter.setConfig(edfConfig);
            bdfRecorder.setDataListener(new BdfDataListener() {
                @Override
                public void onDataRecordReceived(int[] dataRecord) {
                    try{
                        synchronized (BdfRecorderApp1.this) {
                            edfFileWriter.writeDigitalSamples(dataRecord);
                        }
                        numberOfWrittenDataRecords.incrementAndGet();
                    } catch (Exception ex) {
                        // although stop() will be called from not-GUI thread
                        // it could not coincide with startRecording() course
                        // DataListener works only when recorder is already "recording".
                        // And if it coincide with another stop() called from GUI or
                        // it will not course any problem
                        stop1();
                        String errMsg = MessageFormat.format(FAILED_TO_WRITE_DATA_MSG, numberOfWrittenDataRecords.get() + 1, edfFile);
                        log.error(errMsg + "\n"+ex.getMessage());
                        sendMessage(errMsg + "\n"+ex.getMessage());
                    }
                }
            });
            bdfRecorder.setLeadOffListener(new LeadOffListener() {
                @Override
                public void onLeadOffDataReceived(Boolean[] leadOffMask) {
                    leadOffBitMask = leadOffMask;
                }
            });
        } catch (BdfFileNotFoundRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(FILE_NOT_ACCESSIBLE_MSG, fileToWrite);
            sendMessage(errMSg);
        }
        edfFile = fileToWrite;

    }

    private OperationResult stop1() {
        boolean isStopSuccess = true;
        boolean isFileCloseSuccess = true;
        String errMSg = "";
        if(bdfRecorder != null) {
            bdfRecorder.removeDataListener();
            bdfRecorder.removeLeadOffListener();
            leadOffBitMask = null;
            isStopSuccess = bdfRecorder.stop();
            if(!isStopSuccess) {
                log.error(FAILED_TO_STOP_MSG);
                errMSg = FAILED_TO_STOP_MSG;
            }
            if(edfFileWriter != null) {
                try {
                    synchronized (this) {
                        edfFileWriter.close();
                    }
                    edfFileWriter = null;
                } catch (Exception ex) {
                    isFileCloseSuccess = false;
                    log.error(ex);
                    errMSg = errMSg + "\n" + MessageFormat.format(FAILED_TO_CLOSE_FILE_MSG, edfFile)+"\n" + ex.getMessage();
                }
            }
        }
        return new OperationResult(isStopSuccess && isFileCloseSuccess, errMSg);
    }

    // may be called from different threads (GUI and not-GUI (DataListener thread))
    public void stop() {
        OperationResult stopResult = stop1();
        if(!stopResult.isSuccess) {
            sendMessage(stopResult.getMessage());
        }
    }


    private OperationResult disconnect()  {
        OperationResult stopResult = stop1();
        boolean isDisconnectedOk = true;
        String errMsg = stopResult.getMessage();
        if(bdfRecorder != null) {
            bdfRecorder.removeEventsListener();
            isDisconnectedOk = bdfRecorder.disconnect();
            if(!isDisconnectedOk) {
               String disconnectionErrMsg = MessageFormat.format(FAILED_TO_DISCONNECT_MSG, bdfRecorder.getComportName());
               log.error(disconnectionErrMsg);
               if(errMsg.isEmpty()) {
                   errMsg = disconnectionErrMsg;
               } else {
                   errMsg = errMsg + "\n"+disconnectionErrMsg;
               }
            }
        }
        edfFile = null;
        leadOffBitMask = null;
        bdfRecorder = null;
        return new OperationResult(stopResult.isSuccess && isDisconnectedOk, errMsg);
    }

    // will be called only from GUI
    public void setComport(String comportName) {
        // if recorder already connected with that port
        if(bdfRecorder != null && bdfRecorder.getComportName().equals(comportName)) {
            return;
        }
        try {
            disconnect();
            // restart connection task with new comport name
            if(connectionTask != null) {
                connectionTask.cancel();
            }
            connectionTask = new ConnectionTask(comportName);
            connectionTimer.schedule(connectionTask, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
        } catch (Exception ex) {
            // do nothing!
        }
    }


    public Boolean[] getLeadOffMask() {
        return leadOffBitMask;
    }

    private synchronized void sendMessage(String message) {
        if(messageListener != null) {
            messageListener.showMessage(message);
        }
    }

    public void createDirectory(String directory) {
        try {
            File dir = new File(directory);
            if(!dir.exists()) {
                String msg = MessageFormat.format(DIR_CREATION_CONFIRMATION_MSG, directory);
                boolean isConfirmed = messageListener.askConfirmation(msg);
                if(isConfirmed) {
                    dir.mkdir();
                }
            }
        } catch (Exception ex) {
            String errMSg = MessageFormat.format(FAILED_TO_CREATE_DIR_MSG, directory) + "\n"+ex.getMessage();
            sendMessage(errMSg);
        }
    }

    private File createFileToSave(String dirToSave, String fileName) {
        return new File(dirToSave, normalizeFilename(fileName));
    }

    /**
     * If the comportName is not equal to any available port we add it to the list.
     * <p>
     * String full comparison is very "heavy" operation.
     * So instead we will compare only lengths and 2 last symbols...
     * That will be quick and good enough for our purpose
     * @return available comports list with selected port included
     */
    public String[] getComportNames(String selectedComportName) {
        String[] availablePorts = BdfRecorder1.getAvailableComportNames();
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

    public void setNotificationListener(NotificationListener l) {
        notificationListener = l;
    }

    public void setMessageListener(MessageListener l) {
        messageListener = l;
    }


    public boolean isRecording() {
        if(bdfRecorder != null && bdfRecorder.getRecorderState() == RecorderState.RECORDING) {
            return true;
        }
        return false;
    }

    public boolean isActive() {
        if(bdfRecorder != null && bdfRecorder.isActive()) {
            return true;
        }
        return false;
    }

    public String getStateReport() {
        String stateString = "Disconnected";
        if(isActive()) {
            stateString = "Connected";
        }

        if(isRecording()) {
            if(numberOfWrittenDataRecords.get() == 0) {
                stateString = "Starting...";
            } else {
                stateString = "Recording... " + numberOfWrittenDataRecords + " data records";
            }
        } else {
            if(numberOfWrittenDataRecords.get() > 0) {
                stateString = "Saved to file: " + edfFile;
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


    private void closeApplication(int status) {
        try {
            notificationTimer.cancel();
            connectionTimer.cancel();
            if(bdfRecorder != null) {
                bdfRecorder.stop();
                bdfRecorder.disconnect();
            }
            System.exit(status);
        } catch (Exception e) {
            String errMsg = "Error during closing application";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void closeApplication(AppConfig1 appConfig) {
        try{
            preferences.saveConfig(appConfig);
        } catch (Exception ex) {
            String errMsg = "Error during saving preferences";
            log.error(errMsg, ex);
        }
        closeApplication(SUCCESS_STATUS);
    }

    private NotificationListener createNotificationNullListener() {
        return new NotificationListener() {
            @Override
            public void update() {
                // do nothing
            }
        };
    }

    private MessageListener createMessageNullListener() {
        return new MessageListener() {
            @Override
            public void showMessage(String message) {
                // do nothing
            }

            @Override
            public boolean askConfirmation(String message) {
                return false;
            }
        };
    }

    class NullTimerTask extends TimerTask {
        @Override
        public void run() {
            // do nothing;
        }
    }

}
