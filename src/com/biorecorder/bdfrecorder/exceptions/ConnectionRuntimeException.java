package com.biorecorder.bdfrecorder.exceptions;

/**
 * Created by galafit on 29/5/17.
 */
public class ConnectionRuntimeException extends BdfRecorderRuntimeException {
    public ConnectionRuntimeException(String message) {
        super(message);
    }

    public ConnectionRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionRuntimeException(Throwable cause) {
        super(cause);
    }
}
