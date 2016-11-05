package com.biorecorder.ads;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AdsConfiguration {
    

    private Sps sps = Sps.S500;     // samples per second (sample rate)
    private ArrayList<AdsChannelConfiguration> adsChannels = new ArrayList<AdsChannelConfiguration>();
    private boolean isAccelerometerEnabled = true;
    private boolean isBatteryVoltageMeasureEnabled = false;
    private Divider accelerometerDivider = Divider.D10;
    private String comPortName = "COM1";
    private boolean isHighResolutionMode = true;
    private DeviceType deviceType;
    private int noiseDivider;
    private String directoryToSave;
    private boolean isAccelerometerOneChannelMode;

    public boolean isAccelerometerOneChannelMode() {
        return isAccelerometerOneChannelMode;
    }

    public void setAccelerometerOneChannelMode(boolean accelerometerOneChannelMode) {
        isAccelerometerOneChannelMode = accelerometerOneChannelMode;
    }

    public String getDirectoryToSave() {
        if(directoryToSave == null) {
            File recordsDir = new File(System.getProperty("user.dir"), "records");
            if(!recordsDir.exists() || !recordsDir.isDirectory()) {
                recordsDir.mkdir();
            }
          directoryToSave = recordsDir.getPath();
        }
        return directoryToSave;
    }

    public void setDirectoryToSave(String directoryToSave) {
        this.directoryToSave = directoryToSave;
    }

    public boolean isBatteryVoltageMeasureEnabled() {
        return isBatteryVoltageMeasureEnabled;
    }

    public void setBatteryVoltageMeasureEnabled(boolean batteryVoltageMeasureEnabled) {
        isBatteryVoltageMeasureEnabled = batteryVoltageMeasureEnabled;
    }

    public boolean isHighResolutionMode() {
        return isHighResolutionMode;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getComPortName() {
        return comPortName;
    }

    public void setComPortName(String comPortName) {
        this.comPortName = comPortName;
    }

    public List<AdsChannelConfiguration> getAdsChannels(){
        return adsChannels;
    }

    public void setAccelerometerEnabled(boolean accelerometerEnabled) {
        isAccelerometerEnabled = accelerometerEnabled;
    }

    public void setAccelerometerDivider(Divider accelerometerDivider) {
        this.accelerometerDivider = accelerometerDivider;
    }

    public Divider getAccelerometerDivider() {
        return accelerometerDivider;
    }

    public boolean isAccelerometerEnabled() {
        return isAccelerometerEnabled;
    }

    public boolean isLoffEnabled() {
        for (AdsChannelConfiguration adsChannel : adsChannels) {
           if(adsChannel.isEnabled && adsChannel.isLoffEnable()){
               return true;
           }
        }
        return false;
    }

    public Sps getSps() {
        return sps;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (AdsChannelConfiguration adsChannel : adsChannels) {
            sb.append(adsChannel.toString());
            sb.append("\r");
        }
        return "AdsConfiguration{" +
                "sps=" + sps +
                ", isAccelerometerEnabled=" + isAccelerometerEnabled +
                ", accelerometerDivider=" + accelerometerDivider +
                ", comPortName='" + comPortName + '\'' +
                ", isHighResolutionMode=" + isHighResolutionMode +
                ", deviceType=" + deviceType +
                '}' + sb.toString();
    }

    public void setSps(Sps sps) {
        this.sps = sps;
    }

    public void setNoiseDivider(int noiseDivider) {
       this.noiseDivider = noiseDivider;
    }

    public int getNoiseDivider() {
        return noiseDivider;
    }
}
