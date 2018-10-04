package com.biorecorder;

/**
 * Created by galafit on 28/7/18.
 */
public class IORuntimeException extends RuntimeException {
    public IORuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public IORuntimeException(Throwable cause) {
        super(cause);
    }
}
