package com.biorecorder.recorder;

import com.biorecorder.ads.*;
import com.biorecorder.dataformat.*;
import com.biorecorder.dataformat.DataRecordListener;
import com.biorecorder.dataformat.NullDataRecordListener;
import com.biorecorder.filters.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

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
    private volatile AdsDataHandler adsDataHandler;

    private volatile DataRecordListener dataRecordListener = new NullDataRecordListener();
    private volatile EventsListener eventsListener = new NullEventsListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();

    private volatile Map<Integer, List<NamedDigitalFilter>> filters = new HashMap();

    public BioRecorder(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads(comportName);
        } catch (ComportRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
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
        adsDataHandler = new AdsDataHandler(ads, recorderConfig);
        return adsDataHandler.startRecording();
    }

    public boolean stop() throws IllegalStateException {
        if (adsDataHandler != null) {
            return adsDataHandler.stop();
        }
        return true;
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
    public DataRecordConfig getDataConfig(RecorderConfig recorderConfig) {
        DataRecordConfig dataRecordConfig = new AdsDataHandler(ads, recorderConfig).getResultantDataConfig();
        return dataRecordConfig;
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
        if(adsDataHandler != null) {
            return adsDataHandler.getStartMeasuringTime();
        }
        return 0;
    }

    /**
     * Gets the calculated duration of data records
     *
     * @return calculated duration of data records
     */
    public double getCalculatedDurationOfDataRecord() {
        if(adsDataHandler != null) {
            return adsDataHandler.getCalculatedDurationOfDataRecord();
        }
        return 0;
    }

    /**
     * BioRecorder permits to add only ONE DataRecordListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addDataListener(DataRecordListener listener) {
        if (listener != null) {
            dataRecordListener = listener;
        }
    }

    public void removeDataListener() {
        dataRecordListener = new NullDataRecordListener();
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

    /**
     * This class:
     * <br>1) join short ads data records into more long ones (normally with duration = 1 sec)
     * <br>2) apply specified digital filters to data records
     * <br>3) delete lead off detection info and battery charge info
     * (if flag deleteBatteryVoltageChannel = true) from data records
     * <br> and send resultant filtered and clean data records to the dataRecordListener
     */
    class AdsDataHandler {
        private AdsDataRecordSender adsDataSender;
        private DataRecordSender resultantDataSender;
        private int numberOfRecordsToJoin = 1;


        public AdsDataHandler(Ads ads, RecorderConfig recorderConfig1) {
            // make copy to safely change
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
                } else { // we enable some ads channel to make possible accelerometer measuring
                    isAccelerometerOnly = true;
                    recorderConfig.setChannelEnabled(0, true);
                    recorderConfig.setChannelDivider(0, RecorderDivider.D10);
                    recorderConfig.setChannelLeadOffEnable(0, false);
                    recorderConfig.setSampleRate(RecorderSampleRate.S500);
                }
            }

            adsDataSender = new AdsDataRecordSender(ads, recorderConfig.getAdsConfig());
            adsDataSender.addButteryLevelListener(batteryListener);
            adsDataSender.addLeadOffListener(leadOffListener);

            resultantDataSender = adsDataSender;

            // join DataRecords to have data records length = resultantDataRecordDuration;
            numberOfRecordsToJoin = recorderConfig.getNumberOfAdsRecordsToJoin();

            if(numberOfRecordsToJoin > 1) {
                RecordsJoiner edfJoiner = new RecordsJoiner(resultantDataSender, numberOfRecordsToJoin);
                resultantDataSender = edfJoiner;
            } else {
                numberOfRecordsToJoin = 1;
            }

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
                    if(divider > 1) {
                        extraDividers.put(enableChannelsCount, divider);
                    }
                    enableChannelsCount++;
                }
            }

            if(recorderConfig.isAccelerometerEnabled()) {
                Integer divider = recorderConfig.getAccelerometerDivider().getExtraDivider();
                if(divider > 1) {
                    if(recorderConfig.isAccelerometerOneChannelMode()) {
                        extraDividers.put(enableChannelsCount++, divider);
                    } else {
                        extraDividers.put(enableChannelsCount++, divider);
                        extraDividers.put(enableChannelsCount++, divider);
                        extraDividers.put(enableChannelsCount++, divider);
                    }
                }
            }

            // reduce signals frequencies
            if(!extraDividers.isEmpty()) {
                RecordSignalsFrequencyReducer edfFrequencyDivider = new RecordSignalsFrequencyReducer(resultantDataSender);
                for (Integer signal : extraDividers.keySet()){
                    edfFrequencyDivider.addDivider(signal, extraDividers.get(signal));
                }
                resultantDataSender = edfFrequencyDivider;
            }

            // Add digital filters to ads channels
            if(!enableChannelsFilters.isEmpty()) {
                RecordSignalsDigitalFilter edfSignalsFilter = new RecordSignalsDigitalFilter(resultantDataSender);
                for (Integer signal : enableChannelsFilters.keySet()){
                    List<NamedDigitalFilter> channelFilters = enableChannelsFilters.get(signal);
                    for (NamedDigitalFilter filter : channelFilters) {
                        edfSignalsFilter.addSignalFilter(signal, filter, filter.getName());
                    }
                }
                resultantDataSender = edfSignalsFilter;
            }
            int batteryChannelNumber = -1;
            int leadOffChannelNumber = -1;
            if(recorderConfig.isBatteryVoltageMeasureEnabled()) {
                batteryChannelNumber = enableChannelsCount;
                enableChannelsCount++;
            }
            if(recorderConfig.isLeadOffEnabled()) {
                leadOffChannelNumber = enableChannelsCount;
                enableChannelsCount++;
            }

            // if some helper channels have to be deleted
            if(isAccelerometerOnly || recorderConfig.isLeadOffEnabled() || (recorderConfig.isBatteryVoltageMeasureEnabled() && recorderConfig.isBatteryVoltageChannelDeletingEnable())) {

                // Filter to remove helper channels
                RecordSignalsRemover edfSignalsRemover = new RecordSignalsRemover(resultantDataSender);
                if (isAccelerometerOnly) {
                    // delete helper enabled channel
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
                edfSignalsRemover.addDataListener(dataRecordListener);
                resultantDataSender = edfSignalsRemover;
            }
        }

        public DataRecordConfig getResultantDataConfig() {
            return resultantDataSender.dataConfig();
        }

        public Future<Boolean> startRecording() throws IllegalStateException, IllegalArgumentException {
            return adsDataSender.startRecording();
        }

        public boolean stop() throws IllegalStateException {
            return adsDataSender.stop();
        }

        public long getStartMeasuringTime() {
            return adsDataSender.getStartMeasuringTime();
        }

        public double getCalculatedDurationOfDataRecord() {
            return adsDataSender.calculateDurationOfDataRecord() * numberOfRecordsToJoin;
        }
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

}
