package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.Sps;

/**
 * Created by galafit on 1/4/18.
 */
public enum RecorderSampleRate {
    S500(Sps.S500),
    S1000(Sps.S1000),
    S2000(Sps.S2000);

    private Sps adsSps;

    RecorderSampleRate(Sps adsSps) {
        this.adsSps = adsSps;
    }

    public Sps getAdsSps() {
        return adsSps;
    }

    public static RecorderSampleRate valueOf(Sps adsSps) throws IllegalArgumentException {
        for (RecorderSampleRate sampleRate : RecorderSampleRate.values()) {
            if(sampleRate.getAdsSps() == adsSps) {
                return sampleRate;
            }

        }
        String msg = "Invalid Ads sps: "+adsSps;
        throw new IllegalArgumentException(msg);
    }

    public static RecorderSampleRate valueOf(int sampleRateValue) {
        return RecorderSampleRate.valueOf(Sps.valueOf(sampleRateValue));
    }

    public int getValue(){
        return adsSps.getValue();
    }

    @Override
    public String toString(){
        return new Integer(getValue()).toString();
    }
}
