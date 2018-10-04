package com.biorecorder.recorder;

import com.biorecorder.ads.*;
import com.biorecorder.dataformat.*;
import com.biorecorder.filters.RecordListener;
import com.biorecorder.filters.NullRecordListener;
import com.biorecorder.filters.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Wrapper class that does some transformations with Ads data-frames:
 * <ul>
 * <li>joins Ads data frames so that resultant frame has standard duration = 1 sec</li>
 * <li>removes  helper technical info about lead-off status and battery charge</li>
 * <li>permits to add to ads channels data some filters. At the moment - filter removing "50Hz noise" (Moving average filter)</li>
 * </ul>
 * <p>
 * Thus resultant DataFrames (that BioRecorder sends to its listeners) have standard edf/bdf structure and could be
 * directly written to to bdf/edf file
 */
public class BioRecorder {
    private static final String ALL_CHANNELS_DISABLED_MSG = "All channels and accelerometer are disabled. Recording Impossible";

    private final Ads ads;
    private volatile Map<Integer, List<NamedDigitalFilter>> filters = new HashMap();

    private volatile RecordListener dataListener = new NullRecordListener();
    private volatile EventsListener eventsListener = new NullEventsListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();


    private final LinkedBlockingQueue<NumberedDataRecord> dataQueue = new LinkedBlockingQueue<>();
    private final ExecutorService singleThreadExecutor;
    private volatile Future executorFuture;
    private volatile int lastDataRecordNumber = -1;
    private volatile long firstRecordTime;
    private volatile long lastRecordTime;
    private volatile double calculatedDurationOfDataRecord; // sec
    private volatile int batterryCurrentPct = 100; // 100%


    public BioRecorder(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads(comportName);
        } catch (ComportRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "«Ads» thread");
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
     * @param recorderConfig object with ads config info
     * @return Future<Boolean> that get true if starting  was successful
     * and false otherwise. Usually starting fails due to device is not connected
     * or wrong device type is specified in config (which does not coincide
     * with the really connected device type)
     * @throws IllegalStateException    if BioRecorder was disconnected and
     *                                  its work was finalised or if it is already recording and should be stopped first
     * @throws IllegalArgumentException if all channels and accelerometer are disabled
     */
    public Future<Boolean> startRecording(RecorderConfig recorderConfig) throws IllegalStateException, IllegalArgumentException {
        // make copy to safely change in the case of accelerometer only mode
        RecorderConfig recorderConfig1 = new RecorderConfig(recorderConfig);

        boolean isAllChannelsDisabled = true;
        for (int i = 0; i < recorderConfig1.getChannelsCount(); i++) {
            if (recorderConfig1.isChannelEnabled(i)) {
                isAllChannelsDisabled = false;
                break;
            }
        }
        boolean isAccelerometerOnly = false;
        if (isAllChannelsDisabled) {
            if (!recorderConfig1.isAccelerometerEnabled()) {
                throw new IllegalArgumentException(ALL_CHANNELS_DISABLED_MSG);
            } else { // we enable some ads channel to make possible accelerometer measuring
                isAccelerometerOnly = true;
                recorderConfig1.setChannelEnabled(0, true);
                recorderConfig1.setChannelDivider(0, RecorderDivider.D10);
                recorderConfig1.setChannelLeadOffEnable(0, false);
                recorderConfig1.setSampleRate(RecorderSampleRate.S500);
            }
        }

        RecordFilter dataFilter = createDataFilter(recorderConfig1, isAccelerometerOnly);
        AdsConfig adsConfig = recorderConfig1.getAdsConfig();
        dataFilter.setRecordConfig(ads.getDataConfig(adsConfig));

        dataQueue.clear();
        ads.addDataListener(new NumberedDataListener() {
            @Override
            public void onDataReceived(int[] dataRecord, int recordNumber) {
                try {
                    if (recordNumber == 0) {
                        firstRecordTime = System.currentTimeMillis();
                        lastRecordTime = firstRecordTime;
                    } else {
                        lastRecordTime = System.currentTimeMillis();
                    }
                    if (recordNumber > 0) {
                        calculatedDurationOfDataRecord = (lastRecordTime - firstRecordTime) / (recordNumber * 1000.0);
                    }

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
                    if (percentageRound < batterryCurrentPct) {
                        batterryCurrentPct = percentageRound;
                    }

                    notifyBatteryLevelListener(batterryCurrentPct);
                }
            }
        });

        executorFuture = singleThreadExecutor.submit(new DataHandlingTask(dataFilter));
        return ads.startRecording(adsConfig);
    }


    class DataHandlingTask implements Runnable {
        RecordStream dataStream;

