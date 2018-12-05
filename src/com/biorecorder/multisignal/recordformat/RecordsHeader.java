package com.biorecorder.multisignal.recordformat;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Most multi signal formats (Edf, Bdf, Gdf...)
 * store and exchange multichannel biological
 * and physical data in forms of data records.
 * <p>
 * Tha base idea is that all samples received from multiple
 * measuring channels (signals) within the specified time interval
 * are placed in one data record (or package) as follows:
 * <br>n_0 samples belonging to signal 0,
 * <br>n_1 samples belonging to signal 1,
 * <br>...
 * <br>n_k samples belonging to  signal k
 * <p>
 * Essentially data record is just an array of integers. Every signal may have its own sample frequency!
 * <br>The number of samples n_i = (sample frequency of the signal_i) * (time interval).
 * <p>
 * Every measured
 * digital sample is saved as one integer. A linear relationship between
 * digital (integer) values stored in data record and the corresponding
 * physical values are assumed. To convert digital values to
 * the physical ones (and vice versa) <b>digital minimum and maximum</b>
 * and the corresponding <b> physical minimum and maximum</b>
 * must be specified for every signal.
 * These 4 extreme values specify offset and amplification of the signal:
 * <p>
 * (physValue - physMin) / (digValue - digMin)  = constant [Gain] = (physMax - physMin) / (digMax - digMin)
 * <p>
 * <br>digValue = (physValue / Gain) - Offset;
 * <br>physValue = (digValue + Offset) * Gain
 * <br>
 * Where scaling factors:
 * <br>Gain = (physMax - physMin) / (digMax - digMin)
 * <br>Offset = (physMax / Gain) - digMax;
 * <p>
 * In general "Gain" refers to multiplication of a signal
 * and "Offset"  refer to addition to a signal, i.e. out = (in + Offset) * Gain
 * <p>
 * RecordsHeader contains all necessary info to correctly extract
 * samples belonging to different signals from records
 * and convert it to corresponding physical values.
  */
public class RecordsHeader {
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long recordingStartTime = 0;
    private int numberOfDataRecords = -1;

    private FormatVersion versionFormat;
    protected double durationOfDataRecord = 1; // sec
    protected ArrayList<Signal> signals = new ArrayList<Signal>();

    /**
     * This constructor creates an instance of RecordsHeader
     * with the specified  version format and number of measuring channels (signals)
     * @param versionFormat   16BIT or 24BIT
     * @param numberOfSignals number of signals in data records
     * @throws IllegalArgumentException if numberOfSignals < 0
     */
    public RecordsHeader(FormatVersion versionFormat, int numberOfSignals) throws IllegalArgumentException {
        if (numberOfSignals < 0) {
            String errMsg = MessageFormat.format("Number of signals is invalid: {0}. Expected {1}", numberOfSignals, ">= 0");
            throw new IllegalArgumentException(errMsg);
        }
        this.versionFormat = versionFormat;
        for (int i = 0; i < numberOfSignals; i++) {
            addSignal();
        }
         
    }


    /**
     * Constructor to make a copy of the given recordsHeader
     *
     * @param recordsHeader HeaderMetadata instance that will be copied
     */
    public RecordsHeader(RecordsHeader recordsHeader) {
        durationOfDataRecord = recordsHeader.durationOfDataRecord;
        versionFormat = recordsHeader.versionFormat;
        patientIdentification = recordsHeader.patientIdentification;
        recordingIdentification = recordsHeader.recordingIdentification;
        numberOfDataRecords = recordsHeader.numberOfDataRecords;
        recordingStartTime = recordsHeader.recordingStartTime;

        for (int i = 0; i < recordsHeader.numberOfSignals(); i++) {
            signals.add(new Signal(recordsHeader.signals.get(i)));
        }
    }

    /**
     * Return the number of measuring channels (signals).
     *
     * @return the number of measuring channels
     */
    public int numberOfSignals() {
        return signals.size();
    }


    /**
     * Gets the measuring time interval or duration of data records
     * in seconds. 1 sec by default
     *
     * @return duration of data record in seconds
     */
    public double getDurationOfDataRecord() {
        return durationOfDataRecord;
    }


