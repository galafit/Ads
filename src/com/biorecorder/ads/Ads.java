package com.biorecorder.ads;



import com.biorecorder.ads.comport.ComPort;
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

    public void comPortConnect(){
        comPortTest();
        try{
            comPort = new ComPort(adsConfig.getComPortName(), 460800);
        } catch (SerialPortException e) {
            String failConnectMessage = "No connection to port " + adsConfig.getComPortName();
            log.error(failConnectMessage, e);
            throw new AdsException(failConnectMessage, e);
        }
    }

    private void comPortTest() {
        try {
            comPort = new ComPort(adsConfig.getComPortName(), 460800);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        List<Byte> pingCommand1 = new ArrayList<Byte>();
        pingCommand1.add((byte)0xFA);

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig) {
            @Override
            public void notifyListeners(int[] decodedFrame) {
                notifyAdsDataListeners(decodedFrame);
            }
        };

        comPort.setComPortListener(frameDecoder);
        comPort.writeToPort(pingCommand1);
        System.out.println("finished "+ Thread.currentThread().getName());
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            log.warn(e);
        }
        comPort.disconnect();
    }

    public void startRecording() {
            FrameDecoder frameDecoder = new FrameDecoder(adsConfig) {
                @Override
                public void notifyListeners(int[] decodedFrame) {
                    notifyAdsDataListeners(decodedFrame);
                }
            };
        if(comPort == null){
            comPortConnect();
        }
        if(!comPort.isConnected()){
            comPortConnect();
        }
        if(!comPort.getComPortName().equals(adsConfig.getComPortName()))  {
            comPort.disconnect();
            comPortConnect();
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

    public void comPortDisconnect() {
        if(comPort!=null) {
            comPort.disconnect();
        }
    }

    public static  String[] getAvailableComPortNames() {
        return SerialPortList.getPortNames();
    }

    /**
     * returns dividers list for all active channels including 3 accelerometer channels
     */
    private static List<Integer> getDividersForActiveChannels(AdsConfig adsConfiguration) {
        List<Integer> dividersList = new ArrayList<Integer>();
        for (int i = 0; i < adsConfiguration.getNumberOfAdsChannels(); i++) {
            AdsChannelConfig channelConfiguration = adsConfiguration.getAdsChannel(i);
            if (channelConfiguration.isEnabled()) {
                dividersList.add(channelConfiguration.getDivider().getValue());
            }
        }
        int n = adsConfiguration.isAccelerometerOneChannelMode() ? 1 : 3;
        for (int i = 0; i < n; i++) {
            if (adsConfiguration.isAccelerometerEnabled()) {
                dividersList.add(adsConfiguration.getAccelerometerDivider().getValue());
            }
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
            dividersList.add(10);
        }
        return dividersList;
    }

    public static int getDecodedFrameSize(AdsConfig adsConfiguration) {
        int frameSize = 0;
        for (Integer divider : getDividersForActiveChannels(adsConfiguration)) {
            frameSize += adsConfiguration.getMaxDiv() / divider;
        }
        return frameSize + 2; // 2 values for device specific information (counter of loff status);
    }

}
