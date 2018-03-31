package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.*;
import com.biorecorder.bdfrecorder.exceptions.ConnectionRuntimeException;
import com.biorecorder.bdfrecorder.exceptions.InvalidDeviceTypeRuntimeException;
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
import java.util.List;

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

    private final Ads1 ads;
    private List<BdfDataListener> dataListeners = new ArrayList<BdfDataListener>();
    private List<LoffListener> loffListeners = new ArrayList<LoffListener>();
    private List<LowButteryEventListener> butteryEventListeners = new ArrayList<LowButteryEventListener>();
    private double resultantDataRecordDuration = 1; // sec
    private EdfSignalsFilter edfSignalsFilter;

    public BdfRecorder1(String comportName) throws ConnectionRuntimeException {
        try {
            ads = new Ads1(comportName);
            ads.setLowButteryListener(new AdsLowButteryListener() {
                @Override
                public void handleLowButtery() {
                    for (LowButteryEventListener listener : butteryEventListeners) {
                        listener.handleLowButteryEvent();
                    }
                }

            });
            edfSignalsFilter = createChannelsFilter();
        } catch (SerialPortRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
    }

    private EdfSignalsFilter createChannelsFilter() {
        EdfWriter dataReceiver = new EdfWriter() {
            @Override
            public void writeDigitalSamples(int[] ints, int i, int i1) {
                for (BdfDataListener dataListener : dataListeners) {
                    dataListener.onDataRecordReceived(ints);
                }
            }

            @Override
            public void close() {

            }
        };

        return new EdfSignalsFilter(dataReceiver);
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

    public void startRecording(BdfRecorderConfig1 bdfRecorderConfig) throws ConnectionRuntimeException, InvalidDeviceTypeRuntimeException {
        try {
            ads.startRecording(bdfRecorderConfig.getAdsConfig());
            ads.setDataListener(new AdsDataHandler(bdfRecorderConfig));

        } catch (SerialPortRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        } catch (InvalidAdsTypeRuntimeException ex) {
            throw new InvalidDeviceTypeRuntimeException(ex);
        }
    }

    public void startMonitoring() {
        ads.startMonitoring();
    }

    public void stop() throws ConnectionRuntimeException {
        try {
            ads.stop();
            ads.setDataListener(null); // remove ads data listener
            ads.setLowButteryListener(null); // remove ads low buttery listener

        } catch (SerialPortRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
    }

    public boolean isActive() {
        if(ads!= null && ads.isActive()) {
            return true;
        }
        return false;
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


    public void disconnect() throws ConnectionRuntimeException {
        try {
            stop();
            ads.disconnect();
        } catch (SerialPortRuntimeException ex) {
            throw new ConnectionRuntimeException(ex);
        }
    }

    /**
     * Attention! This method is DENGAROUS!!!
     * Serial port lib (jssc) en Mac and Linux to create portNames list
     * actually OPENS and CLOSES every port (suppose to be sure it is exist). So
     * this operation can course serious bugs...
     * Like possibility to have multiple connections with the same  port
     * and so loose incoming data. See {@link com.biorecorder.TestSerialPort}.
     *
     * @return array of names of all comports or empty array.
     */
    public static String[] getAvailableComportNames() {
        return Ads1.getAvailableComportNames();
    }

    public void addBdfDataListener(BdfDataListener listener) {
        dataListeners.add(listener);
    }

    public void addLowButteryEventListener(LowButteryEventListener listener) {
        butteryEventListeners.add(listener);
    }

    public void addLoffListener(LoffListener listener) {
        loffListeners.add(listener);
    }


    class AdsDataHandler implements AdsDataListener {
        private EdfFilter dataReceiver;
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
                boolean[] loffMask = Ads1.lofDetectionIntToBitMask(dataFrame[dataFrame.length - 1], bdfRecorderConfig.getNumberOfChannels());
                Boolean[] resultantLoffMask = new Boolean[loffMask.length];
                for (int i = 0; i < bdfRecorderConfig.getNumberOfChannels(); i++) {
                    if (bdfRecorderConfig.isChannelEnabled(i) && bdfRecorderConfig.isChannelLeadOffEnable(i)
                            && bdfRecorderConfig.getChannelRecordingMode(i).equals(RecordingMode.INPUT.name())) {
                        resultantLoffMask[2 * i] = loffMask[2 * i];
                        resultantLoffMask[2 * i + 1] = loffMask[2 * i + 1];
                    }

                }

                for (LoffListener loffListener : loffListeners) {
                    loffListener.onLoffDataReceived(resultantLoffMask);
                }
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
