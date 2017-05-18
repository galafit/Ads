package com.biorecorder.ads.exceptions;

/**
 * Created by gala on 12/05/17.
 */
public class AdsConnectionRuntimeException extends RuntimeException {
    public AdsConnectionRuntimeException(String message) {
        super(message);
    }

    public AdsConnectionRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdsConnectionRuntimeException(Throwable cause) {
        super(cause);
    }
}
