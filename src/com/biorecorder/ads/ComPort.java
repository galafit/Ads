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
    SerialPort comPort;
    private ComPortListener comPortListener;
    private String comPortName;

    ComPort(String comPortName, int speed) throws SerialPortException {
        this.comPortName = comPortName;
        comPort = new SerialPort(comPortName);
        if (!comPort.isOpened()) {
            comPort.openPort();//Open serial port
            comPort.setParams(speed,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            // Строка serialPort.setEventsMask(SerialPort.MASK_RXCHAR) устанавливает маску ивентов для com порта,
            // фактически это список событий, на которые мы хотим реагировать.
            // В данном случае MASK_RXCHAR будет извещать слушателей о приходе данных во входной буфер порта.
            comPort.setEventsMask(SerialPort.MASK_RXCHAR);
            comPort.addEventListener(this);
            comPort.setRTS(true);
            comPort.setDTR(true);
            try {
                Thread.sleep(100);
                comPort.setDTR(false);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.error(e); //To change body of catch statement use File | Settings | File Templates.
                e.printStackTrace();
            }
        }
    }

    static String[] getAvailableComPortNames() {
        return SerialPortList.getPortNames();
    }


    static boolean isComPortAvailable(String comPortName) {
        if(comPortName != null) {
            comPortName = comPortName.trim();
            for(String name : getAvailableComPortNames()) {
                if(comPortName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }


    String getComPortName(){
        return comPortName;
    }

    boolean isConnected(){
        return comPort.isOpened();
    }



    /**
     * work only with new comports adapters
     * @return true if ads device is connected and false if not
     * @throws SerialPortException
     */
    boolean isActive() throws SerialPortException {
        return comPort.isCTS();
    }

    void disconnect() throws SerialPortException {
        if (comPort.isOpened()) {
            comPort.closePort();
        }
    }

    void writeToPort(List<Byte> bytes) {
        if (comPort.isOpened()) {
                final byte[] bytesArray = new byte[bytes.size()];
                for (int i = 0; i < bytes.size(); i++) {
                    bytesArray[i] = bytes.get(i);
                }
            Runnable rnbl = new Runnable() {
                @Override
                public void run() {
                    try {
                        comPort.writeBytes(bytesArray);
                    } catch (SerialPortException ex) {
                        log.error(ex);
                    }
                }
            };
            Thread thrd = new Thread(rnbl);
            thrd.start();
        } else {
            log.warn("Com port disconnected. Can't write to port.");
        }
    }

    void setComPortListener(ComPortListener comPortListener) {
        this.comPortListener = comPortListener;
    }


    @Override
    public void serialEvent(SerialPortEvent event)  {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                byte[] buffer = comPort.readBytes();
                if(buffer != null) {
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
