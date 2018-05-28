package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.dataformat.DataSender;
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
 *     <li>removes  helper technical info about lead-off status and buttery charge</li>
 *     <li>permits to add to ads channels data some filters. At the moment - filter removing "50Hz noise" (Moving average filter)</li>
 * </ul>
 *
 * Thus resultant DataFrames (that BdfRecorder sends to its listeners) have standard edf/bdf structure and could be
 * directly written to to bdf/edf file
 *
 */
public class BdfRecorder {
    private static final Log log = LogFactory.getLog(BdfRecorder.class);
    private double resultantDataRecordDuration = 1; // sec

    private final Ads ads;
    private volatile DataListener dataListener;
    private volatile LeadOffListener leadOffListener;
    private volatile RecorderEventsListener recorderEventsListener;

    private Map<Integer, List<NamedDigitalFilter>> filters = new HashMap();

    public BdfRecorder(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads(comportName);
            ads.setAdsEventsListener(new AdsEventsListener() {
                @Override
                public void handleLowButtery() {
                    recorderEventsListener.handleLowButtery();
                }
            });
        } catch (SerialPortRuntimeException ex) {
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
     * and false otherwise. Throws IllegalArgumentException if device type specified in config
     * does not coincide with the really connected device type.
     * @throws IllegalStateException if Recorder was disconnected and
     * its work was finalised or if it is already recording and should be stopped first
     */
    public Future<Boolean> startRecording(RecorderConfig recorderConfig) throws IllegalStateException {
        DataSender adsFilterDataSender = createAdsFilterDataSender(ads, recorderConfig);
        adsFilterDataSender.addDataListener(dataListener);
        return ads.startRecording(recorderConfig.getAdsConfig());
    }

    public boolean stopRecording() throws IllegalStateException {
        ads.removeDataListener();
        ads.removeEventsListener();
        return ads.stopRecording();
    }

    public boolean disconnect()  {
        ads.removeDataListener();
        ads.removeEventsListener();
        return ads.disconnect();
    }

    public void startMonitoring() throws IllegalStateException  {
        ads.startMonitoring();
    }

    public void stopMonitoring() {
        ads.stopMonitoring();
    }


    public boolean isActive() {
        return ads.isActive();
    }

    public String getComportName() {
        return ads.getComportName();
    }

    public RecorderState getRecorderState() {
        return RecorderState.valueOf(ads.getAdsState());
    }


    /**
     * Get the info describing the structure of resultant dataRecords
     * that BdfRecorder sends to its listeners and write to the Edf/Bdf file
     *
     * @return object with info about recording process and dataRecords structure
     */
    public DataConfig getDataConfig(RecorderConfig recorderConfig) {
        return createAdsFilterDataSender(ads, recorderConfig).dataConfig();
    }


    public static String[] getAvailableComportNames() {
        return Ads.getAvailableComportNames();
    }


    public RecorderType getDeviceType() {
        return RecorderType.valueOf(ads.getAdsType());
    }


    public void setDataListener(DataListener listener) {
        dataListener = listener;
    }

    public void removeDataListener() {
        dataListener = new NullDataListener();
    }


    public void setLeadOffListener(LeadOffListener listener) {
        leadOffListener = listener;
    }

    public void removeLeadOffListener() {
        leadOffListener = new NullLeadOffListener();
    }

    public void setEventsListener(RecorderEventsListener listener) {
        recorderEventsListener = listener;
    }

    public void removeEventsListener() {
        recorderEventsListener = new NullEventsListener();
    }


    class NullDataListener implements DataListener {
        @Override
        public void onDataReceived(int[] dataRecord) {
            // do nothing;
        }
    }

    class NullLeadOffListener implements LeadOffListener {
        @Override
        public void onLeadOffDataReceived(Boolean[] leadOffMask) {
            // do nothing
        }
    }

    class NullEventsListener implements RecorderEventsListener {
        @Override
        public void handleLowButtery() {

        }
    }


    class AdsDataSender implements DataSender {
        private final Ads ads;
        private final RecorderConfig recorderConfig;

        public AdsDataSender(Ads ads, RecorderConfig recorderConfig) {
            this.ads = ads;
            this.recorderConfig = recorderConfig;
        }
        @Override
        public DataConfig dataConfig() {
            return ads.getDataConfig(recorderConfig.getAdsConfig());
        }

        @Override
        public void addDataListener(DataListener dataListener) {
            ads.setDataListener(new DataListener() {
                @Override
                public void onDataReceived(int[] dataFrame) {
                    dataListener.onDataReceived(dataFrame);

                    if (recorderConfig.isLeadOffEnabled()) {
                        boolean[] loffMask = Ads.leadOffIntToBitMask(dataFrame[dataFrame.length - 1], recorderConfig.getChannelsCount());
                        Boolean[] resultantLoffMask = new Boolean[loffMask.length];
                        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
                            if (recorderConfig.isChannelEnabled(i) && recorderConfig.isChannelLeadOffEnable(i)
                                    && recorderConfig.getChannelRecordingMode(i).equals(RecordingMode.INPUT)) {
                                resultantLoffMask[2 * i] = loffMask[2 * i];
                                resultantLoffMask[2 * i + 1] = loffMask[2 * i + 1];
                            }

                        }
                        leadOffListener.onLeadOffDataReceived(resultantLoffMask);
                    }
                }
            });
        }

        @Override
        public void removeDataListener(DataListener dataListener) {
            ads.removeDataListener();

        }
    }


    private DataSender createAdsFilterDataSender(Ads ads, RecorderConfig recorderConfig) {
        AdsConfig adsConfig = recorderConfig.getAdsConfig();
        AdsDataSender adsDataSender = new AdsDataSender(ads, recorderConfig);
        // join DataRecords to have data records length = resultantDataRecordDuration;
        int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsConfig.getDurationOfDataRecord());
        DataRecordsJoiner edfJoiner = new DataRecordsJoiner(adsDataSender, numberOfFramesToJoin);

        // Add digital filters to ads channels
        SignalsFilter signalsFilter = new SignalsFilter(edfJoiner);
        int enableChannelsCount = 0;
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            if(adsConfig.isAdsChannelEnabled(i)) {
                List<NamedDigitalFilter> channelFilters = filters.get(i);
                for (NamedDigitalFilter filter : channelFilters) {
                    signalsFilter.addSignalFilter(enableChannelsCount, filter, filter.getName());
                }
                enableChannelsCount++;
            }
        }

        // Remove helper channels
        SignalsRemover signalsRemover = new SignalsRemover(signalsFilter);
        if (adsConfig.isLeadOffEnabled()) {
            // delete helper Lead-off channel
            signalsRemover.removeSignal(adsConfig.allEnableChannelsCount() - 1);
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            // delete helper BatteryVoltage channel
            if (adsConfig.isLeadOffEnabled()) {
                signalsRemover.removeSignal(adsConfig.allEnableChannelsCount() - 2);
            } else {
                signalsRemover.removeSignal(adsConfig.allEnableChannelsCount() - 1);
            }
        }

        return signalsRemover;
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
