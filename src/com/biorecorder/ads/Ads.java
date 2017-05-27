package com.biorecorder.ads;


import com.biorecorder.ads.exceptions.AdsConnectionRuntimeException;
import com.biorecorder.ads.exceptions.AdsTypeIvalidRuntimeException;
import com.biorecorder.ads.exceptions.ComPortNotFoundRuntimeException;
import jssc.SerialPortException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Ads {
    private static final Log log = LogFactory.getLog(Ads.class);
    private static final int COMPORT_SPEED = 460800;
    private static final byte PING_COMMAND = (byte) (0xFB & 0xFF);
    private static final byte HELLO_REQUEST = (byte) (0xFD & 0xFF);
    private static final byte STOP_REQUEST = (byte) (0xFF & 0xFF);
    private static final byte HARDWARE_REQUEST = (byte) (0xFA & 0xFF);

    private static final int PING_TIMER_DELAY_MS = 1000;
    private static final int WATCHDOG_TIMER_DELAY_MS = 500;
    int MAX_START_TIMEOUT_SEC = 60;

    private List<AdsDataListener> adsDataListeners = new ArrayList<AdsDataListener>();
    private List<AdsEventsListener> adsEventsListeners = new ArrayList<AdsEventsListener>();
    private volatile ComPort comPort;
    private volatile AdsConfig adsConfig = new AdsConfig();
    private volatile Timer pingTimer;
    private volatile Timer monitoringTimer = new Timer();
    private final AdsState adsState = new AdsState();


    public synchronized AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public synchronized void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }


    private void comPortSimpleConnect(String comPortName) throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        if (!isComPortAvailable(comPortName)) {
            String msg = MessageFormat.format("No serial port with the name: \"{0}\"", comPortName);
            throw new ComPortNotFoundRuntimeException(msg);
        }
        try {
            comPort = new ComPort(comPortName, COMPORT_SPEED);
        } catch (SerialPortException e) {
            String msg = MessageFormat.format("Error while connecting to serial port: \"{0}\"", comPortName);
            System.out.println(msg);
            throw new AdsConnectionRuntimeException(msg, e);
        }
    }

    private void comPortConnect(String comPortName) throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        if (comPort == null) {
            comPortSimpleConnect(comPortName);
        }
        if (!comPort.isOpened()) {
            comPortSimpleConnect(comPortName);
        }
        if (!comPort.getComPortName().equals(comPortName)) {
            try {
                comPort.close();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comPort.getComPortName());
                System.out.println(msg);
                throw new AdsConnectionRuntimeException(msg, e);
            }
            comPortSimpleConnect(comPortName);
        }

        System.out.println("portName " + comPortName);
    }


    public synchronized void connect(String comPortName) throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        comPortConnect(comPortName);
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        startMonitoringTimer(frameDecoder, true);
        comPort.setComPortListener(frameDecoder);

    }

    public synchronized void startRecording(String comPortName) throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        comPortConnect(comPortName);
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                for (AdsDataListener l : adsDataListeners) {
                    l.onDataReceived(dataFrame);
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
        comPort.setComPortListener(frameDecoder);
        comPort.writeBytes(adsConfig.getDeviceType().getAdsConfigurationCommand(adsConfig));
        adsState.setStoped(false);
        //---------------------------
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                comPort.writeByte(PING_COMMAND);
            }
        };
        pingTimer = new Timer();
        pingTimer.schedule(timerTask, PING_TIMER_DELAY_MS, PING_TIMER_DELAY_MS);
    }


    public synchronized void stopRecording() {
        if (comPort != null) {
            comPort.writeByte(STOP_REQUEST);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn(e);
            }
            FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
            startMonitoringTimer(frameDecoder, true);
            comPort.setComPortListener(frameDecoder);
        }
        if (pingTimer != null) {
            pingTimer.cancel();
        }
    }

    public boolean isActive() {
        return adsState.isActive();
    }

    public boolean isSendingData() {
        return adsState.isDataComing();
    }

    /**
     * Sends request for hardware config. If receive ads_type return it.
     * Otherwise return null
     * @return ads type (2 or 8 channel) or null if ads not contests for some reasons
     */
    public synchronized  DeviceType  requestDeviceType() {
        if (comPort != null) {
            comPort.writeByte(HARDWARE_REQUEST);
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log.warn(e);
                }
                DeviceType deviceType = adsState.getDeviceType();
                if(deviceType != null) {
                    return deviceType;
                }
            }
        }
        return null;
    }

    public void addAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.add(adsDataListener);
    }

    public void removeAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.remove(adsDataListener);
    }

    public void addAdsEventsListener(AdsEventsListener eventListener) {
        adsEventsListeners.add(eventListener);
    }

    public void removeAdsEventsListener(AdsEventsListener eventListener) {
        adsEventsListeners.remove(eventListener);
    }

    public synchronized void disconnect() {
        if (comPort != null) {
            try {
                comPort.close();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comPort.getComPortName());
                System.out.println("comport disconnecting failed");
                throw new AdsConnectionRuntimeException(msg, e);
            }
        }
    }

    public static boolean isComPortAvailable(String comPortName) {
        return ComPort.isComPortAvailable(comPortName);
    }

    public static String[] getAvailableComPortNames() {
        return ComPort.getAvailableComPortNames();
    }


    private void startMonitoringTimer(FrameDecoder frameDecoder, boolean isHelloRequestsActivated) {
        monitoringTimer.cancel();
        monitoringTimer = new Timer();
        MonitoringTask watchDogTask = new MonitoringTask(adsState);
        frameDecoder.addMessageListener(watchDogTask);
        frameDecoder.addDataListener(watchDogTask);
        monitoringTimer.schedule(watchDogTask, WATCHDOG_TIMER_DELAY_MS, WATCHDOG_TIMER_DELAY_MS);

        if(isHelloRequestsActivated) {
            monitoringTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    comPort.writeByte(HARDWARE_REQUEST);
                }
            }, WATCHDOG_TIMER_DELAY_MS, WATCHDOG_TIMER_DELAY_MS);
        }
    }



 /*   public synchronized Future startRecording_full(String comPortName) throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        comPortConnect(comPortName);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                for (AdsDataListener l : adsDataListeners) {
                    l.onDataReceived(dataFrame);
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
        comPort.setComPortListener(frameDecoder);
        //---------------------------
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                comPort.writeByte(PING_COMMAND);
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
                    comPort.writeByte(HARDWARE_REQUEST);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Ads startRecording(): Interruption during ads hardware requesting.";
                        log.info(msg, e);
                    }
                }
                System.out.println("device type detected "+adsState.getDeviceType());
                if (adsState.getDeviceType() != adsConfig.getDeviceType()) {
                    String msg = MessageFormat.format("Device type is invalid: {0}. Expected: ", adsConfig.getDeviceType(), adsState.getDeviceType());
                    throw new AdsTypeIvalidRuntimeException(msg);
                }

                if (!adsState.isStoped()) {
                    comPort.writeByte(STOP_REQUEST);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        String msg = "Ads startRecording(): Interruption during ads stopping.";
                        log.info(msg, e);
                    }
                }

                comPort.writeBytes(adsConfig.getDeviceType().getAdsConfigurationCommand(adsConfig));

                System.out.println("device config written");

                adsState.setStoped(false);
                while (!adsState.isDataComing()) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Ads startRecording(): Interruption waiting data coming.";
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
                   stopRecording();
               }
            }
        }, MAX_START_TIMEOUT_SEC, TimeUnit.SECONDS);


        return future;
    }*/


}
