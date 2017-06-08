package com.biorecorder;

import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TimerTask;

/**
 * Created by galafit on 28/5/17.
 */
public class Test {
    public static void main(String[] args) {
        //portTest();
        frameTest();
    }


    public static void portTest() {
        //String[] names = SerialPortList.getPortNames();
        // String[]  names = {"one", "two"};
        String name = "/dev/tty.usbserial-A602DB2W";
        try {

            SerialPort comPort1 = new SerialPort(name);
            comPort1.openPort();
            System.out.println(Thread.currentThread() + ": open comport "+name);
            comPort1.setParams(460800,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);


            // SerialPort   comPort2 = new SerialPort(names[0]);
            // comPort2.openPort();

        } catch (SerialPortException e) {
            e.printStackTrace();
        }
    }

    public static void frameTest() {
        String name = SerialPortList.getPortNames()[0];
        int CONNECTION_PERIOD_MS = 1000;
        java.util.Timer connectionTimer;
        connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //  String comportName = appConfig.getComportName();
                // createAvailablePortNamesList();
                System.out.println( "start get port names.");
                System.out.println( SerialPortList.getPortNames()[0]);
                System.out.println( "finish get port names.");
                System.out.println("\n");

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);

        try {
            SerialPort comPort1 = new SerialPort(name);
            comPort1.openPort();
            System.out.println(Thread.currentThread() + ": open comport1 "+name);
            System.out.println("is port1 open: "+comPort1.isOpened());
            SerialPort comPort2 = new SerialPort(name);
            comPort2.openPort();
            System.out.println(Thread.currentThread() + ": open comport2 "+name);
            System.out.println("is port2 open: "+comPort2.isOpened());
            System.out.println("is port1 open: "+comPort1.isOpened());


        } catch (SerialPortException e) {
            e.printStackTrace();
        }

    }

    public static void printThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        int i = 0;
        for (Thread thread : threadArray) {
            i++;
            System.out.println(i+" thread: " + thread.getName());
        }
    }
}