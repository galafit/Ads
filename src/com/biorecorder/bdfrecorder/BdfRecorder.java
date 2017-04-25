package com.biorecorder.bdfrecorder;


import com.biorecorder.ads.Ads;
import com.biorecorder.ads.AdsConfigUtil;
import com.biorecorder.ads.AdsConfiguration;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.HeaderConfig;
import com.biorecorder.gui.SettingsWindow;

public class BdfRecorder {
    public static void main(String[] args) {
        Ads ads = new Ads();
        AdsConfigUtil adsConfigUtil = new AdsConfigUtil();
        AdsConfiguration adsConfiguration = adsConfigUtil.readConfiguration();
        Controller controller = new Controller(ads);
        SettingsWindow settingsWindow = new SettingsWindow(controller, adsConfiguration);
        controller.setSettingsWindow(settingsWindow);
        ads.addAdsDataListener(settingsWindow);
        controller.comPortConnect(adsConfiguration);
    }
}
