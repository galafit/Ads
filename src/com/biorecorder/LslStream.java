package com.biorecorder;

import com.biorecorder.dataformat.RecordConfig;
import edu.ucsd.sccn.LSL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 * Class thread safe
 */
public class LslStream implements RecordStream {
    private static final Log log = LogFactory.getLog(LslStream.class);
    private LSL.StreamInfo info;
    private LSL.StreamOutlet outlet;
    private RecordConfig dataRecordConfig;

    private int adsChannelsCount;
    private int channelsCount;
    private int accFactor = 1;
    private int adsChannelFactor = 1;
    private int numberOfLslRecords;
    private int dataRecordLength = 0;

    /**
     * @throws IllegalArgumentException if channels have different frequencies or
     * all channels and accelerometer are disabled
     */
    public LslStream(RecordConfig dataRecordConfig,int adsChannelsCount, int accChannelsCount) throws IllegalArgumentException {
        this.dataRecordConfig = dataRecordConfig;
        this.adsChannelsCount = adsChannelsCount;
        channelsCount = adsChannelsCount + accChannelsCount;

        int numberOfAdsChSamples = 0;
        int numberOfAccChSamples = 0;


        for (int i = 0; i < adsChannelsCount; i++) {
            if(numberOfAdsChSamples == 0) {
               numberOfAdsChSamples = dataRecordConfig.getNumberOfSamplesInEachDataRecord(i);
            } else {
                if (numberOfAdsChSamples != dataRecordConfig.getNumberOfSamplesInEachDataRecord(i)) {
                    String errMsg = "Channels frequencies must be the same";
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }

        if(accChannelsCount > 0) {
            numberOfAccChSamples = dataRecordConfig.getNumberOfSamplesInEachDataRecord(channelsCount - 1);
        }

        dataRecordLength = numberOfAdsChSamples * adsChannelsCount + numberOfAccChSamples * accChannelsCount;

        // las need equal frequencies for all channels.
        // So if numberOfAccChSamples != numberOfAdsChSamples we
        // will add additional samples to make
        // ads and acc numbers of samples in each data record equals

        int maxNumberOfSamplesInRecord;
        if(accChannelsCount == 0) { // accelerometer disabled
            maxNumberOfSamplesInRecord = numberOfAdsChSamples;
        } else if(adsChannelsCount == 0) { // all ads channels disabled
            maxNumberOfSamplesInRecord = numberOfAccChSamples;
        } else if(accChannelsCount > 0 && adsChannelsCount > 0) {
            maxNumberOfSamplesInRecord = Math.max(numberOfAccChSamples, numberOfAdsChSamples);
            accFactor = maxNumberOfSamplesInRecord / numberOfAccChSamples;
            adsChannelFactor = maxNumberOfSamplesInRecord / numberOfAdsChSamples;
        } else {
            String errMsg = "All channels and accelerometer are disabled";
            throw new IllegalArgumentException(errMsg);
        }


        int maxFrequency = (int)Math.round(maxNumberOfSamplesInRecord / dataRecordConfig.getDurationOfDataRecord());
        info = new LSL.StreamInfo("BioSemi", "EEG", dataRecordConfig.signalsCount(), maxFrequency, LSL.ChannelFormat.float32, "myuid324457");
        outlet = new LSL.StreamOutlet(info);

        numberOfLslRecords = maxNumberOfSamplesInRecord;

        log.info("MatlabDataListener initialization. Number of enabled channels = " + dataRecordConfig.signalsCount() +
                ". Frequency = " + maxFrequency + ". Number of samples in BDF data record = " + maxNumberOfSamplesInRecord);
    }

    @Override
    public synchronized void writeRecord(int[] dataRecord) {
        // convert one "edf record" to the list of multiple "mathlab records"
        // Mathlab record structure: one sample per every ads channel and accelerometer channel
        ArrayList<float[]> lslRecords = new ArrayList<>(numberOfLslRecords);

        for (int i = 0; i < numberOfLslRecords; i++) {
            lslRecords.add(new float[channelsCount]);
        }

        int channelCount = 0;
        int sampleCount = 0;
        int lslRecordCount;
        for (int i = 0; i < dataRecordLength; i++) {
            if (channelCount < adsChannelsCount) {
                for (int j = 0; j < adsChannelFactor; j++) {
                    lslRecordCount = sampleCount * adsChannelFactor + j;
                    lslRecords.get(lslRecordCount)[channelCount] = (float) RecordConfig.digitalToPhysical(dataRecordConfig, channelCount, dataRecord[i]);
                }
                lslRecordCount = sampleCount;
                lslRecords.get(lslRecordCount)[channelCount] = (float) RecordConfig.digitalToPhysical(dataRecordConfig, channelCount, dataRecord[i]);
            } else {
                for (int j = 0; j < accFactor; j++) {
                    lslRecordCount = sampleCount * accFactor + j;
                    lslRecords.get(lslRecordCount)[channelCount] = (float) RecordConfig.digitalToPhysical(dataRecordConfig, channelCount, dataRecord[i]);
                }
            }

            sampleCount++;
            if (sampleCount == dataRecordConfig.getNumberOfSamplesInEachDataRecord(channelCount)) {
                sampleCount = 0;
                channelCount++;
            }
        }

        for (float[] mathlabRecord : lslRecords) {
            outlet.push_sample(mathlabRecord);
        }
    }

   @Override
    public synchronized void close() {
        outlet.close();
        info.destroy();
    }
}
