package com.biorecorder.ads;

import com.biorecorder.edflib.BdfWriter;
import com.biorecorder.edflib.DataRecordsWriter;
import com.biorecorder.edflib.filters.*;
import com.biorecorder.edflib.filters.SignalMovingAverageFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class AdsListenerBdfWriter implements AdsDataListener {
    private static final Log LOG = LogFactory.getLog(AdsListenerBdfWriter.class);
    private int numberOfFramesToJoin;
    private DataRecordsWriter dataRecordsWriter;
    private BdfWriter bdfWriter;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        bdfWriter = new BdfWriter(bdfHeaderData.getFileToSave());
        bdfWriter.setDurationOfDataRecordsComputable(true);

        // join DataRecords to have data records length = 1 sec;
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() /
                bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file
        DataRecordsJoiner dataRecordsJoiner = new DataRecordsJoiner(numberOfFramesToJoin, bdfWriter);

        // apply MovingAveragePrefilter to ads channels to reduce 50HZ
        DataRecordsSignalsManager dataRecordsSignalsManager = new DataRecordsSignalsManager(dataRecordsJoiner);
        List<AdsChannelConfiguration> channels = bdfHeaderData.getAdsConfiguration().getAdsChannels();
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getDeviceType().getNumberOfAdsChannels();
        int sps = bdfHeaderData.getAdsConfiguration().getSps().getValue();
        int enableSignalsCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channelConfiguration = channels.get(i);
            if (channelConfiguration.isEnabled()) {
                if (channelConfiguration.is50HzFilterEnabled()) {
                    int divider = channelConfiguration.getDivider().getValue();
                    dataRecordsSignalsManager.addSignalPrefiltering(enableSignalsCounter, new SignalMovingAverageFilter(sps / (divider * 50)));
                }
                enableSignalsCounter++;
            }
        }

        // delete helper Loff channel
        if (bdfHeaderData.getAdsConfiguration().isLoffEnabled()) {
            dataRecordsSignalsManager.removeSignal(bdfHeaderData.getHeaderConfig().getNumberOfSignals() - 1);
        }
        dataRecordsWriter = dataRecordsSignalsManager;
        dataRecordsWriter.open(bdfHeaderData.getHeaderConfig());
    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
        try {
            dataRecordsWriter.writeDigitalDataRecord(dataFrame);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStopRecording() {
        try {
            dataRecordsWriter.close();
            // information about startRecordingTime, stopRecordingTime and actual DataRecordDuration
            LOG.info(bdfWriter.getWritingInfo());
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
