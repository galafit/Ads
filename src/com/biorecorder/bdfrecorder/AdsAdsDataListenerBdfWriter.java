package com.biorecorder.bdfrecorder;


import com.biorecorder.ads.AdsChannelConfig;
import com.biorecorder.ads.AdsDataListener;
import com.biorecorder.bdfrecorder.exceptions.UserInfoRuntimeException;
import com.biorecorder.edflib.*;
import com.biorecorder.edflib.exceptions.EdfRepositoryNotFoundRuntimeException;
import com.biorecorder.edflib.filters.MovingAverageFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;



class AdsAdsDataListenerBdfWriter implements AdsDataListener {
    private static final Log log = LogFactory.getLog(AdsAdsDataListenerBdfWriter.class);
    private int numberOfFramesToJoin;
    private EdfFileWriter edfFileWriter;
    private File edfFile;
    private EdfWriter edfWriter;
    private boolean isStoped = false;

    AdsAdsDataListenerBdfWriter(BdfRecorderConfig bdfRecorderConfig) throws UserInfoRuntimeException {
        try {
            edfFile = bdfRecorderConfig.getFileToSave();
            edfFileWriter = new EdfFileWriter(edfFile);
            edfFileWriter.setDurationOfDataRecordsComputable(true);

            // join DataRecords to have data records length = 1 sec;
            numberOfFramesToJoin = bdfRecorderConfig.getAdsConfig().getSps().getValue() /
                    bdfRecorderConfig.getAdsConfig().getMaxDiv(); // 1 second duration of a data record in bdf edfFile
            EdfJoiner dataRecordsJoiner = new EdfJoiner(numberOfFramesToJoin, edfFileWriter);

            // apply MovingAveragePrefilter to ads channels to reduce 50HZ
            EdfSignalsFilter signalsFilter = new EdfSignalsFilter(dataRecordsJoiner);
            int numberOfAdsChannels = bdfRecorderConfig.getAdsConfig().getNumberOfAdsChannels();
            int sps = bdfRecorderConfig.getAdsConfig().getSps().getValue();
            int enableSignalsCounter = 0;
            for (int i = 0; i < numberOfAdsChannels; i++) {
                AdsChannelConfig channelConfiguration = bdfRecorderConfig.getAdsConfig().getAdsChannel(i);
                if (channelConfiguration.isEnabled()) {
                    if (bdfRecorderConfig.is50HzFilterEnabled(i)) {
                        int divider = channelConfiguration.getDivider().getValue();
                        signalsFilter.addSignalFilter(enableSignalsCounter, new MovingAverageFilter(sps / (divider * 50)));
                    }
                    enableSignalsCounter++;
                }
            }
            // delete helper Loff channel
            EdfSignalsRemover signalsRemover = new EdfSignalsRemover(signalsFilter);
            if (bdfRecorderConfig.getAdsConfig().isLoffEnabled()) {
                signalsRemover.removeSignal(bdfRecorderConfig.getHeaderConfig().getNumberOfSignals() - 1);
            }
            edfWriter = signalsRemover;
            edfWriter.setHeader(bdfRecorderConfig.getHeaderConfig());
        } catch (EdfRepositoryNotFoundRuntimeException e) {
            throw new UserInfoRuntimeException(e);
        }

    }

    int getNumberOfWrittenDataRecords() {
        return edfFileWriter.getNumberOfWrittenDataRecords();
    }

    File getEdfFile() {
        return edfFile;
    }

    @Override
    public void onDataReceived(int[] dataFrame)  {
        if(!isStoped) {
            try {
                edfWriter.writeDigitalSamples(dataFrame);
            } catch (Exception e) {
                log.error(e);
            }

        }
    }

    public void stop() {
        isStoped = true;
        edfWriter.close();
        // information about startRecordingTime, stopRecordingTime and actual DataRecordDuration
        log.info(edfFileWriter.getWritingInfo());
    }
}
