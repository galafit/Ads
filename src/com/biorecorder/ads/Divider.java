package com.biorecorder.ads;

/**
 * 
 */
public enum Divider {
    D1(1),
    D2(2),
    D5(5),
    D10(10);

    private int value;

    private Divider(int value) {
        this.value = value;
    }

    public static Divider valueOf(int value) throws IllegalArgumentException {
        for (Divider divider : Divider.values()) {
            if (divider.getValue() == value) {
                return divider;
            }
        }
        String msg = "Invalid Divider value: "+value;
        throw new IllegalArgumentException(msg);
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString(){
        return new Integer(value).toString();
    }

    public static Divider getMaxDivider() {
        return D10;
    }
}
