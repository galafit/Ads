package com.biorecorder.ads;

import java.util.ArrayList;
import java.util.Set;
import java.util.TimerTask;


/**
 * Created by galafit on 6/6/17.
 */
public class TestSerialPort1 {
    int CONNECTION_PERIOD_MS = 1000;
    java.util.Timer connectionTimer;
    int i= 0;

    public TestSerialPort1() {
       connectionTimer = new java.util.Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
               // System.out.println(Thread.currentThread()+ ": start get port names.");
                System.out.println(Thread.currentThread()+ ": creating port list "+ Comport.getAvailableComportNames()[0]);
                System.out.println("\n");

                i++;
                if(i>2) {
                    connectionTimer.cancel();
                    System.out.println("Timer canceled \n");
                    printThreads();
                }

            }
        }, CONNECTION_PERIOD_MS, CONNECTION_PERIOD_MS);
    }

    public Comport openPort(String name) {
        try {
            int speed = 460800;
            Comport comPort = new Comport(name, speed);
            System.out.println(Thread.currentThread() + ": comport "+name+" openned");
            return comPort;

        } catch (ComportRuntimeException e) {
            System.out.println("!!!Exceprion!!!!  ");
            // throw new RuntimeException("runtime exception ");
            return null;
        }
    }

    public static void printThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (int i = 0; i < threadArray.length; i++) {
            System.out.println(i+" Thread: " + threadArray[i].getName());
        }
    }

    public static void main(String[] args) {
        String portName = Comport.getAvailableComportNames()[0];
        TestSerialPort1 comportTest = new TestSerialPort1();
        ArrayList<Comport> ports = new ArrayList<>();
        ports.add(comportTest.openPort(portName));
        System.out.println("1 added.");
        ports.add(comportTest.openPort(portName));
        System.out.println("2 added.");
        ports.add(comportTest.openPort(portName));
        System.out.println("3 added.");
        for (int i = 0; i < ports.size(); i++) {
            if(ports.get(i) != null) {
                try {
                    ports.get(i).writeByte((byte)i);
                    System.out.println(i + " open port: "+ports.get(i).getComportName());
                    ports.get(i).close();
                } catch (ComportRuntimeException e) {
                    System.out.println("Exception");
                    e.printStackTrace();
                }
            }

        }
        System.out.println("End main ");

    }
}

