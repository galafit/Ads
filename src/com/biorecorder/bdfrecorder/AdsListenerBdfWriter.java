package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsDataListener;
import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.FileType;
import com.biorecorder.edflib.base.EdfConfig;
import com.biorecorder.edflib.base.EdfWriter;
import com.biorecorder.edflib.filters.EdfFilter;
import com.biorecorder.edflib.filters.EdfJoiner;
import com.biorecorder.edflib.filters.EdfSignalsFilter;
import com.biorecorder.edflib.filters.EdfSignalsRemover;
import com.biorecorder.edflib.filters.signalfilters.MovingAverageFilter;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;

/**
 * Created by galafit on 12/6/17.
 */
class AdsListenerBdfWriter implements AdsDataListener {
    private static final Log LOG = LogFactory.getLog(AdsListenerBdfWriter.class);
    EdfDataReceiver edfDataReceiver;
    private EdfFilter edfWriter;
    List<BdfDataListener> dataListeners;

    public AdsListenerBdfWriter(BdfRecorderConfig bdfRecorderConfig, @Nullable File file, double resultantDataRecordDuration, List<BdfDataListener> dataListeners)  {
        this.dataListeners = dataListeners;
        EdfDataReceiver dataReceiver = new EdfDataReceiver(file, bdfRecorderConfig.isDurationOfDataRecordComputable());
        EdfConfig adsDataRecordConfig = bdfRecorderConfig.getAdsDataRecordConfig();

        // join DataRecords to have data records length = resultantDataRecordDuration;
        int numberOfFramesToJoin = (int) (resultantDataRecordDuration / adsDataRecordConfig.getDurationOfDataRecord());
        EdfJoiner edfJoiner = new EdfJoiner(numberOfFramesToJoin, dataReceiver);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        EdfSignalsFilter edfSignalsFilter = new EdfSignalsFilter(edfJoiner);
        int enableSignalsCounter = 0;
        for (int i = 0; i < bdfRecorderConfig.getNumberOfAdsChannels(); i++) {
            if (bdfRecorderConfig.isAdsChannelEnabled(i)) {
                if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                    edfSignalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(bdfRecorderConfig.getAdsChannelFrequency(i) / 50));
                }
                enableSignalsCounter++;
            }
        }

        EdfSignalsRemover edfSignalsRemover = new EdfSignalsRemover(edfSignalsFilter);
        if (bdfRecorderConfig.isLeadOffEnabled()) {
            // delete helper Lead-off channel
            edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
        }
        if(bdfRecorderConfig.isBatteryVoltageMeasureEnabled() && bdfRecorderConfig.isButteryVoltageChannelDelite()) {
            // delete helper BatteryVoltage channel
            if(bdfRecorderConfig.isLeadOffEnabled()) {
                edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 2);
            } else {
                edfSignalsRemover.removeSignal(adsDataRecordConfig.getNumberOfSignals() - 1);
            }
        }

        edfSignalsRemover.setConfig(adsDataRecordConfig);
        edfWriter = edfSignalsRemover;
        edfDataReceiver = dataReceiver;
    }

    @Override
    public void onDataReceived(int[] dataFrame) {
        try {
            edfWriter.writeDigitalSamples(dataFrame);
        } catch (Exception e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    public EdfConfig getResultantEdfConfig() {
       return  edfWriter.getResultantConfig();
    }

    public int getNumberOfWrittenDataRecords() {
        return edfWriter.getNumberOfWrittenDataRecords();
    }

    public File getFile() {
        return edfDataReceiver.getFile();
    }

    public void close() {
        try {
            edfWriter.close();
        } catch (Exception e) {
            LOG.error(e);
            // throw new RuntimeException(e);
        }
    }

    class EdfDataReceiver extends EdfWriter {
        private EdfFileWriter edfFileWriter = null;


        public EdfDataReceiver(@Nullable File file, boolean isDurationOfDataRecordComputable) {
            if(file != null) {
                edfFileWriter = new EdfFileWriter(file, FileType.BDF_24BIT);
                edfFileWriter.setDurationOfDataRecordsComputable(isDurationOfDataRecordComputable);
            }
        }

        public File getFile() {
            if(edfFileWriter != null) {
                return edfFileWriter.getFile();
            }
            return null;
        }


        @Override
        public void setConfig(EdfConfig recordingInfo) {
            super.setConfig(recordingInfo);
            if(edfFileWriter != null) {
                edfFileWriter.setConfig(recordingInfo);
            }
        }

        @Override
        public void writeDigitalSamples(int[] samples, int offset, int length) {
            sampleCounter += length;
            for (BdfDataListener listener : dataListeners) {
                listener.onDataRecordReceived(samples);
            }
            if(edfFileWriter != null) {
                edfFileWriter.writeDigitalSamples(samples, offset, length);
            }
        }

        @Override
        public void close() {
            if(edfFileWriter != null) {
                edfFileWriter.close();
                // information about startRecordingTime, stopRecordingTime and actual DataRecordDuration
                LOG.info(edfFileWriter.getWritingInfo());
                if(getNumberOfReceivedDataRecords() == 0) {
                    File bdfFile = edfFileWriter.getFile();
                    bdfFile.delete();
                }
            }
        }
    }
}

