package com.biorecorder.ads;

import com.biorecorder.edflib.BdfWriter;
import com.biorecorder.edflib.DataRecordsWriter;
import com.biorecorder.edflib.filters.DataRecordsJoiner;
import com.biorecorder.edflib.filters.FrequencyAdjuster;
import com.biorecorder.edflib.filters.SignalsSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

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
        int numberOfSignals =  bdfHeaderData.getHeaderConfig().getNumberOfSignals();
        boolean[] signqalsMask = new boolean[numberOfSignals];
        for(int i = 0; i < numberOfSignals; i++) {
           signqalsMask[i]  = true;
        }

        dataRecordsWriter = new DataRecordsJoiner(numberOfFramesToJoin, new SignalsSelector(frequencyAdjuster, signqalsMask));
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
