package com.biorecorder.ads;

/**
 * Listener for messages from Ads and Frame decoder
 */
interface MessageListener {
    public void onMessage(AdsMessage message, String additionalInfo);
}
