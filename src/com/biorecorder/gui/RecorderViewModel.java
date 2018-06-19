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
    Boolean[] getContactsMask();
    Integer getBatteryLevel();
    String getProgressInfo();


    boolean isActive();
    boolean isRecording();
    boolean isCheckingContacts();
    
    void changeComport(String comportName);
    RecorderSettings changeDeviceType(RecorderSettings settings);
    RecorderSettings changeMaxFrequency(RecorderSettings settings);

    boolean isDirectoryExist(String directory);
    OperationResult createDirectory(String directory);
    OperationResult startRecording(RecorderSettings settings);
    OperationResult checkContacts(RecorderSettings settings);
    OperationResult stop();
    void closeApplication(RecorderSettings settings);
}
