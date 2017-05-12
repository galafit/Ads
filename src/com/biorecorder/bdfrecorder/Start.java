package com.biorecorder.bdfrecorder;


import com.biorecorder.ads.Ads;
import com.biorecorder.gui.SettingsWindow;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Start {
    private static  final Log log = LogFactory.getLog(Start.class);

    public static void main(String[] args) {
        Preferences preferences = new JsonPreferences();
        BdfRecorder bdfRecorder = new BdfRecorder(preferences);
        SettingsWindow settingsWindow = new SettingsWindow(bdfRecorder);
        bdfRecorder.addNotificationListener(settingsWindow);
        bdfRecorder.connect();
    }
}
