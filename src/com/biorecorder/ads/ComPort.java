package com.biorecorder.ads;

import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Library jSSC is used.
 * jSSC (Java Simple Serial Connector) - library for working with serial ports from Java.
 * jSSC support Win32(Win98-Win8), Win64, Linux(x86, x86-64, ARM), Solaris(x86, x86-64),
 * Mac OS X 10.5 and higher(x86, x86-64, PPC, PPC64)
 * https://code.google.com/p/java-simple-serial-connector/
 * http://www.quizful.net/post/java-serial-ports
 */
class ComPort implements SerialPortEventListener {

    private static Log log = LogFactory.getLog(ComPort.class);
    SerialPort serialPort;
    private ComPortListener comPortListener;
    private String comPortName;

    ComPort(String comPortName, int speed) throws SerialPortException {
        this.comPortName = comPortName;
        serialPort = new SerialPort(comPortName);
        serialPort.openPort();//Open serial port
        serialPort.setParams(speed,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        // Строка serialPort.setEventsMask(SerialPort.MASK_RXCHAR) устанавливает маску ивентов для com порта,
        // фактически это список событий, на которые мы хотим реагировать.
        // В данном случае MASK_RXCHAR будет извещать слушателей о приходе данных во входной буфер порта.
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
        serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
        serialPort.addEventListener(this);
    }

    static String[] getAvailableComPortNames() {
        return SerialPortList.getPortNames();
    }


    static boolean isComPortAvailable(String comPortName) {
        if (comPortName != null) {
            comPortName = comPortName.trim();
            for (String name : getAvailableComPortNames()) {
                if (comPortName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    int getOutputBufferBytesCount() throws SerialPortException {
        return serialPort.getOutputBufferBytesCount();
    }

    String getComPortName() {
        return comPortName;
    }

    boolean isConnected() {
        return serialPort.isOpened();
    }


    /**
     * work only with new comports adapters
     *
     * @return true if ads device is connected and false if not
     * @throws SerialPortException
     */
    boolean isActive() throws SerialPortException {
        return serialPort.isCTS();
    }

    void disconnect() throws SerialPortException {
        serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
        serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
        serialPort.closePort();
        System.out.println("disconnect");
    }

    void writeBytes(byte[] bytes) {
        try {
            serialPort.writeBytes(bytes);
        } catch (SerialPortException ex) {
            log.error(ex);
        }
    }


    boolean writeByte(byte b) {
        try {
            return serialPort.writeByte(b);
        } catch (SerialPortException e) {
            log.error(e);
            System.out.println("Error while writing to port");
            e.printStackTrace();
        }
        return false;
    }

    void setComPortListener(ComPortListener comPortListener) {
        this.comPortListener = comPortListener;
    }


    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte[] buffer = serialPort.readBytes();
                if (buffer != null) {
                    for (int i = 0; i < buffer.length; i++) {
                        if (comPortListener != null) {
                            comPortListener.onByteReceived((buffer[i]));
                        }
                    }
                }
            } catch (SerialPortException ex) {
                log.error(ex);
            }
        }
    }
}
