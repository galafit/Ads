package com.biorecorder.ads;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.util.TimerTask;

/**
 * This class demonstrate a bag of jssc lib in unix system.
 * It show that it is possible to open comport  with the same name 2 times!!!
 * if in parallel in another thread we call method: SerialPortList.getPortNames()
 *
 * PS Sometimes we need to run this program a few times
 * to achieve open the port 2 times.
 *
 */
public class TestSerialPort {
    int CONNECTION_PERIOD_MS = 1000;
    java.util.Timer connectionTimer;

    public TestSerialPort() {
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

    public SerialPort openPort(String name) {
        try {
            SerialPort comPort = new SerialPort(name);
            comPort.openPort();
            int portBaudRate = 460800;
            comPort.setParams(portBaudRate,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            return comPort;

        } catch (SerialPortException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String[] getPortNames() {
        return SerialPortList.getPortNames();
    }

    public static void main(String[] args) {
        String[] portNames = getPortNames();
        if(portNames.length == 0) {
            System.out.println("There are no available comports");
        } else {
            String portName = portNames[0];
            TestSerialPort comportTest = new TestSerialPort();
            SerialPort port1 = comportTest.openPort(portName);
            System.out.println(Thread.currentThread() + ": Comport " + port1.getPortName()+" was successfully opened first time => "+port1.isOpened());
            SerialPort port2 = comportTest.openPort(portName);
            System.out.println(Thread.currentThread() + ": Comport " + port2.getPortName()+" was successfully opened second time => "+port2.isOpened());

            try {
                port1.writeByte((byte) 1);
                port1.writeByte((byte) 1);
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
            System.out.println("\n");
            System.out.println(Thread.currentThread() + ": byte was successfully written in both ports");
            System.out.println("\n");
        }
    }
}

