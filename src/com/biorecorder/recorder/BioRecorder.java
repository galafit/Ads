package com.biorecorder.recorder;

import com.biorecorder.ads.*;
import com.biorecorder.digitalfilter.DigitalFilter;
import com.biorecorder.multisignal.recordformat.RecordConfig;
import com.biorecorder.multisignal.recordformat.RecordStream;
import com.biorecorder.multisignal.recordfilter.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Wrapper class that does some transformations with Ads data-frames
 * in separated thread (before to send them to the listener):
 * <ul>
 * <li>convert numbered data records to simple data records ("restoring"/supplementing the lost frames)</li>
 * <li>extract lead off info and battery charge info and send it to the appropriate listeners</li>
 * <li>remove  helper technical info about lead-off status and battery charge</li>
 * <li>permits to add to ads channels some filters. At the moment - filter removing "50Hz noise" (Moving average filter)</li>
 * </ul>
 * <p>
 * Thus resultant DataFrames (that BioRecorder sends to its listeners) have standard edf/bdf structure and could be
 * directly written to to bdf/edf file
 */
public class BioRecorder {
    private static final String ALL_CHANNELS_DISABLED_MSG = "All channels and accelerometer are disabled. Recording Impossible";
    public static final int START_CHECKING_PERIOD_MS = 500;

    private final Ads ads;
    private volatile Map<Integer, List<NamedDigitalFilter>> filters = new HashMap();

    private volatile RecordListener dataListener = new NullRecordListener();
    private volatile EventsListener eventsListener = new NullEventsListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();


    private final LinkedBlockingQueue<NumberedDataRecord> dataQueue = new LinkedBlockingQueue<>();
    private final ExecutorService singleThreadExecutor;
    private volatile Future executorFuture;
    private volatile long firstRecordTime;
    private volatile long lastRecordTime;
    private volatile double durationOfDataRecord;
    private volatile long recordsCount;

    private volatile int batteryCurrentPct = 100; // 100%


