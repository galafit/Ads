package com.biorecorder;

import com.biorecorder.gui.BdfRecorderWindow;

public class Start {
    public static void main(String[] args) {
        JsonPreferences preferences = new JsonPreferences();
        BdfRecorderApp bdfRecorder = new BdfRecorderApp(preferences);
        BdfRecorderWindow bdfRecorderWindow = new BdfRecorderWindow(bdfRecorder);
        bdfRecorder.setMessageListener(bdfRecorderWindow);
        bdfRecorder.setNotificationListener(bdfRecorderWindow);
    }
}
