package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.CommutatorState;

/**
 * Created by galafit on 30/3/18.
 */
public enum RecordingMode {
    INPUT(CommutatorState.INPUT),
    INPUT_SHORT(CommutatorState.INPUT_SHORT),
    TEST_SIGNAL(CommutatorState.TEST_SIGNAL);

    private CommutatorState adsCommutatorState;

    RecordingMode(CommutatorState adsCommutatorState) {
        this.adsCommutatorState = adsCommutatorState;
    }

    public CommutatorState getAdsCommutatorState() {
        return adsCommutatorState;
    }

    public static RecordingMode valueOf(CommutatorState adsCommutatorState) throws IllegalArgumentException {
        for (RecordingMode recordingMode : RecordingMode.values()) {
            if(recordingMode.getAdsCommutatorState() == adsCommutatorState) {
                return recordingMode;
            }

        }
        String msg = "Invalid Ads CommutatorState: "+adsCommutatorState;
        throw new IllegalArgumentException(msg);
    }
}
