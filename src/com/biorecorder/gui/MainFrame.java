package com.biorecorder.gui;

import com.biorecorder.AvailableComportsListener;
import com.biorecorder.Message;
import com.biorecorder.ProgressListener;
import com.biorecorder.StateChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by galafit on 31/7/18.
 */
public class MainFrame extends JFrame implements ProgressListener, StateChangeListener, AvailableComportsListener {
    private static final String TITLE = "BioRecorder";

    RecorderView recorderView;

    public MainFrame(RecorderViewModel recorder) throws HeadlessException {
        setTitle(TITLE);
        recorderView = new RecorderView(recorder, this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                recorderView.close();
            }
        });
        add(recorderView);
        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);

    }

    @Override
    public void onProgress() {
        recorderView.onProgress();
    }

    @Override
    public void onAvailableComportsChanged(String[] availableComports) {
        recorderView.onAvailableComportsChanged(availableComports);
    }

    @Override
    public void onStateChanged(Message message) {
        recorderView.onStateChanged(message);
    }
}
