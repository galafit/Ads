package com.biorecorder.ads;

/**
 *
 */
public enum Commutator {
    INPUT(0),
    INPUT_SHORT(1),
    TEST_SIGNAL(5);

    private int registerBits;

    private Commutator(int registerBits) {
        this.registerBits = registerBits;
    }

    public int getRegisterBits(){
        return registerBits;
    }
}