    /**
     * Sets duration of data records in seconds.
     * Default value = 1 sec.
     *
     * @param durationOfDataRecord duration of data records in seconds
     * @throws IllegalArgumentException if getCalculatedDurationOfDataRecord <= 0.
     */
    public void setDurationOfDataRecord(double durationOfDataRecord) throws IllegalArgumentException {
        if (durationOfDataRecord <= 0) {
            String errMsg = MessageFormat.format("Duration of data record is invalid: {0}. Expected {1}", durationOfDataRecord, "> 0");
            throw new IllegalArgumentException(errMsg);
        }
        this.durationOfDataRecord = durationOfDataRecord;
    }

    /**
     * Gets the patient identification string (name, surname, etc).
     *
     * @return patient identification string
     */
    public String getPatientIdentification() {
        return patientIdentification;
    }

    /**
     * Sets the patient identification string (name, surname, etc).
     * This method is optional
     *
     * @param patientIdentification patient identification string
     */
    public void setPatientIdentification(String patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    /**
     * Gets the recording identification string.
     *
     * @return recording (experiment) identification string
     */
    public String getRecordingIdentification() {
        return recordingIdentification;
    }

    /**
     * Sets the recording identification string.
     * This method is optional
     *
     * @param recordingIdentification recording (experiment) identification string
     */
    public void setRecordingIdentification(String recordingIdentification) {
        this.recordingIdentification = recordingIdentification;
    }

    /**
     * Gets recording start date and time measured in milliseconds,
     * since midnight, January 1, 1970 UTC.
     *
     * @return the difference, measured in milliseconds,
     * between the recording startRecording time
     * and midnight, January 1, 1970 UTC.
     */
    public long getRecordingStartTimeMs() {
        return recordingStartTime;
    }


    /**
     * Helper method that sets recording start date and time.
     * This function is optional.
     *
     * @param year   1970 - 3000
     * @param month  1 - 12
     * @param day    1 - 31
     * @param hour   0 - 23
     * @param minute 0 - 59
     * @param second 0 - 59
     * @throws IllegalArgumentException if some parameter (year, month...) is out of its range
     */
    public void setRecordingStartDateTime(int year, int month, int day, int hour, int minute, int second) throws IllegalArgumentException {
        if (year < 1970 || year > 3000) {
            String errMsg = MessageFormat.format("Year is invalid: {0}. Expected: {1}", year, "1970 - 3000");
            throw new IllegalArgumentException(errMsg);
        }
        if (month < 1 || month > 12) {
            String errMsg = MessageFormat.format("Month is invalid: {0}. Expected: {1}", month, "1 - 12");
            throw new IllegalArgumentException(errMsg);
        }
        if (day < 1 || day > 31) {
            String errMsg = MessageFormat.format("Day is invalid: {0}. Expected: {1}", day, "1 - 31");
            throw new IllegalArgumentException(errMsg);
        }
        if (hour < 0 || hour > 23) {
            String errMsg = MessageFormat.format("Hour is invalid: {0}. Expected: {1}", hour, "0 - 23");
            throw new IllegalArgumentException(errMsg);
        }
        if (minute < 0 || minute > 59) {
            String errMsg = MessageFormat.format("Minute is invalid: {0}. Expected: {1}", minute, "0 - 59");
            throw new IllegalArgumentException(errMsg);
        }
        if (second < 0 || second > 59) {
            String errMsg = MessageFormat.format("Second is invalid: {0}. Expected: {1}", second, "0 - 59");
            throw new IllegalArgumentException(errMsg);
        }

        Calendar calendar = Calendar.getInstance();
        // in java month indexing from 0
        calendar.set(year, month - 1, day, hour, minute, second);
        this.recordingStartTime = calendar.getTimeInMillis();
    }

    /**
     * Sets recording start time measured in milliseconds,
     * since midnight, January 1, 1970 UTC.
     * This function is optional.
     *
     * @param recordingStartTime the difference, measured in milliseconds,
     *                           between the recording startRecording time
     *                           and midnight, January 1, 1970 UTC.
     * @throws IllegalArgumentException if recordingStartTime < 0
     */
    public void setRecordingStartTimeMs(long recordingStartTime) {
        if (recordingStartTime < 0) {
            String errMsg = "Invalid startRecording time: " + recordingStartTime + " Expected >= 0";
            throw new IllegalArgumentException(errMsg);
        }
        this.recordingStartTime = recordingStartTime;
    }

    /**
     * Gets the number of data records.
     * Return -1 if unknown.
     * @return total number of all data records or -1 if unknown
     */
    public int getNumberOfDataRecords() {
        return numberOfDataRecords;
    }

    /**
     * Sets the number of data records (data packages) in Edf/Bdf file.
     * The default value = -1 means that file writing is not finished yet.
     * This method should not be used by users because
     * EdfWriter calculate and sets the number of data records automatically
     *
     * @param numberOfDataRecords number of data records (data packages) in Edf/Bdf file
     * @throws IllegalArgumentException if number of data records < -1
     */
    public void setNumberOfDataRecords(int numberOfDataRecords) throws IllegalArgumentException {
        if (numberOfDataRecords < -1) {
            String errMsg = "Invalid number of data records: " + numberOfDataRecords + " Expected >= -1";
            throw new IllegalArgumentException(errMsg);
        }
        this.numberOfDataRecords = numberOfDataRecords;
    }


    /*****************************************************************
     *                   Signals Info                                *
     *****************************************************************/

    /**
     * Gets the getLabel/name of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return label of the signal
     */
    public String getLabel(int signalNumber) {
        return signals.get(signalNumber).getLabel();
    }

    /**
     * Gets getTransducer(electrodes) name ("AgAgCl cup electrodes", etc)
     * used for measuring data belonging to the signal.
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing getTransducer (electrodes) used for measuring
     */
    public String getTransducer(int signalNumber) {
        return signals.get(signalNumber).getTransducer();
    }

    /**
     * Gets the filters names that were applied to the samples belonging to the signal
     * ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing filters that were applied
     */
    public String getPrefiltering(int signalNumber) {
        return signals.get(signalNumber).getPrefiltering();
    }

    /**
     * Specify the extreme minimum value of the samples belonging to the given
     * signal that can occur in data records.
     * These often are the extreme minimum output value of the A/D converter.
     * @param signalNumber  number of the signal (channel). Numeration starts from 0
     * @return minimum value of the samples belonging to the
     * signal that can occur in data records
     */
    public int getDigitalMin(int signalNumber) {
        return signals.get(signalNumber).getDigitalMin();
    }

    /**
     * Specify the extreme maximum value of the samples belonging to the given
     * signal that can occur in data records.
     * These often are the extreme maximum output value of the A/D converter.
     * @param signalNumber  number of the signal (channel). Numeration starts from 0
     * @return maximum value of the samples belonging to the
     * signal that can occur in data records
     */
    public int getDigitalMax(int signalNumber) {
        return signals.get(signalNumber).getDigitalMax();
    }

    /**
     * Specify the physical (usually also physiological) minimum
     * of the signal that corresponds to its digital minimum.
     * Physical minimum should be expressed in the physical dimension
     * (physical units) of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return physical minimum of the signal expressed in physical units
     * that corresponds to its digital minimum
     */
    public double getPhysicalMin(int signalNumber) {
        return signals.get(signalNumber).getPhysicalMin();
    }

    /**
     * Specify the physical (usually also physiological) maximum
     * of the signal that corresponds to its digital maximum.
     * Physical  maximum should be expressed in the physical dimension
     * (physical units) of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return physical  maximum of the signal expressed in physical units
     * that corresponds to its digital  maximum
     */
    public double getPhysicalMax(int signalNumber) {
        return signals.get(signalNumber).getPhysicalMax();
    }

    /**
     * Gets physical dimension or units of physical values
     * of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     * @param signalNumber     number of the signal (channel). Numeration starts from 0
     * @return String describing units of physical values of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public String getPhysicalDimension(int signalNumber) {
        return signals.get(signalNumber).getPhysicalDimension();
    }


    /**
     * Gets the number of samples n_i belonging to the given signal_i
     * in each data record.
     * n_i = (sample frequency of the signal_i) * (duration of data record)
     * <p>
     * When duration of data record = 1 sec (default):
     * n_i = sample frequency of the signal_i
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return number of samples belonging to the given signal
     * in each data record
     */

    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return signals.get(signalNumber).getNumberOfSamplesInEachDataRecord();
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
     * @throws IllegalArgumentException if:
     *                                  <br>digitalMin < -32768 (EDF_16BIT  file format).
     *                                  <br>digitalMin < -8388608 (BDF_24BIT  file format).
     *                                  <br>digitalMax > 32767 (EDF_16BIT  file format).
     *                                  <br>digitalMax > 8388607 (BDF_24BIT  file format).
     *                                  <br>digitalMin >= digitalMax
     */
    public void setDigitalRange(int signalNumber, int digitalMin, int digitalMax) throws IllegalArgumentException {
        if (versionFormat == FormatVersion.EDF_16BIT && digitalMin < -32768) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital min: {1}.  Expected: {2}", signalNumber, digitalMin, ">= -32768");
            throw new IllegalArgumentException(errMsg);
        }
        if (versionFormat == FormatVersion.BDF_24BIT && digitalMin < -8388608) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital min: {1}.  Expected: {2}", signalNumber, digitalMin, ">= -8388608");
            throw new IllegalArgumentException(errMsg);
        }

