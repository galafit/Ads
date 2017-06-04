package com.biorecorder.bdfrecorder.exceptions;

/**
 * If file to write data could not be created or
 * does not have permission to write
 */
public class BdfFileNotFoundRuntimeException extends BdfRecorderRuntimeException {
    public BdfFileNotFoundRuntimeException(String message) {
        super(message);
    }

    public BdfFileNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public BdfFileNotFoundRuntimeException(Throwable cause) {
        super(cause);
    }
}
