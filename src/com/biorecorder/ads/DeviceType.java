package com.biorecorder.ads;



import static com.biorecorder.ads.Divider.*;

public enum DeviceType {
    ADS_8channel(8, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10}),

    ADS_2channel(2, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10});

    private int numberOfAdsChannels;
    private Divider[] channelsAvailableDividers;
    private Divider[] getAccelerometerAvailableDividers;
    private Divider maxDiv;

    DeviceType(int numberOfAdsChannels, Divider maxDiv, Divider[] channelsAvailableDividers, Divider[] getAccelerometerAvailableDividers) {
        this.maxDiv = maxDiv;
        this.numberOfAdsChannels = numberOfAdsChannels;
        this.channelsAvailableDividers = channelsAvailableDividers;
        this.getAccelerometerAvailableDividers = getAccelerometerAvailableDividers;
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

    AdsConfigurator getAdsConfigurator(){
        if(numberOfAdsChannels == 2){
            return new AdsConfigurator2Ch();
        }
        if(numberOfAdsChannels == 8) {
            return new AdsConfigurator8Ch();
        }
        throw new IllegalStateException("Number of Ads channel should be 2 or 8");
    }
}
