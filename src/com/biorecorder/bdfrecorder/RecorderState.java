package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsState;


/**
 * Created by galafit on 12/4/18.
 */
public enum RecorderState {
    RECORDING(AdsState.RECORDING),
    STOPPED(AdsState.STOPPED),
    UNDEFINED(AdsState.UNDEFINED);

    private AdsState adsState;

    RecorderState(AdsState adsState) {
        this.adsState = adsState;
    }

    AdsState getAdsState() {
        return adsState;
    }

    public static RecorderState valueOf(AdsState adsState) throws IllegalArgumentException {
        for (RecorderState recorderState : RecorderState.values()) {
            if(recorderState.getAdsState() == adsState) {
                return recorderState;
            }
        }
        String msg = "Invalid Ads State: "+adsState;
        throw new IllegalArgumentException(msg);
    }
}
