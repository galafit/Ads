package com.biorecorder;

/**
 * Created by galafit on 20/4/18.
 */
public class OperationResult {
    boolean isSuccess;
    String message = "";

    public OperationResult(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }
}
