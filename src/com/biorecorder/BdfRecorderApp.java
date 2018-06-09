package com.biorecorder;

import com.biorecorder.bdfrecorder.*;

import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.edflib.DataFormat;
import com.biorecorder.edflib.EdfHeader;
import com.biorecorder.edflib.EdfWriter;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private static final String FAILED_DISCONNECT_MSG = "Failed to disconnect Recorder. Comport name: {0}";
    private static final String FAILED_SAVE_PREFERENCES = "Failed to save preferences";

    private static final String START_FAILED_MSG = "Start failed!\nCheck whether the Recorder is on" +
            "\nand selected ComPort is correct and try again.";
    private static final String START_CANCELLED_MSG = "Start cancelled";
    private static final String WRONG_DEVICE_TYPE_MSG = "Start cancelled.\nSpecified Recorder type is invalid: {0}.\nConnected: {1}";


    private volatile Preferences preferences;
    private volatile BdfRecorder bdfRecorder;
    private volatile File edfFile;
    private volatile EdfWriter edfWriter;
    private volatile Boolean[] leadOffBitMask;
    private volatile Integer batteryVoltage;
    private volatile String[] availableComports;

    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    private int COMPORT_LIST_PERIOD_MS = 3000;
    private int START_FUTURE_CHECKING_PERIOD_MS = 1000;
    private final Timer timer = new Timer();

    private final MessageSender messageSender = new MessageSender();
    private volatile NotificationListener notificationListener;

    private volatile String comportName = "";
    private volatile TimerTask connectionTask = new ConnectionTask();
    private volatile TimerTask startFutureHandlingTask;
    private volatile boolean isLoffDetecting;


    public BdfRecorderApp(Preferences preferences) {
        this.preferences = preferences;
        availableComports = checkAvailableComports();
        notificationListener = new NullNotificationListener();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                fireNotification();
            }
        }, NOTIFICATION_PERIOD_MS, NOTIFICATION_PERIOD_MS);
        setComportName(preferences.getConfig().getComportName());
    }

    public boolean setComportName(String comportName) {
        if(comportName != null && comportName == this.comportName) {
            return true;
        }
        if(comportName == null || comportName.isEmpty() || isRecording()) {
            return false;
        }
        this.comportName = comportName;
        availableComports = checkAvailableComports();
        connectionTask.cancel();
        connectionTask = new ConnectionTask();
        timer.schedule(connectionTask, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
        return true;
    }

    public AppConfig getConfig() {
        return preferences.getConfig();
    }

    public void changeDeviceType(String recorderType) {
        AppConfig config = preferences.getConfig();
        config.setDeviceType(recorderType);
        preferences.saveConfig(config);
    }

    public String getComportName() {
        return comportName;
    }

    public boolean isLoffDetecting() {
        return isLoffDetecting;
    }

    public String[] getAvailableComports() {
        return availableComports;
    }

    public synchronized boolean isRecording() {
        if(bdfRecorder != null &&  bdfRecorder.isRecording()) {
            return true;
        }
        return false;
    }

    public synchronized boolean isActive() {
        if(bdfRecorder != null && bdfRecorder.isActive()) {
            return true;
        }
        return false;
    }

    private void fireNotification() {
        notificationListener.update();
    }

    private void sendMessage(String msg) {
        messageSender.sendMessage(msg);
    }

    private synchronized void createRecorder(String comportName) throws ConnectionRuntimeException {
        if(bdfRecorder != null) {
            if(bdfRecorder.getComportName().equals(comportName)) {
                return;
            } else {
                if(!bdfRecorder.disconnect()) {
                    String errMsg = MessageFormat.format(FAILED_DISCONNECT_MSG, bdfRecorder.getComportName());
                    log.error(errMsg);
                }
                bdfRecorder = null;
            }
        }

        bdfRecorder = new BdfRecorder(comportName);
        bdfRecorder.addEventsListener(new EventsListener() {
            @Override
            public void handleLowBattery() {
                sendMessage(LOW_BUTTERY_MSG);
            }
        });
        bdfRecorder.addButteryVoltageListener(new BatteryVoltageListener() {
            @Override
            public void onBatteryVoltageReceived(int batteryVoltage1) {
                batteryVoltage = batteryVoltage1;
            }
        });
    }

    private synchronized  void startMonitoring() throws IllegalStateException {
        if(bdfRecorder != null) {
            bdfRecorder.startMonitoring();
        }
    }

    private synchronized Future<Boolean> startRecorder(RecorderConfig recorderConfig) throws IllegalStateException {
        return bdfRecorder.startRecording(recorderConfig);
    }

    private synchronized boolean stopRecorder() {
        if(bdfRecorder != null) {
            return bdfRecorder.stop();
        }
        return true;
    }

    private synchronized boolean disconnectRecorder() {
        if(bdfRecorder != null) {
            boolean isDisconnectOk = bdfRecorder.disconnect();
            bdfRecorder = null;
            return isDisconnectOk;
        }
        return true;
    }

    public OperationResult startRecording(AppConfig appConfig, boolean isLoffDetection) {
        if(isRecording()) {
            return new OperationResult(false, ALREADY_RECORDING_MSG);
        }

        String comportName = appConfig.getComportName();

        if(comportName == null || comportName.isEmpty()) {
            return new OperationResult(false, COMPORT_NULL_MSG);
        }

        this.comportName = comportName;
        connectionTask.cancel();
        this.isLoffDetecting = isLoffDetection;

        try {
            createRecorder(comportName);
        } catch (ConnectionRuntimeException ex) {
            String errMSg = ex.getMessage();
            if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_BUSY) {
                errMSg = MessageFormat.format(COMPORT_BUSY_MSG, comportName);
            }
            if(ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_NOT_FOUND) {
                errMSg = MessageFormat.format(COMPORT_NOT_FOUND_MSG, comportName);
            }
            log.error(ex);
            return new OperationResult(false, errMSg);
        }

        // remove all previously added filters
        bdfRecorder.removeChannelsFilters();


        if(isLoffDetection) {
            leadOffBitMask = null;
            for (int i = 0; i < appConfig.getChannelsCount(); i++) {
                appConfig.setChannelLeadOffEnable(i, true);
            }

            bdfRecorder.addLeadOffListener(new LeadOffListener() {
                @Override
                public void onLeadOffMaskReceived(Boolean[] leadOffMask) {
                    leadOffBitMask = leadOffMask;
                }
            });
        } else {
            for (int i = 0; i < appConfig.getChannelsCount(); i++) {
                appConfig.setChannelLeadOffEnable(i, false);
            }

            // Apply MovingAverage filters to to ads channels to reduce 50Hz noise
            for (int i = 0; i < appConfig.getChannelsCount(); i++) {
                if (appConfig.is50HzFilterEnabled(i)) {
                    int numberOfAveragingPoints = appConfig.getChannelSampleRate(i) / 50;
                    bdfRecorder.addChannelFilter(i, new MovingAverageFilter(numberOfAveragingPoints), "MovAvg:"+ numberOfAveragingPoints);
                }
            }

            String dirToSave = appConfig.getDirToSave();
            if(!isDirectoryExist(dirToSave)) {
                String errMSg = MessageFormat.format(DIRECTORY_NOT_EXIST_MSG, dirToSave);
                return new OperationResult(false, errMSg);
            }

            numberOfWrittenDataRecords.set(0);

            edfFile = new File(dirToSave, normalizeFilename(appConfig.getFileName()));

            DataConfig dataConfig = bdfRecorder.getDataConfig(appConfig.getRecorderConfig());
            // copy data from dataConfig to the EdfHeader
            EdfHeader edfHeader = configToHeader(dataConfig);
            edfHeader.setPatientIdentification(appConfig.getPatientIdentification());
            edfHeader.setRecordingIdentification(appConfig.getRecordingIdentification());

            try {
                edfWriter = new EdfWriter(edfFile, edfHeader);
            } catch (FileNotFoundException ex) {
                log.error(ex);
                String errMSg = MessageFormat.format(FILE_NOT_ACCESSIBLE_MSG, edfFile);
                return new OperationResult(false, errMSg);
            }
            edfWriter.setDurationOfDataRecordsComputable(appConfig.isDurationOfDataRecordComputable());

            bdfRecorder.addDataListener(new DataListener() {
                @Override
                public void onDataReceived(int[] dataRecord) {
                    try{
                        edfWriter.writeDigitalRecord(dataRecord);
                        numberOfWrittenDataRecords.incrementAndGet();
                    } catch (IOException ex) {
                        // although stop() will be called from not-GUI thread
                        // it could not coincide with startRecording() course
                        // this exception can be thrown only
                        // when Recorder is already "recording".
                        // And if it coincide with another stop() called from GUI or
                        // it will not course any problem
                        stop();
                        String errMsg = MessageFormat.format(FAILED_WRITE_DATA_MSG, numberOfWrittenDataRecords.get() + 1, edfFile);
                        log.error(errMsg + "\n"+ex.getMessage());
                        sendMessage(errMsg + "\n"+ex.getMessage());
                    } catch (IllegalStateException ex) {
                        // after stopping Recorder and closing edfWriter
                        // some records still can be received and this
                        // exception can be thrown.
                        log.info(ex);
                    }
                }
            });
        }

        Future<Boolean> startFuture = startRecorder(appConfig.getRecorderConfig());
        startFutureHandlingTask = new StartFutureHandlerTask(startFuture, edfWriter, RecorderType.valueOf(appConfig.getDeviceType()));
        timer.schedule(startFutureHandlingTask, START_FUTURE_CHECKING_PERIOD_MS, START_FUTURE_CHECKING_PERIOD_MS);
        return new OperationResult(true);
    }

    private OperationResult stop1() {
        boolean isStopSuccess = true;
        boolean isFileCloseSuccess = true;
        String msg = "";
        isStopSuccess = stopRecorder();
        if(startFutureHandlingTask != null) {
            startFutureHandlingTask.cancel();
        }
        isLoffDetecting = false;
        if(!isStopSuccess) {
            log.error(FAILED_STOP_MSG);
            msg = FAILED_STOP_MSG;
        }
        if(edfWriter != null) {
            try {
                edfWriter.close();
            } catch (Exception ex) {
                isFileCloseSuccess = false;
                log.error(ex);
                if(!msg.isEmpty()) {
                    msg = "msg"+"\n";
                }
                msg = msg + MessageFormat.format(FAILED_CLOSE_FILE_MSG, edfFile)+"\n" + ex.getMessage();
            }
        }
        return new OperationResult(isStopSuccess && isFileCloseSuccess, msg);
    }


    public OperationResult stop() {
        OperationResult stopResult = stop1();
        startMonitoring();
        return stopResult;
    }

    public void closeApplication(AppConfig appConfig) {
        OperationResult stopResult = stop1();
        if(!stopResult.isSuccess) {
            log.error(stopResult.getMessage());
        }
        timer.cancel();
        messageSender.stop();
        if(!disconnectRecorder()) {
            String errMsg = MessageFormat.format(FAILED_DISCONNECT_MSG, bdfRecorder.getComportName());
            log.error(errMsg);
        }
        try{
            preferences.saveConfig(appConfig);
        } catch (Exception ex) {
            log.error(FAILED_SAVE_PREFERENCES, ex);
        }
        System.exit(SUCCESS_STATUS);
    }

    public Boolean[] getLeadOffMask() {
        if(isLoffDetecting) {
            return leadOffBitMask;
        }
        return null;
    }

    public Integer getBatteryVoltage() {
        return batteryVoltage;
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
    private String[] checkAvailableComports() {
        String[] availablePorts = BdfRecorder.getAvailableComportNames();
        if(comportName == null || comportName.isEmpty()) {
            return availablePorts;
        }
        if(availablePorts.length == 0) {
            String[] resultantPorts = new String[1];
            resultantPorts[0] = comportName;
            return resultantPorts;
        }

        boolean isSelectedPortAvailable = false;
        for (String port : availablePorts) {
            if(port.length() == comportName.length()
                    && port.charAt(port.length() - 1) == comportName.charAt(comportName.length() - 1)
                    && port.charAt(port.length() - 2) == comportName.charAt(comportName.length() - 2)) {
                isSelectedPortAvailable = true;
                break;
            }
        }
        if(isSelectedPortAvailable) {
            return availablePorts;
        } else {
            String[] resultantPorts = new String[availablePorts.length + 1];
            resultantPorts[0] = comportName;
            System.arraycopy(availablePorts, 0, resultantPorts, 1, availablePorts.length);
            return resultantPorts;
        }
    }

    public void setNotificationListener(NotificationListener l) {
        notificationListener = l;
    }

    public void setMessageListener(MessageListener l) {
        messageSender.addMessageListener(l);
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

    /*
     * copy data from dataConfig to the EdfHeader
    */
    private EdfHeader configToHeader(DataConfig dataConfig) {
        EdfHeader edfHeader = new EdfHeader(DataFormat.BDF_24BIT, dataConfig.signalsCount());
        edfHeader.setDurationOfDataRecord(dataConfig.getDurationOfDataRecord());
        for (int i = 0; i < dataConfig.signalsCount(); i++) {
            edfHeader.setNumberOfSamplesInEachDataRecord(i, dataConfig.getNumberOfSamplesInEachDataRecord(i));
            edfHeader.setPrefiltering(i, dataConfig.getPrefiltering(i));
            edfHeader.setTransducer(i, dataConfig.getTransducer(i));
            edfHeader.setLabel(i, dataConfig.getLabel(i));
            edfHeader.setDigitalRange(i, dataConfig.getDigitalMin(i), dataConfig.getDigitalMax(i));
            edfHeader.setPhysicalRange(i, dataConfig.getPhysicalMin(i), dataConfig.getPhysicalMax(i));
            edfHeader.setPhysicalDimension(i, dataConfig.getPhysicalDimension(i));
        }
        return edfHeader;
    }


    class NullNotificationListener implements NotificationListener {
        @Override
        public void update() {
            // do nothing
        }
    }

    class CheckAvailableComportsTask extends TimerTask {
        @Override
        public void run() {
            availableComports = checkAvailableComports();
        }
    }

    class ConnectionTask extends TimerTask {
        @Override
        public void run() {
            try {
                createRecorder(comportName);
                startMonitoring();
                cancel();
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    class StartFutureHandlerTask extends TimerTask {
        private Future<Boolean> future;
        private EdfWriter edfWriter1;
        private RecorderType recorderType;

        public StartFutureHandlerTask(Future future, EdfWriter edfWriter, RecorderType recorderType) {
            this.future = future;
            this.edfWriter1 = edfWriter;
            this.recorderType = recorderType;
        }

        @Override
        public void run() {
            if (future.isDone()){
                try {
                    String errMsg = START_FAILED_MSG;
                    if(!future.get()) {
                        cancelStart();
                        if(recorderType != bdfRecorder.getDeviceType()) {
                            errMsg = MessageFormat.format(WRONG_DEVICE_TYPE_MSG, recorderType, bdfRecorder.getDeviceType());
                        }
                        sendMessage(errMsg);
                    }
                } catch (InterruptedException e) {
                    // do nothing;
                } catch (CancellationException e) {
                    // do nothing
                } catch (ExecutionException e) {
                    cancelStart();
                    sendMessage(e.getMessage());
                }
                cancel(); // cancel this task
            }
        }

        private void cancelStart() {
            try {
                edfWriter1.close();
                File writtenFile = edfWriter1.getFile();
                writtenFile.delete();
                startMonitoring();
            } catch (Exception ex) {
                log.error(ex);
            }
        }
    }


}
