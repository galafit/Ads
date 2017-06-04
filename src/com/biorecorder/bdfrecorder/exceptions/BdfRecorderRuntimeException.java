package com.biorecorder.bdfrecorder.exceptions;

/**
 * Created by galafit on 2/6/17.
 */
public class BdfRecorderRuntimeException extends RuntimeException {
    public BdfRecorderRuntimeException(String message) {
        super(message);
    }

    public BdfRecorderRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BdfRecorderRuntimeException(Throwable cause) {
        super(cause);
    }
}
