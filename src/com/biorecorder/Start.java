package com.biorecorder;

import com.biorecorder.gui.BdfRecorderWindow;

public class Start {
    public static void main(String[] args) {
        JsonPreferences preferences = new JsonPreferences();
        AppConfig recordingSettings = preferences.getConfig();
        BdfRecorderApp bdfRecorder = new BdfRecorderApp(preferences, recordingSettings.getComportName());
        BdfRecorderWindow bdfRecorderWindow = new BdfRecorderWindow(bdfRecorder, recordingSettings);
        bdfRecorder.setMessageListener(bdfRecorderWindow);
        bdfRecorder.setNotificationListener(bdfRecorderWindow);
    }
}
