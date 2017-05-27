package com.biorecorder.ads.exceptions;

/**
 * Created by gala on 26/05/17.
 */
public class AdsException extends RuntimeException{
    public AdsException(String message) {
        super(message);
    }

    public AdsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdsException(Throwable cause) {
        super(cause);
    }
}
