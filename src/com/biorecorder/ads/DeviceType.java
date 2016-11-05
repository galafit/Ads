package com.biorecorder.ads;



import static com.biorecorder.ads.Divider.*;

public enum DeviceType {
    ADS1298(8, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10}),

    ADS1292(2, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10});

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

    public Divider getMaxDiv(){
        return maxDiv;
    }

    public int getNumberOfAdsChannels() {
        return numberOfAdsChannels;
    }

    public Divider[] getChannelsAvailableDividers() {
        return channelsAvailableDividers;
    }

    public Divider[] getGetAccelerometerAvailableDividers() {
        return getAccelerometerAvailableDividers;
    }

    public AdsConfigurator getAdsConfigurator(){
        if(numberOfAdsChannels == 2){
            return new AdsConfigurator2Ch();
        }
        if(numberOfAdsChannels == 8) {
            return new AdsConfigurator8Ch();
        }
        throw new IllegalStateException("Number of Ads channel shoul be 2 or 8");
    }
}
