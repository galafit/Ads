package com.biorecorder.recorder;


import com.biorecorder.ads.AdsType;
import com.biorecorder.ads.Divider;

/**
 * Created by galafit on 30/3/18.
 */
public enum RecorderType {
    Recorder_8(AdsType.ADS_8),
    Recorder_2(AdsType.ADS_2);

    private AdsType adsType;

    RecorderType(AdsType adsType) {
        this.adsType = adsType;
    }

    public RecorderDivider getAccelerometerAvailableDivider() {
        return RecorderDivider.valueOf(AdsType.getAccelerometerAvailableDivider());
    }

    public int getChannelsCount() {
        return adsType.getAdsChannelsCount();
    }

    public AdsType getAdsType() {
        return adsType;
    }

    public static RecorderType valueOf(AdsType adsType) throws IllegalArgumentException {
        for (RecorderType recorderType : RecorderType.values()) {
            if(recorderType.getAdsType() == adsType) {
                return recorderType;
            }
        }
        String msg = "Invalid device type: "+adsType;
        throw new IllegalArgumentException(msg);
    }

    @Override
    public String toString(){
        return  getChannelsCount() + " channels";
    }

    public static int getMaxChannelsCount() {
        return AdsType.getMaxChannelsCount();
    }

}
