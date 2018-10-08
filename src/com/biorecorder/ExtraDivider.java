package com.biorecorder;

/**
 * Created by galafit on 6/10/18.
 */
public enum ExtraDivider {
    D1(1),
    D2(2);

    private int value;

    private ExtraDivider(int value) {
        this.value = value;
    }

    public static ExtraDivider valueOf(int value) throws IllegalArgumentException {
        for (ExtraDivider divider : ExtraDivider.values()) {
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
}
