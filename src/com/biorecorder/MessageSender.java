package com.biorecorder;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread safe class to send messages from one tread to another.
 * Normally "consuming"(reading) a message take some time.
 * That is why we need that class. It buffers incoming messages
 * and send then to the listener only when previous messages are read.
 */
public class MessageSender {
    private static final int DEFAULT_MESSAGE_QUEUE_CAPACITY = 10;
    private final LinkedBlockingQueue<String> messagesQueue;
    private final Thread messagesHandlingThread;
    private volatile boolean isStopped = false;
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
                while (!isStopped) {
                    try {
                        String msg = messagesQueue.take();
                        messageListener.onMessage(msg);
                    } catch (InterruptedException ie) {
                        isStopped = true;
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };
        messagesHandlingThread.start();
    }

    /**
     * MessageSender permits to add only ONE MessageListener!
     * So if a new listener added
     * the old one is automatically removed
     */
    public void addMessageListener(MessageListener listener) {
        if(listener != null) {
            messageListener = listener;
        }
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
            isStopped = true;
            Thread.currentThread().interrupt();
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
