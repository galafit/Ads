package com.biorecorder.bdfrecorder.exceptions;

/**
 * Created by galafit on 2/6/17.
 */
public class ComportBusyRuntimeException extends ConnectionRuntimeException {
    public ComportBusyRuntimeException(String message) {
        super(message);
    }

    public ComportBusyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComportBusyRuntimeException(Throwable cause) {
        super(cause);
    }
}
