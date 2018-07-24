package com.biorecorder.gui;

import com.biorecorder.*;

/**
 * Created by galafit on 14/6/18.
 */
public interface RecorderViewModel {

    void addProgressListener(ProgressListener l);
    void addStateChangeListener(StateChangeListener l);
    void addAvailableComportsListener(AvailableComportsListener l);

    RecorderSettingsImpl getInitialSettings();

    /**
     * Mask gives TRUE if electrode is DISCONNECTED,
     * FALSE if electrode is CONNECTED and
     * NULL if channel is disabled (or work in mode different from "input")
     * @return disconnection bit mask for positive and negative electrode of every channel
     */
    Boolean[] getDisconnectionMask();
    Integer getBatteryLevel();
    String getProgressInfo();


    boolean isActive();
    boolean isRecording();
    boolean isCheckingContacts();
    
    void changeComport(String comportName);
    RecorderSettings changeDeviceType(RecorderSettings settings);
    RecorderSettings changeMaxFrequency(RecorderSettings settings);

    String getNormalizedFilename(String directory, String filename);
    boolean isFileExist(String directory, String filename);
    boolean isDirectoryExist(String directory);
    OperationResult createDirectory(String directory);
    OperationResult startRecording(RecorderSettings settings);
    OperationResult checkContacts(RecorderSettings settings);
    OperationResult stop();
    void closeApplication(RecorderSettings settings);
}