        public DataHandlingTask(RecordStream dataStream) {
            this.dataStream = dataStream;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    // block until a request arrives
                    NumberedDataRecord numberedDataRecord = dataQueue.take();
                    // send to listener
                    dataStream.writeRecord(numberedDataRecord.getRecord());
                    int numberOfLostFrames = numberedDataRecord.getRecordNumber() - lastDataRecordNumber - 1;
                    for (int i = 0; i < numberOfLostFrames; i++) {
                        dataStream.writeRecord(numberedDataRecord.getRecord());
                    }
                    lastDataRecordNumber = numberedDataRecord.getRecordNumber();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    // stop
                    break;
                }
            }
        }
    }


    public boolean stop() throws IllegalStateException {
        return ads.stop();
    }

    public boolean disconnect() {
        if (ads.disconnect()) {
            ads.removeDataListener();
            ads.removeMessageListener();
            removeButteryLevelListener();
            removeLeadOffListener();
            removeEventsListener();
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
        RecordFilter dataFilter =  createDataFilter(recorderConfig, false);
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
     * Gets the start measuring time (time of starting measuring the fist data record) =
     * time of the first received data record - duration of data record
     *
     * @return start measuring time
     */
    public long getStartMeasuringTime() {
        if (firstRecordTime == 0) {
            return 0;
        }
        return firstRecordTime - (long) (calculatedDurationOfDataRecord * 1000);
    }

    /**
     * Gets the calculated duration of data records = (lastDataRecordTime - firstDataRecordTime) / number of received data records
     *
     * @return calculated duration of data records
     */
    public double getCalculatedDurationOfDataRecord() {
        return calculatedDurationOfDataRecord;
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


    private RecordFilter createDataFilter(RecorderConfig recorderConfig, boolean isAccelerometerOnly) {
        Map<Integer, List<NamedDigitalFilter>> enableChannelsFilters = new HashMap<>();
        Map<Integer, Integer> extraDividers = new HashMap<>();
        int enableChannelsCount = 0;
        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
            if (recorderConfig.isChannelEnabled(i)) {
                List<NamedDigitalFilter> channelFilters = filters.get(i);
                if (channelFilters != null) {
                    enableChannelsFilters.put(enableChannelsCount, channelFilters);
                }
                Integer divider = recorderConfig.getChannelDivider(i).getExtraDivider();
                if (divider > 1) {
                    extraDividers.put(enableChannelsCount, divider);
                }
                enableChannelsCount++;
            }
        }

        if (recorderConfig.isAccelerometerEnabled()) {
            Integer divider = recorderConfig.getAccelerometerDivider().getExtraDivider();
            if (recorderConfig.isAccelerometerOneChannelMode()) {
                if (divider > 1) {
                    extraDividers.put(enableChannelsCount, divider);
                }
                enableChannelsCount++;
            } else {
                if (divider > 1) {
                    extraDividers.put(enableChannelsCount, divider);
                    extraDividers.put(enableChannelsCount + 1, divider);
                    extraDividers.put(enableChannelsCount + 2, divider);
                }
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

        RecordFilter dataFilter = new RecordFilter(recordStream);

        // delete helper channels
        if (isAccelerometerOnly || recorderConfig.isLeadOffEnabled() || (recorderConfig.isBatteryVoltageMeasureEnabled() && recorderConfig.isBatteryVoltageChannelDeletingEnable())) {

            SignalRemover edfSignalsRemover = new SignalRemover(dataFilter);
            if (isAccelerometerOnly) {
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
            SignalDigitalFilter edfSignalsFilter = new SignalDigitalFilter(dataFilter);
            for (Integer signal : enableChannelsFilters.keySet()) {
                List<NamedDigitalFilter> channelFilters = enableChannelsFilters.get(signal);
                for (NamedDigitalFilter filter : channelFilters) {
                    edfSignalsFilter.addSignalFilter(signal, filter, filter.getName());
                }
            }
            dataFilter = edfSignalsFilter;
        }

        // reduce signals frequencies
        if (!extraDividers.isEmpty()) {
            SignalFrequencyReducer edfFrequencyDivider = new SignalFrequencyReducer(dataFilter);
            for (Integer signal : extraDividers.keySet()) {
                edfFrequencyDivider.addDivider(signal, extraDividers.get(signal));
            }

            dataFilter = edfFrequencyDivider;
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

    /**
     * This class:
     * <br>1) convert numbered data records to simple data records (supplementing the lost ones) and
     * send them to the listener in separated thread
     * <br>2) extract lead off in
     * <br>3) apply specified digital filters to data records
     * <br>4) delete lead off detection info and battery charge info
     * (if flag deleteBatteryVoltageChannel = true) from data records
     * <br> and send resultant filtered and clean data records to the dataListener
     */

}
