package com.biorecorder.ads;


import com.sun.istack.internal.Nullable;
import jssc.SerialPortList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Ads packs samples from all channels received during the
 * time = MaxDiv/SampleRate (durationOfDataRecord)
 * in one array of int. Every array (data record or data package) has
 * the following structure (in case of 8 channels):
 * <p>
 * <br>{
 * <br>  n_0 samples from ads_channel_0 (if this ads channel enabled)
 * <br>  n_1 samples from ads_channel_1 (if this ads channel enabled)
 * <br>  ...
 * <br>  n_8 samples from ads_channel_8 (if this ads channel enabled)
 * <br>  n_acc_x samples from accelerometer_x channel
 * <br>  n_acc_y samples from accelerometer_y channel
 * <br>  n_acc_z samples from accelerometer_Z channel
 * <br>  1 sample with BatteryVoltage info (if BatteryVoltageMeasure  enabled)
 * <br>  1 (for 2 channels) or 2 (for 8 channels) samples with lead-off detection info (if lead-off detection enabled)
 * <br>}
 * <p>
 *  Where n_i = ads_channel_i_sampleRate * durationOfDataRecord
 *  <br>ads_channel_i_sampleRate = sampleRate / ads_channel_i_divider
 * <p>
 *  n_acc_x = n_acc_y = n_acc_z =  accelerometer_sampleRate * durationOfDataRecord
 *  <br>accelerometer_sampleRate = sampleRate / accelerometer_divider
 * <p>
 * If for Accelerometer  one channel mode is chosen then samples from
 * acc_x_channel, acc_y_channel and acc_z_channel will be summarized and data records will
 * have "one accelerometer channel" instead of three:
 * <p>
 * <br>{
 * <br>  n_0 samples from ads_channel_0 (if this ads channel enabled)
 * <br>  n_1 samples from ads_channel_1 (if this ads channel enabled)
 * <br>  ...
 * <br>  n_8 samples from ads_channel_8 (if this ads channel enabled)
 * <br>  n_acc samples from accelerometer channels
 * <br>  1 sample with BatteryVoltage info (if BatteryVoltageMeasure  enabled)
 * <br>  1 (for 2 channels) or 2 (for 8 channels) samples with lead-off detection info (if lead-off detection enabled)
 * <br>}
 * <p>
 * Where n_acc =  accelerometer_sampleRate * durationOfDataRecord
 *
 */
public class Ads1 {
    private static final Log log = LogFactory.getLog(Ads1.class);
    private final int COMPORT_SPEED = 460800;
    private final byte PING_COMMAND = (byte) (0xFB & 0xFF);
    private final byte HELLO_REQUEST = (byte) (0xFD & 0xFF);
    private final byte STOP_REQUEST = (byte) (0xFF & 0xFF);
    private final byte HARDWARE_REQUEST = (byte) (0xFA & 0xFF);

    private static final int PING_TIMER_PERIOD_MS = 1000;
    private static final int MONITORING_TIMER_PERIOD_MS = 500;

    private AdsDataListener dataListener;
    private AdsLowButteryListener lowButteryListener;
    private final Comport1 comport;
    private volatile Timer pingTimer;
    private volatile Timer monitoringTimer;
    private volatile long lastEventTime;
    private volatile int[] lastDataFrame;
    private volatile AdsType adsType;


    public Ads1(String comportName) throws SerialPortRuntimeException {
        comport = new Comport1(comportName, COMPORT_SPEED);
        setFrameDecoder(null);
        comport.writeByte(STOP_REQUEST);
        comport.writeByte(HARDWARE_REQUEST);
    }


