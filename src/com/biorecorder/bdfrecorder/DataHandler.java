package com.biorecorder.bdfrecorder;

import com.biorecorder.bdfrecorder.dataformat.DataListener;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by galafit on 7/5/18.
 */
public class DataHandler {
    private final LinkedBlockingQueue<int[]> dataQueue = new LinkedBlockingQueue<>();
    private Thread dataHandlingThread;
    private volatile boolean isStopped;
    private DataListener dataListener;


    public DataHandler(RecorderConfig recorderConfig) {
        dataHandlingThread = new Thread("«Bdf data handling» thread") {
            @Override
            public void run() {
                while (!Thread.interrupted() && !isStopped) {
                    try {
                        // block until a request arrives
                        int[] dataRecord = dataQueue.take();
                        // send to listener
                        dataListener.onDataReceived(dataRecord);
                    } catch (InterruptedException ie) {
                        // stop
                        break;
                    }
                }
            }
        };
        dataHandlingThread.start();
    }

    public void onDataReceived(int[] dataRecord) throws IllegalStateException {
        if(isStopped) {
            String errMsg = "Data handler is stopped";
            throw new IllegalStateException(errMsg);
        }
        try {
            dataQueue.put(dataRecord);
        } catch (InterruptedException e) {
            // do nothing;
        }
    }

    public void stop() {
        if(!isStopped) {
            isStopped = true;
            dataHandlingThread.interrupt();
        }
    }



}
