package com.biorecorder.ads;


import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final int ACTIVE_PERIOD_MS = 1000;
    private static final int SLEEP_TIME_MS = 1000;

    private static final int MAX_STARTING_TIME_SEC = 30;

    private static final String DISCONNECTED_MSG = "Ads is disconnected and its work is finalised";
    private static final String ALREADY_RECORDING_MSG = "Device is already recording. Stop it first";

    private final Comport1 comport;
    private final Timer pingTimer = new Timer("Ping Timer");
    private final Timer monitoringTimer = new Timer("Monitoring Timer");
    private volatile TimerTask pingTask;
    private volatile TimerTask monitoringTask;
    
    private volatile long lastEventTime;
    private volatile boolean isDataReceived;
    private volatile boolean isConnected = true;

    private volatile AdsType adsType;

    // we use AtomicReference to do atomic "compare and set"
    private AtomicReference<AdsState1> adsStateAtomicReference =
            new AtomicReference<AdsState1>(AdsState1.UNDEFINED);

    private ExecutorService startExecutor;
    private volatile Future<Boolean> startFuture;

    private volatile AdsDataListener dataListener;
    private volatile AdsEventsListener eventsListener;


    public Ads1(String comportName) throws SerialPortRuntimeException {
        comport = new Comport1(comportName, COMPORT_SPEED);
        comport.setComPortListener(createFrameDecoder(null));
        dataListener = createNullDataListener();
        eventsListener = createNullEventsListener();
        comport.writeByte(STOP_REQUEST);
        comport.writeByte(HARDWARE_REQUEST);
        pingTask = new PingTask();
        monitoringTask = new MonitoringTask();

        ThreadFactory namedThreadFactory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "«Starting» thread");
            }
        };

        startExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);

    }

    private AdsDataListener createNullDataListener() {
        return new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                // Do nothing !!!
            }
        };
    }

    private AdsEventsListener createNullEventsListener() {
        return new AdsEventsListener() {
            @Override
            public void handleLowButtery() {
                // do nothing;
            }

            @Override
            public void handleFrameBroken(String eventInfo) {
                // do nothing;
            }
        };
    }

    private FrameDecoder createFrameDecoder(@Nullable AdsConfig adsConfig) {
        FrameDecoder frameDecoder = new FrameDecoder(adsConfig);
        frameDecoder.addDataListener(new AdsDataListener() {
            @Override
            public void onDataReceived(int[] dataFrame) {
                isDataReceived = true;
                lastEventTime = System.currentTimeMillis();
                dataListener.onDataReceived(dataFrame);
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
                if (adsMessage == AdsMessage.STOP_RECORDING) {
                    adsStateAtomicReference.compareAndSet(AdsState1.UNDEFINED, AdsState1.STOPPED);
                    lastEventTime = System.currentTimeMillis();
                }
                if (adsMessage == AdsMessage.FRAME_BROKEN) {
                    log.info(additionalInfo);
                }
                if (adsMessage == AdsMessage.LOW_BATTERY) {
                    eventsListener.handleLowButtery();
                }
            }
        });
       return frameDecoder;
    }



    /**
     * Start "monitoring timer" which every second sends to
     * Ads some request (HARDWARE_REQUEST or HELLO request ) to check that
     * Ads is connected and ok.
     *
     * @throws IllegalStateException if Ads was disconnected and its work is finalised
     */
    public void startMonitoring() throws IllegalStateException  {
        monitoringTask.cancel();

        monitoringTask = new MonitoringTask();
        monitoringTimer.schedule(monitoringTask, MONITORING_TIMER_PERIOD_MS, MONITORING_TIMER_PERIOD_MS);
    }

    public void stopMonitoring() {
        monitoringTask.cancel();
    }


    /**
     * Start Ads measurements.
     *
     * @param adsConfig object with ads config info
     * @return Future<Boolean> that get true if starting  was successful
     * and false otherwise. Throws IllegalArgumentException if device type specified in config
     * does not coincide with the really connected device type.
     * @throws IllegalStateException if Ads was disconnected and its work was finalised
     * or if it is already recording and should be stopped first
     */
    public Future<Boolean> startRecording(AdsConfig adsConfig) throws IllegalStateException {
        if(!isConnected) {
            throw new IllegalStateException(DISCONNECTED_MSG);
        }

        if(adsStateAtomicReference.get() == AdsState1.RECORDING) {
            throw new IllegalStateException(ALREADY_RECORDING_MSG);
        }

        AdsState1 stateBeforeStart = adsStateAtomicReference.get();
        adsStateAtomicReference.set(AdsState1.RECORDING);
        isDataReceived = false;
        startFuture = startExecutor.submit(new StartingTask(adsConfig, stateBeforeStart));
        return startFuture;
    }

    class StartingTask implements Callable<Boolean> {
        private AdsConfig config;
        private AdsState1 stateBeforeStart;

        public StartingTask(AdsConfig config, AdsState1 stateBeforeStart) {
            this.config = config;
            this.stateBeforeStart = stateBeforeStart;

        }

        @Override
        public Boolean call() throws Exception {
            long startTime = System.currentTimeMillis();
            // 1) to correctly tart we need to be sure that the specified in config adsType is ok
            while (adsType == null && (System.currentTimeMillis() - startTime) < MAX_STARTING_TIME_SEC * 1000) {
                comport.writeByte(HARDWARE_REQUEST);
                Thread.sleep(SLEEP_TIME_MS);
            }

            if(adsType == null) {
                adsStateAtomicReference.set(AdsState1.UNDEFINED);
                return false;
            } else if(adsType != config.getAdsType()) {
                String errMsg = "Device type specified in the config " +
                        "\ndoes not coincide with the really connected device type";
                adsStateAtomicReference.set(AdsState1.UNDEFINED);
                throw new IllegalArgumentException(errMsg);
            } else { // if adsType is ok
                // 2) try to stopRecording ads first if it was not stopped before
                if(stateBeforeStart == AdsState1.UNDEFINED) {
                    if(comport.writeByte(STOP_REQUEST)) {
                        // give the ads time to stopRecording
                        Thread.currentThread().sleep(SLEEP_TIME_MS);
                    }
                }

                // 3) send "start" command
                boolean startSentOk = comport.writeBytes(config.getAdsConfigurationCommand());
                if(startSentOk) {
                    // create frame decoder corresponding to the configuration
                    // and set is as listener to comport
                    comport.setComPortListener(createFrameDecoder(config));

                    // waiting for data
                    while (!isDataReceived && (System.currentTimeMillis() - startTime) < MAX_STARTING_TIME_SEC * 1000) {
                        Thread.sleep(SLEEP_TIME_MS);
                    }
                    if(isDataReceived) { // if data comes
                        // 4) start ping timer

                        // ping timer permits Ads to detect bluetooth connection problems
                        // and restart connection when it is necessary
                        pingTask.cancel();
                        pingTask = new PingTask();
                        pingTimer.schedule(pingTask, PING_TIMER_PERIOD_MS, PING_TIMER_PERIOD_MS);
                        return true;
                    }
                }
                adsStateAtomicReference.set(AdsState1.UNDEFINED);
                return false;
            }
        }
    }

    /**
     * Stop ads measurements
     *
     * @throws IllegalStateException if Ads was disconnected and its work is finalised
     */
    public boolean stopRecording() throws IllegalStateException {
        if(!isConnected) {
            throw new IllegalStateException(DISCONNECTED_MSG);
        }
        // cancel starting
        if(startFuture != null) {
            startFuture.cancel(true);
        }

        pingTask.cancel();
        adsStateAtomicReference.compareAndSet(AdsState1.RECORDING, AdsState1.UNDEFINED);
        if(comport.writeByte(STOP_REQUEST)) {
            try {
                Thread.sleep(SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }


    public boolean isConnected() {
        return isConnected;
    }

    public boolean disconnect() {
        // cancel starting
        if(startFuture != null) {
            startFuture.cancel(true);
        }
        if(!startExecutor.isShutdown()) {
            startExecutor.shutdownNow();
        }

        isConnected = false;
        monitoringTask.cancel();
        pingTask.cancel();
        monitoringTimer.cancel();
        pingTimer.cancel();

        if(comport.isOpened()) {
            comport.writeByte(STOP_REQUEST);
            return comport.close();
        } else {
            return true;
        }
    }

    public AdsType getAdsType() {
        return adsType;
    }

    public void setDataListener(AdsDataListener listener) {
        dataListener = listener;
    }

    public void setAdsEventsListener(AdsEventsListener listener) {
        this.eventsListener = listener;
    }

    public void removeDataListener() {
        dataListener = createNullDataListener();
    }

    public void removeEventsListener() {
        eventsListener = createNullEventsListener();
    }

    /**
     * This method return true ff last ads monitoring message (device_type)
     * or data_frame was received less then ACTIVE_PERIOD_MS (1 sec) ago
     */
    public boolean isActive() {
        if((System.currentTimeMillis() - lastEventTime) <= ACTIVE_PERIOD_MS) {
            return true;
        }
        return false;
    }

    public AdsState1 getAdsState() {
        return adsStateAtomicReference.get();
    }

    public String getComportName() {
        return comport.getComportName();
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
     * @param leadOffInt - integer with lead-off info
     * @param adsChannelsCount - number of ads channels (2 or 8)
     * @return leadOff detection mask or null if ads is stopped or
     * leadOff detection is disabled
     */
    public static boolean[] leadOffIntToBitMask(int leadOffInt, int adsChannelsCount) {
        int maskLength = 2 * adsChannelsCount; // 2 electrodes for every channel
        if(adsChannelsCount == 2) {

            boolean[] bm = new boolean[maskLength];
            for (int k = 0; k < bm.length; k++) {
                bm[k] = false;
                if (((leadOffInt >> k) & 1) == 1) {
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
                    if (((leadOffInt >> k) & 1) == 1) {
                        bm[2 * k + 1] = true;
                    }
                } else { // second byte
                    if (((leadOffInt >> k) & 1) == 1) {
                        bm[2 * (k - 8)] = true;
                    }
                }

            }
            return bm;
        }

        String msg = "Invalid Ads channels count: "+adsChannelsCount+ ". Number of Ads channels should be 2 or 8";
        throw new IllegalArgumentException(msg);
    }

    class PingTask extends TimerTask {
        @Override
        public void run() {
            comport.writeByte(PING_COMMAND);
        }
    }

    class MonitoringTask extends TimerTask {
        @Override
        public void run() {
            comport.writeByte(HARDWARE_REQUEST);
        }
    }

}

