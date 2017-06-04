package com.biorecorder.ads.exceptions;

/**
 * Created by gala on 12/05/17.
 */
public class PortNotFoundRuntimeException extends PortRuntimeException {
    public PortNotFoundRuntimeException(String message) {
        super(message);
    }

    public PortNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortNotFoundRuntimeException(Throwable cause) {
        super(cause);
    }
}
