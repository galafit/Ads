package com.biorecorder.ads;

/**
 * Serves to send and share data between threads
 */
public class AdsState {
    private  volatile boolean isActive;
    private  volatile AdsType adsType;
    private volatile boolean isStoped;
    private volatile boolean isDataComing;
    private volatile boolean[] loffMask;

    public synchronized boolean isDataComing() {
        return isDataComing;
    }

    public synchronized void setDataComing(boolean dataComing) {
        isDataComing = dataComing;
    }

    public synchronized boolean isStoped() {
        return isStoped;
    }

    public synchronized void setStoped(boolean stoped) {
        isStoped = stoped;
    }

    public synchronized AdsType getAdsType() {
        return adsType;
    }

    public synchronized void setAdsType(AdsType adsType) {
        this.adsType = adsType;
    }

    public synchronized boolean isActive() {
        return isActive;
    }

    public synchronized void setActive(boolean active) {
        isActive = active;
    }

    public boolean[] getLeadOffMask() {
        return loffMask;
    }

    public void setLoffMask(boolean[] loffMask) {
        this.loffMask = loffMask;
    }
}
