package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.gui.SettingsWindow;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import javax.swing.*;
import java.io.IOException;

public class Controller {

    private boolean isRecording;
    private SettingsWindow settingsWindow;
    private Ads ads;
    private AdsListenerBdfWriter bdfWriter;

    private static  final Log log = LogFactory.getLog(Controller.class);

    public Controller(Ads ads) {
        this.ads = ads;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(BdfHeaderData bdfHeaderData) {
        new AdsConfigUtil().saveAdsConfiguration(bdfHeaderData.getAdsConfiguration());
        log.info(bdfHeaderData.getAdsConfiguration().toString());
        isRecording = true;
        if (bdfWriter != null) {
            ads.removeAdsDataListener(bdfWriter);
        }

        //TODO exeptions handling and messages
        try {
            bdfWriter = new AdsListenerBdfWriter(bdfHeaderData);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
        ads.addAdsDataListener(bdfWriter);
        try {
            ads.startRecording(bdfHeaderData.getAdsConfiguration());
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    public void stopRecording() {
        if (!isRecording) return;
        ads.stopRecording();
        isRecording = false;
    }

    public void closeApplication(BdfHeaderData bdfHeaderData) {
        new AdsConfigUtil().saveAdsConfiguration(bdfHeaderData.getAdsConfiguration());
        stopRecording();
        ads.comPortDisconnect();
        System.exit(0);
    }

    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }

    public void comPortConnect(AdsConfiguration adsConfiguration) {
        try {
            ads.comPortConnect(adsConfiguration);
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
}
