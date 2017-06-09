package com.biorecorder.ads;


import com.biorecorder.ads.exceptions.PortBusyRuntimeException;
import com.biorecorder.ads.exceptions.PortNotFoundRuntimeException;
import com.biorecorder.ads.exceptions.PortRuntimeException;
import com.sun.istack.internal.Nullable;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.text.MessageFormat;
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

    private List<AdsDataListener> dataListeners = new ArrayList<AdsDataListener>(1);
    private List<AdsEventsListener> eventsListeners = new ArrayList<AdsEventsListener>(1);
    private volatile Comport comport;
    private volatile Timer pingTimer;
    private volatile Timer monitoringTimer = new Timer();
    private final AdsState adsState = new AdsState();



    public synchronized void connect(String comportName) throws PortNotFoundRuntimeException, PortBusyRuntimeException, PortRuntimeException {
        if (comport != null && comport.getComportName().equals(comportName)) {
            return;
        }
        if (comport != null && !comport.getComportName().equals(comportName)) {
            try {
                comport.close();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while closing serial port: \"{0}\"", comport.getComportName());
                log.error(msg, e);
            }
        }
        comport = new Comport(comportName, COMPORT_SPEED);
        FrameDecoder frameDecoder = new FrameDecoder(null);
        startMonitoringTimer(frameDecoder, null,  true);
        comport.setComPortListener(frameDecoder);
    }


    public synchronized boolean isConnected() {
        if (comport != null) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Send command to start ads measurements
     *
     * @return true if command was successfully written, and false - otherwise
     * @throws IllegalStateException if ads is not connected to comport
     */
    public synchronized boolean sendStartCommand(AdsConfig adsConfig) throws IllegalStateException {
        if (comport == null) {
            throw new IllegalStateException(CONNECTION_ERROR_MESSAGE);
        }

        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                for (AdsDataListener listener : dataListeners) {
                    listener.onDataReceived(dataFrame);
                }
            }
        });
        frameDecoder.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(AdsMessage adsMessage, String additionalInfo) {
                if (adsMessage == AdsMessage.LOW_BATTERY) {
                    for (AdsEventsListener listener : eventsListeners) {
                        listener.handleAdsLowButtery();
                    }
                }
                if (adsMessage == AdsMessage.FRAME_BROKEN) {
                    for (AdsEventsListener listener : eventsListeners) {
                        listener.handleAdsFrameBroken(additionalInfo);
                    }
                }

            }
        });
        startMonitoringTimer(frameDecoder, adsConfig,  false);
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
     * @throws IllegalStateException if ads is not connected to comport
     */
    public synchronized boolean sendStopRecordingCommand() throws IllegalStateException {
        if (comport == null) {
            throw new IllegalStateException(CONNECTION_ERROR_MESSAGE);
        }
        if (pingTimer != null) {
            pingTimer.cancel();
        }
        FrameDecoder frameDecoder = new FrameDecoder(null);
        startMonitoringTimer(frameDecoder, null, true);
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
     * @throws IllegalStateException if ads is not connected to comport
     */
    public synchronized DeviceType sendDeviceTypeRequest() throws IllegalStateException {
        if (comport == null) {
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


    /**
     * "Lead-Off" detection serves to alert/notify when an electrode is making poor electrical
     * contact or disconnecting. Therefore in Lead-Off detection mask TRUE means DISCONNECTED and
     * FALSE means CONNECTED.
     * <p>
     * Every ads-channel has 2 electrodes (Positive and Negative) so in leadOff detection mask:
     * <br>
     * element-0 and element-1 correspond to Positive and Negative electrodes of ads channel 0,
     * element-2 and element-3 correspond to Positive and Negative electrodes of ads channel 1,
     * ...
     * element-14 and element-15 correspond to Positive and Negative electrodes of ads channel 8.
     * <p>
     * @return leadOff detection mask or null if ads is stopped or
     * leadOff detection is disabled
     */
    public boolean[] getLeadOfDetectionMask() {
        return adsState.getLeadOffMask();
    }

    public boolean isActive() {
        return adsState.isActive();
    }

    public void addAdsDataListener(AdsDataListener adsDataListener) {
        dataListeners.add(adsDataListener);
    }

    public void addAdsEventsListener(AdsEventsListener adsEventsListener) {
        eventsListeners.add(adsEventsListener);
    }

    public synchronized void disconnect() throws PortRuntimeException {
        if (comport != null) {
            try {
                comport.close();
            } catch (SerialPortException e) {
                String msg = MessageFormat.format("Error while disconnecting from serial port: \"{0}\"", comport.getComportName());
                throw new PortRuntimeException(msg, e);
            } finally {
                comport = null;
            }
        }
    }

    /**
     * Serial port lib (jssc) en Mac and linux to create portNames list
     * actually OPENS and CLOSES every port (suppose to be sure it is exist). So
     * this operation is really DANGEROUS and can course serious bugs...
     * Like possibility to have multiple connections with the same  port
     * and so loose incoming data. See {@link com.biorecorder.TestSerialPort}.
     * That is why the method is synchronized.
     *
     * @return array of names of all comports or empty array.
     */
    public synchronized String[] getAvailableComportNames() {
        return SerialPortList.getPortNames();
    }


    private void startMonitoringTimer(FrameDecoder frameDecoder, @Nullable AdsConfig adsConfig, boolean isHelloRequestsActivated) {
        monitoringTimer.cancel();
        monitoringTimer = new Timer();
        MonitoringTask monitoringTask = new MonitoringTask(adsState, adsConfig);
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
