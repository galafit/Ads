package com.biorecorder.ads;

/**
 * Created by galafit on 7/6/18.
 */
class AdsChannelConfig {
    private String name = "Channel";
    private Divider divider = Divider.D1;
    private Gain gain = Gain.G2;
    private Commutator commutator = Commutator.INPUT;
    private boolean isEnabled = true;
    private boolean isLoffEnable = false;
    private boolean isRldSenseEnabled = false;

    public AdsChannelConfig() {
    }

    public AdsChannelConfig(AdsChannelConfig configToCopy) {
        name = configToCopy.name;
        divider = configToCopy.divider;
        isEnabled = configToCopy.isEnabled;
        gain = configToCopy.gain;
        commutator = configToCopy.commutator;
        isLoffEnable = configToCopy.isLoffEnable;
        isRldSenseEnabled = configToCopy.isRldSenseEnabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLoffEnable(boolean loffEnable) {
        isLoffEnable = loffEnable;
    }

    public void setRldSenseEnabled(boolean rldSenseEnabled) {
        isRldSenseEnabled = rldSenseEnabled;
    }

    public boolean isLoffEnable() {
        return isLoffEnable;
    }

    public boolean isRldSenseEnabled() {
        return isEnabled ? isRldSenseEnabled : false;
    }

    public Gain getGain() {
        return gain;
    }

    public void setGain(Gain gain) {
        this.gain = gain;
    }

    public Commutator getCommutator() {
        return (!isEnabled) ? Commutator.INPUT_SHORT : commutator;
    }

    public void setCommutator(Commutator commutator) {
        this.commutator = commutator;
    }

    public Divider getDivider() {
        return divider;
    }

    public void setDivider(Divider divider) {
        this.divider = divider;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public String toString() {
        return "AdsChannelConfig{" +
                "divider=" + divider +
                ", isEnabled=" + isEnabled +
                ", calculateGain=" + gain +
                ", commutator=" + commutator +
                ", isLoffEnable=" + isLoffEnable +
                ", isRldSenseEnabled=" + isRldSenseEnabled +
                '}' + "\n";
    }
}

