package com.biorecorder.ads;

import com.biorecorder.ads.exceptions.*;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;

/**
 * Library jSSC is used.
 * jSSC (Java Simple Serial Connector) - library for working with serial ports from Java.
 * jSSC support Win32(Win98-Win8), Win64, Linux(x86, x86-64, ARM), Solaris(x86, x86-64),
 * Mac OS X 10.5 and higher(x86, x86-64, PPC, PPC64)
 * https://code.google.com/p/java-simple-serial-connector/
 * http://www.quizful.net/post/java-serial-ports
 */
class Comport implements SerialPortEventListener {
    private static Log log = LogFactory.getLog(Comport.class);
    SerialPort serialPort;
    private ComPortListener comPortListener;
    private String comportName;

    public Comport(String comportName, int speed) throws PortNotFoundRuntimeException, PortBusyRuntimeException, PortRuntimeException {
        this.comportName = comportName;
        serialPort = new SerialPort(comportName);
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(speed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            // Строка serialPort.setEventsMask(SerialPort.MASK_RXCHAR) устанавливает маску ивентов для com порта,
            // фактически это список событий, на которые мы хотим реагировать.
            // В данном случае MASK_RXCHAR будет извещать слушателей о приходе данных во входной буфер порта.
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener(this);
        } catch(SerialPortException ex) {
            if(ex.getExceptionType().equals(SerialPortException.TYPE_PORT_BUSY)) {
                throw new PortBusyRuntimeException(ex.getMessage(), ex);
            }
            if(ex.getExceptionType().equals(SerialPortException.TYPE_PORT_NOT_FOUND)) {
                throw new PortNotFoundRuntimeException(ex.getMessage(), ex);
            }
            String msg = MessageFormat.format("Error while connecting to serial port: \"{0}\"", comportName);
            throw new PortRuntimeException(msg, ex);
        }
    }

    public static String[] getAvailableComportNames() {
        return SerialPortList.getPortNames();
    }


    public static boolean isComportAvailable(String comPortName) {
        if (comPortName != null && !comPortName.isEmpty()) {
            comPortName = comPortName.trim();
            for (String name : getAvailableComportNames()) {
                if (comPortName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getComportName() {
        return comportName;
    }

    public boolean isOpened() {
        return serialPort.isOpened();
    }


    public void close() throws SerialPortException {
        serialPort.closePort();
    }

    public boolean writeBytes(byte[] bytes) {
        try {
            serialPort.writeBytes(bytes);
        } catch (SerialPortException e) {
            log.error(e);
        }
        return false;
    }


    public boolean writeByte(byte b) {
        try {
            return serialPort.writeByte(b);
        } catch (SerialPortException e) {
            log.error(e);
        }
        return false;
    }

    public void setComPortListener(ComPortListener comPortListener) {
        this.comPortListener = comPortListener;
    }


    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte[] buffer = serialPort.readBytes();
                for (int i = 0; i < buffer.length; i++) {
                    if (comPortListener != null) {
                        comPortListener.onByteReceived((buffer[i]));
                    }
                }
            } catch (SerialPortException ex) {
                log.error(ex);
            }
        }
    }
}
