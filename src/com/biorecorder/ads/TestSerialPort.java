package com.biorecorder.ads;

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
    volatile SerialPort comPort;

    public TestSerialPort() {
        connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+ ": startRecording get port names.");
                System.out.println(Thread.currentThread()+ ": "+ getPortNames()[0]);
                System.out.println(Thread.currentThread()+  ": finish get port names.");
                System.out.println("\n");
                i++;
                if(i>3) {
                    connectionTimer.cancel();
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    private SerialPort openPort(String name) {
        try {
            comPort = new SerialPort(name);
            System.out.println(comPort.getPortName()+": is port opened_before =" +comPort.isOpened());
            comPort.openPort();
            System.out.println(Thread.currentThread() + ": comport opened "+name);
            System.out.println(comPort.getPortName()+": is port opened_after =" +comPort.isOpened());
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

   public void isComportOpend() {
        System.out.println("is port opened " +comPort.isOpened());
   }

    private String[] getPortNames() {
        return SerialPortList.getPortNames();
    }

    public static void main(String[] args) {
        String portName = SerialPortList.getPortNames()[0];
        TestSerialPort comportTest = new TestSerialPort();
        comportTest.openPort(portName);
        comportTest.isComportOpend();
        comportTest.openPort(portName);
    }
}

