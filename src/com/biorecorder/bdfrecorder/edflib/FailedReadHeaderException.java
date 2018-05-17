package com.biorecorder.bdfrecorder.edflib;

/**
 * Created by galafit on 17/5/18.
 */
public class FailedReadHeaderException  extends Exception {
    public FailedReadHeaderException(String message) {
        super(message);
    }

    public FailedReadHeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
