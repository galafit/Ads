package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.ComportRuntimeException;

/**
 * Created by galafit on 29/5/17.
 */
public class ConnectionRuntimeException extends RuntimeException {
    public static final String TYPE_PORT_ALREADY_OPENED = "Port already opened";
    public static final String TYPE_PORT_NOT_OPENED = "Port not opened";
    public static final String TYPE_CANT_SET_MASK = "Can't set mask";
    public static final String TYPE_LISTENER_ALREADY_ADDED = "Event listener already added";
    public static final String TYPE_LISTENER_THREAD_INTERRUPTED = "Event listener thread interrupted";
    public static final String TYPE_CANT_REMOVE_LISTENER = "Can't remove event listener, because listener not added";
    public static final String TYPE_PARAMETER_IS_NOT_CORRECT = "Parameter is not correct";
    public static final String TYPE_NULL_NOT_PERMITTED = "Null not permitted";
    public static final String TYPE_PORT_BUSY = "Port busy";
    public static final String TYPE_PORT_NOT_FOUND = "Port not found";
    public static final String TYPE_PERMISSION_DENIED = "Permission denied";
    public static final String TYPE_INCORRECT_SERIAL_PORT = "Incorrect serial port";

    private String portName;
    private String exceptionType;

    public ConnectionRuntimeException(String message) {
        super(message);
    }

    public ConnectionRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionRuntimeException(Throwable cause) {
        super(cause);
    }


    public ConnectionRuntimeException(ComportRuntimeException ex) {
        super(ex.getMessage());
        this.portName = ex.getPortName();
        this.exceptionType = ex.getExceptionType();
    }

    public ConnectionRuntimeException(String msg, ComportRuntimeException ex) {
        super(msg, ex);
        this.portName = ex.getPortName();
        this.exceptionType = ex.getExceptionType();
    }

    public String getPortName() {
        return this.portName;
    }

    public String getExceptionType() {
        return this.exceptionType;
    }
}
