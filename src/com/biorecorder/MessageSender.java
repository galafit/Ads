package com.biorecorder;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread safe class to send messages from one tread to another
 */
public class MessageSender {
    private static final int DEFAULT_MESSAGE_QUEUE_CAPACITY = 10;
    private final LinkedBlockingQueue<String> messagesQueue;
    private Thread messagesHandlingThread;
    private volatile boolean isStopped;
    private volatile MessageListener messageListener;

    public MessageSender() {
        this(DEFAULT_MESSAGE_QUEUE_CAPACITY);
    }

    public MessageSender(int messageQueueCapacity) {
        messagesQueue = new LinkedBlockingQueue<>(messageQueueCapacity);
        messageListener = new NullMessageListener();
        messagesHandlingThread = new Thread("«Messages handling» thread") {
            @Override
            public void run() {
                while ( ! Thread.interrupted() && ! isStopped) {
                    try {
                        // block until a request arrives
                        String message = messagesQueue.take();
                        // send to listener
                        messageListener.onMessage(message);
                    } catch (InterruptedException ie) {
                        // stop
                        break;
                    }
                }
            }
        };
        messagesHandlingThread.start();
    }


    public void setMessageListener(MessageListener listener) {
        messageListener = listener;
    }

    public void removeMessageListener() {
        messageListener = new NullMessageListener();
    }

    public void sendMessage(String message) throws IllegalStateException {
       if(isStopped) {
           String errMsg = "Message sender stopped";
           throw new IllegalStateException(errMsg);
       }
        try {
            messagesQueue.put(message);
        } catch (InterruptedException e) {
            // do nothing;
        }
    }

    public void stop() {
        if(!isStopped) {
            isStopped = true;
            messagesHandlingThread.interrupt();
        }
    }

    class NullMessageListener implements  MessageListener {
        @Override
        public void onMessage(String message) {
            // do nothing;
        }
    }
}
