package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.edflib.base.DefaultEdfConfig;
import com.biorecorder.edflib.base.EdfConfig;
import com.biorecorder.edflib.base.EdfWriter;
import com.biorecorder.edflib.filters.EdfFilter;
import com.biorecorder.edflib.filters.EdfJoiner;
import com.biorecorder.edflib.filters.EdfSignalsFilter;
import com.biorecorder.edflib.filters.EdfSignalsRemover;
import com.biorecorder.edflib.filters.signalfilters.SignalFilter;
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
    private volatile BdfDataListener dataListener;
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


    private EdfSignalsFilter createChannelsFilter() {
        EdfWriter dataReceiver = new EdfWriter() {
            @Override
            public void writeDigitalSamples(int[] ints, int i, int i1) {
                dataListener.onDataRecordReceived(ints);
            }

            @Override
            public void close() {

            }
        };

        return new EdfSignalsFilter(dataReceiver);
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
        ads.setDataListener(new AdsDataHandler(recorderConfig));
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
    public EdfConfig getResultantRecordingInfo(RecorderConfig recorderConfig) {
        AdsDataHandler adsDataHandler = new AdsDataHandler(recorderConfig);
        return adsDataHandler.getResultantEdfConfig();
    }


    public static String[] getAvailableComportNames() {
        return Ads.getAvailableComportNames();
    }


    public RecorderType getDeviceType() {
        return RecorderType.valueOf(ads.getAdsType());
    }

    public void addChannelFilter(int channelNumber, DigitalFilter filter, String filterName) {
        List<NamedDigitalFilter> channelFilters = filters.get(channelNumber);
        if(channelFilters == null) {
            channelFilters = new ArrayList();
            filters.put(channelNumber, channelFilters);
        }
        channelFilters.add(new NamedDigitalFilter(filter, filterName));
    }

    public void removeAllChannelsFilters() {
        filters = new HashMap<>();
    }


    public void setDataListener(BdfDataListener listener) {
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




    class NamedDigitalFilter implements SignalFilter {
        private DigitalFilter filter;
        private String filterName;

        public NamedDigitalFilter(DigitalFilter filter, String filterName) {
            this.filter = filter;
            this.filterName = filterName;
        }

        @Override
        public double getFilteredValue(double v) {
            return filter.getFilteredValue(v);
        }

        @Override
        public String getName() {
            return filterName;
        }
    }


    class NullDataListener implements  BdfDataListener {
        @Override
        public void onDataRecordReceived(int[] dataRecord) {
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

 /*   class AdsDataFilter {
        public AdsDataFilter(RecorderConfig recorderConfig, Map<Integer, List<NamedDigitalFilter>> channelsFilters, double resultantDataRecordDuration) {

            EdfConfig adsDataRecordConfig = getAdsDataRecordConfig(recorderConfig);

            // join DataRecords to have data records length = resultantDataRecordDuration;
            int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
            EdfJoiner edfJoiner = new EdfJoiner(numberOfFramesToJoin, edfSignalsFilter);


            BdfSignalsRemover edfSignalsRemover = new BdfSignalsRemover(edfJoiner);
            if (recorderConfig.isLeadOffEnabled()) {
                // delete helper Lead-off channel
                edfSignalsRemover.removeSignal(adsDataRecordConfig.getSignalsCount() - 1);
            }
            if (recorderConfig.isBatteryVoltageMeasureEnabled()) {
                // delete helper BatteryVoltage channel
                if (recorderConfig.isLeadOffEnabled()) {
                    edfSignalsRemover.removeSignal(adsDataRecordConfig.getSignalsCount() - 2);
                } else {
                    edfSignalsRemover.removeSignal(adsDataRecordConfig.getSignalsCount() - 1);
                }
            }

            edfSignalsRemover.setConfig(adsDataRecordConfig);
            dataReceiver = edfSignalsRemover;
        }


        public EdfConfig getResultantEdfConfig() {
            return dataReceiver.getResultantConfig();
        }

        private EdfConfig getAdsDataRecordConfig(RecorderConfig recorderConfig) {
            AdsConfig adsConfig = recorderConfig.getAdsConfig();
            DefaultEdfConfig edfConfig = new DefaultEdfConfig();
            edfConfig.setRecordingIdentification(recorderConfig.getRecordingIdentification());
            edfConfig.setPatientIdentification(recorderConfig.getPatientIdentification());
            edfConfig.setDurationOfDataRecord(adsConfig.getDurationOfDataRecord());
            for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                if (adsConfig.isAdsChannelEnabled(i)) {
                    edfConfig.addSignal();
                    int signalNumber = edfConfig.getSignalsCount() - 1;
                    edfConfig.setTransducer(signalNumber, "Unknown");
                    edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAdsChannelsPhysicalDimension());
                    edfConfig.setPhysicalRange(signalNumber, adsConfig.getAdsChannelPhysicalMin(i), adsConfig.getAdsChannelPhysicalMax(i));
                    edfConfig.setDigitalRange(signalNumber, adsConfig.getAdsChannelsDigitalMin(), adsConfig.getAdsChannelsDigitalMax());
                    edfConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAdsChannelSampleRate(i));
                    edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                    edfConfig.setLabel(signalNumber, adsConfig.getAdsChannelName(i));
                }
            }

            if (adsConfig.isAccelerometerEnabled()) {
                if (adsConfig.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                    edfConfig.addSignal();
                    int signalNumber = edfConfig.getSignalsCount() - 1;
                    edfConfig.setLabel(signalNumber, "Accelerometer");
                    edfConfig.setTransducer(signalNumber, "None");
                    edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                    edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                    edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                    edfConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                    edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                } else {
                    String[] accelerometerChannelNames = {"Accelerometer X", "Accelerometer Y", "Accelerometer Z"};
                    for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                        edfConfig.addSignal();
                        int signalNumber = edfConfig.getSignalsCount() - 1;
                        edfConfig.setLabel(signalNumber, accelerometerChannelNames[i]);
                        edfConfig.setTransducer(signalNumber, "None");
                        edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                        edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                        edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                        edfConfig.setPrefiltering(signalNumber, "None");
                        int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                        edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                    }
                }
            }
            if (adsConfig.isBatteryVoltageMeasureEnabled()) {
                edfConfig.addSignal();
                int signalNumber = edfConfig.getSignalsCount() - 1;
                edfConfig.setLabel(signalNumber, "Battery voltage");
                edfConfig.setTransducer(signalNumber, "None");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getBatteryVoltageDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getBatteryVoltagePhysicalMin(), adsConfig.getBatteryVoltagePhysicalMax());
                edfConfig.setDigitalRange(signalNumber, adsConfig.getBatteryVoltageDigitalMin(), adsConfig.getBatteryVoltageDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = 1;
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            }
            if (adsConfig.isLeadOffEnabled()) {
                edfConfig.addSignal();
                int signalNumber = edfConfig.getSignalsCount() - 1;
                edfConfig.setLabel(signalNumber, "Lead Off Status");
                edfConfig.setTransducer(signalNumber, "None");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getLeadOffStatusDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getLeadOffStatusPhysicalMin(), adsConfig.getLeadOffStatusPhysicalMax());
                edfConfig.setDigitalRange(signalNumber, adsConfig.getLeadOffStatusDigitalMin(), adsConfig.getLeadOffStatusDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = 1;
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            }
            return edfConfig;
        }



    }*/

    class AdsDataHandler implements AdsDataListener {
        private final EdfFilter dataReceiver;
        private final RecorderConfig recorderConfig;

        public AdsDataHandler(RecorderConfig recorderConfig) {

            EdfWriter dataWriter = new EdfWriter() {
                @Override
                public void writeDigitalSamples(int[] ints, int i, int i1) {
                    dataListener.onDataRecordReceived(ints);
                }

                @Override
                public void close() {

                }
            };

            EdfSignalsFilter edfSignalsFilter = new EdfSignalsFilter(dataWriter);
            this.recorderConfig = recorderConfig;

            EdfConfig adsDataRecordConfig = getAdsDataRecordConfig(recorderConfig);

            // join DataRecords to have data records length = resultantDataRecordDuration;
            int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
            EdfJoiner edfJoiner = new EdfJoiner(numberOfFramesToJoin, edfSignalsFilter);


            EdfSignalsRemover edfSignalsRemover = new EdfSignalsRemover(edfJoiner);
            if (recorderConfig.isLeadOffEnabled()) {
                // delete helper Lead-off channel
                edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
            }
            if (recorderConfig.isBatteryVoltageMeasureEnabled()) {
                // delete helper BatteryVoltage channel
                if (recorderConfig.isLeadOffEnabled()) {
                    edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 2);
                } else {
                    edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
                }
            }

            edfSignalsRemover.setConfig(adsDataRecordConfig);
            dataReceiver = edfSignalsRemover;
        }

        @Override
        public void onDataReceived(int[] dataFrame) {
            dataReceiver.writeDigitalSamples(dataFrame);

            if (recorderConfig.isLeadOffEnabled()) {
                boolean[] loffMask = Ads.leadOffIntToBitMask(dataFrame[dataFrame.length - 1], recorderConfig.getNumberOfChannels());
                Boolean[] resultantLoffMask = new Boolean[loffMask.length];
                for (int i = 0; i < recorderConfig.getNumberOfChannels(); i++) {
                    if (recorderConfig.isChannelEnabled(i) && recorderConfig.isChannelLeadOffEnable(i)
                            && recorderConfig.getChannelRecordingMode(i).equals(RecordingMode.INPUT)) {
                        resultantLoffMask[2 * i] = loffMask[2 * i];
                        resultantLoffMask[2 * i + 1] = loffMask[2 * i + 1];
                    }

                }
                leadOffListener.onLeadOffDataReceived(resultantLoffMask);
            }
        }

        public EdfConfig getResultantEdfConfig() {
            return dataReceiver.getResultantConfig();
        }

        private EdfConfig getAdsDataRecordConfig(RecorderConfig recorderConfig) {
            AdsConfig adsConfig = recorderConfig.getAdsConfig();
            DefaultEdfConfig edfConfig = new DefaultEdfConfig();
            edfConfig.setRecordingIdentification(recorderConfig.getRecordingIdentification());
            edfConfig.setPatientIdentification(recorderConfig.getPatientIdentification());
            edfConfig.setDurationOfDataRecord(adsConfig.getDurationOfDataRecord());
            for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
                if (adsConfig.isAdsChannelEnabled(i)) {
                    edfConfig.addSignal();
                    int signalNumber = edfConfig.getNumberOfSignals() - 1;
                    edfConfig.setTransducer(signalNumber, "Unknown");
                    edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAdsChannelsPhysicalDimension());
                    edfConfig.setPhysicalRange(signalNumber, adsConfig.getAdsChannelPhysicalMin(i), adsConfig.getAdsChannelPhysicalMax(i));
                    edfConfig.setDigitalRange(signalNumber, adsConfig.getAdsChannelsDigitalMin(), adsConfig.getAdsChannelsDigitalMax());
                    edfConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAdsChannelSampleRate(i));
                    edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                    edfConfig.setLabel(signalNumber, adsConfig.getAdsChannelName(i));
                }
            }

            if (adsConfig.isAccelerometerEnabled()) {
                if (adsConfig.isAccelerometerOneChannelMode()) { // 1 accelerometer channels
                    edfConfig.addSignal();
                    int signalNumber = edfConfig.getNumberOfSignals() - 1;
                    edfConfig.setLabel(signalNumber, "Accelerometer");
                    edfConfig.setTransducer(signalNumber, "None");
                    edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                    edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                    edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                    edfConfig.setPrefiltering(signalNumber, "None");
                    int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                    edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                } else {
                    String[] accelerometerChannelNames = {"Accelerometer X", "Accelerometer Y", "Accelerometer Z"};
                    for (int i = 0; i < 3; i++) {     // 3 accelerometer channels
                        edfConfig.addSignal();
                        int signalNumber = edfConfig.getNumberOfSignals() - 1;
                        edfConfig.setLabel(signalNumber, accelerometerChannelNames[i]);
                        edfConfig.setTransducer(signalNumber, "None");
                        edfConfig.setPhysicalDimension(signalNumber, adsConfig.getAccelerometerPhysicalDimension());
                        edfConfig.setPhysicalRange(signalNumber, adsConfig.getAccelerometerPhysicalMin(), adsConfig.getAccelerometerPhysicalMax());
                        edfConfig.setDigitalRange(signalNumber, adsConfig.getAccelerometerDigitalMin(), adsConfig.getAccelerometerDigitalMax());
                        edfConfig.setPrefiltering(signalNumber, "None");
                        int nrOfSamplesInEachDataRecord = (int) Math.round(adsConfig.getDurationOfDataRecord() * adsConfig.getAccelerometerSampleRate());
                        edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
                    }
                }
            }
            if (adsConfig.isBatteryVoltageMeasureEnabled()) {
                edfConfig.addSignal();
                int signalNumber = edfConfig.getNumberOfSignals() - 1;
                edfConfig.setLabel(signalNumber, "Battery voltage");
                edfConfig.setTransducer(signalNumber, "None");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getBatteryVoltageDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getBatteryVoltagePhysicalMin(), adsConfig.getBatteryVoltagePhysicalMax());
                edfConfig.setDigitalRange(signalNumber, adsConfig.getBatteryVoltageDigitalMin(), adsConfig.getBatteryVoltageDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = 1;
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            }
            if (adsConfig.isLeadOffEnabled()) {
                edfConfig.addSignal();
                int signalNumber = edfConfig.getNumberOfSignals() - 1;
                edfConfig.setLabel(signalNumber, "Lead Off Status");
                edfConfig.setTransducer(signalNumber, "None");
                edfConfig.setPhysicalDimension(signalNumber, adsConfig.getLeadOffStatusDimension());
                edfConfig.setPhysicalRange(signalNumber, adsConfig.getLeadOffStatusPhysicalMin(), adsConfig.getLeadOffStatusPhysicalMax());
                edfConfig.setDigitalRange(signalNumber, adsConfig.getLeadOffStatusDigitalMin(), adsConfig.getLeadOffStatusDigitalMax());
                edfConfig.setPrefiltering(signalNumber, "None");
                int nrOfSamplesInEachDataRecord = 1;
                edfConfig.setNumberOfSamplesInEachDataRecord(signalNumber, nrOfSamplesInEachDataRecord);
            }
            return edfConfig;
        }
    }

}
