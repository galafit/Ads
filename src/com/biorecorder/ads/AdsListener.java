package com.biorecorder.ads;

/**
 *
 */
public interface AdsListener {
    public void onAdsDataReceived(int[] dataFrame);
    public void onStopRecording();

}
