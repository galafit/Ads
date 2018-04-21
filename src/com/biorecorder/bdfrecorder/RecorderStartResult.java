package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsStartResult;

public enum RecorderStartResult {
    SUCCESS(AdsStartResult.SUCCESS, AdsStartResult.SUCCESS.getMessage()),
    ALREADY_RECORDING(AdsStartResult.ALREADY_RECORDING, AdsStartResult.ALREADY_RECORDING.getMessage()),
    FAILED_TO_SEND_COMMAND(AdsStartResult.FAILED_TO_SEND_COMMAND, AdsStartResult.FAILED_TO_SEND_COMMAND.getMessage()),
    WRONG_DEVICE_TYPE(AdsStartResult.WRONG_DEVICE_TYPE, AdsStartResult.WRONG_DEVICE_TYPE.getMessage());

    private AdsStartResult adsOperationResult;
    private String message;

    RecorderStartResult(AdsStartResult adsOperationResult, String message) {
        this.adsOperationResult = adsOperationResult;
        this.message = message;
    }

    public boolean isSuccess() {
        return adsOperationResult.isSuccess();
    }

    public String getMessage() {
        return message;
    }


    AdsStartResult getAdsOperationResult() {
        return adsOperationResult;
    }

    public static RecorderStartResult valueOf(AdsStartResult adsOperationResult) throws IllegalArgumentException {
        for (RecorderStartResult recorderOperationResult : RecorderStartResult.values()) {
            if(recorderOperationResult.getAdsOperationResult() == adsOperationResult) {
                return recorderOperationResult;
            }
        }
        String msg = "Invalid AdsStartResult: "+adsOperationResult;
        throw new IllegalArgumentException(msg);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
