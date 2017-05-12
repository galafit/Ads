package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class BdfRecorder {
    private boolean isRecording;
    private Ads ads =  new Ads();
    private AdsListenerBdfWriter bdfWriter;
    private BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
    private Preferences preferences;
    private int notificationDelayMs = 500;
    Timer notificationTimer;
    private List<NotificationListener> recordsNumberListeners = new ArrayList<NotificationListener>(1);

    private static final Log log = LogFactory.getLog(BdfRecorder.class);

    public BdfRecorder(Preferences preferences) {
        this.preferences = preferences;
        bdfRecorderConfig = preferences.getConfig();
        ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
        notificationTimer = new Timer(notificationDelayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (NotificationListener recordsNumberListener : recordsNumberListeners) {
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

    public void connect() {
        try {
            ads.connect();
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
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
        recordsNumberListeners.add(l);
    }

    public int getNumberOfWrittenDataRecords() {
        if (bdfWriter != null) {
            return bdfWriter.getNumberOfWrittenDataRecords();
        }
        return 0;
    }


    public String[] getComportNames() {
        String selectedPort = bdfRecorderConfig.getAdsConfig().getComPortName();
        String[] ports = Ads.getAvailableComPortNames();
        if (selectedPort != null && !selectedPort.isEmpty()) {
            boolean containSelectedPort = false;
            for (String port : ports) {
                if (port.equalsIgnoreCase(selectedPort)) {
                    containSelectedPort = true;
                }
            }
            if (!containSelectedPort) {
                String[] newPorts = new String[ports.length + 1];
                newPorts[0] = selectedPort;
                System.arraycopy(ports, 0, newPorts, 1, ports.length);
                ports = newPorts;
            }
        }
        return ports;
    }

    public void startRecording() {
        isRecording = true;
        if (bdfWriter != null) {
            ads.removeAdsDataListener(bdfWriter);
        }

        //TODO exeptions handling and messages
        try {
            bdfWriter = new AdsListenerBdfWriter(bdfRecorderConfig);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        ads.addAdsDataListener(bdfWriter);
        notificationTimer.start();
        try {
            ads.setAdsConfig(bdfRecorderConfig.getAdsConfig());
            ads.startRecording();
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    public void stopRecording() {
        if (!isRecording) return;
        ads.stopRecording();
        notificationTimer.stop();
        isRecording = false;
    }

    public void closeApplication(BdfRecorderConfig bdfRecorderConfig) {
        stopRecording();
        ads.disconnect();
        preferences.saveConfig(bdfRecorderConfig);
        System.exit(0);
    }
}
