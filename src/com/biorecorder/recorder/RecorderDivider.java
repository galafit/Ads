package com.biorecorder.recorder;

import com.biorecorder.ads.Divider;

/**
 * Created by galafit on 8/6/18.
 */
public enum RecorderDivider {
    D1(Divider.D1),
    D2(Divider.D2),
    D5(Divider.D5),
    D10(Divider.D10),
    D20(Divider.D10, 2);


    private Divider adsDivider;
    private int extraDivider = 1;

    RecorderDivider(Divider adsDivider) {
        this.adsDivider = adsDivider;
    }
    RecorderDivider(Divider adsDivider, int extraDivider) {
        this.adsDivider = adsDivider;
        this.extraDivider = extraDivider;
    }

    public Divider getAdsDivider() {
        return adsDivider;
    }

    public int getExtraDivider() {
        return extraDivider;
    }

    public int getValue() {
        return adsDivider.getValue() * extraDivider;
    }

    public static RecorderDivider valueOf(int divider, int extraDivider) {
        try{
            return valueOf(divider * extraDivider);
        } catch (IllegalArgumentException ex) {
            String msg = "Invalid divider values. Ads Divider = "+divider + ", Extra Divider = "+extraDivider;
            throw new IllegalArgumentException(msg);
        }
    }

    public static RecorderDivider valueOf(int divider) {
        Divider adsMaxDivider = Divider.getMaxDivider();
        if(divider <= adsMaxDivider.getValue()) {
            for (RecorderDivider recorderDivider : RecorderDivider.values()) {
                if(recorderDivider.getAdsDivider().getValue() == divider) {
                    return recorderDivider;
                }
            }
        } else {
            if(divider % adsMaxDivider.getValue() == 0) {
                int extraDivider = divider / adsMaxDivider.getValue();
                for (RecorderDivider recorderDivider : RecorderDivider.values()) {
                    if(recorderDivider.getExtraDivider() == extraDivider) {
                        return recorderDivider;
                    }
                }
            }
        }

        String msg = "Invalid divider value: "+divider;
        throw new IllegalArgumentException(msg);
    }


    @Override
    public String toString(){
        return new Integer(getValue()).toString();
    }

}
