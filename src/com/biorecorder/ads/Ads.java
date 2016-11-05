package com.biorecorder.ads;


import com.biorecorder.filter.MovingAveragePreFilter;
import com.biorecorder.comport.ComPort;
import jssc.SerialPortException;
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
    private AdsConfiguration adsConfiguration;

    private List<Byte> pingCommand = new ArrayList<Byte>();
    private Timer pingTimer;
    private List<MovingAveragePreFilter> movingAveragePreFilters = new ArrayList<MovingAveragePreFilter>();
    //private MovingAveragePreFilter movingAveragePreFilter = new MovingAveragePreFilter(10);

    public Ads() {
        super();
        pingCommand.add((byte)0xFB);
    }

    public void startRecording(AdsConfiguration adsConfiguration) {
        movingAveragePreFilters.clear();
        int sps = adsConfiguration.getSps().getValue();
        for (int i = 0; i < adsConfiguration.getDeviceType().getNumberOfAdsChannels(); i++) {
            int channelDivider = adsConfiguration.getAdsChannels().get(i).getDivider().getValue();
            movingAveragePreFilters.add(new MovingAveragePreFilter(sps/(channelDivider * 50)));
        }
        this.adsConfiguration = adsConfiguration;
        String failConnectMessage = "Connection failed. Check com port settings.\nReset power on the target amplifier. Restart the application.";
        try {
            FrameDecoder frameDecoder = new FrameDecoder(adsConfiguration) {
                @Override
                public void notifyListeners(int[] decodedFrame) {
                    notifyAdsDataListeners(decodedFrame);
                }
            };
            comPort = new ComPort(adsConfiguration.getComPortName(), 460800);
            comPort.setComPortListener(frameDecoder);
            comPort.writeToPort(adsConfiguration.getDeviceType().getAdsConfigurator().writeAdsConfiguration(adsConfiguration));
            isRecording = true;
        } catch (SerialPortException e) {
            log.error(failConnectMessage, e);
            throw new AdsException(failConnectMessage, e);
        }
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
        comPort.disconnect();
        pingTimer.cancel();
    }

    public void addAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.add(adsDataListener);
    }

    private void notifyAdsDataListeners(int[] dataRecord) {
        int[] filteredDataRecord = applyMovingAverageFilter(dataRecord);
        for (AdsDataListener adsDataListener : adsDataListeners) {
            //applyMovingAverageFilter(dataRecord);
            //adsDataListener.onAdsDataReceived(dataRecord);
            adsDataListener.onAdsDataReceived(filteredDataRecord);
        }
    }

    private int[] applyMovingAverageFilter(int[] dataRecord) {
        int[] filteredDataRecord = new int[dataRecord.length];
        for (int i = 0; i < filteredDataRecord.length; i++) {
            filteredDataRecord[i] = dataRecord[i];
        }
       List<AdsChannelConfiguration> channels = adsConfiguration.getAdsChannels();
        int numberOfAdsChannels = adsConfiguration.getDeviceType().getNumberOfAdsChannels();
        int maxDiv = adsConfiguration.getDeviceType().getMaxDiv().getValue();
        int dataRecordCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channelConfiguration = channels.get(i);
            if(channelConfiguration.isEnabled()) {
                int divider = channelConfiguration.getDivider().getValue();
                int numberOfSamples = maxDiv/divider;
                for (int j = 0; j < numberOfSamples; j++) {
                    if(channelConfiguration.is50HzFilterEnabled()){
                        filteredDataRecord[dataRecordCounter] = movingAveragePreFilters.get(i).getFilteredValue(dataRecord[dataRecordCounter]);
                   }
                    dataRecordCounter++;
                }
            }
        }
        return filteredDataRecord;
    }



    public void removeAdsDataListener(AdsDataListener adsDataListener) {
        adsDataListeners.remove(adsDataListener);
    }
}
