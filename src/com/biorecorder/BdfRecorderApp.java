package com.biorecorder;

import com.biorecorder.bdfrecorder.*;

import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.EdfConfig;
import com.biorecorder.edflib.exceptions.FileNotFoundRuntimeException;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderApp {
    private static final Log log = LogFactory.getLog(BdfRecorderApp.class);

    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;


    private static final String FILE_NOT_ACCESSIBLE_MSG = "File: {0}\ncould not be created or accessed.";
    private static final String COMPORT_BUSY_MSG = "ComPort: {0} is busy.";
    private static final String COMPORT_NOT_FOUND_MSG = "ComPort: {0} is not found.";
    private static final String COMPORT_NULL_MSG = "Comport name can not be null or empty";

     private static final String ALREADY_RECORDING_MSG = "Recorder is already recording. Stop it first";

    private static final String LOW_BUTTERY_MSG = "The buttery is low. BdfRecorder was stopped.";

    private static final String FAILED_CREATE_DIR_MSG = "Directory: {0}\ncan not be created.";
    private static final String DIRECTORY_EXIST_MSG = "Directory: {0}\nalready exist.";
    private static final String DIRECTORY_NOT_EXIST_MSG = "Directory: {0}\ndoes not exist.";

    private static final String FAILED_WRITE_DATA_MSG = "Failed to write data record {0} to the file:\n{1}";

    private static final String FAILED_STOP_MSG = "Failed to stop recorder.";
    private static final String FAILED_CLOSE_FILE_MSG = "File: {0} was not correctly saved";
    private static final String FAILED_DISCONNECT_MSG = "Failed to disconnect from comport {0}";

    private static final String START_FAILED_MSG = "Start failed!\nCheck whether the Recorder is on" +
            "\nand selected ComPort is correct and try again.";
    private static final String START_CANCELLED_MSG = "Start cancelled";
    private static final String WRONG_DEVICE_TYPE_MSG = "Start cancelled.\nSpecified Recorder type is invalid: {0}.\nConnected: {1}";


    private Preferences preferences;
    private BdfRecorder bdfRecorder;
    private volatile EdfFileWriter edfFileWriter;
    private volatile File edfFile;
    private volatile Boolean[] leadOffBitMask;

    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    private final Timer notificationTimer = new Timer("Notification timer");
    private final Timer connectionTimer = new Timer("ComPort connection timer");
    private volatile TimerTask connectionTask;
    private final ExecutorService futureHandlingExecutor;

    private final MessageSender messageSender = new MessageSender();
    private volatile NotificationListener notificationListener;


    public BdfRecorderApp(Preferences preferences, String comportName) {
        this.preferences = preferences;
        notificationListener = new NullNotificationListener();

        ThreadFactory namedThreadFactory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "«Future handling» thread");
            }
        };

        futureHandlingExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);


        notificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fireNotification();
            }
        }, NOTIFICATION_PERIOD_MS, NOTIFICATION_PERIOD_MS);

        if(comportName != null && !comportName.isEmpty()) {
            connectionTask = new ConnectionTask(comportName);
            connectionTimer.schedule(connectionTask, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
        }
    }

    private void fireNotification() {
        notificationListener.update();
    }

    private void sendMessage(String msg) {
        messageSender.sendMessage(msg);
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
                bdfRecorder.startMonitoring();
                cancel();
            } catch (Exception e) {
                // do nothing;
            }
        }
    }

    private synchronized void createRecorder(String comportName) throws ConnectionRuntimeException {
        if (bdfRecorder == null) {
            bdfRecorder = new BdfRecorder(comportName);
            bdfRecorder.setEventsListener(new RecorderEventsListener() {
                @Override
                public void handleLowButtery() {
                    stop();
                    fireNotification();
                    sendMessage(LOW_BUTTERY_MSG);
                }
            });
        }
    }

    // will be called only from GUI
    public OperationResult startRecording(AppConfig appConfig) {
        String comportName = appConfig.getComportName();
        BdfRecorderConfig bdfRecorderConfig = appConfig.getBdfRecorderConfig();

        if(isRecording()) {
            return new OperationResult(false, ALREADY_RECORDING_MSG);
        }

        if(comportName == null || comportName.isEmpty()) {
            return new OperationResult(false, COMPORT_NULL_MSG);
        }

        String dirToSave = appConfig.getDirToSave();
        if(!isDirectoryExist(dirToSave)) {
            String errMSg = MessageFormat.format(DIRECTORY_NOT_EXIST_MSG, dirToSave);
            return new OperationResult(false, errMSg);
        }


        if(bdfRecorder != null) {
            bdfRecorder.stopMonitoring();

            if(!bdfRecorder.getComportName().equals(comportName)) {
                disconnect();
            }
        }

        if(bdfRecorder == null) {
            try {
                createRecorder(comportName);
            } catch (ConnectionRuntimeException ex) {
                if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_BUSY) {
                    log.error(ex);
                    String errMSg = MessageFormat.format(COMPORT_BUSY_MSG, comportName);
                    return new OperationResult(false, errMSg);
                } else if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_NOT_FOUND) {
                    log.error(ex);
                    String errMSg = MessageFormat.format(COMPORT_NOT_FOUND_MSG, comportName);
                    return new OperationResult(false, errMSg);
                } else {
                    log.error(ex);
                    return new OperationResult(false, ex.getMessage());
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



        File fileToWrite = new File(appConfig.getDirToSave(), normalizeFilename(appConfig.getFileName()));
        boolean isDurationOfDataRecordComputable = appConfig.isDurationOfDataRecordComputable();
        EdfConfig edfConfig = bdfRecorder.getResultantRecordingInfo(bdfRecorderConfig);
        try {
            edfFileWriter = new EdfFileWriter(fileToWrite, FileType.BDF_24BIT);
            edfFileWriter.setDurationOfDataRecordsComputable(isDurationOfDataRecordComputable);
            edfFileWriter.setConfig(edfConfig);

        } catch (FileNotFoundRuntimeException ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(FILE_NOT_ACCESSIBLE_MSG, fileToWrite);
            return new OperationResult(false, errMSg);
        }
        edfFile = fileToWrite;

        numberOfWrittenDataRecords.set(0);
        bdfRecorder.setDataListener(new BdfDataListener() {
            @Override
            public void onDataRecordReceived(int[] dataRecord) {
                try{
                    synchronized (BdfRecorderApp.this) {
                        edfFileWriter.writeDigitalSamples(dataRecord);
                    }
                    numberOfWrittenDataRecords.incrementAndGet();
                } catch (Exception ex) {
                    // although stopRecording() will be called from not-GUI thread
                    // it could not coincide with startRecording() course
                    // DataListener works only when recorder is already "recording".
                    // And if it coincide with another stopRecording() called from GUI or
                    // it will not course any problem
                    stop1();
                    String errMsg = MessageFormat.format(FAILED_WRITE_DATA_MSG, numberOfWrittenDataRecords.get() + 1, edfFile);
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

        Future<Boolean> startFuture = bdfRecorder.startRecording(bdfRecorderConfig);
        futureHandlingExecutor.submit(new FutureHandlerTask(startFuture, bdfRecorderConfig));
        return new OperationResult(true);
    }

    class FutureHandlerTask implements Runnable {
        private Future<Boolean> future;
        private BdfRecorderConfig config;

        public FutureHandlerTask(Future future, BdfRecorderConfig config) {
            this.future = future;
            this.config = config;
        }

        @Override
        public void run() {
            while (!future.isDone()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            try {
                if(!future.get()) {
                    cancelStart();
                    fireNotification();
                    sendMessage(START_FAILED_MSG);
                }
            } catch (ExecutionException e) {
                cancelStart();
                if(e.getCause() instanceof IllegalArgumentException) {
                    String errMSg = MessageFormat.format(WRONG_DEVICE_TYPE_MSG, config.getDeviceType(), bdfRecorder.getDeviceType());
                    fireNotification();
                    sendMessage(errMSg);
                } else {
                    log.error(e.getCause());
                    fireNotification();
                    sendMessage(e.getCause().getMessage());
                }
            } catch (CancellationException e) {
                cancelStart();
                fireNotification();
                sendMessage(START_CANCELLED_MSG);
            } catch (InterruptedException e) {
                cancelStart();
                fireNotification();
                sendMessage(START_CANCELLED_MSG);
            }
        }

        private void cancelStart() {
            bdfRecorder.removeDataListener();
            bdfRecorder.removeLeadOffListener();
            leadOffBitMask = null;
            if(edfFileWriter != null) {
                try {
                    edfFileWriter.close();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
            if(edfFile != null) {
                try {
                    edfFile.delete();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
            edfFileWriter = null;
            edfFile = null;
            bdfRecorder.startMonitoring();
        }
    }

    private OperationResult stop1() {
        boolean isStopSuccess = true;
        boolean isFileCloseSuccess = true;
        String errMSg = "";
        if(bdfRecorder != null) {
            bdfRecorder.removeDataListener();
            bdfRecorder.removeLeadOffListener();
            leadOffBitMask = null;
            isStopSuccess = bdfRecorder.stopRecording();
            if(!isStopSuccess) {
                log.error(FAILED_STOP_MSG);
                errMSg = FAILED_STOP_MSG;
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
                    errMSg = errMSg + "\n" + MessageFormat.format(FAILED_CLOSE_FILE_MSG, edfFile)+"\n" + ex.getMessage();
                }
            }
        }
        return new OperationResult(isStopSuccess && isFileCloseSuccess, errMSg);
    }

    // may be called from different threads (GUI and not-GUI (DataListener thread))
    public OperationResult stop() {
        OperationResult stopResult = stop1();
        if(bdfRecorder != null) {
            bdfRecorder.startMonitoring();
        }
        return stopResult;
    }


    private OperationResult disconnect()  {
        if(bdfRecorder != null) {
            bdfRecorder.stopMonitoring();
            OperationResult stopResult = stop1();
            boolean isDisconnectedOk = true;
            String errMsg = stopResult.getMessage();
            bdfRecorder.removeEventsListener();
            isDisconnectedOk = bdfRecorder.disconnect();
            if(!isDisconnectedOk) {
               String disconnectionErrMsg = MessageFormat.format(FAILED_DISCONNECT_MSG, bdfRecorder.getComportName());
               log.error(disconnectionErrMsg);
               if(errMsg.isEmpty()) {
                   errMsg = disconnectionErrMsg;
               } else {
                   errMsg = errMsg + "\n"+disconnectionErrMsg;
               }
            }
            edfFile = null;
            leadOffBitMask = null;
            bdfRecorder = null;
            return new OperationResult(stopResult.isSuccess && isDisconnectedOk, errMsg);
        }
        return new OperationResult(true);

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

    public boolean isDirectoryExist(String directory) {
        File dir = new File(directory);
        if(dir.exists() && dir.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * Create directory if it does not exist.
     * @param directory name of the directory to create
     * @return OperationResult: successful if  and only if the  directory was created
     */
    public OperationResult createDirectory(String directory)  {
        File dir = new File(directory);
        if(isDirectoryExist(directory)) {
            String errMSg = MessageFormat.format(DIRECTORY_EXIST_MSG, dir.getName());
            return new OperationResult(false, errMSg);
        }
        try {
            if(dir.mkdir()) {
                return new OperationResult(true);
            } else {
                String errMSg = MessageFormat.format(FAILED_CREATE_DIR_MSG, dir);
                return new OperationResult(false, errMSg);
            }

        } catch (Exception ex) {
            String errMSg = MessageFormat.format(FAILED_CREATE_DIR_MSG, dir) + "\n"+ex.getMessage();
            return new OperationResult(false, errMSg);
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
    public String[] getComportNames(String selectedComportName) {
        String[] availablePorts = BdfRecorder.getAvailableComportNames();
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
        messageSender.setMessageListener(l);
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
        notificationTimer.cancel();
        connectionTimer.cancel();
        messageSender.stop();
        if(!futureHandlingExecutor.isShutdown()) {
            futureHandlingExecutor.shutdownNow();
        }
        if(bdfRecorder != null) {
            bdfRecorder.disconnect();
        }
        System.exit(status);
    }

    public void closeApplication(AppConfig appConfig) {
        try{
            preferences.saveConfig(appConfig);
        } catch (Exception ex) {
            String errMsg = "Error during saving preferences";
            log.error(errMsg, ex);
        }
        closeApplication(SUCCESS_STATUS);
    }


    class NullNotificationListener implements NotificationListener {
        @Override
        public void update() {
            // do nothing
        }
    }

}
