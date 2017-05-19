package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.ads.exceptions.AdsConnectionRuntimeException;
import com.biorecorder.ads.exceptions.ComPortNotFoundRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;


public class BdfRecorder {
    private boolean isRecording;
    private Ads ads =  new Ads();
    private AdsListenerBdfWriter bdfWriter;
    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    private Preferences preferences;
    private int notificationDelayMs = 1000;
    Timer notificationTimer;
    private List<NotificationListener> notificationListeners = new ArrayList<NotificationListener>(1);

    private static final Log log = LogFactory.getLog(BdfRecorder.class);

    public BdfRecorder(Preferences preferences) {
        this.preferences = preferences;
        bdfRecorderConfig = preferences.getConfig();
        ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
        notificationTimer = new Timer(notificationDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                for (NotificationListener recordsNumberListener : notificationListeners) {
                    recordsNumberListener.update();
                }
            }
        });

    /*    java.util.Timer timer = new java.util.Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                formPortNames();
            }
        }, 1000, 1000); */
    }

    public void setBdfRecorderConfig(BdfRecorderConfig bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
        log.info(bdfRecorderConfig.getAdsConfig().toString());
    }

    public BdfRecorderConfig getBdfRecorderConfig() {
        return bdfRecorderConfig;
    }

    public void connect() {
        try {
            ads.connect();
        } catch (Exception e) {
            // do nothing !
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public File getSavedFile() {
      if(bdfWriter != null) {
          return bdfWriter.getEdfFile();
      }
      return null;
    }

    public void addNotificationListener(NotificationListener l) {
        notificationListeners.add(l);
    }

    public int getNumberOfWrittenDataRecords() {
        if (bdfWriter != null) {
            return bdfWriter.getNumberOfWrittenDataRecords();
        }
        return 0;
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

    public String[] getComportNames_() {
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

    public void startRecording() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        isRecording = true;
        if (bdfWriter != null) {
            ads.removeAdsDataListener(bdfWriter);
        }

        bdfWriter = new AdsListenerBdfWriter(bdfRecorderConfig);
        ads.addAdsDataListener(bdfWriter);
      //  notificationTimer.start();
        ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
        ads.startRecording();
    }

    public void stopRecording() {
        if (!isRecording) return;
        ads.stopRecording();
      //  notificationTimer.stop();
        isRecording = false;
    }

    public void closeApplication(BdfRecorderConfig bdfRecorderConfig) {
        notificationTimer.stop();
        stopRecording();
        ads.disconnect();
        preferences.saveConfig(bdfRecorderConfig);
        System.exit(0);
    }
}
