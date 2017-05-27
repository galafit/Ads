package com.biorecorder.ads.exceptions;

/**
 * Created by gala on 26/05/17.
 */
public class AdsTypeIvalidRuntimeException extends AdsException {
    public AdsTypeIvalidRuntimeException(String message) {
        super(message);
    }

    public AdsTypeIvalidRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdsTypeIvalidRuntimeException(Throwable cause) {
        super(cause);
    }
}
