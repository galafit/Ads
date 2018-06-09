package com.biorecorder;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by galafit on 4/6/18.
 */
public class ConcurrentTest {
    Timer timer = new Timer();

    public ConcurrentTest() {

        timer.schedule(new Gala(), 1000, 1000);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer.schedule(new Sasha(), 2000, 2000);


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer.cancel();
    }

    class Gala extends TimerTask {
        int i;
        @Override
        public void run() {
            System.out.println(i++ + " gala");
        }
    }

    class Sasha extends TimerTask {
        int i;
        @Override
        public void run() {
            System.out.println(i++ + " sasha");
        }
    }

    public static void main(String[] args) {
         ConcurrentTest test = new ConcurrentTest();
    }

}
