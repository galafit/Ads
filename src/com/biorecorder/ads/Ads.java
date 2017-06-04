package com.biorecorder.ads;


import com.biorecorder.ads.exceptions.PortBusyRuntimeException;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import com.biorecorder.ads.exceptions.PortRuntimeException;
import jssc.SerialPortException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.text.MessageFormat;
import java.util.*;

/**
 *
 */
public class Ads {
    private static final Log log = LogFactory.getLog(Ads.class);
    private final int COMPORT_SPEED = 460800;
    private final byte PING_COMMAND = (byte) (0xFB & 0xFF);
    private final byte HELLO_REQUEST = (byte) (0xFD & 0xFF);
    private final byte STOP_REQUEST = (byte) (0xFF & 0xFF);
    private final byte HARDWARE_REQUEST = (byte) (0xFA & 0xFF);

    private final String CONNECTION_ERROR_MESSAGE = "Ads must be connected to some serial port";

    private static final int PING_TIMER_DELAY_MS = 1000;
    private static final int WATCHDOG_TIMER_PERIOD_MS = 500;

    private AdsDataListener adsDataListener;
    private AdsEventsListener adsEventsListener;
    private volatile Comport comport;
    private volatile AdsConfig adsConfig = new AdsConfig();
    private volatile Timer pingTimer;
    private volatile Timer monitoringTimer = new Timer();
    private final AdsState adsState = new AdsState();


    public synchronized AdsConfig getConfig() {
        return adsConfig;
    }

