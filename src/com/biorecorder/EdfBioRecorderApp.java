package com.biorecorder;

import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.edflib.DataFormat;
import com.biorecorder.edflib.EdfHeader;
import com.biorecorder.edflib.EdfWriter;
import com.biorecorder.recorder.*;
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
public class EdfBioRecorderApp {
    private static final Log log = LogFactory.getLog(EdfBioRecorderApp.class);

    private static final String FILE_NOT_ACCESSIBLE_MSG = "File: {0}\ncould not be created or accessed";
    private static final String COMPORT_BUSY_MSG = "Comport: {0} busy";
    private static final String COMPORT_NOT_FOUND_MSG = "Comport: {0} not found";
    private static final String COMPORT_NULL_MSG = "Comport name can not be null or empty";

    private static final String ALREADY_RECORDING_MSG = "Recorder already recording. Stop it first";
    private static final String ALL_CHANNELS_AND_ACCELEROMETER_DISABLED_MSG = "All channels and accelerometer disabled.\nEnable some of them to record";
    private static final String ALL_CHANNELS_DISABLED_MSG = "All channels disabled.\nEnable some of them to check contacts";

    private static final String FAILED_CREATE_DIR_MSG = "Directory: {0}\ncan not be created.";
    private static final String DIRECTORY_EXIST_MSG = "Directory: {0}\nalready exist.";
    private static final String DIRECTORY_NOT_EXIST_MSG = "Directory: {0}\ndoes not exist.";

    private static final String FAILED_STOP_MSG = "Failed to stop recorder.";
    private static final String FAILED_CLOSE_FILE_MSG = "File: {0} was not correctly closed and saved";
    private static final String FAILED_DISCONNECT_MSG = "Failed to disconnect Recorder. Comport name: {0}";

    private static final String START_FAILED_MSG = "Start failed!\nCheck whether the Recorder is on" +
            "\nand selected Comport is correct and try again.";
    private static final String WRONG_DEVICE_TYPE_MSG = "Start failed!\nWrong Recorder type: {0}.\nConnected: {1}";
    private static final String START_CANCELED = "Start canceled!";


    private static final String LOW_BUTTERY_MSG = "Recorder was stopped\nThe buttery is low";
    private static final String FAILED_WRITE_DATA_MSG = "Recorder was stopped\nFailed to write data record {0} to the file:\n{1}";

    private static final int PROGRESS_NOTIFICATION_PERIOD_MS = 1000;
    private static final int COMPORT_CONNECTION_PERIOD_MS = 2000;
    private static final int AVAILABLE_COMPORTS_CHECKING_PERIOD_MS = 3000;
    private static final int FUTURE_CHECKING_PERIOD_MS = 1000;