    public BioRecorder(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads(comportName);
        } catch (ComportRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "«Ads» data handling thread");
            }
        };
        singleThreadExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);

    }

    public void addChannelFilter(int channelNumber, DigitalFilter filter, String filterName) {
        List<NamedDigitalFilter> channelFilters = filters.get(channelNumber);
        if (channelFilters == null) {
            channelFilters = new ArrayList();
            filters.put(channelNumber, channelFilters);
        }
        channelFilters.add(new NamedDigitalFilter(filter, filterName));
    }

    public void removeChannelsFilters() {
        filters = new HashMap();
    }

    /**
     * Start BioRecorder measurements.
     *
     * @param recorderConfig1 object with ads config info
     * @return Future: if starting was successful future.get() return null,
     * otherwise throw RuntimeException. Usually starting fails due to device is not connected
     * or wrong device type is specified in config (that does not coincide
     * with the really connected device type)
     * @throws IllegalStateException    if BioRecorder was disconnected and
     *                                  its work was finalised or if it is already recording and should be stopped first
     * @throws IllegalArgumentException if all channels and accelerometer are disabled
     */
    public Future<Void> startRecording(RecorderConfig recorderConfig1) throws IllegalStateException, IllegalArgumentException {
        // make copy to safely change in the case of "accelerometer only" mode
        RecorderConfig recorderConfig = new RecorderConfig(recorderConfig1);

        boolean isAllChannelsDisabled = true;
        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
            if (recorderConfig.isChannelEnabled(i)) {
                isAllChannelsDisabled = false;
                break;
            }
        }
        boolean isAccelerometerOnly = false;
        if (isAllChannelsDisabled) {
            if (!recorderConfig.isAccelerometerEnabled()) {
                throw new IllegalArgumentException(ALL_CHANNELS_DISABLED_MSG);
            } else { // enable one ads channel to make possible accelerometer measuring
                isAccelerometerOnly = true;
                recorderConfig.setChannelEnabled(0, true);
                recorderConfig.setChannelDivider(0, RecorderDivider.D10);
                recorderConfig.setChannelLeadOffEnable(0, false);
                recorderConfig.setSampleRate(RecorderSampleRate.S500);
            }
        }

        FilterRecordStream dataFilter = createDataFilter(recorderConfig, isAccelerometerOnly);
        AdsConfig adsConfig = recorderConfig.getAdsConfig();
        dataFilter.setRecordConfig(ads.getDataConfig(adsConfig));

        dataQueue.clear();
        recordsCount = 0;
        durationOfDataRecord = recorderConfig.getDurationOfDataRecord();

        ads.addDataListener(new AdsDataHandler(adsConfig));
        Future startFuture = ads.startRecording(adsConfig);
        executorFuture = singleThreadExecutor.submit(new StartFutureHandlingTask(startFuture, new DataHandlingTask(dataFilter)));
        return startFuture;
    }

    class AdsDataHandler implements NumberedDataListener {
        private final AdsConfig adsConfig;

        public AdsDataHandler(AdsConfig adsConfig) {
            this.adsConfig = adsConfig;
        }

        @Override
        public void onDataReceived(int[] dataRecord, int recordNumber) {
            try {
                if (recordNumber == 0) {
                    firstRecordTime = System.currentTimeMillis();
                    lastRecordTime = firstRecordTime;
                } else {
                    lastRecordTime = System.currentTimeMillis();
                }
                recordsCount = recordNumber + 1;

                dataQueue.put(new NumberedDataRecord(dataRecord, recordNumber));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // notify lead off listener
            if (adsConfig.isLeadOffEnabled()) {
                notifyLeadOffListeners(Ads.extractLeadOffBitMask(dataRecord, adsConfig));
            }

            // notify battery voltage listener
            if (adsConfig.isBatteryVoltageMeasureEnabled()) {
                int batteryPct = Ads.extractLithiumBatteryPercentage(dataRecord, adsConfig);
                // Percentage level actually are estimated roughly.
                // So we round its value to tens: 100, 90, 80, 70, 60, 50, 40, 30, 20, 10.
                int percentageRound = ((int) Math.round(batteryPct / 10.0)) * 10;

                // this permits to avoid "forward-back" jumps when percentageRound are
                // changing from one ten to the next one (30-40 or 80-90 ...)
                if (percentageRound < batteryCurrentPct) {
                    batteryCurrentPct = percentageRound;
                }

                notifyBatteryLevelListener(batteryCurrentPct);
            }
        }
    }

    class StartFutureHandlingTask implements Callable<Void> {
        private volatile Future startFuture;
        private Callable dataHandlingTask;

        public StartFutureHandlingTask(Future startFuture, Callable dataHandlingTask) {
            this.startFuture = startFuture;
            this.dataHandlingTask = dataHandlingTask;
        }

        @Override
        public Void call() throws Exception {
            while(!startFuture.isDone()) {
                Thread.sleep(START_CHECKING_PERIOD_MS);
            }
            startFuture.get();
            executorFuture = singleThreadExecutor.submit(dataHandlingTask);
            return null;
        }
    }


    class DataHandlingTask implements Callable<Void> {
        RecordStream dataStream;
        private volatile int lastDataRecordNumber = -1;

        public DataHandlingTask(RecordStream dataStream) {
            this.dataStream = dataStream;
        }

        @Override
        public Void call() throws Exception {
            while (true) {
                // block until a request arrives
                NumberedDataRecord numberedDataRecord = dataQueue.take();
                // send to listener
                dataStream.writeRecord(numberedDataRecord.getRecord());
                int numberOfLostFrames = numberedDataRecord.getRecordNumber() - lastDataRecordNumber - 1;
                for (int i = 0; i < numberOfLostFrames; i++) {
                    dataStream.writeRecord(numberedDataRecord.getRecord());
                }
                lastDataRecordNumber = numberedDataRecord.getRecordNumber();
            }
        }
    }

    public RecordingInfo stop() throws IllegalStateException {
        if(executorFuture != null) {
            executorFuture.cancel(true);
        }
        if(recordsCount > 1) {
            durationOfDataRecord = (lastRecordTime - firstRecordTime) / ((recordsCount - 1) * 1000.0);
        }

        RecordingInfo recordingInfo = null;
        if(recordsCount > 0) {
            long startTime = firstRecordTime - (long) (durationOfDataRecord * 1000);
            recordingInfo = new RecordingInfo(startTime, durationOfDataRecord);
        }

        ads.stop();
        return recordingInfo;
    }

    public boolean disconnect() {
        singleThreadExecutor.shutdownNow();
        if (ads.disconnect()) {
            ads.removeDataListener();
            ads.removeMessageListener();
            removeButteryLevelListener();
            removeLeadOffListener();
            removeEventsListener();
            removeDataListener();
            return true;
        }
        return false;
    }

    public void startMonitoring() throws IllegalStateException {
        ads.startMonitoring();
    }

    public boolean isActive() {
        return ads.isActive();
    }

    public String getComportName() {
        return ads.getComportName();
    }

    public boolean isRecording() {
        return ads.isRecording();
    }

    /**
     * Get the info describing the structure of resultant data records
     * that BioRecorder sends to its listeners
     *
     * @return object describing data records structure
     */
    public RecordConfig getDataConfig(RecorderConfig recorderConfig) {
        RecordConfig adsDataConfig = ads.getDataConfig(recorderConfig.getAdsConfig());
        FilterRecordStream dataFilter =  createDataFilter(recorderConfig, false);
        dataFilter.setRecordConfig(adsDataConfig);
        RecordConfig config = dataFilter.getResultantConfig();
        return config;
    }

    public RecorderType getDeviceType() {
        AdsType adsType = ads.getAdsType();
        if (adsType == null) {
            return null;
        }
        return RecorderType.valueOf(adsType);
    }

    public static String[] getAvailableComportNames() {
        return Ads.getAvailableComportNames();
    }


    /**
     * BioRecorder permits to add only ONE RecordListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addDataListener(RecordListener listener) {
        if (listener != null) {
            dataListener = listener;
        }
    }

    public void removeDataListener() {
        dataListener = new NullRecordListener();
    }

    /**
     * BioRecorder permits to add only ONE LeadOffListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addLeadOffListener(LeadOffListener listener) {
        if (listener != null) {
            leadOffListener = listener;
        }
    }

    public void removeLeadOffListener() {
        leadOffListener = new NullLeadOffListener();
    }

    /**
     * BioRecorder permits to add only ONE ButteryVoltageListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addButteryLevelListener(BatteryLevelListener listener) {
        if (listener != null) {
            batteryListener = listener;
        }
    }

    public void removeButteryLevelListener() {
        batteryListener = new NullBatteryLevelListener();
    }

    /**
     * BioRecorder permits to add only ONE EventsListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addEventsListener(EventsListener listener) {
        if (listener != null) {
            eventsListener = listener;
        }
        ads.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(AdsMessageType messageType, String message) {
                if (messageType == AdsMessageType.LOW_BATTERY) {
                    notifyEventsListeners();
                }
            }

        });
    }

    public void removeEventsListener() {
        eventsListener = new NullEventsListener();
    }

    private void notifyEventsListeners() {
        eventsListener.handleLowBattery();
    }

    private void notifyDataListeners(int[] dataRecord) {
        dataListener.writeRecord(dataRecord);
    }

    private void notifyBatteryLevelListener(int batteryVoltage) {
        batteryListener.onBatteryLevelReceived(batteryVoltage);
    }

    private void notifyLeadOffListeners(Boolean[] leadOffMask) {
        leadOffListener.onLeadOffMaskReceived(leadOffMask);
    }


    private FilterRecordStream createDataFilter(RecorderConfig recorderConfig, boolean isZeroChannelShouldBeRemoved) {
        Map<Integer, List<NamedDigitalFilter>> enableChannelsFilters = new HashMap<>();
        int enableChannelsCount = 0;
        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
            if (recorderConfig.isChannelEnabled(i)) {
                List<NamedDigitalFilter> channelFilters = filters.get(i);
                if (channelFilters != null) {
                    enableChannelsFilters.put(enableChannelsCount, channelFilters);
                }
                enableChannelsCount++;
            }
        }

        if (recorderConfig.isAccelerometerEnabled()) {
            if (recorderConfig.isAccelerometerOneChannelMode()) {
                enableChannelsCount++;

            } else {
                enableChannelsCount = enableChannelsCount + 3;
            }
        }

        int batteryChannelNumber = -1;
        int leadOffChannelNumber = -1;
        if (recorderConfig.isBatteryVoltageMeasureEnabled()) {
            batteryChannelNumber = enableChannelsCount;
            enableChannelsCount++;
        }
        if (recorderConfig.isLeadOffEnabled()) {
            leadOffChannelNumber = enableChannelsCount;
            enableChannelsCount++;
        }

        RecordStream recordStream = new RecordStream() {
            @Override
            public void writeRecord(int[] dataRecord) {
                dataListener.writeRecord(dataRecord);
            }

            @Override
            public void setRecordConfig(RecordConfig recordConfig) {
                // do nothing
            }


            @Override
            public void close() {
                // do nothing
            }
        };

        FilterRecordStream dataFilter = new FilterRecordStream(recordStream);

        // delete helper channels
        if (isZeroChannelShouldBeRemoved || recorderConfig.isLeadOffEnabled() || (recorderConfig.isBatteryVoltageMeasureEnabled() && recorderConfig.isBatteryVoltageChannelDeletingEnable())) {

            SignalRemover edfSignalsRemover = new SignalRemover(dataFilter);
            if (isZeroChannelShouldBeRemoved) {
                // delete helper ads channel
                edfSignalsRemover.removeSignal(0);
            }
            if (recorderConfig.isLeadOffEnabled()) {
                // delete helper Lead-off channel
                edfSignalsRemover.removeSignal(leadOffChannelNumber);
            }
            if (recorderConfig.isBatteryVoltageMeasureEnabled() && recorderConfig.isBatteryVoltageChannelDeletingEnable()) {
                // delete helper BatteryVoltage channel
                edfSignalsRemover.removeSignal(batteryChannelNumber);
            }

            dataFilter = edfSignalsRemover;
        }

        // Add digital filters to ads channels
        if (!enableChannelsFilters.isEmpty()) {
            SignalFilter edfSignalsFilter = new SignalFilter(dataFilter);
            for (Integer signal : enableChannelsFilters.keySet()) {
                List<NamedDigitalFilter> channelFilters = enableChannelsFilters.get(signal);
                for (NamedDigitalFilter filter : channelFilters) {
                    edfSignalsFilter.addSignalFilter(signal, filter, filter.getName());
                }
            }
            dataFilter = edfSignalsFilter;
        }

        return dataFilter;
    }


    class NullLeadOffListener implements LeadOffListener {
        @Override
        public void onLeadOffMaskReceived(Boolean[] leadOffMask) {
            // do nothing
        }
    }

    class NullEventsListener implements EventsListener {
        @Override
        public void handleLowBattery() {
            // do nothing;
        }
    }

    class NullBatteryLevelListener implements BatteryLevelListener {
        @Override
        public void onBatteryLevelReceived(int batteryLevel) {
            // do nothing;
        }
    }


    class NamedDigitalFilter implements DigitalFilter {
        private DigitalFilter filter;
        private String filterName;

        public NamedDigitalFilter(DigitalFilter filter, String filterName) {
            this.filter = filter;
            this.filterName = filterName;
        }

        @Override
        public double filteredValue(double v) {
            return filter.filteredValue(v);
        }

        public String getName() {
            return filterName;
        }
    }

    class NumberedDataRecord {
        int[] record;
        int recordNumber;

        public NumberedDataRecord(int[] record, int recordNumber) {
            this.record = record;
            this.recordNumber = recordNumber;
        }

        public int[] getRecord() {
            return record;
        }

        public int getRecordNumber() {
            return recordNumber;
        }
    }

    class NullRecordListener implements  RecordListener{
        @Override
        public void writeRecord(int[] dataRecord) {
            // do nothing
        }
    }

}
