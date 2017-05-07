package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.gui.SettingsWindow;
import com.biorecorder.gui.comport_gui.ComportDataProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class Controller implements ComportDataProvider{
    private static final File propertyFile = new File(System.getProperty("user.dir"), "config.json");

    private boolean isRecording;
    private SettingsWindow settingsWindow;
    private Ads ads;
    private AdsListenerBdfWriter bdfWriter;
    BdfHeaderData bdfHeaderData;

    private static  final Log log = LogFactory.getLog(Controller.class);

    public Controller(Ads ads) {
        this.ads = ads;
        bdfHeaderData = new BdfHeaderData();

        if(propertyFile.exists() && propertyFile.isFile()) {
            JsonProperties properties = new JsonProperties(propertyFile);
            try {
                bdfHeaderData = (BdfHeaderData) properties.getConfig(BdfHeaderData.class);
             } catch (IOException e) {
                e.printStackTrace();
                log.error("Problem with property file reading: "+ propertyFile +"! " +e.getMessage());
            }
        }
    }

    public int getDecodedFrameSize(AdsConfig adsConfiguration) {
        return Ads.getDecodedFrameSize(adsConfiguration);
    }

    @Override
    public String[] getAvailableComports() {
        return Ads.getAvailableComPortNames();
    }

    public BdfHeaderData getBdfHeaderData() {
        return bdfHeaderData;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording(BdfHeaderData bdfHeaderData) {
        log.info(bdfHeaderData.getAdsConfig().toString());
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
            ads.setAdsConfig(bdfHeaderData.getAdsConfig());
            ads.startRecording();
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
        try {
            JsonProperties properties = new JsonProperties(propertyFile);
            properties.saveCongfig(bdfHeaderData);

        } catch (IOException e) {
            e.printStackTrace();
        }
        stopRecording();
        ads.comPortDisconnect();
        System.exit(0);
    }

    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }

    public void comPortConnect() {
        try {
            ads.comPortConnect();
        } catch (AdsException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
}
