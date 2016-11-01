package com.crostec.ads;

/**
 * Utility class to save AdsConfiguration to properties file and read saved data
 */
public class AdsConfigUtil {

    AdsConfigProperties adsConfigProperties = new AdsConfigProperties();

    public  AdsConfiguration readConfiguration() {
        AdsConfiguration adsConfiguration = new AdsConfiguration();
        adsConfiguration.setDeviceType(adsConfigProperties.getDeviceType());
        adsConfiguration.setSps(adsConfigProperties.getSps());
        adsConfiguration.setComPortName(adsConfigProperties.getComPortName());
        adsConfiguration.setAccelerometerEnabled(adsConfigProperties.isAccelerometerEnabled());
        adsConfiguration.setBatteryVoltageMeasureEnabled(adsConfigProperties.isBatteryMeasureEnabled());
        adsConfiguration.setAccelerometerDivider(adsConfigProperties.getAccelerometerDivider());
        adsConfiguration.setDirectoryToSave(adsConfigProperties.getDirectoryToSave());
        for (int chNum = 0; chNum < adsConfigProperties.getDeviceType().getNumberOfAdsChannels(); chNum++) {
            AdsChannelConfiguration adsChannelConfiguration = new AdsChannelConfiguration();
            adsChannelConfiguration.setDivider(adsConfigProperties.getChannelDivider(chNum));
            adsChannelConfiguration.setGain(adsConfigProperties.getChannelGain(chNum));
            adsChannelConfiguration.setCommutatorState(adsConfigProperties.getChannelCommutatorState(chNum));
            adsChannelConfiguration.setLoffEnable(adsConfigProperties.isChannelLoffEnable(chNum));
            adsChannelConfiguration.setRldSenseEnabled(adsConfigProperties.isChannelRldSenseEnable(chNum));
            adsChannelConfiguration.setEnabled(adsConfigProperties.isChannelEnabled(chNum));
            adsChannelConfiguration.set50HzFilterEnabled(adsConfigProperties.is50HzFilterEnabled(chNum));
            adsConfiguration.getAdsChannels().add(adsChannelConfiguration);
        }
        return adsConfiguration;
    }

    public void saveAdsConfiguration(AdsConfiguration adsConfiguration) {
        adsConfigProperties.setSps(adsConfiguration.getSps());
        adsConfigProperties.setComPortName(adsConfiguration.getComPortName());
        adsConfigProperties.setAccelerometerDivider(adsConfiguration.getAccelerometerDivider());
        adsConfigProperties.setAccelerometerEnabled(adsConfiguration.isAccelerometerEnabled());
        adsConfigProperties.setBatteryMeasureEnabled(adsConfiguration.isBatteryVoltageMeasureEnabled());
        adsConfigProperties.setDirectoryToSave(adsConfiguration.getDirectoryToSave());
        for (int i = 0; i < adsConfiguration.getDeviceType().getNumberOfAdsChannels(); i++) {
            AdsChannelConfiguration channel = adsConfiguration.getAdsChannels().get(i);
            adsConfigProperties.setChannelDivider(i, channel.getDivider());
            adsConfigProperties.setChannelGain(i, channel.getGain());
            adsConfigProperties.setChannelCommutatorState(i, channel.getCommutatorState());
            adsConfigProperties.setChannelEnabled(i, channel.isEnabled());
            adsConfigProperties.set50HzFilterEnabled(i, channel.is50HzFilterEnabled());
            adsConfigProperties.setChannelLoffEnabled(i, channel.isLoffEnable());
            adsConfigProperties.setChannelRldSenseEnabled(i, channel.isRldSenseEnabled());
        }
        adsConfigProperties.save();
    }

}
