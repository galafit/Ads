package com.biorecorder;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.util.TimerTask;

/**
 * Created by galafit on 6/6/17.
 */
public class TestSerialPort {
    int CONNECTION_PERIOD_MS = 1000;
    java.util.Timer connectionTimer;
    int i= 0;

    public static void main(String[] args) {
        String portName = SerialPortList.getPortNames()[0];
        TestSerialPort comportTest = new TestSerialPort();
        comportTest.openPort(portName);
        comportTest.openPort(portName);
    }

    public TestSerialPort() {
        connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+ ": start get port names.");
                System.out.println(Thread.currentThread()+ ": "+ getPortNames()[0]);
                System.out.println(Thread.currentThread()+  ": finish get port names.");
                System.out.println("\n");
                i++;
                if(i>2) {
                    connectionTimer.cancel();
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    private synchronized SerialPort openPort(String name) {
        try {
            SerialPort comPort = new SerialPort(name);
            comPort.openPort();
            System.out.println(Thread.currentThread() + ": comport opened "+name);
            comPort.setParams(460800,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            return comPort;

        } catch (SerialPortException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private synchronized String[] getPortNames() {
        return SerialPortList.getPortNames();
    }
}

