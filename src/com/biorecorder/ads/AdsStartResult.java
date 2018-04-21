package com.biorecorder.ads;

/**
 * Created by galafit on 11/4/18.
 */
public enum AdsStartResult {
    SUCCESS(true, "Success"),
    ALREADY_RECORDING(false, "Device is already recording. Stop it first"),
    FAILED_TO_SEND_COMMAND(false,"Failed to send command to the device"),
    WRONG_DEVICE_TYPE(false,"Specified device type does not match the type of connected device");

    private boolean isSuccess;
    private String message;

    AdsStartResult(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
