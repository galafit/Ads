package com.biorecorder.filters;

import com.biorecorder.dataformat.DataConfig;

/**
 * Wrapper around some DataConfig that permits
 * to change only some methods
 */
class DataConfigWrapper implements DataConfig {
    final DataConfig inConfig;

    public DataConfigWrapper(DataConfig inConfig) {
        this.inConfig = inConfig;
    }

    @Override
    public double getDurationOfDataRecord() {
        return inConfig.getDurationOfDataRecord();
    }

    @Override
    public int signalsCount() {
        return inConfig.signalsCount();
    }


    @Override
    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return inConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
    }

    @Override
    public String getLabel(int signalNumber) {
        return inConfig.getLabel(signalNumber);
    }

    @Override
    public String getTransducer(int signalNumber) {
        return inConfig.getTransducer(signalNumber);
    }

    @Override
    public String getPrefiltering(int signalNumber) {
        return inConfig.getPrefiltering(signalNumber);
    }

    @Override
    public String getPhysicalDimension(int signalNumber) {
        return inConfig.getPhysicalDimension(signalNumber);
    }

    @Override
    public int getDigitalMin(int signalNumber) {
        return inConfig.getDigitalMin(signalNumber);
    }

    @Override
    public int getDigitalMax(int signalNumber) {
        return inConfig.getDigitalMax(signalNumber);
    }

    @Override
    public double getPhysicalMin(int signalNumber) {
        return inConfig.getPhysicalMin(signalNumber);
    }

    @Override
    public double getPhysicalMax(int signalNumber) {
        return inConfig.getPhysicalMax(signalNumber);
    }
}
