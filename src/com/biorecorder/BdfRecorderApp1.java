package com.biorecorder;

import com.biorecorder.bdfrecorder.*;
import com.biorecorder.bdfrecorder.exceptions.*;

import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.EdfConfig;
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

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderApp1  {
    private static final Log log = LogFactory.getLog(BdfRecorderApp1.class);

    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    private final int MAX_START_TIMEOUT_SEC = 30;

    private static final String FILE_NOT_ACCESSIBLE_ERR = "File: {0} could not be created or accessed.";
    private static final String COMPORT_BUSY_ERR = "ComPort: {0} is busy.";
    private static final String COMPORT_NOT_FOUND_ERR = "ComPort: {0} is not found.";
    private static final String APP_ERR = "Error: {0}";
    private static final String LOW_BUTTERY_MSG = "The buttery is low. BdfRecorder was stopped.";
    private static final String START_CANCELLED_MSG = "Start cancelled.";
    private static final String START_FAILED_MSG = "Start failed. Check whether the device is connected" +
            "\nand selected ComPort is correct and try again.";
    private static final String DIR_CREATION_CONFIRMATION_MSG = "Directory: {0}\ndoes not exist. Do you want to create it?";


    private Preferences1 appConfig;
    private BdfRecorder1 bdfRecorder;
    private volatile EdfFileWriter edfFileWriter;
    private volatile File edfFile;
    private volatile long numberOfWrittenDataRecords;
    private volatile Boolean[] loffBitMask;

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    private javax.swing.Timer notificationTimer;
    private java.util.Timer connectionTimer;
    private volatile String comportName;
    private AppConfig1 recordingSettings;

    private NotificationListener notificationListener;
    private MessageListener messageListener;
    private volatile boolean isRecording = false;
    private volatile long recordingStartTime;

    public BdfRecorderApp1(Preferences1 appConfig, String comportName) {
        this.appConfig = appConfig;
        this.comportName = comportName;

        notificationTimer = new javax.swing.Timer(NOTIFICATION_PERIOD_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isRecording && numberOfWrittenDataRecords == 0 &&  (System.currentTimeMillis() - recordingStartTime) > MAX_START_TIMEOUT_SEC * 1000) {
                    stopRecording();
                }

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
                try {
                    createBdfRecorder();
                   // connectionTimer.cancel();
                } catch (ConnectionRuntimeException e) {
                   // do nothing;
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);

    }

    public void changeRecorder() {
        stopRecording();
        recordingSettings = null;
        loffBitMask = null;
        edfFileWriter = null;
        edfFile = null;
        if(bdfRecorder != null) {
            bdfRecorder.disconnect();
        }
        bdfRecorder = null;
    }

    private void createBdfRecorder() throws ConnectionRuntimeException {
        if (bdfRecorder == null && comportName != null && !comportName.isEmpty()) {
            bdfRecorder = new BdfRecorder1(comportName);
            bdfRecorder.addLowButteryEventListener(new LowButteryEventListener() {
                @Override
                public void handleLowButteryEvent() {
                    stopRecording();
                    sendMessage(LOW_BUTTERY_MSG);
                }
            });
            bdfRecorder.startMonitoring();
        }
    }

    public void setComportName(String comportName) {
        changeRecorder();
        this.comportName = comportName;
    }

    public Boolean[] getLoffMask() {
        return loffBitMask;
    }

    private void sendMessage(String message) {
        if(messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }


    public void startRecording(AppConfig1 recordingSettings)  {
        this.recordingSettings = recordingSettings;
        comportName = recordingSettings.getComportName();

        String dirToSave = recordingSettings.getDirToSave();
        File dir = new File(dirToSave);
        if(!dir.exists()) {
            String msg = MessageFormat.format(DIR_CREATION_CONFIRMATION_MSG, dirToSave);
            boolean isConfirmed = messageListener.onConfirmationAsked(msg);
            if(isConfirmed) {
                dir.mkdir();
            }
            else {
                return;
            }
        }
        String filename =  normalizeFilename(recordingSettings.getFileName());
        File fileToWrite = new File(dirToSave, filename);

        try {
            createBdfRecorder();


            // Apply MovingAverage filters to to ads channels to reduce 50Hz noise
            int enableSignalsCounter = 0;
            BdfRecorderConfig1 bdfRecorderConfig = recordingSettings.getBdfRecorderConfig();
            for (int i = 0; i < bdfRecorderConfig.getNumberOfChannels(); i++) {
                if (bdfRecorderConfig.isChannelEnabled(i)) {
                    if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                        int numberOfAveragingPoints = bdfRecorderConfig.getChannelFrequency(i) / 50;
                        bdfRecorder.addChannelFilter(enableSignalsCounter, new MovingAverageFilter(numberOfAveragingPoints), "MovAvg:"+ numberOfAveragingPoints);
                    }
                    enableSignalsCounter++;
                }
            }


            EdfConfig edfConfig = bdfRecorder.getResultantRecordingInfo(recordingSettings.getBdfRecorderConfig());
            edfFileWriter = new EdfFileWriter(fileToWrite, FileType.BDF_24BIT);
            edfFileWriter.setDurationOfDataRecordsComputable(recordingSettings.isDurationOfDataRecordComputable());
            edfFileWriter.setConfig(edfConfig);
            bdfRecorder.addBdfDataListener(new BdfDataListener() {
                @Override
                public void onDataRecordReceived(int[] dataRecord) {
                    edfFileWriter.writeDigitalSamples(dataRecord);
                    numberOfWrittenDataRecords = edfFileWriter.getNumberOfReceivedDataRecords();
                }
            });
            bdfRecorder.startRecording(recordingSettings.getBdfRecorderConfig());
            bdfRecorder.addLoffListener(new LoffListener() {
                @Override
                public void onLoffDataReceived(Boolean[] loffMask) {
                    loffBitMask = loffMask;
                }
            });

            edfFile = fileToWrite;
            recordingStartTime = System.currentTimeMillis();
            isRecording = true;

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

    public void stopRecording() throws BdfRecorderRuntimeException {
        try {
            if(bdfRecorder != null) {
                bdfRecorder.stop();
            }
            if(edfFileWriter != null) {
                edfFileWriter.close();
            }

            isRecording = false;
            loffBitMask = null;
        } catch (Exception ex) {
            log.error(ex);
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
            closeApplication(ERROR_STATUS);
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
    public String[] getComportNames() {
        String[] availablePorts = getAvailableComportNames();
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

    private static String[] getAvailableComportNames() {
        return BdfRecorder1.getAvailableComportNames();
    }


    public void setNotificationListener(NotificationListener l) {
        notificationListener = l;
    }

    public void setMessageListener(MessageListener l) {
        messageListener = l;
    }


    public boolean isRecording() {
        return isRecording;
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

        if(isRecording) {
            if(numberOfWrittenDataRecords == 0) {
                stateString = "Starting...";
            } else {
                stateString = "Recording... " + numberOfWrittenDataRecords + " data records";
            }
        } else {
            if(numberOfWrittenDataRecords > 0) {
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
            notificationTimer.stop();
            connectionTimer.cancel();
            if(bdfRecorder != null) {
                bdfRecorder.stop();
                bdfRecorder.disconnect();
            }
            if(recordingSettings != null) {
                try{
                    appConfig.saveConfig(recordingSettings);
                } catch (Exception ex) {
                    String errMsg = "Error during saving appConfig";
                    log.error(errMsg, ex);
                }
            }
            System.exit(status);
        } catch (Exception e) {
            String errMsg = "Error during closing application";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void closeApplication(AppConfig1 recordingSettings) {
        this.recordingSettings = recordingSettings;
        closeApplication(SUCCESS_STATUS);
    }

}
