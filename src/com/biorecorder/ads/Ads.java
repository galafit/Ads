package com.biorecorder.ads;


import com.biorecorder.ads.exceptions.AdsConnectionRuntimeException;
import com.biorecorder.ads.exceptions.ComPortNotFoundRuntimeException;
import jssc.SerialPortException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
    private static final int HELLO_TIMER_DELAY_MS = 1000;
    int MAX_START_TIMEOUT_SEC = 60;
    int i;

    private List<AdsDataListener> adsDataListeners = new ArrayList<AdsDataListener>();
    private List<AdsEventsListener> adsEventsListeners = new ArrayList<AdsEventsListener>();
    private ComPort comPort;
    private boolean isRecording;
    private AdsConfig adsConfig = new AdsConfig();
    private List<Byte> pingCommand = new ArrayList<Byte>();
    private List<Byte> helloRequest = new ArrayList<Byte>();
    private List<Byte> hardwareRequest = new ArrayList<Byte>();
    private List<Byte> stopRequest = new ArrayList<Byte>();
    private Timer pingTimer;
    private Timer helloTimer;
    private AdsState adsState = new AdsState();

    // just for testing and debugging
    private PrintWriter out;


    public Ads() {
        pingCommand.add(PING_COMMAND);
        helloRequest.add(HELLO_REQUEST);
        hardwareRequest.add(HARDWARE_REQUEST);
        stopRequest.add(STOP_REQUEST);

        // just for testing and debugging
        File file = new File(System.getProperty("user.dir"), "frames.txt");
        try {
            out = new PrintWriter(file);
        } catch (FileNotFoundException e) {
           // e.printStackTrace();
        }
    }

    public AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }


    private void simpleConnect() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        if (!isComPortAvailable(adsConfig.getComPortName())) {
            String msg = MessageFormat.format("No serial port with the name: \"{0}\"", adsConfig.getComPortName());
            throw new ComPortNotFoundRuntimeException(msg);
        }
        try {
            comPort = new ComPort(adsConfig.getComPortName(), COMPORT_SPEED);
        } catch (SerialPortException e) {
            String msg = MessageFormat.format("Error while connecting to serial port: \"{0}\"", adsConfig.getComPortName());
            throw new AdsConnectionRuntimeException(msg, e);
        }
    }

    public void connect() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        if (comPort == null) {
            simpleConnect();
        }
        if (!comPort.isConnected()) {
            simpleConnect();
        }
        if (!comPort.getComPortName().equals(adsConfig.getComPortName())) {
            try {
                comPort.disconnect();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comPort.getComPortName());
                throw new AdsConnectionRuntimeException(msg, e);
            }
            simpleConnect();
        }
    }

    public void startRecording() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        connect();
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
            public void onMessageReceived(AdsMessage adsMessage) {
                if(adsMessage == AdsMessage.LOW_BATTERY) {
                    for (AdsEventsListener l : adsEventsListeners) {
                        l.handleAdsLowButtery();
                    }
                }
                if (adsMessage == AdsMessage.HELLO) {
                    adsState.setActive(true);
                }
                if (adsMessage == AdsMessage.STOP_RECORDING) {
                    adsState.setStoped(true);
                }
                if (adsMessage == AdsMessage.ADS_2_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_2);
                }
                if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_8);
                }
            }
        });
        comPort.setComPortListener(frameDecoder);
        comPort.writeToPort(adsConfig.getDeviceType().getAdsConfigurator().writeAdsConfiguration(adsConfig));
        isRecording = true;
        //---------------------------
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                comPort.writeToPort(pingCommand);
            }
        };
        pingTimer = new Timer();
        pingTimer.schedule(timerTask, PING_TIMER_DELAY_MS, PING_TIMER_DELAY_MS);
    }

    public void stopRecording() {
        //if (!isRecording) return;
        comPort.writeToPort(stopRequest);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        if(pingTimer != null) {
            pingTimer.cancel();
        }

        // just for debugging
        if (out != null) {
            out.close();
        }
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

    public void disconnect() {
        if (comPort != null) {
            try {
                comPort.disconnect();
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


    public void test() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        connect();

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig, out);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                for (AdsDataListener l : adsDataListeners) {
                    l.onDataReceived(dataFrame);
                }
                System.out.println("data recived ");
            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage) {
                if(adsMessage == AdsMessage.LOW_BATTERY) {
                    for (AdsEventsListener l : adsEventsListeners) {
                        l.handleAdsLowButtery();
                    }
                }
                if (adsMessage == AdsMessage.HELLO) {
                    adsState.setActive(true);
                    i++;
                    System.out.println("hello recived "+i);

                }
                if (adsMessage == AdsMessage.STOP_RECORDING) {
                    adsState.setStoped(true);
                }
                if (adsMessage == AdsMessage.ADS_2_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_2);
                }
                if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_8);
                }
            }
        });
        comPort.setComPortListener(frameDecoder);

        int delay = 100;
        int i = 0;
        while (i > 10) {
            i++;
            comPort.writeToPort(helloRequest);
            System.out.println("hello request "+i);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        comPort.writeToPort(helloRequest);
        System.out.println("hello request ");
       // comPort.writeToPort(adsConfig.getDeviceType().getAdsConfigurator().writeAdsConfiguration(adsConfig));
    }



    public Future testRecording() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        connect();

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig, out);
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
            public void onMessageReceived(AdsMessage adsMessage) {
                if(adsMessage == AdsMessage.LOW_BATTERY) {
                    for (AdsEventsListener l : adsEventsListeners) {
                        l.handleAdsLowButtery();
                    }
                }
                if (adsMessage == AdsMessage.HELLO) {
                    adsState.setActive(true);
                }
                if (adsMessage == AdsMessage.STOP_RECORDING) {
                    adsState.setStoped(true);
                }
                if (adsMessage == AdsMessage.ADS_2_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_2);
                }
                if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
                    adsState.setDeviceType(DeviceType.ADS_8);
                }
            }
        });
        comPort.setComPortListener(frameDecoder);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final Future future = executor.submit(new Runnable() {
            @Override
            public void run() {
                int delay = 500;
                while (adsState.getDeviceType() == null) {
                    comPort.writeToPort(hardwareRequest);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Method startRecording(): Interruption during ads hardware requesting.";
                        log.info(msg, e);
                    }
                }
                System.out.println("ads type checked: "+ adsState.getDeviceType());

                if (adsState.getDeviceType() != adsConfig.getDeviceType()) {
                    String msg = MessageFormat.format("Device type is invalid: {0}. Expected: ", adsConfig.getDeviceType(), adsState.getDeviceType());
                    throw new RuntimeException(msg);
                }

                while (!adsState.isStoped()) {
                    comPort.writeToPort(stopRequest);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        String msg = "Method startRecording(): Interruption during ads stopping.";
                        log.info(msg, e);
                    }
                }

                System.out.println("ads stopped: "+ adsState.isStoped());

                comPort.writeToPort(adsConfig.getDeviceType().getAdsConfigurator().writeAdsConfiguration(adsConfig));
                adsState.setStoped(false);
                System.out.println("ads config sended. ");
                //---------------------------
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        comPort.writeToPort(pingCommand);
                    }
                };
                pingTimer = new Timer();
                pingTimer.schedule(timerTask, PING_TIMER_DELAY_MS, PING_TIMER_DELAY_MS);
                System.out.println("ping timer started: ");
            }
        });

        // запускаем второй паралельный поток который прервет первый
        // через MAX_START_TIMEOUT_SEC
        executor.schedule(new Runnable(){
            public void run(){
                future.cancel(true);
            }
        }, MAX_START_TIMEOUT_SEC, TimeUnit.SECONDS);

        isRecording = true;
        return future;
    }

    private void startHelloTimer() {
        helloTimer = new Timer();
        HelloTimerTask helloTask = new HelloTimerTask(adsState, comPort, hardwareRequest);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addMessageListener(helloTask);
        comPort.setComPortListener(frameDecoder);

        helloTimer.schedule(helloTask, HELLO_TIMER_DELAY_MS, HELLO_TIMER_DELAY_MS);
    }


}
