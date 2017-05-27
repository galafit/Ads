package com.biorecorder.ads;

/**
 * Serves to send and share data between threads
 */
public class AdsState {
    private  volatile boolean isActive;
    private  volatile DeviceType deviceType;
    private volatile boolean isStoped;
    private volatile boolean isDataComing;

    public boolean isDataComing() {
        return isDataComing;
    }

    public void setDataComing(boolean dataComing) {
        isDataComing = dataComing;
    }

    public synchronized boolean isStoped() {
        return isStoped;
    }

    public synchronized void setStoped(boolean stoped) {
        isStoped = stoped;
    }

    public synchronized DeviceType getDeviceType() {
        return deviceType;
    }

    public synchronized void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public synchronized boolean isActive() {
        return isActive;
    }

    public synchronized void setActive(boolean active) {
        isActive = active;
    }
}
