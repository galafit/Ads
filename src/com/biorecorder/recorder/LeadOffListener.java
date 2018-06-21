package com.biorecorder.recorder;

/**
 * Created by galafit on 25/3/18.
 */
public interface LeadOffListener {
    /**
     * Disconnection bit mask gives TRUE if electrode is DISCONNECTED,
     * FALSE if electrode is CONNECTED and
     * NULL if channel is disabled (or work in mode different from "input")
     */
    public void onLeadOffMaskReceived(Boolean[] leadOffMask);
}
