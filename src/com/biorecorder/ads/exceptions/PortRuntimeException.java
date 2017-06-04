package com.biorecorder.ads.exceptions;

/**
 * Created by galafit on 29/5/17.
 */
public class PortRuntimeException extends RuntimeException {
    public PortRuntimeException(String message) {
        super(message);
    }

    public PortRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortRuntimeException(Throwable cause) {
        super(cause);
    }
}