    private final Timer timer = new Timer();
    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);

    private volatile ProgressListener progressListener = new NullProgressListener();
    private volatile StateChangeListener stateChangeListener = new NullStateChangeListener();
    private volatile AvailableComportsListener availableComportsListener = new NullAvailableComportsListener();

    private volatile BioRecorder bioRecorder;
    private volatile EdfWriter edfWriter;
    private volatile Boolean[] leadOffBitMask;
    private volatile Integer batteryLevel;
    private volatile String comportName;
    private volatile TimerTask connectionTask = new ConnectionTask();
    private volatile TimerTask startFutureHandlingTask;
    private volatile boolean isLoffDetecting;

    private volatile boolean isDurationOfDataRecordComputable = false;

    public EdfBioRecorderApp() {
        timer.schedule(new TimerTask() {
            public void run() {
                notifyProgress();
            }
        }, PROGRESS_NOTIFICATION_PERIOD_MS, PROGRESS_NOTIFICATION_PERIOD_MS);

        timer.schedule(new CheckAvailableComportsTask(), AVAILABLE_COMPORTS_CHECKING_PERIOD_MS, AVAILABLE_COMPORTS_CHECKING_PERIOD_MS);
    }


    public boolean connectToComport(String comportName) {
        if (comportName == null || comportName.isEmpty() || isRecording()) {
            return false;
        }
        this.comportName = comportName;
        connectionTask.cancel();
        connectionTask = new ConnectionTask();
        timer.schedule(connectionTask, COMPORT_CONNECTION_PERIOD_MS, COMPORT_CONNECTION_PERIOD_MS);
        return true;
    }


    public String getComportName() {
        return comportName;
    }


    public synchronized boolean isRecording() {
        if (bioRecorder != null && bioRecorder.isRecording()) {
            return true;
        }
        return false;
    }

    public boolean isLoffDetecting() {
        if (isLoffDetecting && isRecording()) {
            return true;
        }
        return false;
    }


    public synchronized boolean isActive() {
        if (bioRecorder != null && (bioRecorder.isActive() || bioRecorder.isRecording())) {
            return true;
        }
        return false;
    }

    private synchronized void createRecorder(String comportName) throws ConnectionRuntimeException {
        if (bioRecorder != null) {
            if (bioRecorder.getComportName().equals(comportName)) {
                return;
            } else {
                disconnectRecorder();
            }
        }

        bioRecorder = new BioRecorder(comportName);
        bioRecorder.addEventsListener(new EventsListener() {
            public void handleLowBattery() {
                stop();
                notifyStateChange(new StateChangeReason(StateChangeReason.REASON_LOW_BUTTERY, LOW_BUTTERY_MSG));
            }
        });
        bioRecorder.addButteryLevelListener(new BatteryLevelListener() {

            public void onBatteryLevelReceived(int batteryLevel) {
                EdfBioRecorderApp.this.batteryLevel = batteryLevel;
            }
        });
        bioRecorder.addLeadOffListener(new LeadOffListener() {
            public void onLeadOffMaskReceived(Boolean[] leadOffMask) {
                leadOffBitMask = leadOffMask;
            }
        });
    }

    private synchronized void startMonitoring() throws IllegalStateException {
        if (bioRecorder != null) {
            bioRecorder.startMonitoring();
        }
    }

    public synchronized OperationResult detectLoffStatus(AppConfig appConfig) {
        OperationResult operationResult = start(appConfig, true);
        notifyStateChange(new StateChangeReason(StateChangeReason.REASON_CHECK_CONTACTS_INVOKED));
        return operationResult;
    }

    public synchronized OperationResult startRecording(AppConfig appConfig) {
        OperationResult operationResult = start(appConfig, false);
        notifyStateChange(new StateChangeReason(StateChangeReason.REASON_START_RECORDING_INVOKED));
        return operationResult;
    }

    private synchronized OperationResult start(AppConfig appConfig, boolean isLoffDetection) {
        if (isRecording()) {
            return new OperationResult(false, ALREADY_RECORDING_MSG);
        }

        String comportName = appConfig.getComportName();

        if (comportName == null || comportName.isEmpty()) {
            return new OperationResult(false, COMPORT_NULL_MSG);
        }

        RecorderConfig recorderConfig = appConfig.getRecorderConfig();
        boolean isAllChannelsDisabled = true;
        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
            if (recorderConfig.isChannelEnabled(i)) {
                isAllChannelsDisabled = false;
                break;
            }
        }
        if (isAllChannelsDisabled) {
            if (!recorderConfig.isAccelerometerEnabled()) {
                return new OperationResult(false, ALL_CHANNELS_AND_ACCELEROMETER_DISABLED_MSG);
            }
            if (isLoffDetection) {
                return new OperationResult(false, ALL_CHANNELS_DISABLED_MSG);
            }
        }

        this.comportName = comportName;
        connectionTask.cancel();
        this.isLoffDetecting = isLoffDetection;
        isDurationOfDataRecordComputable = appConfig.isDurationOfDataRecordComputable();

        try {
            createRecorder(comportName);
        } catch (ConnectionRuntimeException ex) {
            String errMSg = ex.getMessage();
            if (ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_BUSY) {
                errMSg = MessageFormat.format(COMPORT_BUSY_MSG, comportName);
            }
            if (ex.getExceptionType() == ConnectionRuntimeException.TYPE_PORT_NOT_FOUND) {
                errMSg = MessageFormat.format(COMPORT_NOT_FOUND_MSG, comportName);
            }
            return new OperationResult(false, errMSg);
        }

        // remove all previously added filters
        bioRecorder.removeChannelsFilters();

        if (isLoffDetection) { // lead off detection
            leadOffBitMask = null;
            recorderConfig.setSampleRate(RecorderSampleRate.S500);
            for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
                recorderConfig.setChannelLeadOffEnable(i, true);
                recorderConfig.setChannelDivider(i, RecorderDivider.D10);
            }
            bioRecorder.removeDataListener();
        } else { // normal recording and writing to the file

            for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
                recorderConfig.setChannelLeadOffEnable(i, false);

                if (appConfig.is50HzFilterEnabled(i)) {
                    // Apply MovingAverage filter to the channel to reduce 50Hz noise
                    int numberOfAveragingPoints = recorderConfig.getChannelSampleRate(i) / 50;
                    bioRecorder.addChannelFilter(i, new MovingAverageFilter(numberOfAveragingPoints), "MovAvg:" + numberOfAveragingPoints);
                }
            }

            // check if directory exist
            String dirToSave = appConfig.getDirToSave();
            if (!isDirectoryExist(dirToSave)) {
                String errMSg = MessageFormat.format(DIRECTORY_NOT_EXIST_MSG, dirToSave);
                return new OperationResult(false, errMSg);
            }

            numberOfWrittenDataRecords.set(0);

            File edfFile = new File(dirToSave, normalizeFilename(appConfig.getFileName()));

            DataConfig dataConfig = bioRecorder.getDataConfig(recorderConfig);
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

            bioRecorder.addDataListener(new DataListener() {

                public void onDataReceived(int[] dataRecord) {
                    try {
                        edfWriter.writeDigitalRecord(dataRecord);
                        numberOfWrittenDataRecords.incrementAndGet();
                    } catch (IOException ex) {
                        // although stop() will be called from not-GUI thread
                        // it could not coincide with startRecording() course
                        // this exception can be thrown only
                        // when BioRecorder is already "recording".
                        // And if it coincide with another stop() called from GUI or
                        // it will not course any problem
                        stop();
                        String errMsg = MessageFormat.format(FAILED_WRITE_DATA_MSG, numberOfWrittenDataRecords.get() + 1, edfFile)
                                + "\n" + ex.getMessage();
                        log.error(errMsg);
                        notifyStateChange(new StateChangeReason(StateChangeReason.REASON_FAILED_WRITING_DATA, errMsg));
                    } catch (IllegalStateException ex) {
                        // after stopping BioRecorder and closing edfWriter
                        // some records still can be received and this
                        // exception can be thrown.
                        log.info(ex);
                    }
                }
            });
        }

        Future<Boolean> startFuture = bioRecorder.startRecording(recorderConfig);
        startFutureHandlingTask = new StartFutureHandlerTask(startFuture, edfWriter, recorderConfig.getDeviceType());
        timer.schedule(startFutureHandlingTask, FUTURE_CHECKING_PERIOD_MS, FUTURE_CHECKING_PERIOD_MS);
        return new OperationResult(true);
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

        public void run() {
            if (future.isDone()) {
                try {
                    if (!future.get()) {
                        cancelStart();
                        RecorderType realDeviceType = bioRecorder.getDeviceType();
                        if (realDeviceType != null && realDeviceType != recorderType) {
                            String errMsg = MessageFormat.format(WRONG_DEVICE_TYPE_MSG, recorderType, bioRecorder.getDeviceType());
                            notifyStateChange(new StateChangeReason(StateChangeReason.REASON_FAILED_STARTING_WRONG_DEVICE_TYPE, errMsg));

                        } else {
                            String errMsg = START_FAILED_MSG;
                            notifyStateChange(new StateChangeReason(StateChangeReason.REASON_FAILED_STARTING, errMsg));
                        }
                    }
                } catch (InterruptedException e) {
                    cancelStart();
                    notifyStateChange(new StateChangeReason(StateChangeReason.REASON_CANCEL_STARTING, START_CANCELED));
                } catch (CancellationException e) {
                    cancelStart();
                    notifyStateChange(new StateChangeReason(StateChangeReason.REASON_CANCEL_STARTING, START_CANCELED));
                } catch (ExecutionException e) {
                    cancelStart();
                    log.error(e.getMessage());
                    notifyStateChange(new StateChangeReason(StateChangeReason.REASON_FAILED_STARTING, e.getMessage()));
                }
                cancel(); // cancel this task
            }
        }

        private void cancelStart() {
            try {
                if (edfWriter1 != null) {
                    edfWriter1.close();
                    File writtenFile = edfWriter1.getFile();
                    writtenFile.delete();
                }
                startMonitoring();
            } catch (Exception ex) {
                log.error(ex);
            }
        }
    }

    private synchronized OperationResult stop1() {
        if (bioRecorder != null) {
            bioRecorder.stop();
            bioRecorder.removeDataListener();
        }

        if (startFutureHandlingTask != null) {
            startFutureHandlingTask.cancel();
        }

        String msg = "";
        boolean isFileCloseOk = true;
        if (edfWriter != null && !isLoffDetecting) {
            File edfFile = edfWriter.getFile();
            try {
                edfWriter.setStartRecordingTime(bioRecorder.getStartMeasuringTime());
                if(isDurationOfDataRecordComputable) {
                    edfWriter.setDurationOfDataRecords(bioRecorder.getDurationOfDataRecord());
                }
                edfWriter.close();
                msg = "Data saved to file:\n"+ edfFile+"\n\n" + edfWriter.getWritingInfo();
                log.info(msg);
            } catch (Exception ex) {
                isFileCloseOk = false;
                log.error(ex);
                msg = MessageFormat.format(FAILED_CLOSE_FILE_MSG, edfFile) + "\n" + ex.getMessage();
            }
        }
        return new OperationResult(isFileCloseOk, msg);
    }

    private OperationResult stopAndMonitor() {
        OperationResult stopResult = stop1();
        startMonitoring();
        return stopResult;
    }

    public OperationResult stop() {
        OperationResult operationResult = stopAndMonitor();
        notifyStateChange(new StateChangeReason(StateChangeReason.REASON_STOP_INVOKED));
        return operationResult;
    }


    private synchronized void disconnectRecorder() {
        if (bioRecorder != null) {
            if (!bioRecorder.disconnect()) {
                String errMsg = MessageFormat.format(FAILED_DISCONNECT_MSG, bioRecorder.getComportName());
                log.error(errMsg);
            }
            bioRecorder = null;
        }
    }

    public void finalize() {
        if (isRecording()) {
            stop1();
        }
        disconnectRecorder();
        timer.cancel();
    }

    public Boolean[] getLeadOffMask() {
        return leadOffBitMask;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public boolean isDirectoryExist(String directory) {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * Create directory if it does not exist.
     *
     * @param directory name of the directory to create
     * @return OperationResult: successful if  and only if the  directory was created
     */

    public OperationResult createDirectory(String directory) {
        File dir = new File(directory);
        if (isDirectoryExist(directory)) {
            String errMSg = MessageFormat.format(DIRECTORY_EXIST_MSG, dir.getName());
            return new OperationResult(false, errMSg);
        }
        try {
            if (dir.mkdir()) {
                return new OperationResult(true);
            } else {
                String errMSg = MessageFormat.format(FAILED_CREATE_DIR_MSG, dir);
                return new OperationResult(false, errMSg);
            }

        } catch (Exception ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(FAILED_CREATE_DIR_MSG, dir) + "\n" + ex.getMessage();
            return new OperationResult(false, errMSg);
        }
    }


    /**
     * If the comportName is not equal to any available port we add it to the list.
     * <p>
     * String full comparison is very "heavy" operation.
     * So instead we will compare only lengths and 2 last symbols...
     * That will be quick and good enough for our purpose
     *
     * @return available comports list with selected port included
     */
    public String[] getAvailableComports() {
        String[] availablePorts = BioRecorder.getAvailableComportNames();
        if (comportName == null || comportName.isEmpty()) {
            return availablePorts;
        }
        if (availablePorts.length == 0) {
            String[] resultantPorts = new String[1];
            resultantPorts[0] = comportName;
            return resultantPorts;
        }

        boolean isSelectedPortAvailable = false;
        for (String port : availablePorts) {
            if (port.length() == comportName.length()
                    && port.charAt(port.length() - 1) == comportName.charAt(comportName.length() - 1)
                    && port.charAt(port.length() - 2) == comportName.charAt(comportName.length() - 2)) {
                isSelectedPortAvailable = true;
                break;
            }
        }
        if (isSelectedPortAvailable) {
            return availablePorts;
        } else {
            String[] resultantPorts = new String[availablePorts.length + 1];
            resultantPorts[0] = comportName;
            System.arraycopy(availablePorts, 0, resultantPorts, 1, availablePorts.length);
            return resultantPorts;
        }
    }

    /**
     * EdfBioRecorderApp permits to add only ONE ProgressListener!
     * So if a new listener added the old one are automatically removed
     */
    public void addProgressListener(ProgressListener l) {
        if (l != null) {
            progressListener = l;
        }
    }

    /**
     * EdfBioRecorderApp permits to add only ONE StateChangeListener!
     * So if a new listener added the old one are automatically removed
     */
    public void addStateChangeListener(StateChangeListener l) {
        if (l != null) {
            stateChangeListener = l;
        }
    }

    /**
     * EdfBioRecorderApp permits to add only ONE AvailableComportsListener!
     * So if a new listener added the old one are automatically removed
     */
    public void addAvailableComportsListener(AvailableComportsListener l) {
        if (l != null) {
            availableComportsListener = l;
        }
    }

    public long getNumberOfWrittenDataRecords() {
        return numberOfWrittenDataRecords.get();
    }


    private String normalizeFilename(@Nullable String filename) {
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
            return defaultFilename + filename;
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

    private void notifyProgress() {
        progressListener.onProgress();
    }

    private void notifyStateChange(StateChangeReason stateChangeReason) {
        stateChangeListener.onStateChanged(stateChangeReason);
    }

    private void notifyAvailableComports(String[] availableComports) {
        availableComportsListener.onAvailableComportsChanged(availableComports);
    }

    class NullProgressListener implements ProgressListener {
        @Override
        public void onProgress() {
            // do nothing
        }
    }

    class NullStateChangeListener implements StateChangeListener {
        @Override
        public void onStateChanged(StateChangeReason changeReason) {
            // do nothing
        }
    }

    class NullAvailableComportsListener implements AvailableComportsListener {
        @Override
        public void onAvailableComportsChanged(String[] availableComports) {
            // do nothing
        }
    }

    class CheckAvailableComportsTask extends TimerTask {
        public void run() {
            if (!isRecording()) {
                notifyAvailableComports(getAvailableComports());
            }
        }
    }

    class ConnectionTask extends TimerTask {

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
}
