package com.biorecorder;

import com.biorecorder.bdfrecorder.BdfRecorder;
import com.biorecorder.bdfrecorder.LowButteryEventListener;
import com.biorecorder.bdfrecorder.exceptions.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderApp implements LowButteryEventListener {
    private static final Log log = LogFactory.getLog(BdfRecorderApp.class);

    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;

    String FILE_NOT_ACCESSIBLE_ERR = "File: {0} could not be created or accessed.";
    String COMPORT_BUSY_ERR = "ComPort: {0} is busy.";
    String COMPORT_NOT_FOUND_ERR = "ComPort: {0} is not found.";
    String APP_ERR = "Error: {0}";
    String LOW_BUTTERY_MSG = "The buttery is low. BdfRecorder was stopped";


    private Preferences preferences;

    private final BdfRecorder bdfRecorder = new BdfRecorder();

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    javax.swing.Timer notificationTimer;
    java.util.Timer connectionTimer;
    private volatile String[] comportNames;
    private volatile String selectedComportName;
    private AppConfig recordingSettings;

    private List<NotificationListener> notificationListeners = new ArrayList<NotificationListener>(1);
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>(1);

    public BdfRecorderApp(Preferences preferences, String selectedComportName) {
        this.preferences = preferences;
        this.selectedComportName = selectedComportName;
        bdfRecorder.addLowButteryEventListener(this);

        notificationTimer = new javax.swing.Timer(NOTIFICATION_PERIOD_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (NotificationListener listener : notificationListeners) {
                    listener.update();
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
        String[] ports = availablePorts;
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


    @Override
    public void handleLowButteryEvent() {
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

    public void addNotificationListener(NotificationListener l) {
        notificationListeners.add(l);
    }

    public void addMessageListener(MessageListener l) {
        messageListeners.add(l);
    }

    private void sendMessage(String message) {
        for (MessageListener l : messageListeners) {
            l.onMessageReceived(message);
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
        File fileToWrite = new File(recordingSettings.getDirToSave(), recordingSettings.getFilename());
        String comportName = recordingSettings.getComportName();
        selectedComportName = comportName;
        try {
            bdfRecorder.connect(comportName);
            bdfRecorder.startRecording(recordingSettings.getBdfRecorderConfig(), fileToWrite);
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
