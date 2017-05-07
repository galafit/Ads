package com.biorecorder.bdfrecorder;


import com.biorecorder.ads.Ads;
import com.biorecorder.gui.SettingsWindow;



public class BdfRecorder {
    public static void main(String[] args) {
        Ads ads = new Ads();
        Controller controller = new Controller(ads);
        SettingsWindow settingsWindow = new SettingsWindow(controller);
        controller.setSettingsWindow(settingsWindow);
        ads.addAdsDataListener(settingsWindow);
        controller.comPortConnect();
    }
}
