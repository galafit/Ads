package com.biorecorder;

/**
 * If we will want in the future in RecorderView (or RecorderViewModel)
 * instead of using messages from EdfBioRecorderApplication create
 * its own messages (potentially multilingual) the static fields describing the
 * type of result/error will permit to do that
 */
public class OperationResult {
    public static final String TYPE_COMPORT_BUSY = "Comport busy";
    public static final String TYPE_COMPORT_NOT_FOUND = "Comport not found";
    public static final String TYPE_COMPORT_NULL = "Comport name can not be null or empty";
    public static final String TYPE_ALREADY_RECORDING = "Recorder already recording";
    public static final String TYPE_ALL_CHANNELS_AND_ACCELEROMETER_DISABLED = "All channels and accelerometer disabled";

    public static final String TYPE_ALL_CHANNELS_DISABLED = "All channels disabled";
    public static final String TYPE_DIRECTORY_NOT_EXIST = "Directory not exist";
    public static final String TYPE_FILE_NOT_ACCESSIBLE = "File could not be created or accessed";

    public static final String TYPE_FAILED_CREATE_DIR = "Failed create directory";
    public static final String TYPE_FAILED_CLOSE_FILE = "Failed correctly close and save file";

    public static final String TYPE_SUCCESS = "Successfully completed";

    private boolean isSuccess;
    private String message = "";
    private String resultType;

    public OperationResult(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public OperationResult(boolean isSuccess) {
        this(isSuccess, "");
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getResultType() {
        return resultType;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMessageEmpty() {
        if(message == null || message.isEmpty()) {
            return true;
        }
        return false;
    }
}
