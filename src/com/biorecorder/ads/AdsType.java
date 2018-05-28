package com.biorecorder.ads;



import static com.biorecorder.ads.Divider.*;

public enum AdsType {
    ADS_2(2, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10}),
    ADS_8(8, D10, new Divider[]{D1, D2, D5, D10}, new Divider[]{D10});

    private int numberOfAdsChannels;
    private Divider[] channelsAvailableDividers;
    private Divider[] getAccelerometerAvailableDividers;
    private Divider maxDiv;
    private AdsConfigurator adsConfigurator;

    private AdsType(int numberOfAdsChannels, Divider maxDiv, Divider[] channelsAvailableDividers, Divider[] getAccelerometerAvailableDividers) {
        this.maxDiv = maxDiv;
        this.numberOfAdsChannels = numberOfAdsChannels;
        this.channelsAvailableDividers = channelsAvailableDividers;
        this.getAccelerometerAvailableDividers = getAccelerometerAvailableDividers;

        if(numberOfAdsChannels == 2){
            adsConfigurator = new AdsConfigurator2Ch();
        } else if(numberOfAdsChannels == 8) {
            adsConfigurator =  new AdsConfigurator8Ch();
        } else {
            String msg = "Invalid Ads channels count: "+numberOfAdsChannels+ ". Number of Ads channels should be 2 or 8";
            throw new IllegalArgumentException(msg);
        }
    }

    public static AdsType valueOf(int channelsCount) throws IllegalArgumentException {
        for (AdsType adsType : AdsType.values()) {
            if(adsType.getAdsChannelsCount() == channelsCount) {
                return adsType;
            }

        }
        String msg = "Invalid Ads channels count: "+channelsCount+ ". Number of Ads channels should be 2 or 8";
        throw new IllegalArgumentException(msg);
    }

    public int getAdsChannelsCount() {
        return numberOfAdsChannels;
    }

    Divider getMaxDiv(){
        return maxDiv;
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
