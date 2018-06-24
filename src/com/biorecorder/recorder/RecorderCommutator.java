package com.biorecorder.recorder;

import com.biorecorder.ads.Commutator;

/**
 * Created by galafit on 30/3/18.
 */
public enum RecorderCommutator {
    input(Commutator.INPUT),
    test_signal(Commutator.TEST_SIGNAL),
    input_short(Commutator.INPUT_SHORT);

    private Commutator adsCommutator;

    RecorderCommutator(Commutator adsCommutator) {
        this.adsCommutator = adsCommutator;
    }

    public Commutator getAdsCommutator() {
        return adsCommutator;
    }

    public static RecorderCommutator valueOf(Commutator adsCommutator) throws IllegalArgumentException {
        for (RecorderCommutator recorderCommutator : RecorderCommutator.values()) {
            if(recorderCommutator.getAdsCommutator() == adsCommutator) {
                return recorderCommutator;
            }

        }
        String msg = "Invalid commutator: "+ adsCommutator;
        throw new IllegalArgumentException(msg);
    }
}
