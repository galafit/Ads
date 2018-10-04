package com.biorecorder;

/**
 * Created by galafit on 4/10/18.
 */
public class FileNotFoundRuntimeException extends RuntimeException {
    public FileNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotFoundRuntimeException(Throwable cause) {
        super(cause);
    }
}
