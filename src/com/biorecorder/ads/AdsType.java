package com.biorecorder.ads;


public enum AdsType {
    ADS_2(2),
    ADS_8(8);

    private int numberOfAdsChannels;

    private AdsConfigurator adsConfigurator;

    private AdsType(int numberOfAdsChannels) {
        this.numberOfAdsChannels = numberOfAdsChannels;

        if(numberOfAdsChannels == 2){
            adsConfigurator = new AdsConfigurator2Ch();
        } else if(numberOfAdsChannels == 8) {
            adsConfigurator =  new AdsConfigurator8Ch();
        } else {
            String msg = "Invalid Ads channels count: "+numberOfAdsChannels+ ". Number of Ads channels may be 2 or 8";
            throw new IllegalArgumentException(msg);
        }
    }

    public static Divider getAccelerometerAvailableDivider() {
        return Divider.D10;
    }

    public static AdsType valueOf(int channelsCount) throws IllegalArgumentException {
        for (AdsType adsType : AdsType.values()) {
            if(adsType.getAdsChannelsCount() == channelsCount) {
                return adsType;
            }

        }
        String msg = "Invalid Ads channels count: "+channelsCount+ ". Number of Ads channels may be 2 or 8";
        throw new IllegalArgumentException(msg);
    }

    public int getAdsChannelsCount() {
        return numberOfAdsChannels;
    }

    byte[] getAdsConfigurationCommand(AdsConfig adsConfig){
       return  adsConfigurator.getAdsConfigurationCommand(adsConfig);
    }

    public static int getMaxChannelsCount() {
        return 8;
    }

}
