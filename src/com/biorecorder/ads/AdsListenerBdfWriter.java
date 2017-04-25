package com.biorecorder.ads;


import com.biorecorder.edflib.EdfFileWriter;
import com.biorecorder.edflib.EdfWriter;
import com.biorecorder.edflib.filters.EdfJoiner;
import com.biorecorder.edflib.filters.EdfSignalsFilter;
import com.biorecorder.edflib.filters.EdfSignalsRemover;
import com.biorecorder.edflib.filters.digital_filters.MovingAverageFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;


public class AdsListenerBdfWriter implements AdsDataListener {
    private static final Log LOG = LogFactory.getLog(AdsListenerBdfWriter.class);
    private int numberOfFramesToJoin;
    EdfFileWriter edfFileWriter;
    private EdfWriter edfWriter;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        edfFileWriter = new EdfFileWriter(bdfHeaderData.getFileToSave());
        edfFileWriter.setDurationOfDataRecordsComputable(true);

        // join DataRecords to have data records length = 1 sec;
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() /
                bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file
        EdfJoiner dataRecordsJoiner = new EdfJoiner(numberOfFramesToJoin, edfFileWriter);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        EdfSignalsFilter signalsFilter = new EdfSignalsFilter(dataRecordsJoiner);
        List<AdsChannelConfiguration> channels = bdfHeaderData.getAdsConfiguration().getAdsChannels();
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getDeviceType().getNumberOfAdsChannels();
        int sps = bdfHeaderData.getAdsConfiguration().getSps().getValue();
        int enableSignalsCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channelConfiguration = channels.get(i);
            if (channelConfiguration.isEnabled()) {
                if (channelConfiguration.is50HzFilterEnabled()) {
                    int divider = channelConfiguration.getDivider().getValue();
                    signalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(sps / (divider * 50)));
                }
                enableSignalsCounter++;
            }
        }

        // delete helper Loff channel
        EdfSignalsRemover signalsRemover = new EdfSignalsRemover(signalsFilter);
        if (bdfHeaderData.getAdsConfiguration().isLoffEnabled()) {
            signalsRemover.removeSignal(bdfHeaderData.getHeaderConfig().getNumberOfSignals() - 1);
        }
        edfWriter = signalsRemover;
        edfWriter.open(bdfHeaderData.getHeaderConfig());
    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
        try {
            edfWriter.writeDigitalSamples(dataFrame);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStopRecording() {
        try {
            edfWriter.close();
            // information about startRecordingTime, stopRecordingTime and actual DataRecordDuration
            LOG.info(edfFileWriter.getWritingInfo());
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
