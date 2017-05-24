package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.ads.exceptions.ComPortNotFoundRuntimeException;
import com.biorecorder.bdfrecorder.exceptions.UserInfoRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;


public class BdfRecorder implements AdsEventsListener {
    private static final Log log = LogFactory.getLog(BdfRecorder.class);
    private static final int SUCCESS_STATUS = 0;
    private static final int ERROR_STATUS = 1;
    private boolean isRecording;
    private Ads ads = new Ads();
    private AdsAdsDataListenerBdfWriter bdfWriter;
    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    private Preferences preferences;
    private int notificationDelayMs = 1000;
    Timer notificationTimer;
    private List<NotificationListener> notificationListeners = new ArrayList<NotificationListener>(1);

    public BdfRecorder(Preferences preferences) {
        this.preferences = preferences;
        bdfRecorderConfig = preferences.getConfig();
        ads.addAdsEventsListener(this);
        ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
        notificationTimer = new Timer(notificationDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                for (NotificationListener recordsNumberListener : notificationListeners) {
                    recordsNumberListener.update();
                }
            }
        });
    }

    public void setBdfRecorderConfig(BdfRecorderConfig bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
        log.info(bdfRecorderConfig.getAdsConfig().toString());
    }

    public BdfRecorderConfig getBdfRecorderConfig() {
        return bdfRecorderConfig;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public File getSavedFile() {
        if (bdfWriter != null) {
            return bdfWriter.getEdfFile();
        }
        return null;
    }

    public void addNotificationListener(NotificationListener l) {
        notificationListeners.add(l);
    }

    public int getNumberOfWrittenDataRecords() {
        if (bdfWriter != null) {
            try {
                return bdfWriter.getNumberOfWrittenDataRecords();
            } catch (Exception e) {
                String errMsg = "Error during getting number of written datarecords";
                log.error(errMsg, e);
                System.exit(ERROR_STATUS);
            }

        }
        return 0;
    }

    @Override
    public void handleAdsLowButtery() {

    }

    public String[] getComportNames() {
        String[] availablePorts = Ads.getAvailableComPortNames();
        String selectedPort = bdfRecorderConfig.getAdsConfig().getComPortName();
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
        return ports;
    }

    public void connect() {
        try {
            ads.connect();
        } catch (ComPortNotFoundRuntimeException e) {
            throw new UserInfoRuntimeException(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error during start recording";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void startRecording() throws UserInfoRuntimeException {
        try {
            isRecording = true;
            if (bdfWriter != null) {
                ads.removeAdsDataListener(bdfWriter);
            }

            bdfWriter = new AdsAdsDataListenerBdfWriter(bdfRecorderConfig);
            ads.addAdsDataListener(bdfWriter);
            notificationTimer.start();
            ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
            ads.startRecording();
        } catch (ComPortNotFoundRuntimeException e) {
            throw new UserInfoRuntimeException(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error during start recording";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void stopRecording() {
        if (!isRecording) return;
        try {
            if (bdfWriter != null) {
                bdfWriter.stop();
            }
            ads.stopRecording();
            notificationTimer.stop();
            isRecording = false;
        } catch (Exception e) {
            String errMsg = "Error during stop recording";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void closeApplication(BdfRecorderConfig bdfRecorderConfig) {
        try {
            stopRecording();
            ads.disconnect();
            preferences.saveConfig(bdfRecorderConfig);
            System.exit(SUCCESS_STATUS);
        } catch (Exception e) {
            String errMsg = "Error during close application";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }
}
