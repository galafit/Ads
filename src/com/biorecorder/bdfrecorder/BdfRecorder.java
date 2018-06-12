package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;
import com.biorecorder.dataformat.NullDataListener;
import com.biorecorder.filters.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Wrapper class that does some transformations with Ads data-frames:
 * <ul>
 *     <li>joins Ads data frames so that resultant frame has standard duration = 1 sec</li>
 *     <li>removes  helper technical info about lead-off status and battery charge</li>
 *     <li>permits to add to ads channels data some filters. At the moment - filter removing "50Hz noise" (Moving average filter)</li>
 * </ul>
 *
 * Thus resultant DataFrames (that BdfRecorder sends to its listeners) have standard edf/bdf structure and could be
 * directly written to to bdf/edf file
 *
 */
public class BdfRecorder {
    private static final Log log = LogFactory.getLog(BdfRecorder.class);
    private static final String ALL_CHANNELS_DISABLED_MSG = "All channels and accelerometer are disabled. Recording Impossible";

    private final Ads ads;
    private volatile DataListener dataListener = new com.biorecorder.dataformat.NullDataListener();
    private volatile EventsListener eventsListener = new NullEventsListener();
    private volatile BatteryLevelListener batteryListener = new NullBatteryLevelListener();
    private volatile LeadOffListener leadOffListener = new NullLeadOffListener();

    private Map<Integer, List<NamedDigitalFilter>> filters = new HashMap();

    public BdfRecorder(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads(comportName);
        } catch (ComportRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
    }

    public void addChannelFilter(int channelNumber, DigitalFilter filter, String filterName) {
        List<NamedDigitalFilter> channelFilters = filters.get(channelNumber);
        if(channelFilters == null) {
            channelFilters = new ArrayList();
            filters.put(channelNumber, channelFilters);
        }
        channelFilters.add(new NamedDigitalFilter(filter, filterName));
    }

    public void removeChannelsFilters() {
        filters = new HashMap();
    }


    /**
     * Start Recorder measurements.
     *
     * @param recorderConfig object with ads config info
     * @return Future<Boolean> that get true if starting  was successful
     * and false otherwise. Usually starting fails due to device is not connected
     * or wrong device type is specified in config (that does not coincide
     * with the really connected device type)
     * @throws IllegalStateException if Recorder was disconnected and
     * its work was finalised or if it is already recording and should be stopped first
     * @throws IllegalArgumentException if all channels and accelerometer are disabled
     */
    public Future<Boolean> startRecording(RecorderConfig recorderConfig) throws IllegalStateException, IllegalArgumentException {
        createAdsListener(ads, recorderConfig);
        return ads.startRecording(recorderConfig.getAdsConfig());
    }

    public boolean stop() throws IllegalStateException {
        ads.removeDataListener();
        return ads.stop();
    }

    public boolean disconnect() {
        if(ads.disconnect()) {
            removeDataListener();
            removeButteryLevelListener();
            removeLeadOffListener();
            removeEventsListener();
            return true;
        }
        return false;
    }

