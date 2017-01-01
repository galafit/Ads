package com.biorecorder.ads;

import com.biorecorder.edflib.BdfWriter;
import com.biorecorder.edflib.DataRecordsWriter;
import com.biorecorder.edflib.filters.*;
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
    private FrequencyAdjuster frequencyAdjuster;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() /
                bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file

        frequencyAdjuster = new FrequencyAdjuster(new BdfWriter(bdfHeaderData.getFileToSave()));
        DataRecordsJoiner dataRecordsJoiner =  new DataRecordsJoiner(numberOfFramesToJoin, frequencyAdjuster);

        AggregateFilter averagingFilter = new AggregateFilter(dataRecordsJoiner);
        List<AdsChannelConfiguration> channels = bdfHeaderData.getAdsConfiguration().getAdsChannels();
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getDeviceType().getNumberOfAdsChannels();
        int sps = bdfHeaderData.getAdsConfiguration().getSps().getValue();
        int availableSignalsCounter = 0;
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channelConfiguration = channels.get(i);
            if(channelConfiguration.isEnabled()) {
                if(channelConfiguration.is50HzFilterEnabled()){
                    int divider = channelConfiguration.getDivider().getValue();
                    averagingFilter.addSignalFilter(availableSignalsCounter, new SignalAveragingFilter(sps / (divider * 50)));
                }
                availableSignalsCounter++;
            }
        }


        if(bdfHeaderData.getAdsConfiguration().isLoffEnabled()) {
            SignalsSelector signalsSelector= new SignalsSelector(averagingFilter);
            int numberOfAllSignals = bdfHeaderData.getHeaderConfig().getNumberOfSignals();
            signalsSelector.excludeSignal(numberOfAllSignals - 1);
            dataRecordsWriter = signalsSelector;

        }
        else{
            dataRecordsWriter = averagingFilter;
        }


        dataRecordsWriter.setHeaderConfig(bdfHeaderData.getHeaderConfig());
    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
        try {
            dataRecordsWriter.writeDigitalDataRecords(dataFrame);
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
            LOG.info(frequencyAdjuster.toString());
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
