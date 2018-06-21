package com.biorecorder.comport;

import com.biorecorder.ads.ComportRuntimeException;
import jssc.SerialPortList;

/**
 * Created by galafit on 21/6/18.
 */
public class ComportFactory {
    /**
     * It is synchronized to avoid its simultaneous execution
     * with the method getAvailableComports()!!!
     * @param name comport name
     * @param speed comport baud rate
     * @return Comport implementation
     * @throws ComportRuntimeException if port can not be created
     */
    public synchronized static Comport getComport(String name, int speed) throws ComportRuntimeException {
       return new ComportJSCC(name, speed);
    }

    /**
     * Attention! This method can be DENGAROUS!!!
     * Serial port lib (jssc) en Mac and Linux to create portNames list
     * actually OPENS and CLOSES every port.
     * That is why this method is SYNCHRONIZED (on the Class object).
     * Without synchronization it becomes possible
     * to have multiple connections with the same port
     * and so loose incoming data. See {@link TestSerialPort} and
     * {@link TestSerialPortSynchronized}.
     *
     * @return array of names of all comports or empty array.
     */
    public synchronized static String[] getAvailableComportNames() {
        return SerialPortList.getPortNames();
    }
}
