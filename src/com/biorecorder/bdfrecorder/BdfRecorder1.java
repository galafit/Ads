package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.bdfrecorder.exceptions.ConnectionRuntimeException;
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
public class BdfRecorder1 {
    private static final Log log = LogFactory.getLog(BdfRecorder1.class);
    private double resultantDataRecordDuration = 1; // sec

    private final Ads1 ads;
    private volatile BdfDataListener dataListener;
    private volatile LeadOffListener leadOffListener;
    private volatile RecorderEventsListener recorderEventsListener;
    private EdfSignalsFilter edfSignalsFilter;

    public BdfRecorder1(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads1(comportName);
            ads.setAdsEventsListener(new AdsEventsListener() {
                @Override
                public void handleLowButtery() {
                    recorderEventsListener.handleLowButtery();
                }

                @Override
                public void handleStartCanceled() {
                    recorderEventsListener.handleStartCanceled();
                }

                @Override
                public void handleFrameBroken(String eventInfo) {
                    // do nothing
                }
            });
            edfSignalsFilter = createChannelsFilter();
            ads.startMonitoring();
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
     * @param bdfRecorderConfig
     * @throws IllegalStateException if Recorder was disconnected and its work is finalised
     */
    public RecorderStartResult startRecording(BdfRecorderConfig1 bdfRecorderConfig) throws IllegalStateException {
        ads.setDataListener(new AdsDataHandler(bdfRecorderConfig));
        AdsStartResult adsResult = ads.startRecording(bdfRecorderConfig.getAdsConfig());
        return RecorderStartResult.valueOf(adsResult);
    }


    public boolean stop() throws IllegalStateException {
        ads.removeDataListener();
        ads.removeEventsListener();
        ads.startMonitoring();
        return ads.stopRecording();
    }

    public boolean disconnect()  {
        ads.removeDataListener();
        ads.removeEventsListener();
        return ads.disconnect();
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
    public EdfConfig getResultantRecordingInfo(BdfRecorderConfig1 bdfRecorderConfig) {
        AdsDataHandler adsDataHandler = new AdsDataHandler(bdfRecorderConfig);
        return adsDataHandler.getResultantEdfConfig();
    }


    public static String[] getAvailableComportNames() {
        return Ads1.getAvailableComportNames();
    }


    public RecorderType getDeviceType() {
        return RecorderType.valueOf(ads.getAdsType());
    }

    public void addChannelFilter(int channelNumber, DigitalFilter filter, String filterName) {
        SignalFilter signalFilter = new SignalFilter() {
            @Override
            public double getFilteredValue(double v) {
                return filter.getFilteredValue(v);
            }

            @Override
            public String getName() {
                return filterName;
            }
        };

        edfSignalsFilter.addSignalFilter(channelNumber, signalFilter);
    }

    public void removeAllChannelsFilters() {
        edfSignalsFilter = createChannelsFilter();
    }


    private BdfDataListener createNullDataListener() {
        return new BdfDataListener() {
            @Override
            public void onDataRecordReceived(int[] dataFrame) {
                // Do nothing !!!
            }
        };
    }

    private LeadOffListener createNullLeadOffListener() {
        return new LeadOffListener() {
            @Override
            public void onLeadOffDataReceived(Boolean[] leadOffMask) {
                // Do nothing !!!
            }
        };
    }


    private RecorderEventsListener createNullEventsListener() {
        return new RecorderEventsListener() {
            @Override
            public void handleLowButtery() {
                // Do nothing !!!
            }

            @Override
            public void handleStartCanceled() {
                // Do nothing !!!
            }
        };
    }


    public void setDataListener(BdfDataListener listener) {
        dataListener = listener;
    }

    public void setEventsListener(RecorderEventsListener listener) {
        recorderEventsListener = listener;
    }

    public void setLeadOffListener(LeadOffListener listener) {
        leadOffListener = listener;
    }

    public void removeDataListener() {
        dataListener = createNullDataListener();
    }

    public void removeEventsListener() {
        recorderEventsListener = createNullEventsListener();
    }

    public void removeLeadOffListener() {
        leadOffListener = createNullLeadOffListener();
    }

    class AdsDataHandler implements AdsDataListener {
        private final EdfFilter dataReceiver;
        private final BdfRecorderConfig1 bdfRecorderConfig;

        public AdsDataHandler(BdfRecorderConfig1 bdfRecorderConfig) {

            this.bdfRecorderConfig = bdfRecorderConfig;

            EdfConfig adsDataRecordConfig = getAdsDataRecordConfig(bdfRecorderConfig);

            // join DataRecords to have data records length = resultantDataRecordDuration;
            int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
            EdfJoiner edfJoiner = new EdfJoiner(numberOfFramesToJoin, edfSignalsFilter);


            EdfSignalsRemover edfSignalsRemover = new EdfSignalsRemover(edfJoiner);
            if (bdfRecorderConfig.isLeadOffEnabled()) {
                // delete helper Lead-off channel
                edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
            }
            if (bdfRecorderConfig.isBatteryVoltageMeasureEnabled()) {
                // delete helper BatteryVoltage channel
                if (bdfRecorderConfig.isLeadOffEnabled()) {
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

            if (bdfRecorderConfig.isLeadOffEnabled()) {
                boolean[] loffMask = Ads1.leadOffIntToBitMask(dataFrame[dataFrame.length - 1], bdfRecorderConfig.getNumberOfChannels());
                Boolean[] resultantLoffMask = new Boolean[loffMask.length];
                for (int i = 0; i < bdfRecorderConfig.getNumberOfChannels(); i++) {
                    if (bdfRecorderConfig.isChannelEnabled(i) && bdfRecorderConfig.isChannelLeadOffEnable(i)
                            && bdfRecorderConfig.getChannelRecordingMode(i).equals(RecordingMode.INPUT)) {
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

        private EdfConfig getAdsDataRecordConfig(BdfRecorderConfig1 bdfRecorderConfig) {
            AdsConfig adsConfig = bdfRecorderConfig.getAdsConfig();
            DefaultEdfConfig edfConfig = new DefaultEdfConfig();
            edfConfig.setRecordingIdentification(bdfRecorderConfig.getRecordingIdentification());
            edfConfig.setPatientIdentification(bdfRecorderConfig.getPatientIdentification());
            edfConfig.setDurationOfDataRecord(adsConfig.getDurationOfDataRecord());
            for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
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
                edfConfig.setLabel(signalNumber, "Loff Status");
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
