package com.biorecorder;

import com.biorecorder.gui.RecorderView;
import com.biorecorder.gui.RecorderViewModel;

import java.util.Set;

public class Start {
    public static void main(String[] args) {
        JsonPreferences preferences = new JsonPreferences();
        RecorderViewModel bdfRecorder = new RecorderViewModelImpl(new EdfBioRecorderApp(), preferences);
        RecorderView recorderView = new RecorderView(bdfRecorder);
        bdfRecorder.addProgressListener(recorderView);
        bdfRecorder.addAvailableComportsListener(recorderView);
        bdfRecorder.addStateChangeListener(recorderView);

    }
}
