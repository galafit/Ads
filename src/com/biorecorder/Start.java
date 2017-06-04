package com.biorecorder;

import com.biorecorder.gui.SettingsWindow;

public class Start {
    public static void main(String[] args) {
        Preferences preferences = new JsonPreferences();
        BdfRecorderApp bdfRecorder = new BdfRecorderApp(preferences);
        SettingsWindow settingsWindow = new SettingsWindow(bdfRecorder);
    }
}
