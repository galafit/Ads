package com.biorecorder;


public class OperationResult {
    private boolean isSuccess;
    private Message message;


    public OperationResult(boolean isSuccess, Message message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public OperationResult(boolean isSuccess) {
        this(isSuccess, null);
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Message getMessage() {
        return message;
    }

    public boolean isMessageEmpty() {
        if(message == null) {
            return true;
        }
        return false;
    }
}
