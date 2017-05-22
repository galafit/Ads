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

    private List<AdsListener> adsListeners = new ArrayList<AdsListener>();
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
    PrintWriter out;


    public Ads() {
        pingCommand.add(PING_COMMAND);
        helloRequest.add(HELLO_REQUEST);
        hardwareRequest.add(HARDWARE_REQUEST);
        stopRequest.add(STOP_REQUEST);

        File file = new File(System.getProperty("user.dir"), "frames.txt");
        try {
            out = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
            // startHelloTimer();
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


    private void startHelloTimer() {
        helloTimer = new Timer();
        HelloTimerTask helloTask = new HelloTimerTask(adsState, comPort, hardwareRequest);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addMessageListener(helloTask);
        comPort.setComPortListener(frameDecoder);

        helloTimer.schedule(helloTask, HELLO_TIMER_DELAY_MS, HELLO_TIMER_DELAY_MS);
    }

    public void startRecording_() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
       // helloTimer.cancel();
        connect();
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataFrameListener(new DataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                notifyAdsDataListeners(dataFrame);
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




    public boolean isComPortActive() {
        return adsState.isActive();
    }

    public Future startRecording() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
        if (helloTimer != null) {
            helloTimer.cancel();
        }
        connect();

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig, out);
        frameDecoder.addDataFrameListener(new DataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                notifyAdsDataListeners(dataFrame);
            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage) {
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

        isRecording = true;
        return future;
    }

    public void stopRecording() {
        out.close();
        for (AdsListener adsListener : adsListeners) {
            adsListener.onStopRecording();
        }
        if (!isRecording) return;
        comPort.writeToPort(stopRequest);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        if(pingTimer != null) {
            pingTimer.cancel();
        }
    }

    public void addAdsDataListener(AdsListener adsListener) {
        adsListeners.add(adsListener);
    }

    public void removeAdsDataListener(AdsListener adsListener) {
        adsListeners.remove(adsListener);
    }

    public void disconnect() {
        if (comPort != null) {
            try {
                comPort.disconnect();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comPort.getComPortName());
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

    private void notifyAdsDataListeners(int[] dataRecord) {
        for (AdsListener adsListener : adsListeners) {
            adsListener.onAdsDataReceived(dataRecord);
        }
    }

}
