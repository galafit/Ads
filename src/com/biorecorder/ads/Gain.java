package com.biorecorder.ads;

/**
 *
 */
public enum Gain {
    G1(0x10, 1),
    G2(0x20, 2),
    G3(0x30, 3),
    G4(0x40, 4),
    G6(0x00, 6),
    G8(0x50, 8),
    G12(0x60, 12);

    private int registerBits;
    private int value;

    Gain(int registerBits, int value) {
        this.registerBits = registerBits;
        this.value = value;
    }

    public static Gain valueOf(int gainValue) throws IllegalArgumentException {
        for (Gain gain : Gain.values()) {
            if (gain.getValue() == gainValue) {
                return gain;
            }
        }
        String msg = "Invalid Gain value: "+gainValue;
        throw new IllegalArgumentException(msg);
    }

    public int getRegisterBits() {
        return registerBits;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString(){
        return new Integer(value).toString();
    }
}
