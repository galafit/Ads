package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsState1;


/**
 * Created by galafit on 12/4/18.
 */
public enum RecorderState {
    RECORDING(AdsState1.RECORDING),
    STOPPED(AdsState1.STOPPED),
    UNDEFINED(AdsState1.UNDEFINED);

    private AdsState1 adsState;

    RecorderState(AdsState1 adsState) {
        this.adsState = adsState;
    }

    AdsState1 getAdsState() {
        return adsState;
    }

    public static RecorderState valueOf(AdsState1 adsState) throws IllegalArgumentException {
        for (RecorderState recorderState : RecorderState.values()) {
            if(recorderState.getAdsState() == adsState) {
                return recorderState;
            }
        }
        String msg = "Invalid Ads State: "+adsState;
        throw new IllegalArgumentException(msg);
    }
}
