package com.biorecorder.ads;

import com.biorecorder.edflib.BdfWriter;
import com.biorecorder.edflib.DataRecordsWriter;
import com.biorecorder.edflib.filters.*;
import com.biorecorder.edflib.filters.signal_filters.SignalAveragingFilter;
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
    private RecordsFrequencyCalculator recordsFrequencyCalculator;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() /
                bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file

        recordsFrequencyCalculator = new RecordsFrequencyCalculator(new BdfWriter(bdfHeaderData.getFileToSave()));
        RecordsJoiner recordsJoiner =  new RecordsJoiner(numberOfFramesToJoin, recordsFrequencyCalculator);

        AggregateFilter averagingFilter = new AggregateFilter(recordsJoiner);
        List<AdsChannelConfiguration> channels = bdfHeaderData.getAdsConfiguration().getAdsChannels();
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getDeviceType().getNumberOfAdsChannels();
        int sps = bdfHeaderData.getAdsConfiguration().getSps().getValue();
        int enableSignalsCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channelConfiguration = channels.get(i);
            if(channelConfiguration.isEnabled()) {
                if(channelConfiguration.is50HzFilterEnabled()){
                    int divider = channelConfiguration.getDivider().getValue();
                    averagingFilter.addSignalFilter(enableSignalsCounter, new SignalAveragingFilter(sps / (divider * 50)));
                }
                enableSignalsCounter++;
            }
        }


        if(bdfHeaderData.getAdsConfiguration().isLoffEnabled()) {
            SignalsRemoval signalsRemoval = new SignalsRemoval(averagingFilter);
            int numberOfAllSignals = bdfHeaderData.getHeaderConfig().getNumberOfSignals();
            signalsRemoval.removeSignal(numberOfAllSignals - 1);
            dataRecordsWriter = signalsRemoval;
        }
        else{
            dataRecordsWriter = averagingFilter;
        }
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
            LOG.info(recordsFrequencyCalculator.toString());
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
