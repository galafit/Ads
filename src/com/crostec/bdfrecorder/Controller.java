package com.crostec.bdfrecorder;

import com.crostec.ads.*;
import com.crostec.bdfrecorder.SettingsWindow;

import javax.swing.*;

public class Controller {

    private boolean isRecording;
    private SettingsWindow settingsWindow;
    private Ads ads;
    private BdfWriter bdfWriter;

    public Controller(Ads ads) {
        this.ads = ads;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(BdfHeaderData bdfHeaderData) {
        isRecording = true;
        if (bdfWriter != null) {
            ads.removeAdsDataListener(bdfWriter);
        }
        bdfWriter = new BdfWriter(bdfHeaderData);
        ads.addAdsDataListener(bdfWriter);
        try {
            ads.startRecording(bdfHeaderData.getAdsConfiguration());
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            System.exit(0);
        }

    }

    public void stopRecording() {
        if (!isRecording) return;
        bdfWriter.stopRecording();
        ads.stopRecording();
        isRecording = false;
    }

    public void closeApplication(BdfHeaderData bdfHeaderData) {
        stopRecording();
        new AdsConfigUtil().saveAdsConfiguration(bdfHeaderData.getAdsConfiguration());
        System.exit(0);
    }

    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }
}
