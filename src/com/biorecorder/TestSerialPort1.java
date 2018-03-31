package com.biorecorder;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Created by galafit on 6/6/17.
 */
public class TestSerialPort1 {
    int CONNECTION_PERIOD_MS = 1000;
    java.util.Timer connectionTimer;
    int i= 0;

    public TestSerialPort1(String name) {
       connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
               // System.out.println(Thread.currentThread()+ ": start get port names.");
                System.out.println(Thread.currentThread()+ ": creating port list "+ getPortNames()[0]);
                System.out.println("\n");

                i++;
                if(i>2) {
                    connectionTimer.cancel();
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    public synchronized  SerialPort openPort(String name) {
        synchronized (TestSerialPort1.class) {
            try {
                SerialPort comPort = new SerialPort(name);
                // System.out.println(Thread.currentThread() + ": comport "+name+" created");
                comPort.openPort();
                System.out.println(Thread.currentThread() + ": comport "+name+" openned");
                comPort.setParams(460800,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                return comPort;

            } catch (SerialPortException e) {
                System.out.println("!!!Exceprion!!!!  ");
                // throw new RuntimeException("runtime exception ");
                return null;
            }
        }
    }


    private synchronized static String[] getPortNames() {
        return SerialPortList.getPortNames();
    }

    public static void main(String[] args) {
        String portName = "/dev/tty.usbserial-A906FEFV";//SerialPortList.getPortNames()[0];
        TestSerialPort1 comportTest = new TestSerialPort1(portName);
        ArrayList<SerialPort> ports = new ArrayList<>();
        ports.add(comportTest.openPort(portName));
        System.out.println("1 added.");
        ports.add(comportTest.openPort(portName));
        System.out.println("2 added.");
        ports.add(comportTest.openPort(portName));
        System.out.println("3 added.");
        for (int i = 0; i < ports.size(); i++) {
            if(ports.get(i) != null) {
                System.out.println(i+" port :"+ports.get(i).getPortName()+ " isOpened "+ports.get(i).isOpened());
                try {
                    ports.get(i).writeInt(i);
                } catch (SerialPortException e) {
                    System.out.println("Exception");
                    e.printStackTrace();
                }
            }

        }
        System.out.println("End main ");

    }
}

