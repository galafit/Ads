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
    private AppConfig appConfig;

    private int NOTIFICATION_PERIOD_MS = 1000;
    private int CONNECTION_PERIOD_MS = 2000;
    javax.swing.Timer notificationTimer;
    java.util.Timer connectionTimer;
    private volatile String[] comportNames;

    private List<NotificationListener> notificationListeners = new ArrayList<NotificationListener>(1);
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>(1);

    public BdfRecorderApp(Preferences preferences) {
        this.preferences = preferences;
        appConfig = preferences.getConfig();
        bdfRecorder.setConfig(appConfig.getBdfRecorderConfig());
        bdfRecorder.addLowButteryEventListener(this);
        createAvailablePortNamesList();

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
                String comportName = appConfig.getComportName();
                createAvailablePortNamesList();
                if (!bdfRecorder.isConnected() && bdfRecorder.isComportAvailable(comportName)) {
                    try {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                bdfRecorder.connect(comportName);
                            }
                        });

                    } catch (Exception ex) {
                        //log.error("Error during connection: "+comportName, ex);
                        // DO NOTHING!
                    }
                }
            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    public synchronized String[] getAvailableComportNames() {
        return comportNames;
    }

    public AppConfig getConfig() {
        return appConfig;
    }

    @Override
    public void handleLowButteryEvent() {
        stopRecording();
        sendMessage(LOW_BUTTERY_MSG);
    }

    private void createAvailablePortNamesList() {
        String[] availablePorts = BdfRecorder.getAvailableComportNames();
        String selectedPort = appConfig.getComportName();
        String[] ports = availablePorts;
        if (selectedPort != null && !selectedPort.isEmpty()) {
            boolean containSelectedPort = false;
            for (String port : availablePorts) {
                if (port.equalsIgnoreCase(selectedPort)) {
                    containSelectedPort = true;
                    break;
                }
            }
            if (!containSelectedPort) {
                String[] newPorts = new String[ports.length + 1];
                newPorts[0] = selectedPort;
                System.arraycopy(availablePorts, 0, newPorts, 1, availablePorts.length);
                ports = newPorts;
            }
        }
        synchronized(this){
            comportNames = ports;
        }

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

    public void connect(String comportName) {
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

    public void startRecording(AppConfig appConfig)  {
        this.appConfig = appConfig;
        File fileToWrite = new File(appConfig.getDirToSave(), appConfig.getFilename());
        String comportName = appConfig.getComportName();
        try {
            bdfRecorder.setConfig(appConfig.getBdfRecorderConfig());
            bdfRecorder.connect(comportName);
            bdfRecorder.startRecording(fileToWrite);
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
            String errMSg = MessageFormat.format(APP_ERR, ex.getMessage());
            sendMessage(errMSg);
            closeApplication(ERROR_STATUS);
        }
    }

    public void stopRecording() throws BdfRecorderRuntimeException {
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

    public int getNumberOfWrittenDataRecords() {
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

    public File getSavedFile() {
        return bdfRecorder.getSavedFile();
    }

    private void closeApplication(int status) {
        try {
            bdfRecorder.stopRecording();
            notificationTimer.stop();
            connectionTimer.cancel();
            preferences.saveConfig(appConfig);
            bdfRecorder.disconnect();
            System.exit(status);
        } catch (Exception e) {
            String errMsg = "Error during close application";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void closeApplication(AppConfig appConfig) {
        this.appConfig = appConfig;
        closeApplication(SUCCESS_STATUS);
    }
}
