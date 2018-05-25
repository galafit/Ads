package com.biorecorder.bdfrecorder.filters;

import com.biorecorder.bdfrecorder.dataformat.DataConfig;

/**
 * Wrapper around some DataConfig that permits
 * to change only some methods
 */
class DataConfigWrapper implements DataConfig {
    DataConfig innerConfig;

    public DataConfigWrapper(DataConfig innerConfig) {
        this.innerConfig = innerConfig;
    }

    @Override
    public double durationOfDataRecord() {
        return innerConfig.durationOfDataRecord();
    }

    @Override
    public int signalsCount() {
        return innerConfig.signalsCount();
    }


    @Override
    public int numberOfSamplesInEachDataRecord(int signalNumber) {
        return innerConfig.numberOfSamplesInEachDataRecord(signalNumber);
    }

    @Override
    public String label(int signalNumber) {
        return innerConfig.label(signalNumber);
    }

    @Override
    public String transducer(int signalNumber) {
        return innerConfig.transducer(signalNumber);
    }

    @Override
    public String prefiltering(int signalNumber) {
        return innerConfig.prefiltering(signalNumber);
    }

    @Override
    public String physicalDimension(int signalNumber) {
        return innerConfig.physicalDimension(signalNumber);
    }

    @Override
    public int digitalMin(int signalNumber) {
        return innerConfig.digitalMin(signalNumber);
    }

    @Override
    public int digitalMax(int signalNumber) {
        return innerConfig.digitalMax(signalNumber);
    }

    @Override
    public double physicalMin(int signalNumber) {
        return innerConfig.physicalMin(signalNumber);
    }

    @Override
    public double physicalMax(int signalNumber) {
        return innerConfig.physicalMax(signalNumber);
    }
}
