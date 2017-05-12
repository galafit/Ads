package com.biorecorder.ads;



import jssc.SerialPortException;
import jssc.SerialPortList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 *
 */
public class Ads {
    private static final Log log = LogFactory.getLog(Ads.class);
    private List<AdsDataListener> adsDataListeners = new ArrayList<AdsDataListener>();
    private ComPort comPort;
    private boolean isRecording;
    private AdsConfig adsConfig = new AdsConfig();
    private List<Byte> pingCommand = new ArrayList<Byte>();
    private Timer pingTimer;

    public Ads() {
        pingCommand.add((byte)0xFB);
    }

    public AdsConfig getAdsConfig() {
        return adsConfig;
    }

    public void setAdsConfig(AdsConfig adsConfig) {
        this.adsConfig = adsConfig;
    }

    public void connect(){
        String comportName = adsConfig.getComPortName();
        if(comportName != null && !comportName.isEmpty()) {
            try{
                comPort = new ComPort(adsConfig.getComPortName(), 460800);
            } catch (SerialPortException e) {
                String failConnectMessage = "No connection to port " + adsConfig.getComPortName();
                log.error(failConnectMessage, e);
                throw new AdsException(failConnectMessage, e);
            }
        }
      //  comPortTest();
    }

    private void comPortTest() {
        try {
            comPort = new ComPort(adsConfig.getComPortName(), 460800);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        List<Byte> pingCommand1 = new ArrayList<Byte>();
        pingCommand1.add((byte)0xFA);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataFrameListener(new DataFrameListener() {
            @Override
            public void onDataFrameReceived(int[] dataFrame) {
                notifyAdsDataListeners(dataFrame);
            }
        });
        comPort.setComPortListener(frameDecoder);
        comPort.writeToPort(pingCommand1);
        System.out.println("finished "+ Thread.currentThread().getName());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        comPort.disconnect();
    }

    public void startRecording() {
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataFrameListener(new DataFrameListener() {
            @Override
            public void onDataFrameReceived(int[] dataFrame) {
                notifyAdsDataListeners(dataFrame);
            }
        });
        if(comPort == null){
            connect();
        }
        if(!comPort.isConnected()){
            connect();
        }
        if(!comPort.getComPortName().equals(adsConfig.getComPortName()))  {
            comPort.disconnect();
            connect();
        }
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
        stopCmd.add((byte)0xFF);
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

    private void notifyAdsDataListeners(int[] dataRecord) {
        for (AdsDataListener adsDataListener : adsDataListeners) {
            adsDataListener.onAdsDataReceived(dataRecord);
        }
    }

    public void removeAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.remove(adsDataListener);
    }

    public void disconnect() {
        if(comPort!=null) {
            comPort.disconnect();
        }
    }

    public static  String[] getAvailableComPortNames() {
        return SerialPortList.getPortNames();
    }
}
