package com.biorecorder.ads;

/**
 * Listener for messages from Ads
 */
public interface MessageListener {
    public void onMessage(AdsMessageType messageType, String message);
}
