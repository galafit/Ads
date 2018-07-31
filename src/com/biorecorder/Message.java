package com.biorecorder;

/**
 * If in the future we will want in RecorderView (or RecorderViewModel)
 * create its own messages (potentially multilingual)
 * instead of using messages from EdfBioRecorderApplication
 * the field "type" will permit to do that
 */
public class Message {
    public static final String TYPE_LOW_BUTTERY = "The buttery charge is low";
    public static final String TYPE_CONNECTION_ERROR = "Connection error";
    public static final String TYPE_COMPORT_BUSY = "Comport busy";
    public static final String TYPE_COMPORT_NOT_FOUND = "Comport not found";
    public static final String TYPE_COMPORT_NULL = "Comport name can not be null or empty";
    public static final String TYPE_ALREADY_RECORDING = "Recorder already recording";

    public static final String TYPE_CHANNELS_AND_ACCELEROMETER_DISABLED = "All channels and accelerometer disabled.\nEnable some of them to record";
    public static final String TYPE_CHANNELS_DISABLED = "All channels disabled.\nEnable some of them to check contacts";

    public static final String TYPE_WRONG_DEVICE = "Specified recorder type does not coincide\nwith the connected one";
    public static final String TYPE_START_FAILED =  "Start failed!\nCheck whether the Recorder is on" +
            "\nand selected Comport is correct and try again.";

    public static final String TYPE_DIRECTORY_NOT_EXIST = "Directory not exist";
    public static final String TYPE_DIRECTORY_NAME_NULL = "Directory name null or empty";

    public static final String TYPE_FAILED_WRITE_DATA = "Failed to write data to file";
    public static final String TYPE_FILE_NOT_ACCESSIBLE = "File could not be created or accessed";

    public static final String TYPE_LAB_STREAMING_FAILED = "Lab Streaming failed to start";

    public static final String TYPE_DATA_SUCCESSFULLY_SAVED = "Data successfully saved to file";

    public static final String TYPE_FAILED_CREATE_DIR = "Failed create directory";
    public static final String TYPE_FAILED_CLOSE_FILE = "Failed correctly close and save file";

    public static final String TYPE_UNKNOWN_ERROR = "Error";

    String type;
    String additionalInfo = "";

    public Message(String type) {
        this.type = type;
    }

    public Message(String type, String additionalInfo) {
        this.additionalInfo = additionalInfo;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        if(additionalInfo.isEmpty()) {
            return type;
        }
        return type + ":\n"+additionalInfo;
    }
}
