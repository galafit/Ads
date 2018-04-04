package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.Gain;

/**
 * Created by galafit on 31/3/18.
 */
public enum RecorderGain {
    G1(Gain.G1),
    G2(Gain.G2),
    G3(Gain.G3),
    G4(Gain.G4),
    G6(Gain.G6),
    G8(Gain.G8),
    G12(Gain.G12);

    private Gain adsGain;

    RecorderGain(Gain adsGain) {
        this.adsGain = adsGain;
    }

    public Gain getAdsGain() {
        return adsGain;
    }

    public int getValue() {
        return adsGain.getValue();
    }

    public static RecorderGain valueOf(Gain adsGain) throws IllegalArgumentException {
        for (RecorderGain recorderGain : RecorderGain.values()) {
            if(recorderGain.getAdsGain() == adsGain) {
                return recorderGain;
            }
        }
        String msg = "Invalid Ads Gain: "+adsGain;
        throw new IllegalArgumentException(msg);
    }

    public static RecorderGain valueOf(int gainValue) {
        return RecorderGain.valueOf(Gain.valueOf(gainValue));
    }


    @Override
    public String toString(){
        return new Integer(getValue()).toString();
    }
}
