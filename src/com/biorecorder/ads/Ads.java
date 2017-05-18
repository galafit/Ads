package com.biorecorder.ads;


import com.biorecorder.ads.exceptions.AdsConnectionRuntimeException;
import com.biorecorder.ads.exceptions.ComPortNotFoundRuntimeException;
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
    private static final int COMPORT_SPEED = 460800;
    private List<AdsDataListener> adsDataListeners = new ArrayList<AdsDataListener>();
    private ComPort comPort;
    private boolean isRecording;
    private AdsConfig adsConfig = new AdsConfig();
    private List<Byte> pingCommand = new ArrayList<Byte>();
    private Timer pingTimer;

    public Ads() {
        pingCommand.add((byte) 0xFB);
    }

    public AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }


    public void connect() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException {
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

    private void comPortTest() throws SerialPortException {
        comPort = new ComPort(adsConfig.getComPortName(), COMPORT_SPEED);
        List<Byte> pingCommand1 = new ArrayList<Byte>();
        pingCommand1.add((byte) 0xFA);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataFrameListener(new DataFrameListener() {
            @Override
            public void onDataFrameReceived(int[] dataFrame) {
                notifyAdsDataListeners(dataFrame);
            }
        });
        comPort.setComPortListener(frameDecoder);
        comPort.writeToPort(pingCommand1);
        System.out.println("finished " + Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        comPort.disconnect();
    }

    public void startRecording() throws ComPortNotFoundRuntimeException, AdsConnectionRuntimeException  {
        if (comPort == null) {
            connect();
        }
        if (!comPort.isConnected()) {
            connect();
        }
        if (!comPort.getComPortName().equals(adsConfig.getComPortName())) {
            try {
                comPort.disconnect();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comPort.getComPortName());
                throw new AdsConnectionRuntimeException(msg, e);
            }
            connect();
        }
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataFrameListener(new DataFrameListener() {
            @Override
            public void onDataFrameReceived(int[] dataFrame) {
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
        pingTimer.schedule(timerTask, 1000, 1000);
    }

    public void stopRecording() {
        for (AdsDataListener adsDataListener : adsDataListeners) {
            adsDataListener.onStopRecording();
        }
        if (!isRecording) return;
        List<Byte> stopCmd = new ArrayList<Byte>();
        stopCmd.add((byte) 0xFF);
        comPort.writeToPort(stopCmd);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        pingTimer.cancel();
    }

    public void addAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.add(adsDataListener);
    }

    public void removeAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.remove(adsDataListener);
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
        for (AdsDataListener adsDataListener : adsDataListeners) {
            adsDataListener.onAdsDataReceived(dataRecord);
        }
    }

}
