package com.biorecorder.recorder;

import com.biorecorder.ads.*;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;
import com.biorecorder.dataformat.NullDataListener;
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

    private volatile DataListener dataListener = new com.biorecorder.dataformat.NullDataListener();
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
            boolean isStopOk = adsDataHandler.stop();
            return isStopOk;
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
    public DataConfig getDataConfig(RecorderConfig recorderConfig) {
        DataConfig dataConfig = new AdsDataHandler(ads, recorderConfig).getResultantDataConfig();
        ads.removeDataListener();
        return dataConfig;
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
    public double getDurationOfDataRecord() {
        if(adsDataHandler != null) {
            return adsDataHandler.getDurationOfDataRecord();
        }
        return 0;
    }


    /**
     * BioRecorder permits to add only ONE DataListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addDataListener(DataListener listener) {
        if (listener != null) {
            dataListener = listener;
        }
    }

    public void removeDataListener() {
        dataListener = new NullDataListener();
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
        ads.removeMessageListener();
    }

    private void notifyEventsListeners() {
        eventsListener.handleLowBattery();
    }

    class AdsDataHandler {
        private Ads ads;
        private AdsConfig adsConfig;
        private AdsDataSender adsDataSender;
        private DataSender resultantDataSender;
        private int numberOfRecordsToJoin = 1;


        public AdsDataHandler(Ads ads, RecorderConfig recorderConfig) {
            this.ads = ads;
            // make copy to safely change
            adsConfig = new AdsConfig(recorderConfig.getAdsConfig());
            boolean isAllChannelsDisabled = true;
            for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                if (adsConfig.isAdsChannelEnabled(i)) {
                    isAllChannelsDisabled = false;
                    break;
                }
            }
            boolean isAccelerometerOnly = false;
            if (isAllChannelsDisabled) {
                if (!adsConfig.isAccelerometerEnabled()) {
                    throw new IllegalArgumentException(ALL_CHANNELS_DISABLED_MSG);
                } else { // we enable some ads channel to make possible accelerometer measuring
                    isAccelerometerOnly = true;
                    adsConfig.setAdsChannelEnabled(0, true);
                    adsConfig.setAdsChannelDivider(0, Divider.D10);
                    adsConfig.setAdsChannelLeadOffEnable(0, false);
                    adsConfig.setSampleRate(Sps.S500);
                }
            }

            adsDataSender = new AdsDataSender(ads, adsConfig);
            adsDataSender.addButteryLevelListener(batteryListener);
            adsDataSender.addLeadOffListener(leadOffListener);

            // join DataRecords to have data records length = resultantDataRecordDuration;
            numberOfRecordsToJoin = (int) (recorderConfig.getDurationOfDataRecord() / adsConfig.getDurationOfDataRecord());
            DataRecordsJoiner edfJoiner = new DataRecordsJoiner(adsDataSender, numberOfRecordsToJoin);

            // Add digital filters to ads channels
            SignalsFilter signalsFilter = new SignalsFilter(edfJoiner);
            if (!isAccelerometerOnly) {
                int enableChannelsCount = 0;
                for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                    if (adsConfig.isAdsChannelEnabled(i)) {
                        List<NamedDigitalFilter> channelFilters = filters.get(i);
                        if (channelFilters != null) {
                            for (NamedDigitalFilter filter : channelFilters) {
                                signalsFilter.addSignalFilter(enableChannelsCount, filter, filter.getName());
                            }
                            enableChannelsCount++;
                        }

                    }
                }
            }

            DataConfig adsDataConfig = ads.getDataConfig(adsConfig);
            // Remove helper channels
            SignalsRemover signalsRemover = new SignalsRemover(signalsFilter);
            if (isAccelerometerOnly) {
                // delete helper enabled channel
                signalsRemover.removeSignal(0);
            }
            if (adsConfig.isLeadOffEnabled()) {
                // delete helper Lead-off channel
                signalsRemover.removeSignal(adsDataConfig.signalsCount() - 1);
            }
            if (adsConfig.isBatteryVoltageMeasureEnabled() && recorderConfig.isBatteryVoltageDeletingEnabled()) {
                // delete helper BatteryVoltage channel
                if (adsConfig.isLeadOffEnabled()) {
                    signalsRemover.removeSignal(adsDataConfig.signalsCount() - 2);
                } else {
                    signalsRemover.removeSignal(adsDataConfig.signalsCount() - 1);
                }
            }
            signalsRemover.addDataListener(dataListener);
            resultantDataSender = signalsRemover;
        }

        public DataConfig getResultantDataConfig() {
            return resultantDataSender.dataConfig();
        }

        public Future<Boolean> startRecording() throws IllegalStateException, IllegalArgumentException {
            return adsDataSender.startRecording(adsConfig);
        }

        public boolean stop() throws IllegalStateException {
            ads.removeDataListener();
            return adsDataSender.stop();
        }

        public long getStartMeasuringTime() {
            return adsDataSender.getStartMeasuringTime();
        }

        public double getDurationOfDataRecord() {
            return adsDataSender.getDurationOfDataRecord() * numberOfRecordsToJoin;
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
