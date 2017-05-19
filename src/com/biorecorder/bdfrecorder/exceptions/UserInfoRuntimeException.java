package com.biorecorder.bdfrecorder.exceptions;

/**
 * This is an exception with user addressed information that is thrown
 * to be shown to end user
 */
public class UserInfoRuntimeException extends RuntimeException {
    public UserInfoRuntimeException(String message) {
        super(message);
    }

    public UserInfoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserInfoRuntimeException(Throwable cause) {
        super(cause);
    }
}
