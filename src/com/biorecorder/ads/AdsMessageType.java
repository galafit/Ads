package com.biorecorder.ads;

/**
 * Messages from Ads and frame decoder
 */
public enum AdsMessageType {
    HELLO,
    STOP_RECORDING,
    LOW_BATTERY,
    ADS_2_CHANNELS,
    ADS_8_CHANNELS,
    FIRMWARE,
    TX_FAIL,
    UNKNOWN,
    FRAME_BROKEN;
}
