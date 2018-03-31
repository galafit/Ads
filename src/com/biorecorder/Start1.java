package com.biorecorder;

import com.biorecorder.gui.BdfRecorderWindow1;

public class Start1 {
    public static void main(String[] args) {
        JsonPreferences preferences = new JsonPreferences();
        AppConfig recordingSettings = preferences.getConfig();
        BdfRecorderApp1 bdfRecorder = new BdfRecorderApp1(preferences, recordingSettings.getComportName());
        BdfRecorderWindow1 bdfRecorderWindow = new BdfRecorderWindow1(bdfRecorder, recordingSettings);
    }
}
