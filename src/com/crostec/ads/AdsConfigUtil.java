package com.crostec.ads;

/**
 * Utility class to save AdsConfiguration to properties file and read saved data
 */
public class AdsConfigUtil {

    public static final int NUMBER_OF_CHANNELS = 8;
    AdsConfigProperties adsConfigProperties = new AdsConfigProperties();

    public  AdsConfiguration readConfiguration() {
        AdsConfiguration adsConfiguration = new AdsConfiguration();
        adsConfiguration.setSps(adsConfigProperties.getSps());
        adsConfiguration.setComPortName(adsConfigProperties.getComPortName());
        adsConfiguration.setAccelerometerEnabled(adsConfigProperties.isAccelerometerEnabled());
        adsConfiguration.setAccelerometerDivider(adsConfigProperties.getAccelerometerDivider());
        for (int chNum = 0; chNum < NUMBER_OF_CHANNELS; chNum++) {
            AdsChannelConfiguration adsChannelConfiguration = new AdsChannelConfiguration();
            adsChannelConfiguration.setDivider(adsConfigProperties.getChannelDivider(chNum));
            adsChannelConfiguration.setGain(adsConfigProperties.getChannelGain(chNum));
            adsChannelConfiguration.setCommutatorState(adsConfigProperties.getChannelCommutatorState(chNum));
            adsChannelConfiguration.setLoffEnable(adsConfigProperties.isChannelLoffEnable(chNum));
            adsChannelConfiguration.setRldSenseEnabled(adsConfigProperties.isChannelRldSenseEnable(chNum));
            adsChannelConfiguration.setEnabled(adsConfigProperties.isChannelEnabled(chNum));
            adsConfiguration.getAdsChannels().add(adsChannelConfiguration);
        }
        return adsConfiguration;
    }

    public void saveAdsConfiguration(AdsConfiguration adsConfiguration) {
        adsConfigProperties.setSps(adsConfiguration.getSps());
        adsConfigProperties.setComPortName(adsConfiguration.getComPortName());
        adsConfigProperties.setAccelerometerDivider(adsConfiguration.getAccelerometerDivider());
        adsConfigProperties.setAccelerometerEnabled(adsConfiguration.isAccelerometerEnabled());
        for (int i = 0; i < NUMBER_OF_CHANNELS; i++) {
            AdsChannelConfiguration channel = adsConfiguration.getAdsChannels().get(i);
            adsConfigProperties.setChannelDivider(i, channel.getDivider());
            adsConfigProperties.setChannelGain(i, channel.getGain());
            adsConfigProperties.setChannelCommutatorState(i, channel.getCommutatorState());
            adsConfigProperties.setChannelEnabled(i, channel.isEnabled());
            adsConfigProperties.setChannelLoffEnabled(i, channel.isLoffEnable());
            adsConfigProperties.setChannelRldSenseEnabled(i, channel.isRldSenseEnabled());
        }
        adsConfigProperties.save();
    }

}
