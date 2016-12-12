package com.biorecorder.ads;

import com.biorecorder.edflib.BdfHeader;
import com.biorecorder.edflib.BdfRecordsJoiner;
import com.biorecorder.edflib.BdfWriter;

import java.io.IOException;

/**
 * Created by gala on 12/12/16.
 */
public class AdsListenerBdfWriter implements AdsDataListener {
    BdfWriter bdfWriter;
    int numberOfFramesToJoin;
    BdfRecordsJoiner bdfRecordsJoiner;

    public AdsListenerBdfWriter(BdfHeaderData bdfHeaderData) throws IOException {
        numberOfFramesToJoin = bdfHeaderData.getAdsConfiguration().getSps().getValue() / bdfHeaderData.getAdsConfiguration().getDeviceType().getMaxDiv().getValue(); // 1 second duration of a data record in bdf file
        bdfRecordsJoiner = new BdfRecordsJoiner(bdfHeaderData.getBdfHeader(), numberOfFramesToJoin);
//        bdfWriter = new BdfWriter(bdfHeaderData.getFileToSave(), bdfRecordsJoiner.getResultingBdfHeader());
        bdfWriter = new BdfWriter(bdfHeaderData.getFileToSave(), bdfHeaderData.getBdfHeader());
    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
//        if(bdfRecordsJoiner.addDataRecord(dataFrame)) {
            //TODO включить мозг насчет эксепшенов
            try {
                bdfWriter.writeDataRecord(dataFrame);
//                bdfWriter.writeDataRecord(bdfRecordsJoiner.getResultingDataRecord());
            } catch (IOException e) {
                e.printStackTrace();
            }
//        }
    }

    @Override
    public void onStopRecording() {
        bdfWriter.stopWriting(true);
    }
}
