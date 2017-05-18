package com.biorecorder.ads.exceptions;

/**
 * Created by gala on 12/05/17.
 */
public class ComPortNotFoundRuntimeException extends AdsConnectionRuntimeException {
    public ComPortNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComPortNotFoundRuntimeException(String message) {
        super(message);
    }
}
