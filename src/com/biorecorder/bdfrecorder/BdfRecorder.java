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
    private Ads ads = new Ads();
    private AdsAdsDataListenerBdfWriter bdfWriter;
    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    private Preferences preferences;

    private boolean isRecording = false;

    private int notificationDelayMs = 1000;
    Timer notificationTimer;
    private List<NotificationListener> notificationListeners = new ArrayList<NotificationListener>(1);

    public BdfRecorder(Preferences preferences)  {
        this.preferences = preferences;
        bdfRecorderConfig = preferences.getConfig();
        ads.addAdsEventsListener(this);
        ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
        ads.addAdsEventsListener(this);
        notificationTimer = new Timer(notificationDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                for (NotificationListener recordsNumberListener : notificationListeners) {
                    recordsNumberListener.update();
                }
            }
        });
        notificationTimer.start();
    }

    public void setBdfRecorderConfig(BdfRecorderConfig bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
        log.info(bdfRecorderConfig.getAdsConfig().toString());
    }

    public BdfRecorderConfig getBdfRecorderConfig() {
        return bdfRecorderConfig;
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
                String errMsg = "Error during getting number of written data records";
                log.error(errMsg, e);
                System.exit(ERROR_STATUS);
            }

        }
        return 0;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isAdsActive() {
        return ads.isActive();
    }

    @Override
    public void handleAdsLowButtery() {

    }

    @Override
    public void handleAdsFrameBroken(String eventAdditionalInfo) {
        log.info(eventAdditionalInfo);
    }

    public String[] getComportNames() {
        String[] availablePorts = Ads.getAvailableComPortNames();
        String selectedPort = bdfRecorderConfig.getComPortName();
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
            ads.connect(bdfRecorderConfig.getComPortName());
        } catch (ComPortNotFoundRuntimeException e) {
            throw new UserInfoRuntimeException(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error during connecting";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void startRecording() throws UserInfoRuntimeException {
        try {
            if (bdfWriter != null) {
                ads.removeAdsDataListener(bdfWriter);
            }
            bdfWriter = new AdsAdsDataListenerBdfWriter(bdfRecorderConfig);
            ads.addAdsDataListener(bdfWriter);
            ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
            ads.startRecording(bdfRecorderConfig.getComPortName());
            isRecording = true;
        } catch (ComPortNotFoundRuntimeException e) {
            throw new UserInfoRuntimeException(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error during connect recording";
            log.error(errMsg, e);
            System.exit(ERROR_STATUS);
        }
    }

    public void stopRecording() {
       // if (!isSendingData) return;
        try {
            if (bdfWriter != null) {
                bdfWriter.stop();
            }
            ads.stopRecording();
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
            notificationTimer.stop();
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
