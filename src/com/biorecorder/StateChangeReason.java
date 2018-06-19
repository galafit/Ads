package com.biorecorder;

/**
 * If we will want in the future in RecorderView (or RecorderViewModel)
 * instead of using messages from EdfBioRecorderApplication create
 * its own messages (potentially multilingual) the static fields describing the
 * reason of changing state will permit to do that
 */
public class StateChangeReason {
    public static final String REASON_LOW_BUTTERY = "The buttery is low";
    public static final String REASON_FAILED_WRITING_DATA = "Failed to write data to file";
    public static final String REASON_FAILED_STARTING = "Starting failed";
    public static final String REASON_FAILED_STARTING_WRONG_DEVICE_TYPE = "Wrong Recorder type";

    public static final String REASON_CANCEL_STARTING = "Starting canceled";
    public static final String REASON_STOP_INVOKED = "Stop invoked";
    public static final String REASON_START_RECORDING_INVOKED = "Start recording invoked";
    public static final String REASON_CHECK_CONTACTS_INVOKED = "Check contacts invoked";

    String reason;
    String message = "";

    public StateChangeReason(String reason) {
        this.reason = reason;
    }

    public StateChangeReason(String reason, String message) {
        this.message = message;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
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