        if (versionFormat == FormatVersion.EDF_16BIT && digitalMax > 32767) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital max: {1}.  Expected: {2}", signalNumber, digitalMax, "<= 32767");
            throw new IllegalArgumentException(errMsg);
        }
        if (versionFormat == FormatVersion.BDF_24BIT && digitalMax > 8388607) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital max: {1}.  Expected: {2}", signalNumber, digitalMax, "<= 8388607");
            throw new IllegalArgumentException(errMsg);
        }

        if (digitalMax <= digitalMin) {
            String errMsg = MessageFormat.format("Signal {0}. Digital min-max range is invalid. Min = {1}, Max = {2}. Expected: {3}", signalNumber, digitalMin, digitalMax, "max > min");
            throw new IllegalArgumentException(errMsg);

        }
        signals.get(signalNumber).setDigitalRange(digitalMin, digitalMax);
    }

    /**
     * Sets the physical minimum and maximum values of the signal (the values of the in
     * of the ADC when the output equals the value of "digital minimum" and "digital maximum").
     * Usually physicalMin = - physicalMax.
     * <p>
     * Physical min and max must be set for every signal!!!
     * <br>Default physicalMin = -32768,  physicalMax = 32767 (EDF_16BIT file format)
     * <br>Default physicalMin = -8388608,  physicalMax = 8388607 (BDF_24BIT file format)
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @param physicalMin  the minimum physical value of the signal
     * @param physicalMax  the maximum physical value of the signal
     * @throws IllegalArgumentException if physicalMin >= physicalMax
     */
    public void setPhysicalRange(int signalNumber, double physicalMin, double physicalMax) throws IllegalArgumentException {
        if (physicalMax <= physicalMin) {
            String errMsg = MessageFormat.format("Signal {0}. Physical min-max range is invalid. Min = {1}, Max = {2}. Expected: {3}", signalNumber, physicalMin, physicalMax, "max > min");
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
     * @param transducer   string describing getTransducer (electrodes) used for measuring
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
     * @param label        getLabel of the signal
     */
    public void setLabel(int signalNumber, String label) {
        signals.get(signalNumber).setLabel(label);
    }

    /**
     * Sets the number of samples belonging to the signal
     * in each data record.
     * <p>
     * When duration of data records = 1 sec (default):
     * NumberOfSamplesInEachDataRecord = sampleFrequency
     * <p>
     * SampleFrequency o NumberOfSamplesInEachDataRecord must be set for every signal!!!
     *
     * @param signalNumber                    number of the signal(channel). Numeration starts from 0
     * @param numberOfSamplesInEachDataRecord number of samples belonging to the signal with the given sampleNumberToSignalNumber
     *                                        in each DataRecord
     * @throws IllegalArgumentException if the given getNumberOfSamplesInEachDataRecord <= 0
     */
    public void setNumberOfSamplesInEachDataRecord(int signalNumber, int numberOfSamplesInEachDataRecord) throws IllegalArgumentException {
        if (numberOfSamplesInEachDataRecord <= 0) {
            String errMsg = MessageFormat.format("Signal {0}. Number of samples in data record is invalid: {1}. Expected {2}", signalNumber, numberOfSamplesInEachDataRecord, "> 0");
            throw new IllegalArgumentException(errMsg);
        }
        signals.get(signalNumber).setNumberOfSamplesInEachDataRecord(numberOfSamplesInEachDataRecord);
    }

    /**
     * Gets the sample size: 16BIT or 24BIT
     *
     * @return sample size: 16BIT or 24BIT
     */
    public FormatVersion getFormatVersion() {
        return versionFormat;
    }


    /**
     * Add new signal.
     */
    public void addSignal() {
        Signal signal = new Signal();
        signal.setLabel("Channel_" + signals.size());
        switch (versionFormat) {
            case EDF_16BIT:
                signal.setDigitalRange(-32768, 32767);
                signal.setPhysicalRange(-32768, 32767);
                break;

            case BDF_24BIT:
                signal.setDigitalRange(-8388608, 8388607);
                signal.setPhysicalRange(-8388608, 8388607);
                break;

            default:
                signal.setDigitalRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
                signal.setPhysicalRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        signals.add(signal);
    }

    /**
     * Removes the signal.
     *
     * @param signalNumber number of the signal(channel) to remove. Numeration starts from 0
     */
    public void removeSignal(int signalNumber) {
        signals.remove(signalNumber);
    }


    /**
     * Helper method.
     * Sets the sample frequency of the signal.
     * This method is just a user friendly wrapper of the method
     * {@link #setNumberOfSamplesInEachDataRecord(int, int)}
     * <p>
     * When duration of data records = 1 sec (default):
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
            String errMsg = MessageFormat.format("Signal {0}. Sample frequency is invalid: {1}. Expected {2}", signalNumber, sampleFrequency, "> 0");
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
     * Helper method. Calculates and gets the number of samples from all signals
     * in data record.
     *
     * @return the size of data record
     */
    public int getRecordSize() {
        int recordSize = 0;
        for (int i = 0; i < numberOfSignals(); i++) {
            recordSize += getNumberOfSamplesInEachDataRecord(i);
        }
        return recordSize;
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
    public double digitalValueToPhysical(int signalNumber, int digValue) {
        return signals.get(signalNumber).digToPys(digValue);
    }

    /**
     * Helper method.
     * Get Gain of the signal:
     * <br>digValue = (physValue / calculateGain) - Offset;
     * <br>physValue = (digValue + calculateOffset)
     *
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
     *
     * @param signalNumber number of the signal(channel). Numeration starts from 0
     * @return Offset of the signal
     */
    public double offset(int signalNumber) {
        return signals.get(signalNumber).getOffset();
    }

    class Signal {
        private int numberOfSamplesInEachDataRecord;
        private String prefiltering = "";
        private String transducerType = "Unknown";
        private String label = "";
        private int digitalMin = Integer.MIN_VALUE;
        private int digitalMax = Integer.MAX_VALUE;
        private double physicalMin = Integer.MIN_VALUE;
        private double physicalMax = Integer.MAX_VALUE;
        private String physicalDimension = "";  // uV or Ohm
        private double gain;
        private double offset;

        Signal(Signal signal) {
            numberOfSamplesInEachDataRecord = signal.numberOfSamplesInEachDataRecord;
            prefiltering = signal.prefiltering;
            transducerType = signal.transducerType;
            label = signal.label;
            digitalMin = signal.digitalMin;
            digitalMax = signal.digitalMax;
            physicalMin = signal.physicalMin;
            physicalMax = signal.physicalMax;
            physicalDimension = signal.physicalDimension;
            gain = signal.gain;
            offset = signal.offset;
        }

        public Signal() {
        }

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
         * @return Offset = getPhysicalMax / calculateGain() - getDigitalMax;
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
        //  sb.append(super.toString());
        sb.append("Format version = " + getFormatVersion());
        sb.append("\nNumber of data records = " + getNumberOfDataRecords());
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        String timeStamp = dateFormat.format(new Date(getRecordingStartTimeMs()));
        sb.append("\nStart date and time = " + timeStamp + " (" + getRecordingStartTimeMs() + " ms)");
        sb.append("\nPatient identification = " + getPatientIdentification());
        sb.append("\nRecording identification = " + getRecordingIdentification());
        sb.append("\nDuration of data records = " + getDurationOfDataRecord());
        sb.append("\nNumber of signals = " + numberOfSignals());
        for (int i = 0; i < numberOfSignals(); i++) {
            sb.append("\n  " + i + " label: " + getLabel(i)
                    + "; number of samples: " + getNumberOfSamplesInEachDataRecord(i)
                    + "; frequency: " + Math.round(getSampleFrequency(i))
                    + "; dig min: " + getDigitalMin(i) + "; dig max: " + getDigitalMax(i)
                    + "; phys min: " + getPhysicalMin(i) + "; phys max: " + getPhysicalMax(i)
                    + "; getPrefiltering: " + getPrefiltering(i)
                    + "; getTransducer: " + getTransducer(i)
                    + "; dimension: " + getPhysicalDimension(i));
        }
        sb.append("\n");
        return sb.toString();
    }
}
