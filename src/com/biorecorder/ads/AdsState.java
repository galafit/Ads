package com.biorecorder.ads;

/**
 * Serves to send and share data between threads
 */
public class AdsState {
    private  boolean isActive;
    private  DeviceType deviceType;
    private boolean isStoped;

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
