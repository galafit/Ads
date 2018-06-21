package com.biorecorder.comport;


import com.biorecorder.ads.ComportRuntimeException;

import java.util.TimerTask;

/**
 * The same example as TestSerialPort but with synchronization.
 * This class demonstrate that synchronization (used in wrapper  class ComportJSCC)
 * fix the jssc library bag and do not permit open 2 comports with the same name
 */
public class TestSerialPortSynchronized {
    int CONNECTION_PERIOD_MS = 1000;
    java.util.Timer connectionTimer;

    public TestSerialPortSynchronized() {
        connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            int i= 0;
            @Override
            public void run() {
                System.out.println("\n");
                System.out.println(Thread.currentThread()+ ": Start creating a list of available comports...");
                System.out.println(Thread.currentThread()+ ": number of available ports = "+ getPortNames().length);
                System.out.println(Thread.currentThread()+  ": A list of available comports done");
                System.out.println("\n");
                i++;
                if(i>1) {
                    connectionTimer.cancel();
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    public Comport openPort(String name) {
        try {
            int portBaudRate = 460800;
            return ComportFactory.getComport(name, portBaudRate);
        } catch (ComportRuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String[] getPortNames() {
        return ComportFactory.getAvailableComportNames();
    }

    public static void main(String[] args) {
        String[] portNames = getPortNames();
        if(portNames.length == 0) {
            System.out.println("There are no available comports");
        } else {
            String portName = portNames[0];
            TestSerialPortSynchronized comportTest = new TestSerialPortSynchronized();
            Comport port1 = comportTest.openPort(portName);
            System.out.println(Thread.currentThread() + ": ComportJSCC " + port1.getComportName()+" was successfully opened first time => "+port1.isOpened());
            Comport port2 = comportTest.openPort(portName);
            System.out.println(Thread.currentThread() + ": ComportJSCC " + port2.getComportName()+" was successfully opened second time => "+port2.isOpened());
        }
    }
}
