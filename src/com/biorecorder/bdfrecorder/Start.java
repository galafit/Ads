package com.biorecorder.bdfrecorder;


import com.biorecorder.gui.SettingsWindow;

public class Start {
    public static void main(String[] args) {
        Preferences preferences = new JsonPreferences();
        BdfRecorder bdfRecorder = new BdfRecorder(preferences);
        SettingsWindow settingsWindow = new SettingsWindow(bdfRecorder);
    }
}