    public synchronized void setConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
        log.info(adsConfig.toString());
    }


    public void connect(String comportName) throws PortNotFoundRuntimeException, PortBusyRuntimeException, PortRuntimeException {
        if (comport != null && comport.isOpened() && comport.getComportName().equals(comportName)) {
            return;
        }
        if (comport != null && comport.isOpened() && !comport.getComportName().equals(comportName)) {
            try {
                comport.close();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while closing serial port: \"{0}\"", comport.getComportName());
                log.error(msg, e);
            }
        }
        comport = new Comport(comportName, COMPORT_SPEED);
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        startMonitoringTimer(frameDecoder, true);
        comport.setComPortListener(frameDecoder);
    }

    public static void printThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
        for (Thread thread : threadArray) {
            System.out.println("thread: " + thread.getName());
        }
    }

    public synchronized boolean isConnected() {
        if (comport != null && comport.isOpened()) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Send command to start ads measurements
     *
     * @return true if command was successfully written, and false - otherwise
     * @throws IllegalStateException if ads was not connected first to some comport
     */
    public synchronized boolean sendStartCommand() throws IllegalStateException {
        if (!isConnected()) {
            throw new IllegalStateException(CONNECTION_ERROR_MESSAGE);
        }

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                adsDataListener.onDataReceived(dataFrame);
            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage, String additionalInfo) {
                if (adsMessage == AdsMessage.LOW_BATTERY) {
                    adsEventsListener.handleAdsLowButtery();
                }
                if (adsMessage == AdsMessage.FRAME_BROKEN) {
                    adsEventsListener.handleAdsFrameBroken(additionalInfo);
                }

            }
        });
        startMonitoringTimer(frameDecoder, false);
        comport.setComPortListener(frameDecoder);
        //---------------------------
        pingTimer = new Timer();
        pingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                comport.writeByte(PING_COMMAND);
            }
        }, PING_TIMER_DELAY_MS, PING_TIMER_DELAY_MS);

        return comport.writeBytes(adsConfig.getDeviceType().getAdsConfigurationCommand(adsConfig));
    }

    /**
     * Send command to stop ads measurements and work
     *
     * @return true if command was successfully written, and false - otherwise
     * @throws IllegalStateException if ads was not connected first to some comport
     */
    public synchronized boolean sendStopRecordingCommand() throws IllegalStateException {
        if (!isConnected()) {
            throw new IllegalStateException(CONNECTION_ERROR_MESSAGE);
        }
        if (pingTimer != null) {
            pingTimer.cancel();
        }
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        startMonitoringTimer(frameDecoder, true);
        comport.setComPortListener(frameDecoder);

        boolean is_writing_ok = comport.writeByte(STOP_REQUEST);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn(e);
        }

        return is_writing_ok;
    }

    /**
     * Sends request for hardware config. If receive ads_type return it.
     * Otherwise return null
     *
     * @return ads type (2 or 8 channel) or null if ads not contests for some reasons
     * @throws IllegalStateException if ads was not connected first to some comport
     */
    public synchronized DeviceType sendDeviceTypeRequest() throws IllegalStateException {
        if (!isConnected()) {
            throw new IllegalStateException(CONNECTION_ERROR_MESSAGE);
        }
        if(adsState.getDeviceType() != null) {
            return adsState.getDeviceType();
        }
        comport.writeByte(HARDWARE_REQUEST);
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn(e);
            }
            if (adsState.getDeviceType() != null) {
                return adsState.getDeviceType();
            }
        }
        return null;
    }


    public boolean isActive() {
        return adsState.isActive();
    }

    public void setAdsDataListener(AdsDataListener adsDataListener) {
        this.adsDataListener = adsDataListener;
    }

    public void setAdsEventsListener(AdsEventsListener adsEventsListener) {
        this.adsEventsListener = adsEventsListener;
    }

    public synchronized void disconnect() throws PortRuntimeException {
        if (comport != null) {
            try {
                comport.close();
                comport = null;
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comport.getComportName());
                throw new PortRuntimeException(msg, e);
            }
        }
    }

    public static boolean isComportAvailable(String comPortName) {
        return Comport.isComportAvailable(comPortName);
    }

    public static String[] getAvailableComportNames() {
        return Comport.getAvailableComportNames();
    }


    private void startMonitoringTimer(FrameDecoder frameDecoder, boolean isHelloRequestsActivated) {
        monitoringTimer.cancel();
        monitoringTimer = new Timer();
        MonitoringTask monitoringTask = new MonitoringTask(adsState);
        frameDecoder.addMessageListener(monitoringTask);
        frameDecoder.addDataListener(monitoringTask);
        monitoringTimer.schedule(monitoringTask, WATCHDOG_TIMER_PERIOD_MS, WATCHDOG_TIMER_PERIOD_MS);

        if (isHelloRequestsActivated) {
            monitoringTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    comport.writeByte(HARDWARE_REQUEST);
                }
            }, WATCHDOG_TIMER_PERIOD_MS, WATCHDOG_TIMER_PERIOD_MS);
        }
    }



 /*   public synchronized Future startRecording_full(String comPortName) throws PortNotFoundRuntimeException, AdsConnectionRuntimeException {
        int MAX_START_TIMEOUT_SEC = 60;
        comPortConnect(comPortName);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataRecordReceived(int[] dataFrame) {
                for (AdsDataListener l : adsDataListeners) {
                    l.onDataRecordReceived(dataFrame);
                }
            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage, String additionalInfo) {
                if (adsMessage == AdsMessage.LOW_BATTERY) {
                    for (AdsEventsListener l : adsEventsListeners) {
                        l.handleAdsLowButtery();
                    }
                }
                if (adsMessage == AdsMessage.FRAME_BROKEN) {
                    for (AdsEventsListener l : adsEventsListeners) {
                        l.handleAdsFrameBroken(additionalInfo);
                    }
                }
            }
        });
        startMonitoringTimer(frameDecoder, false);
        comport.setComPortListener(frameDecoder);
        //---------------------------
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                comport.writeByte(PING_COMMAND);
            }
        };
        pingTimer = new Timer();
        pingTimer.schedule(timerTask, PING_TIMER_DELAY_MS, PING_TIMER_DELAY_MS);

        //------ Start ads in separate thread
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        Future future = executor.submit(new Runnable() {
            @Override
            public void run() {
                int delay = 200;
                while (adsState.getDeviceType() == null) {
                    comport.writeByte(HARDWARE_REQUEST);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Ads sendStartCommand(): Interruption during ads hardware requesting.";
                        log.info(msg, e);
                    }
                }
                System.out.println("device type detected "+adsState.getDeviceType());
                if (adsState.getDeviceType() != adsConfig.getDeviceType()) {
                    String msg = MessageFormat.format("Device type is invalid: {0}. Expected: ", adsConfig.getDeviceType(), adsState.getDeviceType());
                    throw new AdsTypeIvalidRuntimeException(msg);
                }

                if (!adsState.isStoped()) {
                    comport.writeByte(STOP_REQUEST);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        String msg = "Ads sendStartCommand(): Interruption during ads stopping.";
                        log.info(msg, e);
                    }
                }

                comport.writeBytes(adsConfig.getDeviceType().getAdsConfigurationCommand(adsConfig));

                System.out.println("device config written");

                adsState.setStoped(false);
                while (!adsState.isDataComing()) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Ads sendStartCommand(): Interruption waiting data coming.";
                        log.info(msg, e);
                    }
                }
            }
        });

        // запускаем второй паралельный поток который прервет первый
        // через MAX_START_TIMEOUT_SEC если данные не пошли
        executor.schedule(new Runnable() {
            public void run() {
               if(!adsState.isDataComing()) {
                   future.cancel(true);
                   sendStopRecordingCommand();
               }
            }
        }, MAX_START_TIMEOUT_SEC, TimeUnit.SECONDS);


        return future;
    }*/


}
