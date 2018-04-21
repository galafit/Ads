package com.biorecorder.ads;

/**
 * Listener of Ads events such as LOW_BATTERY, FRAME_BROKEN and so on
 */
public interface AdsEventsListener {
    public void handleLowButtery();
    public void handleStartCanceled();
    public void handleFrameBroken(String eventInfo);
}
