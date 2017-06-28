package com.biorecorder.bdfrecorder.exceptions;

/**
 * Created by galafit on 27/6/17.
 */
public class InvalidDeviceTypeRuntimeException extends BdfRecorderRuntimeException {
    public InvalidDeviceTypeRuntimeException(String message) {
        super(message);
    }

    public InvalidDeviceTypeRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDeviceTypeRuntimeException(Throwable cause) {
        super(cause);
    }
}
