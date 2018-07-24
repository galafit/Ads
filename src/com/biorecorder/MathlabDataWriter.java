package com.biorecorder;

import com.biorecorder.dataformat.DataConfig;
import com.biorecorder.dataformat.DataListener;
import com.biorecorder.recorder.RecorderConfig;
import edu.ucsd.sccn.LSL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 * Created by galafit on 24/7/18.
 */
public class MathlabDataWriter implements DataListener {
    private static final Log log = LogFactory.getLog(MathlabDataWriter.class);
    private LSL.StreamInfo info;
    private LSL.StreamOutlet outlet;
    private DataConfig dataConfig;

    private int numberOfAdsChannels;
    private int numberOfAccChannels;
    private int numberOfAllChannels;
    private int accFactor = 1;
    private int adsChannelFactor = 1;
    private int numberOfMathlabRecords;

    /**
     * @throws IllegalArgumentException if channels have different frequencies or
     * all channels and accelerometer are disabled
     */
    public MathlabDataWriter(DataConfig dataConfig, RecorderConfig recorderConfig) throws IllegalArgumentException {
        this.dataConfig = dataConfig;
        int channelsDivider = -1;
        int accelerometerDivider = -1;

        for (int i = 0; i < recorderConfig.getChannelsCount(); i++) {
            if (recorderConfig.isChannelEnabled(i)) {
                numberOfAdsChannels++;
                if (channelsDivider == -1) {
                    channelsDivider = recorderConfig.getChannelDivider(i);
                } else {
                    if (channelsDivider != recorderConfig.getChannelDivider(i)) {
                        String errMsg = "Channels frequencies must be the same";
                        throw new IllegalArgumentException(errMsg);
                    }
                }
            }
        }

        if (recorderConfig.isAccelerometerEnabled()) {
            if (recorderConfig.isAccelerometerOneChannelMode()) {
                numberOfAccChannels = 1;
            } else {
                numberOfAccChannels = 3;
            }
            accelerometerDivider = recorderConfig.getAccelerometerDivider();
        }

        int divider;
        if(numberOfAccChannels == 0) {
            divider = channelsDivider;
        } else if(numberOfAdsChannels == 0) {
            divider = accelerometerDivider;
        } else if(numberOfAccChannels > 0 && numberOfAdsChannels > 0) {
            divider = Math.min(channelsDivider, accelerometerDivider);
            accFactor = accelerometerDivider / divider;
            adsChannelFactor = channelsDivider / divider;
        } else {
            String errMsg = "All channels and accelerometer are disabled";
            throw new IllegalArgumentException(errMsg);
        }


        int maxFrequency = recorderConfig.getSampleRate() / divider;
        int maxNumberOfSamplesInRecord = (int) Math.round(recorderConfig.getDurationOfDataRecord() * maxFrequency / divider);
        info = new LSL.StreamInfo("BioSemi", "EEG", dataConfig.signalsCount(), maxFrequency, LSL.ChannelFormat.float32, "myuid324457");
        outlet = new LSL.StreamOutlet(info);

        numberOfAllChannels = numberOfAccChannels + numberOfAdsChannels;
        numberOfMathlabRecords = maxNumberOfSamplesInRecord;

        log.debug("MatlabDataListener initialization. Number of enabled channels = " + dataConfig.signalsCount() +
                ". Frequency = " + maxFrequency + ". Number of samples in BDF data record = " + maxNumberOfSamplesInRecord);

    }

    @Override
    public void onDataReceived(int[] dataRecord) {
        // convert one "edf record" to the list of multiple "mathlab records"
        // Mathlab record structure: one sample per every channel and accelerometer
        ArrayList<float[]> mathlabRecords = new ArrayList<>(numberOfMathlabRecords);

        for (int i = 0; i < numberOfMathlabRecords; i++) {
            mathlabRecords.add(new float[numberOfAllChannels]);
        }

        int channelCount = 0;
        int sampleCount = 0;
        int mathlabRecordCount;
        for (int i = 0; i < dataRecord.length; i++) {
            if (channelCount <= numberOfAdsChannels) {
                for (int j = 0; j < adsChannelFactor; j++) {
                    mathlabRecordCount = sampleCount + j;
                    mathlabRecords.get(mathlabRecordCount)[channelCount] = (float) DataConfig.digitalToPhysical(dataConfig, channelCount, dataRecord[i]);
                }
                mathlabRecordCount = sampleCount;
                mathlabRecords.get(mathlabRecordCount)[channelCount] = (float) DataConfig.digitalToPhysical(dataConfig, channelCount, dataRecord[i]);
            } else {
                for (int j = 0; j < accFactor; j++) {
                    mathlabRecordCount = sampleCount + j;
                    mathlabRecords.get(mathlabRecordCount)[channelCount] = (float) DataConfig.digitalToPhysical(dataConfig, channelCount, dataRecord[i]);
                }
            }

            sampleCount++;
            if (sampleCount == dataConfig.getNumberOfSamplesInEachDataRecord(channelCount)) {
                sampleCount = 0;
                channelCount++;
            }
        }

        for (float[] mathlabRecord : mathlabRecords) {
            outlet.push_sample(mathlabRecord);
        }
    }

   // @Override
    public void onStopRecording() {
        outlet.close();
        info.destroy();
    }
}
