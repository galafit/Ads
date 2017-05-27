package com.biorecorder.ads;

/**
 * Class-structure to store info about Ads-channels
 */
public class AdsChannelConfig {

    protected Divider divider = Divider.D1;
    protected boolean isEnabled = true;
    private Gain gain = Gain.G2;
    private CommutatorState commutatorState = CommutatorState.INPUT;
    protected boolean isLoffEnable = true;
    private boolean isRldSenseEnabled = false;
    private String name = "Channel";

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

    public CommutatorState getCommutatorState() {
        return (!isEnabled) ? CommutatorState.INPUT_SHORT : commutatorState;
    }

    public void setCommutatorState(CommutatorState commutatorState) {
        this.commutatorState = commutatorState;
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
                ", gain=" + gain +
                ", commutatorState=" + commutatorState +
                ", isLoffEnable=" + isLoffEnable +
                ", isRldSenseEnabled=" + isRldSenseEnabled +
                '}' + "\n";
    }
}
