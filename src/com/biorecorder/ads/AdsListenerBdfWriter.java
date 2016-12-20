package com.biorecorder.ads;

import com.biorecorder.edflib.BdfRecordsJoiner;
import com.biorecorder.edflib.BdfWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Created by gala on 12/12/16.
 */
public class AdsListenerBdfWriter implements AdsDataListener {
    private static final Log LOG = LogFactory.getLog(AdsListenerBdfWriter.class);
    private BdfWriter bdfWriter;
    private int numberOfFramesToJoin;
    private BdfRecordsJoiner bdfRecordsJoiner;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() /
                bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file
        bdfRecordsJoiner = new BdfRecordsJoiner(bdfHeaderData.getBdfHeader(), numberOfFramesToJoin);
        bdfWriter = new BdfWriter(bdfHeaderData.getFileToSave(), bdfRecordsJoiner.getResultingBdfHeader());

    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
        if(bdfRecordsJoiner.addDataRecord(dataFrame)) {
            try {
                bdfWriter.writeDataRecord(bdfRecordsJoiner.getResultingDataRecord());
            } catch (IOException e) {
                LOG.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onStopRecording() {
        try {
            bdfWriter.close(true);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}
