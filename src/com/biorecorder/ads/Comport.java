package com.biorecorder.ads;

import com.biorecorder.ads.exceptions.*;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;
import java.util.Set;

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
            log.info(Thread.currentThread() + " opened comport: "+comportName);
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
            if (ex.getExceptionType().equals(SerialPortException.TYPE_PORT_BUSY)) {
                throw new PortBusyRuntimeException(ex.getMessage(), ex);
            } else if (ex.getExceptionType().equals(SerialPortException.TYPE_PORT_NOT_FOUND)) {
                throw new PortNotFoundRuntimeException(ex.getMessage(), ex);
            } else {
                String msg = MessageFormat.format("Error while connecting to serial port: \"{0}\"", comportName);
                throw new PortRuntimeException(msg, ex);
            }
        }
    }

    public static void printThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (int i = 0; i < threadArray.length; i++) {
            System.out.println(i+" Thread: " + threadArray[i].getName());
        }
    }


    public String getComportName() {
        return comportName;
    }

    public void close() throws SerialPortException {
        serialPort.closePort();
    }

    public boolean writeBytes(byte[] bytes) throws SerialPortException {
        return serialPort.writeBytes(bytes);
     /*   try {
            return serialPort.writeBytes(bytes);
        } catch (SerialPortException e) {
            String errMsg = "Error during writing byte to serial port.";
            log.error(errMsg, e);
        }
        return false;*/
    }


    public boolean writeByte(byte b) throws SerialPortException {
        return serialPort.writeByte(b);
    }

    public void setComPortListener(ComPortListener comPortListener) {
        this.comPortListener = comPortListener;
    }

    public void removeComPortListener() {
        comPortListener = null;
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
                String errMsg = "Error during receiving serial port data: " + ex.getMessage();
                log.error(errMsg, ex);
                throw new PortRuntimeException(errMsg, ex);
            }
        }
    }
}
