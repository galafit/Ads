package com.biorecorder;

import com.biorecorder.recorder.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;

/**
 * Class containing all the necessary settings for running the application
 */
public class AppConfig {
    private RecorderConfig recorderConfig = new RecorderConfig();
    private boolean isDurationOfDataRecordComputable = true;
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private boolean[] filter50HzMask;

    private String comportName;
    private String dirToSave;
    @JsonIgnore
    private String fileName;

    public AppConfig() {
        filter50HzMask = new boolean[RecorderType.getMaxChannelsCount()];
        for (int i = 0; i < filter50HzMask.length; i++) {
            filter50HzMask[i] = true;
        }
    }

    public AppConfig(AppConfig configToCopy) {
        recorderConfig = new RecorderConfig(configToCopy.recorderConfig);
        isDurationOfDataRecordComputable = configToCopy.isDurationOfDataRecordComputable;
        patientIdentification = configToCopy.patientIdentification;
        recordingIdentification = configToCopy.recordingIdentification;
        comportName = configToCopy.comportName;
        dirToSave = configToCopy.dirToSave;
        fileName = configToCopy.fileName;
        for (int i = 0; i < filter50HzMask.length; i++) {
            filter50HzMask[i] = configToCopy.filter50HzMask[i];
        }
    }

    public RecorderConfig getRecorderConfig() {
        return recorderConfig;
    }

    public void setRecorderConfig(RecorderConfig recorderConfig) {
        this.recorderConfig = recorderConfig;
    }


    public String getPatientIdentification() {
        return patientIdentification;
    }


    public void setPatientIdentification(String patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    public String getRecordingIdentification() {
        return recordingIdentification;
    }

    public void setRecordingIdentification(String recordingIdentification) {
        this.recordingIdentification = recordingIdentification;
    }

    public boolean is50HzFilterEnabled(int channelNumber) {
        return filter50HzMask[channelNumber];
    }

    public void set50HzFilterEnabled(int channelNumber, boolean is50HzFilterEnabled) {
         filter50HzMask[channelNumber] = is50HzFilterEnabled;
    }

    public boolean isDurationOfDataRecordComputable() {
        return isDurationOfDataRecordComputable;
    }

    public void setDurationOfDataRecordComputable(boolean durationOfDataRecordComputable) {
        isDurationOfDataRecordComputable = durationOfDataRecordComputable;
    }

    public String getComportName() {
        return comportName;
    }

    public void setComportName(String comportName) {
        this.comportName = comportName;
    }

    public String getDirToSave() {
        // first we try to return «dirToSave» if it is specified
        if(dirToSave != null) {
            return dirToSave;
        }
        // if «dirToSave» is not specified we return «projectDir/records»

        String projectDir = System.getProperty("user.dir");
        String dirName = "records";

        File dir = new File (projectDir, dirName);
        return dir.toString();
       /*
        // finally we return «homeDir/records»
        String userHomeDir = System.getProperty("user.home");
        dir = new File (userHomeDir, dirName);
        return dir.toString();*/
    }

    public void setDirToSave(String dirToSave) {
        this.dirToSave = dirToSave;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