    public void startMonitoring() throws IllegalStateException  {
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
     * that BdfRecorder sends to its listeners
     *
     * @return object describing data records structure
     */
    public DataConfig getDataConfig(RecorderConfig recorderConfig) {
        DataConfig dataConfig = createAdsListener(ads, recorderConfig).dataConfig();
        ads.removeDataListener();
        return dataConfig;
    }

    public RecorderType getDeviceType() {
        AdsType adsType = ads.getAdsType();
        if(adsType == null) {
            return null;
        }
        return RecorderType.valueOf(adsType);
    }

    public static String[] getAvailableComportNames() {
        return Ads.getAvailableComportNames();
    }


    /**
     * Recorder permits to add only ONE DataListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addDataListener(DataListener listener) {
        if(listener != null) {
            dataListener = listener;
        }
    }

    public void removeDataListener() {
        dataListener = new NullDataListener();
    }

    /**
     * Recorder permits to add only ONE LeadOffListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addLeadOffListener(LeadOffListener listener) {
        if(listener != null) {
            leadOffListener = listener;
        }
    }

    public void removeLeadOffListener() {
        leadOffListener = new NullLeadOffListener();
    }

    /**
     * Recorder permits to add only ONE ButteryVoltageListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addButteryLevelListener(BatteryLevelListener listener) {
      if(listener != null)  {
          batteryListener = listener;
      }
    }

    public void removeButteryLevelListener() {
        batteryListener = new NullBatteryLevelListener();
    }

    /**
     * Recorder permits to add only ONE EventsListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addEventsListener(EventsListener listener) {
        if(listener != null) {
            eventsListener = listener;
        }
        ads.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(AdsMessageType messageType, String message) {
                if(messageType == AdsMessageType.LOW_BATTERY) {
                    notifyEventsListeners();
                }
                if(messageType == AdsMessageType.FRAME_BROKEN) {
                    log.info(message);
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


    private DataSender createAdsListener(Ads ads, RecorderConfig recorderConfig) throws IllegalArgumentException {
        AdsConfig adsConfig = recorderConfig.getAdsConfig();
        boolean isAllChannelsDisabled = true;
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            if(adsConfig.isAdsChannelEnabled(i)) {
                isAllChannelsDisabled = false;
                break;
            }
        }
        boolean isAccelerometerOnly = false;
        if(isAllChannelsDisabled) {
            if(!adsConfig.isAccelerometerEnabled()) {
                throw new IllegalArgumentException(ALL_CHANNELS_DISABLED_MSG);
            } else { // we enable some ads channel to make possible accelerometer measuring
                 isAccelerometerOnly = true;
                 adsConfig.setAdsChannelEnabled(0, true);
                 adsConfig.setAdsChannelDivider(0, Divider.D10);
                 adsConfig.setSampleRate(Sps.S500);
            }
        }

        DataConfig adsDataConfig = ads.getDataConfig(adsConfig);
        AdsDataSender adsDataSender = new AdsDataSender(ads, recorderConfig.getAdsConfig());
        adsDataSender.addButteryLevelListener(batteryListener);
        adsDataSender.addLeadOffListener(leadOffListener);

        // join DataRecords to have data records length = resultantDataRecordDuration;
        int numberOfFramesToJoin = (int) (recorderConfig.getDurationOfDataRecord() / adsConfig.getDurationOfDataRecord());
        DataRecordsJoiner edfJoiner = new DataRecordsJoiner(adsDataSender, numberOfFramesToJoin);

        // Add digital filters to ads channels
        SignalsFilter signalsFilter = new SignalsFilter(edfJoiner);
        if(!isAccelerometerOnly) {
            int enableChannelsCount = 0;
            for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                if(adsConfig.isAdsChannelEnabled(i)) {
                    List<NamedDigitalFilter> channelFilters = filters.get(i);
                    if(channelFilters != null) {
                        for (NamedDigitalFilter filter : channelFilters) {
                            signalsFilter.addSignalFilter(enableChannelsCount, filter, filter.getName());
                        }
                        enableChannelsCount++;
                    }

                }
            }
        }

        // Remove helper channels
        SignalsRemover signalsRemover = new SignalsRemover(signalsFilter);
        if(isAccelerometerOnly) {
            // delete helper enabled channel
            signalsRemover.removeSignal(0);
        }
        if (adsConfig.isLeadOffEnabled()) {
            // delete helper Lead-off channel
            signalsRemover.removeSignal(adsDataConfig.signalsCount() - 1);
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            // delete helper BatteryVoltage channel
             if (adsConfig.isLeadOffEnabled()) {
                signalsRemover.removeSignal(adsDataConfig.signalsCount() - 2);
            } else {
                signalsRemover.removeSignal(adsDataConfig.signalsCount() - 1);
            }
        }
        signalsRemover.addDataListener(dataListener);
        return signalsRemover;
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
