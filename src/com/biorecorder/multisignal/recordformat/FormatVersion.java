package com.biorecorder.multisignal.recordformat;

public enum FormatVersion {
    EDF_16BIT(2),
    BDF_24BIT(3),
    INT_32BIT(4);

    int numberOfBytesPerSample;

    FormatVersion(int numberOfBytesPerSample) {
        this.numberOfBytesPerSample = numberOfBytesPerSample;
    }

    public  int getNumberOfBytesPerSample() {
        return numberOfBytesPerSample;
    }
}

