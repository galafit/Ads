package com.biorecorder.recorder;

import com.biorecorder.ads.Divider;

/**
 * Created by galafit on 8/6/18.
 */
public enum RecorderDivider {
    D1(Divider.D1),
    D2(Divider.D2),
    D5(Divider.D5),
    D10(Divider.D10);

    private Divider adsDivider;

    RecorderDivider(Divider adsDivider) {
        this.adsDivider = adsDivider;
    }

    public Divider getAdsDivider() {
        return adsDivider;
    }

    public int getValue() {
        return adsDivider.getValue();
    }

    public static RecorderDivider valueOf(Divider adsDivider) throws IllegalArgumentException {
        for (RecorderDivider recorderDivider : RecorderDivider.values()) {
            if(recorderDivider.getAdsDivider() == adsDivider) {
                return recorderDivider;
            }
        }
        String msg = "Invalid divider: "+adsDivider;
        throw new IllegalArgumentException(msg);
    }

    public static RecorderDivider valueOf(int divider) {
        return RecorderDivider.valueOf(Divider.valueOf(divider));
    }


    @Override
    public String toString(){
        return new Integer(getValue()).toString();
    }

}
