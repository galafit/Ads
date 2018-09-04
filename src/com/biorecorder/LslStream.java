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

    private int numberOfAdsChannels;
    private int numberOfAccChannels;
    private int numberOfAllChannels;
    private int accFactor = 1;
    private int adsChannelFactor = 1;
    private int numberOfLslRecords;

    /**
     * @throws IllegalArgumentException if channels have different frequencies or
     * all channels and accelerometer are disabled
     */
    public LslStream(RecordConfig dataRecordConfig, int accChannelsNumber) throws IllegalArgumentException {
        this.dataRecordConfig = dataRecordConfig;
        numberOfAccChannels = accChannelsNumber;
        numberOfAllChannels = dataRecordConfig.signalsCount();
        numberOfAdsChannels = numberOfAllChannels - numberOfAccChannels;

        int numberOfAdsChSamples = 0;
        int numberOfAccChSamples = 0;

        for (int i = 0; i < numberOfAdsChannels; i++) {
            if(numberOfAdsChSamples == 0) {
               numberOfAdsChSamples = dataRecordConfig.getNumberOfSamplesInEachDataRecord(i);
            } else {
                if (numberOfAdsChSamples != dataRecordConfig.getNumberOfSamplesInEachDataRecord(i)) {
                    String errMsg = "Channels frequencies must be the same";
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }

        if(numberOfAccChannels > 0) {
            numberOfAccChSamples = dataRecordConfig.getNumberOfSamplesInEachDataRecord(numberOfAllChannels - 1);
        }

        int maxNumberOfSamplesInRecord;
        if(numberOfAccChannels == 0) { // accelerometer disabled
            maxNumberOfSamplesInRecord = numberOfAdsChSamples;
        } else if(numberOfAdsChannels == 0) { // all ads channels disabled
            maxNumberOfSamplesInRecord = numberOfAccChSamples;
        } else if(numberOfAccChannels > 0 && numberOfAdsChannels > 0) {
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
            lslRecords.add(new float[numberOfAllChannels]);
        }

        int channelCount = 0;
        int sampleCount = 0;
        int lslRecordCount;
        for (int i = 0; i < dataRecord.length; i++) {
            if (channelCount < numberOfAdsChannels) {
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
