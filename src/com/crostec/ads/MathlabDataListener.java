package com.crostec.ads;

import com.crostec.ads.*;
import edu.ucsd.sccn.LSL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 *
 */
public class MathlabDataListener implements AdsDataListener {

    private static final Log log = LogFactory.getLog(MathlabDataListener.class);
    private AdsConfiguration adsConfiguration;
    LSL.StreamInfo info;
    LSL.StreamOutlet outlet;
    int numberOfEnabledChannels;
    int nrOfSamplesInOneChannel;

    public MathlabDataListener(AdsConfiguration adsConfiguration) {
        this.adsConfiguration = adsConfiguration;
        List<Integer> dividers = AdsUtils.getDividersForActiveChannels(adsConfiguration);
        numberOfEnabledChannels = dividers.size();
        int divider = dividers.get(0);
        int maxDiv = adsConfiguration.getDeviceType().getMaxDiv().getValue();
        nrOfSamplesInOneChannel = maxDiv / divider;
        int frequency = adsConfiguration.getSps().getValue()/divider;
        info = new LSL.StreamInfo("BioSemi", "EEG", numberOfEnabledChannels, frequency, LSL.ChannelFormat.float32, "myuid324457");
        log.debug("MatlabDataListener initialization. Number of enabled channels = " + numberOfEnabledChannels +
        ". Frequency = " +  frequency + ". Number of samples in BDF data record = " +  nrOfSamplesInOneChannel);
        outlet = new LSL.StreamOutlet(info);
    }


    @Override
    public synchronized void onAdsDataReceived(int[] dataFrame) {
//        System.out.println(dataFrame[0]+ "    " + dataFrame[1]+ "    " + dataFrame[2]+ "    " + dataFrame[3]+ "    ");
        for (int j = 0; j < nrOfSamplesInOneChannel; j++) {
            float [] mathlabDataFrame = new float[numberOfEnabledChannels];
            for (int i = 0; i < numberOfEnabledChannels; i++) {
                mathlabDataFrame[i] = dataFrame[i * nrOfSamplesInOneChannel + j];
            }
            outlet.push_sample(mathlabDataFrame);
//            System.out.println(mathlabDataFrame[0] + "          " + mathlabDataFrame[1]);
        }
    }

    @Override
    public synchronized void onStopRecording() {
        outlet.close();
        info.destroy();
    }

    /**
     * Checks if frequencies for all channels are the same
     *
     * @return
     */
    public boolean isFrequencyTheSame() {
        List<AdsChannelConfiguration> channelConfigurations = adsConfiguration.getAdsChannels();
        int divider = 0;
        for (AdsChannelConfiguration channelConfiguration : channelConfigurations) {
            if (!channelConfiguration.isEnabled()) {
                continue;
            }
            int nextDivider = channelConfiguration.getDivider().getValue();
            if (nextDivider == 0) {
                continue;
            } else if (divider == 0) {
                divider = nextDivider;
            } else if (divider != nextDivider) {
                return false;
            }
        }
        return true;
    }
}
