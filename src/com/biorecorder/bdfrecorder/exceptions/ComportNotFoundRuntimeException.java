package com.biorecorder.bdfrecorder.exceptions;

/**
 * Created by galafit on 2/6/17.
 */
public class ComportNotFoundRuntimeException extends ConnectionRuntimeException {
    public ComportNotFoundRuntimeException(String message) {
        super(message);
    }

    public ComportNotFoundRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComportNotFoundRuntimeException(Throwable cause) {
        super(cause);
    }
}
