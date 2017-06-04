package com.biorecorder.ads.exceptions;

/**
 * Created by galafit on 29/5/17.
 */
public class PortBusyRuntimeException extends PortRuntimeException {
    public PortBusyRuntimeException(String message) {
        super(message);
    }

    public PortBusyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortBusyRuntimeException(Throwable cause) {
        super(cause);
    }
}
