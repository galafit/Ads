package com.biorecorder.ads;



import static com.biorecorder.ads.Divider.*;

public enum DeviceType {
    ADS_8(8, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10}),

    ADS_2(2, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10});

    private int numberOfAdsChannels;
    private Divider[] channelsAvailableDividers;
    private Divider[] getAccelerometerAvailableDividers;
    private Divider maxDiv;
    private AdsConfigurator adsConfigurator;

    DeviceType(int numberOfAdsChannels, Divider maxDiv, Divider[] channelsAvailableDividers, Divider[] getAccelerometerAvailableDividers) {
        this.maxDiv = maxDiv;
        this.numberOfAdsChannels = numberOfAdsChannels;
        this.channelsAvailableDividers = channelsAvailableDividers;
        this.getAccelerometerAvailableDividers = getAccelerometerAvailableDividers;

        if(numberOfAdsChannels == 2){
            adsConfigurator = new AdsConfigurator2Ch();
        } else if(numberOfAdsChannels == 8) {
            adsConfigurator =  new AdsConfigurator8Ch();
        } else {
            throw new IllegalStateException("Number of Ads channel should be 2 or 8");
        }
    }

    Divider getMaxDiv(){
        return maxDiv;
    }

    int getNumberOfAdsChannels() {
        return numberOfAdsChannels;
    }

    Divider[] getChannelsAvailableDividers() {
        return channelsAvailableDividers;
    }

    Divider[] getGetAccelerometerAvailableDividers() {
        return getAccelerometerAvailableDividers;
    }

    byte[] getAdsConfigurationCommand(AdsConfig adsConfig){
       return  adsConfigurator.getAdsConfigurationCommand(adsConfig);
    }
}