    private void setFrameDecoder(@Nullable AdsConfig adsConfig) {
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                lastEventTime = System.currentTimeMillis();
                lastDataFrame = dataFrame;
                if(dataListener != null) {
                    dataListener.onDataReceived(dataFrame);
                }

            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage, String additionalInfo) {
                if (adsMessage == AdsMessage.ADS_2_CHANNELS) {
                    adsType = AdsType.ADS_2;
                    lastEventTime = System.currentTimeMillis();
                }
                if (adsMessage == AdsMessage.ADS_8_CHANNELS) {
                    adsType = AdsType.ADS_8;
                    lastEventTime = System.currentTimeMillis();
                }
                // if we receive broken data frame then we resend to the listener the last "normal" frame
                if (adsMessage == AdsMessage.FRAME_BROKEN && lowButteryListener != null) {
                    log.info(additionalInfo);
                    if(lastDataFrame != null && dataListener != null) {
                        dataListener.onDataReceived(lastDataFrame);
                    }
                }
                if (adsMessage == AdsMessage.LOW_BATTERY && lowButteryListener != null) {
                    lowButteryListener.handleLowButtery();
                }

            }
        });
        comport.setComPortListener(frameDecoder);
    }

    /**
     * Start "monitoring timer" which every second sends to
     * Ads some request (HARDWARE_REQUEST or HELLO request ) to check that
     * Ads is connected and ok. If Ads is "measuring" it first will be stopped.
     */
    public void startMonitoring() {
        try {
            comport.writeByte(STOP_REQUEST);
        } catch (SerialPortRuntimeException e) {
            // do nothing!
        }

        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
        }

        if(monitoringTimer == null) {
            monitoringTimer = new Timer();
            monitoringTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        comport.writeByte(HARDWARE_REQUEST);
                    } catch (SerialPortRuntimeException e) {
                        // do nothing!
                    }
                }
            }, MONITORING_TIMER_PERIOD_MS, MONITORING_TIMER_PERIOD_MS);
        }
    }


    /**
     * Start Ads measurements.
     *
     * @return true if command was successfully written, and false - otherwise
     *
     * @throws InvalidAdsTypeRuntimeException if device type specified in config object does
     * not coincide with the real device type.
     * @throws SerialPortRuntimeException if there is some problem with "writing the command" to the serial port
     */
    public boolean startRecording(AdsConfig adsConfig) throws SerialPortRuntimeException, InvalidAdsTypeRuntimeException {
        if(adsType != null && adsConfig.getAdsType() != adsType) {
            String errMsg = "Specified device type is invalid: "+adsConfig.getAdsType()+ ". Connected: "+ adsType;
            throw new InvalidAdsTypeRuntimeException(errMsg);
        }

        if(monitoringTimer != null) {
            monitoringTimer.cancel();
            monitoringTimer = null;
        }

        if (pingTimer == null) {
            // ping timer permits Ads to detect bluetooth connection problems
            // and restart connection when it is necessary
            pingTimer = new Timer();
            pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        comport.writeByte(PING_COMMAND);
                    } catch (SerialPortRuntimeException e) {
                        // do nothing;
                    }
                }
            }, PING_TIMER_PERIOD_MS, PING_TIMER_PERIOD_MS);
        }


        setFrameDecoder(adsConfig);

        try {
            return comport.writeBytes(adsConfig.getAdsConfigurationCommand());
        } catch (SerialPortRuntimeException e) {
            String errMsg = "Error during writing «start recording command» to serial port.";
            log.error(errMsg, e);
            throw e;
        }
    }

    /**
     * Stop ads measurements or stop monitoring timer
     *
     * @return true if command was successfully written, and false - otherwise
     * @throws SerialPortRuntimeException if there is some problem with "writing the command" to the serial port
     */
    public boolean stop() throws SerialPortRuntimeException {
        if(monitoringTimer != null) {
            monitoringTimer.cancel();
            monitoringTimer = null;
        }
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
        }

        try {
            return comport.writeByte(STOP_REQUEST);
        } catch (SerialPortRuntimeException e) {
            String errMsg = "Error during writing «stop command» to serial port.";
            log.error(errMsg, e);
            throw e;
        }
    }

    public void disconnect() throws SerialPortRuntimeException {
        stop();
        comport.close();
        comport.removeComPortListener();
    }

    public void setDataListener(AdsDataListener adsDataListener) {
        dataListener = adsDataListener;
    }

    public void setLowButteryListener(AdsLowButteryListener lowButteryListener) {
        this.lowButteryListener = lowButteryListener;
    }

    public boolean isActive() {
        // if last ads monitoring (device_type) message or data_frame was received less then 1 sec ago
        if((System.currentTimeMillis() - lastEventTime) <= 1000) {
            return true;
        }
        return false;
    }


    public static String[] getAvailableComportNames() {
        return Comport1.getAvailableComportNames();
    }


    /**
     * Helper method to convert integer with lead-off info (last integer of data frame) to the bit-mask.
     * <p>
     * "Lead-Off" detection serves to alert/notify when an electrode is making poor electrical
     * contact or disconnecting. Therefore in Lead-Off detection mask TRUE means DISCONNECTED and
     * FALSE means CONNECTED (or if the channel is disabled or its lead-off detection disabled or
     * its commutator state != "INPUT").
     * <p>
     * Every ads-channel has 2 electrodes (Positive and Negative) so in leadOff detection mask:
     * <br>
     * element-0 and element-1 correspond to Positive and Negative electrodes of ads channel 0,
     * element-2 and element-3 correspond to Positive and Negative electrodes of ads channel 1,
     * ...
     * element-14 and element-15 correspond to Positive and Negative electrodes of ads channel 8.
     * <p>
     * @param leadOffNum - integer with lead-off info
     * @param adsChannelsCount - number of ads channels (2 or 8)
     * @return leadOff detection mask or null if ads is stopped or
     * leadOff detection is disabled
     */
    public static boolean[] lofDetectionIntToBitMask(int leadOffNum, int adsChannelsCount) {
        int maskLength = 2* adsChannelsCount; // 2 electrodes for every channel
        if(adsChannelsCount == 2) {

            boolean[] bm = new boolean[maskLength];
            for (int k = 0; k < bm.length; k++) {
                bm[k] = false;
                if (((leadOffNum >> k) & 1) == 1) {
                    bm[k] = true;
                }
            }
            return bm;
        }

        if(adsChannelsCount == 8) {
        /*
         * ads_8channel send lead-off status in different manner:
         * first byte - states of all negative electrodes from 8 channels
         * second byte - states of all positive electrodes from 8 channels
         */
            boolean[] bm = new boolean[maskLength];
            for (int k = 0; k < bm.length; k++) {
                bm[k] = false;
                if(k < 8) { // first byte
                    if (((leadOffNum >> k) & 1) == 1) {
                        bm[2 * k + 1] = true;
                    }
                } else { // second byte
                    if (((leadOffNum >> k) & 1) == 1) {
                        bm[2 * (k - 8)] = true;
                    }
                }

            }
            return bm;
        }

        String msg = "Invalid Ads channels count: "+adsChannelsCount+ ". Number of Ads channels should be 2 or 8";
        throw new IllegalArgumentException(msg);
    }

}

