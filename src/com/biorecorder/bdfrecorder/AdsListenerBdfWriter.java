package com.biorecorder.bdfrecorder;


import com.biorecorder.ads.AdsChannelConfig;
import com.biorecorder.ads.AdsDataListener;
import com.biorecorder.edflib.*;
import com.biorecorder.edflib.filters.MovingAverageFilter;
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
        numberOfFramesToJoin = bdfHeaderData.getAdsConfig().getSps().getValue() /
                bdfHeaderData.getAdsConfig().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file
        EdfJoiner dataRecordsJoiner = new EdfJoiner(numberOfFramesToJoin, edfFileWriter);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        EdfSignalsFilter signalsFilter = new EdfSignalsFilter(dataRecordsJoiner);
        int numberOfAdsChannels = bdfHeaderData.getAdsConfig().getNumberOfAdsChannels();
        int sps = bdfHeaderData.getAdsConfig().getSps().getValue();
        int enableSignalsCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfig channelConfiguration = bdfHeaderData.getAdsConfig().getAdsChannel(i);
            if (channelConfiguration.isEnabled()) {
                if (bdfHeaderData.is50HzFilterEnabled(i)) {
                    int divider = channelConfiguration.getDivider().getValue();
                    signalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(sps / (divider * 50)));
                }
                enableSignalsCounter++;
            }
        }

        // delete helper Loff channel
        EdfSignalsRemover signalsRemover = new EdfSignalsRemover(signalsFilter);
        if (bdfHeaderData.getAdsConfig().isLoffEnabled()) {
            signalsRemover.removeSignal(bdfHeaderData.getHeaderConfig().getNumberOfSignals() - 1);
        }
        edfWriter = signalsRemover;
        edfWriter.setHeader(bdfHeaderData.getHeaderConfig());
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
           // throw new RuntimeException(e);
        }
    }
}
