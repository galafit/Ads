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
    ExecutorService startExecutor;
    Future  galaResult;
    Future  sashaResult;

    public ConcurrentTest() {
        startExecutor = Executors.newSingleThreadExecutor();
       // galaResult = startExecutor.submit(new Gala());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
       // sashaResult = startExecutor.submit(new Sasha());


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        galaResult.cancel(true);
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sashaResult.cancel(true);
        startExecutor.shutdown();
    }

    class Gala implements Runnable {
        int i;
        @Override
        public void run() {
            while (! Thread.currentThread().isInterrupted()) {
                System.out.println(i++ + " gala");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
           // executorResult = startExecutor.submit(new Sasha());

        }
    }

    class Sasha implements Callable<Boolean> {
        int i;

        @Override
        public Boolean call() throws Exception {
            while (! Thread.currentThread().isInterrupted()) {
                System.out.println(i++ + " sasha");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            return true;

        }
    }

    public static void main(String[] args) {
         ConcurrentTest test = new ConcurrentTest();
    }

}
