package com.biorecorder.ads;

/**
 * Listener for messages from Ads and Frame decoder
 */
interface MessageListener {
    public void onMessageReceived(AdsMessage adsMessage, String additionalInfo);
}
