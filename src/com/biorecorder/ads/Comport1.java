package com.biorecorder.ads;

import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Library jSSC is used.
 * jSSC (Java Simple Serial Connector) - library for working with serial ports from Java.
 * jSSC support Win32(Win98-Win8), Win64, Linux(x86, x86-64, ARM), Solaris(x86, x86-64),
 * Mac OS X 10.5 and higher(x86, x86-64, PPC, PPC64)
 * https://code.google.com/p/java-simple-serial-connector/
 * http://www.quizful.net/post/java-serial-ports
 */
public class Comport1 implements SerialPortEventListener {
    private static Log log = LogFactory.getLog(Comport1.class);
    private final SerialPort serialPort;
    private final String comportName;
    private ComPortListener comPortListener;

    public Comport1(String comportName, int speed) throws SerialPortRuntimeException {
        try {
            /*
             * This block is synchronized on the Class object
             *  to avoid its simultaneous execution with the static method
             *  getAvailableComportNames()!!!
             */
            synchronized (Comport1.class) {
                serialPort = new SerialPort(comportName);
                serialPort.openPort();//Open serial port
            }
        } catch (SerialPortException ex) {
            throw new SerialPortRuntimeException(ex);
        }
        log.info(Thread.currentThread() + " opened comport: "+comportName);

        try {
            serialPort.setParams(speed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            // Строка serialPort.setEventsMask(SerialPort.MASK_RXCHAR) устанавливает маску ивентов для com порта,
            // фактически это список событий, на которые мы хотим реагировать.
            // В данном случае MASK_RXCHAR будет извещать слушателей о приходе данных во входной буфер порта.
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
            throw new SerialPortRuntimeException(ex);
        }

        this.comportName = comportName;
        comPortListener = createNullComPortListener();
    }

    public String getComportName() {
        return comportName;
    }

    public boolean isOpenned() {
        return serialPort.isOpened();
    }


    public boolean close() {
        removeComPortListener();
        // if port already closed we do nothing
        if(!serialPort.isOpened()) {
            return true;
        }
        try {
             /*
             * This block is synchronized on the Class object
             *  to avoid its simultaneous execution with the static method
             *  getAvailableComportNames()!!!
             */
            synchronized (Comport1.class) {
                return serialPort.closePort();
            }
        } catch (SerialPortException ex) {
            return false;
        }
    }

    /**
     * Write array of bytes to the port.
     * @param bytes array of bytes to write to the port
     * @return true if writing was successfull and false otherwise
     * @throws IllegalStateException if the port was close
     */
    public boolean writeBytes(byte[] bytes) throws IllegalStateException {
        try {
            return serialPort.writeBytes(bytes);
        } catch (SerialPortException ex) {
            throw new IllegalStateException("Serial Port "+ getComportName() + " was finalised and closed", ex);
        }
    }

    /**
     * Write one byte to the port.
     * @param b byte to write to the port
     * @return true if writing was successfull and false otherwise
     * @throws IllegalStateException if the port was close
     */
    public boolean writeByte(byte b) throws IllegalStateException {
        try {
            return serialPort.writeByte(b);
        } catch (SerialPortException ex) {
            throw new IllegalStateException("Serial Port "+ getComportName() + " was finalised and closed", ex);
        }
    }

    public void setComPortListener(ComPortListener comPortListener) {
        if(comPortListener != null) {
            this.comPortListener = comPortListener;
        }
    }

    public void removeComPortListener() {
        comPortListener = createNullComPortListener();
    }

    private ComPortListener createNullComPortListener() {
        return new ComPortListener() {
            @Override
            public void onByteReceived(byte inByte) {
                // do nothing!
            }
        };
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte[] buffer = serialPort.readBytes();
                for (int i = 0; i < buffer.length; i++) {
                    comPortListener.onByteReceived((buffer[i]));
                }
            } catch (SerialPortException ex) {
                String errMsg = "Error during receiving serial port data: " + ex.getMessage();
                log.error(errMsg, ex);
                throw new SerialPortRuntimeException(errMsg, ex);
            }
        }
    }

    /**
     * Attention! This method can be DENGAROUS!!!
     * Serial port lib (jssc) en Mac and Linux to create portNames list
     * actually OPENS and CLOSES every port.
     * That is why this method is SYNCHRONIZED (on the Class object).
     * Without synchronization it becomes possible
     * to have multiple connections with the same port
     * and so loose incoming data. See {@link com.biorecorder.TestSerialPort}.
     *
     * @return array of names of all comports or empty array.
     */
    public synchronized static String[] getAvailableComportNames() {
        return SerialPortList.getPortNames();
    }

}
