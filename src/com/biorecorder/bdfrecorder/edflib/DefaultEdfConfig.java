package com.biorecorder.bdfrecorder.edflib;

import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Base (default) realization of EdfConfig.
 */
public class DefaultEdfConfig {
    private double durationOfDataRecord = 1; // sec
    private ArrayList<Signal> signals = new ArrayList<Signal>();


    /**
     * This constructor creates a DefaultEdfConfig instance
     * with the given number of channels (signals)
     *
     * @param numberOfSignals number of signals in data records
     * @throws IllegalArgumentException if numberOfSignals <= 0
     */
    public DefaultEdfConfig(int numberOfSignals) throws IllegalArgumentException {
        if (numberOfSignals <= 0) {
            String errMsg =  MessageFormat.format("Number of signals is invalid: {0}. Expected {1}",  numberOfSignals, ">0");
            throw new IllegalArgumentException(errMsg);
        }
        for (int i = 0; i < numberOfSignals; i++) {
            Signal signalInfo = new Signal();
            signalInfo.setLabel("Channel_" + signals.size());
            signals.add(signalInfo);
        }
    }


    /**
     * Return the number of measuring channels (signals).
     * @return the number of measuring channels
     */
    public int getSignalsCount() {
        return signals.size();
    }


    /**
     * Gets duration of DataRecords (data packages).
     * @return duration of DataRecords in seconds
     */
    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }


    /**
     * Sets duration of DataRecords (data packages) in seconds.
     * Default value = 1 sec.
     *
     * @param durationOfDataRecord duration of DataRecords in seconds
     * @throws IllegalArgumentException if durationOfDataRecord <= 0.
     */
    public void setDurationOfDataRecord(double durationOfDataRecord) throws IllegalArgumentException {
        if (durationOfDataRecord <= 0) {
            String errMsg = MessageFormat.format("Record duration is invalid: {0}. Expected {1}", Double.toString(durationOfDataRecord), ">0");
            throw new IllegalArgumentException(errMsg);
        }
        this.durationOfDataRecord = durationOfDataRecord;
    }


    /**
     * Sets the digital minimum and maximum values of the signal.
     * Usually it's the extreme output of the ADC.
     * <br>-32768 <= digitalMin <= digitalMax <= 32767 (EDF_16BIT  file format).
     * <br>-8388608 <= digitalMin <= digitalMax <= 8388607 (BDF_24BIT file format).
     * <p>
     * Digital min and max must be set for every signal!!!
     * <br>Default digitalMin = -32768,  digitalMax = 32767 (EDF_16BIT file format)
     * <br>Default digitalMin = -8388608,  digitalMax = 8388607 (BDF_24BIT file format)
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @param digitalMin   the minimum digital value of the signal
     * @param digitalMax   the maximum digital value of the signal
     * @throws IllegalArgumentException  if digitalMin >= digitalMax,
     *                            <br>if  32767 <= digitalMin  or digitalMin < -32768 (EDF_16BIT  file format).
     *                            <br>if  32767 < digitalMax  or digitalMax <= -32768 (EDF_16BIT  file format).
     *                            <br>if  8388607 <= digitalMin  or digitalMin < -8388608 (BDF_24BIT  file format).
     *                            <br>if  8388607 < digitalMax  or digitalMax <= -8388608 (BDF_24BIT  file format).
     */
    public void setDigitalRange(int signalNumber, int digitalMin, int digitalMax) throws IllegalArgumentException {
        if (digitalMax <= digitalMin) {
            String errMsg = MessageFormat.format("Digital min/max range of signal {0} is invalid. Min = {1}, Max = {2}. Expected: {3}", signalNumber, Integer.toString(digitalMin), Integer.toString(digitalMax), "max > min");
            throw new IllegalArgumentException(errMsg);

        }
        signals.get(signalNumber).setDigitalRange(digitalMin, digitalMax);
    }

    /**
     * Sets the physical minimum and maximum values of the signal (the values of the input
     * of the ADC when the output equals the value of "digital minimum" and "digital maximum").
     * Usually physicalMin = - physicalMax.
     * <p>
     * Physical min and max must be set for every signal!!!
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @param physicalMin  the minimum physical value of the signal
     * @param physicalMax  the maximum physical value of the signal
     * @throws IllegalArgumentException if physicalMin >= physicalMax
     */
    public void setPhysicalRange(int signalNumber, double physicalMin, double physicalMax) throws IllegalArgumentException {
        if (physicalMax <= physicalMin) {
            String errMsg = MessageFormat.format("Physical min/max range of signal {0} is invalid. Min = {1}, Max = {2}. Expected: {3}", signalNumber, Double.toString(physicalMin), Double.toString(physicalMax), "max > min");
            throw new IllegalArgumentException(errMsg);
        }
        signals.get(signalNumber).setPhysicalRange(physicalMin, physicalMax);
    }


    /**
     * Sets the physical dimension (units) of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     * It is recommended to set physical dimension for every signal.
     *
     * @param signalNumber      number of the signal (channel). Numeration starts from 0
     * @param physicalDimension physical dimension of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public void setPhysicalDimension(int signalNumber, String physicalDimension) {
        signals.get(signalNumber).setPhysicalDimension(physicalDimension);
    }

    /**
     * Sets the transducer (electrodes) name of the signal ("AgAgCl cup electrodes", etc.).
     * This method is optional.
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @param transducer   string describing transducer (electrodes) used for measuring
     */
    public void setTransducer(int signalNumber, String transducer) {
        signals.get(signalNumber).setTransducer(transducer);
    }

    /**
     * Sets the filters names that were applied to the samples belonging to the signal ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @param prefiltering string describing filters that were applied to the signal
     */
    public void setPrefiltering(int signalNumber, String prefiltering) {
        signals.get(signalNumber).setPrefiltering(prefiltering);
    }


    /**
     * Sets the label (name) of signal.
     * It is recommended to set labels for every signal.
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @param label        label of the signal
     */
    public void setLabel(int signalNumber, String label) {
        signals.get(signalNumber).setLabel(label);
    }


    public String getLabel(int signalNumber) {
        return signals.get(signalNumber).getLabel();
    }

    public String getTransducer(int signalNumber) {
        return signals.get(signalNumber).getTransducer();
    }

    public String getPrefiltering(int signalNumber) {
        return signals.get(signalNumber).getPrefiltering();
    }

    public int getDigitalMin(int signalNumber) {
        return signals.get(signalNumber).getDigitalMin();
    }

    public int getDigitalMax(int signalNumber) {
        return signals.get(signalNumber).getDigitalMax();
    }

    public double getPhysicalMin(int signalNumber) {
        return signals.get(signalNumber).getPhysicalMin();
    }

    public double getPhysicalMax(int signalNumber) {
        return signals.get(signalNumber).getPhysicalMax();
    }

    public String getPhysicalDimension(int signalNumber) {
        return signals.get(signalNumber).getPhysicalDimension();
    }


    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return signals.get(signalNumber).getNumberOfSamplesInEachDataRecord();
    }


    /**
     * Sets the number of samples belonging to the signal
     * in each DataRecord (data package).
     * <p>
     * When duration of DataRecords = 1 sec (default):
     * NumberOfSamplesInEachDataRecord = sampleFrequency
     * <p>
     * SampleFrequency o NumberOfSamplesInEachDataRecord must be set for every signal!!!
     * @param signalNumber                    number of the signal(channel). Numeration starts from 0
     * @param numberOfSamplesInEachDataRecord number of samples belonging to the signal with the given sampleNumberToSignalNumber
     *                                        in each DataRecord
     * @throws IllegalArgumentException if the given numberOfSamplesInEachDataRecord <= 0
     */
    public void setNumberOfSamplesInEachDataRecord(int signalNumber, int numberOfSamplesInEachDataRecord) throws IllegalArgumentException {
        if (numberOfSamplesInEachDataRecord <= 0) {
            String errMsg = MessageFormat.format("Number of samples in datarecord of signal {0} is invalid: {1}. Expected {2}", signalNumber, Integer.toString(numberOfSamplesInEachDataRecord), ">0");
            throw new IllegalArgumentException(errMsg);
        }
        signals.get(signalNumber).setNumberOfSamplesInEachDataRecord(numberOfSamplesInEachDataRecord);
    }
    

    /**
     * Helper method.
     * Sets the sample frequency of the signal.
     * This method is just a user friendly wrapper of the method
     * {@link #setNumberOfSamplesInEachDataRecord(int, int)}
     * <p>
     * When duration of DataRecords = 1 sec (default):
     * NumberOfSamplesInEachDataRecord = sampleFrequency
     * <p>
     * SampleFrequency o NumberOfSamplesInEachDataRecord must be set for every signal!!!
     *
     * @param signalNumber    number of the signal(channel). Numeration starts from 0
     * @param sampleFrequency frequency of the samples (number of samples per second) belonging to that channel
     * @throws IllegalArgumentException if the given sampleFrequency <= 0
     */
    public void setSampleFrequency(int signalNumber, int sampleFrequency) throws IllegalArgumentException {
        if (sampleFrequency <= 0) {
            String errMsg = MessageFormat.format("Sample frequency of signal {0} is invalid: {1}. Expected {2}", signalNumber, Double.toString(sampleFrequency), ">0");
            throw new IllegalArgumentException(errMsg);
        }
        Long numberOfSamplesInEachDataRecord = Math.round(sampleFrequency * durationOfDataRecord);
        setNumberOfSamplesInEachDataRecord(signalNumber, numberOfSamplesInEachDataRecord.intValue());
    }


    /**
     * Helper method.
     * Get the frequency of the samples belonging to the signal.
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return frequency of the samples (number of samples per second) belonging to the signal with the given number
     */
    public double getSampleFrequency(int signalNumber) {
        return getNumberOfSamplesInEachDataRecord(signalNumber) / getDurationOfDataRecord();
    }


    /**
     * Helper method.
     * Convert physical value of the signal to digital one on the base
     * of its physical and digital maximums and minimums (Gain and Offset)
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return digital value
     */
    public int physicalValueToDigital(int signalNumber, double physValue) {
        return signals.get(signalNumber).physToDig(physValue);

    }

    /**
     * Helper method.
     * Convert digital value of the signal to physical one  on the base
     * of its physical and digital maximums and minimums (Gain and Offset)
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return physical value
     */
    public  double digitalValueToPhysical(int signalNumber, int digValue) {
        return  signals.get(signalNumber).digToPys(digValue);

    }

    /**
     * Helper method.
     * Get Gain of the signal:
     * <br>digValue = (physValue / calculateGain) - Offset;
     * <br>physValue = (digValue + calculateOffset)
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return Gain of the signal
     */
    public double gain(int signalNumber) {
        return signals.get(signalNumber).getGain();
    }

    /**
     * Helper method.
     * Get Offset of the signal:
     * <br>digValue = (physValue / calculateGain) - Offset;
     * <br>physValue = (digValue + calculateOffset)
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return Offset of the signal
     */
    public double offset(int signalNumber) {
        return signals.get(signalNumber).getOffset();
    }



    class Signal {
        private int numberOfSamplesInEachDataRecord;
        private String prefiltering = "None";
        private String transducerType = "Unknown";
        private String label = "";
        private int digitalMin;
        private int digitalMax;
        private double physicalMin;
        private double physicalMax;
        private String physicalDimension = "";  // uV or Ohm
        private double gain;
        private double offset;


        public int getDigitalMin() {
            return digitalMin;
        }

        public int getDigitalMax() {
            return digitalMax;
        }

        public double getPhysicalMin() {
            return physicalMin;
        }

        public double getPhysicalMax() {
            return physicalMax;
        }

        public String getPhysicalDimension() {
            return physicalDimension;
        }

        public int getNumberOfSamplesInEachDataRecord() {
            return numberOfSamplesInEachDataRecord;
        }

        public int physToDig(double physValue) {
            return (int) (physValue / gain - offset);
        }

        public double digToPys(int digValue) {
            return (digValue + offset) * gain;
        }

        public void setDigitalRange(int digitalMin, int digitalMax) {
            this.digitalMin = digitalMin;
            this.digitalMax = digitalMax;
            gain = calculateGain();
            offset = calculateOffset();
        }

        public void setPhysicalRange(double physicalMin, double physicalMax) {
            this.physicalMin = physicalMin;
            this.physicalMax = physicalMax;
            gain = calculateGain();
            offset = calculateOffset();
        }

        /**
         * Calculate the Gain calibration (adjust) factor of the signal on the base
         * of its physical and digital maximums and minimums
         *
         * @return Gain = (physMax - physMin) / (digMax - digMin)
         */
        public double calculateGain() {
            return (physicalMax - physicalMin) / (digitalMax - digitalMin);
        }


        /**
         * Calculate the Offset calibration (adjust) factor of the signal on the base
         * of its physical and digital maximums and minimums
         *
         * @return Offset = physicalMax / calculateGain() - digitalMax;
         */
        public double calculateOffset() {
            return (physicalMax / gain) - digitalMax;
        }


        public void setPhysicalDimension(String physicalDimension) {
            this.physicalDimension = physicalDimension;
        }


        public void setNumberOfSamplesInEachDataRecord(int numberOfSamplesInEachDataRecord) {
            this.numberOfSamplesInEachDataRecord = numberOfSamplesInEachDataRecord;
        }

        public String getPrefiltering() {
            return prefiltering;
        }

        public void setPrefiltering(String prefiltering) {
            this.prefiltering = prefiltering;
        }

        public String getTransducer() {
            return transducerType;
        }

        public void setTransducer(String transducerType) {
            this.transducerType = transducerType;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public double getGain() {
            return gain;
        }

        public double getOffset() {
            return offset;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("\nDuration of DataRecords = " + getDurationOfDataRecord());
        sb.append("\nNumber of signals = " + getSignalsCount());
        for (int i = 0; i < getSignalsCount(); i++) {
            sb.append("\n  " + i + " label: " + getLabel(i)
                    + "; number of samples: " + getNumberOfSamplesInEachDataRecord(i)
                    + "; frequency: "+  Math.round(getSampleFrequency(i))
                    + "; dig min: " + getDigitalMin(i) + "; dig max: " + getDigitalMax(i)
                    + "; phys min: " + getPhysicalMin(i) + "; phys max: " + getPhysicalMax(i)
                    + "; prefiltering: " + getPrefiltering(i)
                    + "; transducer: " + getTransducer(i)
                    + "; dimension: " + getPhysicalDimension(i));
        }
        sb.append("\n");
        return sb.toString();
    }


}
