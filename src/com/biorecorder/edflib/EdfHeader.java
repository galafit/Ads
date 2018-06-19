package com.biorecorder.edflib;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * This class permits to store the information from the header record and
 * easily work with it. Header info is used to correctly extract and write data
 * from/to the EDF/BDF file.
 * <p>
 * BDF HEADER RECORD
 * <br>8 ascii : version of this data format (0)
 * <br>80 ascii : local patient identification (mind item 3 of the additional EDF+ specs)
 * <br>80 ascii : local recording identification (mind item 4 of the additional EDF+ specs)
 * <br>8 ascii : startdate of recording (dd.mm.yy) (mind item 2 of the additional EDF+ specs)
 * <br>8 ascii : starttime of recording (hh.mm.ss)
 * <br>8 ascii : number of bytes in header record (The header record contains 256 + (ns * 256) bytes)
 * <br>44 ascii : reserved
 * <br>8 ascii : number of data records (-1 if unknown, obey item 10 of the additional EDF+ specs)
 * <br>8 ascii : duration of a data record, in seconds
 * <br>4 ascii : number of signals (ns) in data record
 * <br>ns * 16 ascii : ns * getLabel (e.g. EEG Fpz-Cz or Body temp) (mind item 9 of the additional EDF+ specs)
 * <br>ns * 80 ascii : ns * getTransducer type (e.g. AgAgCl electrode)
 * <br>ns * 8 ascii : ns * physical dimension (e.g. uV or degreeC)
 * <br>ns * 8 ascii : ns * physical minimum (e.g. -500 or 34)
 * <br>ns * 8 ascii : ns * physical maximum (e.g. 500 or 40)
 * <br>ns * 8 ascii : ns * digital minimum (e.g. -2048)
 * <br>ns * 8 ascii : ns * digital maximum (e.g. 2047)
 * <br>ns * 80 ascii : ns * getPrefiltering (e.g. HP:0.1Hz LP:75Hz)
 * <br>ns * 8 ascii : ns * nr of samples in each data record
 * <br>ns * 32 ascii : ns * reserved
 * <p>
 * Detailed information about EDF/BDF format:
 * <a href="http://www.edfplus.info/specs/edf.html">European Data Format. Full specification of EDF</a>
 * <a href="https://www.biosemi.com/faq/file_format.htm">BioSemi or BDF file format</a>
 * <p>
 */
public class EdfHeader {
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long recordingStartTime = 0;
    private int numberOfDataRecords = -1;
    private DataFormat dataFormat = DataFormat.EDF_16BIT;
    private double durationOfDataRecord = 1; // sec
    private ArrayList<Signal> signals = new ArrayList<Signal>();

    /**
     * This constructor creates a EdfHeader instance that specifies the
     * the type of the of the file where data records will be written: EDF_16BIT or BDF_24BIT
     * and the number of measuring channels (signals)
     *
     * @param dataFormat      EDF_16BIT or BDF_24BIT
     * @param numberOfSignals number of signals in data records
     * @throws IllegalArgumentException if numberOfSignals < 0
     */
    public EdfHeader(DataFormat dataFormat, int numberOfSignals) throws IllegalArgumentException {
        this.dataFormat = dataFormat;
        if (numberOfSignals < 0) {
            String errMsg = MessageFormat.format("Number of signals is invalid: {0}. Expected {1}", numberOfSignals, ">= 0");
            throw new IllegalArgumentException(errMsg);
        }
        for (int i = 0; i < numberOfSignals; i++) {
            addSignal(dataFormat);
        }
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
     * Gets recording startRecording date and time measured in milliseconds,
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
     * Sets recording startRecording date and time.
     * This function is optional. If not called,
     * the writer will use the system date and time at runtime
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
     * Sets recording startRecording time measured in milliseconds,
     * since midnight, January 1, 1970 UTC.
     * This function is optional.
     * If not called, the writer will use the system date and time at runtime
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
     * Get the number of bytes in the EDF/BDF header record (when we will create it on the base of this HeaderConfig)
     *
     * @return number of bytes in EDF/BDF header = (number of signals + 1) * 256
     */
    public int getNumberOfBytesInHeaderRecord() {
        return 256 + (signalsCount() * 256);
    }

    /**
     * Gets the type of the file: EDF_16BIT or BDF_24BIT
     *
     * @return type of the file: EDF_16BIT or BDF_24BIT
     */
    public DataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * Gets the number of DataRecords (data packages) in Edf/Bdf file.
     * The default value = -1 and real number of DataRecords is
     * set automatically when we finish to write data to the EdfWileWriter and close it
     *
     * @return number of DataRecords in the file or -1 if data writing is not finished
     */
    public int getNumberOfDataRecords() {
        return numberOfDataRecords;
    }

    /**
     * Sets the number of DataRecords (data packages) in Edf/Bdf file.
     * The default value = -1 means that file writing is not finished yet.
     * This method should not be used by users because
     * EdfWriter calculate and sets the number of DataRecords automatically
     *
     * @param numberOfDataRecords number of DataRecords (data packages) in Edf/Bdf file
     * @throws IllegalArgumentException if number of data records < -1
     */
    void setNumberOfDataRecords(int numberOfDataRecords) throws IllegalArgumentException {
        if (numberOfDataRecords < -1) {
            String errMsg = "Invalid number of data records: " + numberOfDataRecords + " Expected >= -1";
            throw new IllegalArgumentException(errMsg);
        }
        this.numberOfDataRecords = numberOfDataRecords;
    }

    /**
     * Return the number of measuring channels (signals).
     *
     * @return the number of measuring channels
     */
    public int signalsCount() {
        return signals.size();
    }


    /**
     * Gets duration of DataRecords (data packages).
     *
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
     * @throws IllegalArgumentException if getDurationOfDataRecord <= 0.
     */
    public void setDurationOfDataRecord(double durationOfDataRecord) throws IllegalArgumentException {
        if (durationOfDataRecord <= 0) {
            String errMsg = MessageFormat.format("Duration of data record is invalid: {0}. Expected {1}", durationOfDataRecord, "> 0");
            throw new IllegalArgumentException(errMsg);
        }
        this.durationOfDataRecord = durationOfDataRecord;
    }

    /*****************************************************************
     *                   Signals Info                                *
     *****************************************************************/

    /**
     * Gets the getLabel of the signal
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return getLabel of the signal
     */
    public String getLabel(int signalNumber) {
        return signals.get(signalNumber).getLabel();
    }

    /**
     * Get getTransducer(electrodes) name ("AgAgCl cup electrodes", etc)
     * used for measuring data belonging to the signal).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing getTransducer (electrodes) used for measuring
     */
    public String getTransducer(int signalNumber) {
        return signals.get(signalNumber).getTransducer();
    }

    /**
     * Get the filters names that were applied to the samples belonging to the signal ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing filters that were applied to the signal
     */
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

    /**
     * Get physical dimension (units) of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing physical dimension of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public String getPhysicalDimension(int signalNumber) {
        return signals.get(signalNumber).getPhysicalDimension();
    }

    /**
     * Gets the number of samples belonging to the signal
     * in each DataRecord (data package).
     * When duration of DataRecords = 1 sec (default):
     * NumberOfSamplesInEachDataRecord = sampleFrequency
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return number of samples belonging to the signal with the given sampleNumberToSignalNumber
     * in each DataRecord (data package)
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
        if (dataFormat == DataFormat.EDF_16BIT && digitalMin < -32768) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital min: {1}.  Expected: {2}", signalNumber, digitalMin, ">= -32768");
            throw new IllegalArgumentException(errMsg);
        }
        if (dataFormat == DataFormat.BDF_24BIT && digitalMin < -8388608) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital min: {1}.  Expected: {2}", signalNumber, digitalMin, ">= -8388608");
            throw new IllegalArgumentException(errMsg);
        }

        if (dataFormat == DataFormat.EDF_16BIT && digitalMax > 32767) {
            String errMsg = MessageFormat.format("Signal {0}. Invalid digital max: {1}.  Expected: {2}", signalNumber, digitalMax, "<= 32767");
            throw new IllegalArgumentException(errMsg);
        }
        if (dataFormat == DataFormat.BDF_24BIT && digitalMax > 8388607) {
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
     * Sets the getTransducer (electrodes) name of the signal ("AgAgCl cup electrodes", etc.).
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
     * Sets the getLabel (name) of signal.
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
     * in each DataRecord (data package).
     * <p>
     * When duration of DataRecords = 1 sec (default):
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
     * @return the size of data record array
     */
    public int getDataRecordSize() {
        int recordSize = 0;
        for (int i = 0; i < signalsCount(); i++) {
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
    public double getGain(int signalNumber) {
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
    public double getOffset(int signalNumber) {
        return signals.get(signalNumber).getOffset();
    }

    private void addSignal(DataFormat dataFormat) {
        Signal signal = new Signal();
        signal.setLabel("Channel_" + signals.size());
        if (dataFormat == DataFormat.EDF_16BIT) {
            signal.setDigitalRange(-32768, 32767);
            signal.setPhysicalRange(-32768, 32767);
        } else {
            signal.setDigitalRange(-8388608, 8388607);
            signal.setPhysicalRange(-8388608, 8388607);
        }
        signals.add(signal);
    }

    class Signal {
        private int numberOfSamplesInEachDataRecord;
        private String prefiltering = "";
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
        sb.append("file type = " + getDataFormat());
        sb.append("\nNumber of DataRecords = " + getNumberOfDataRecords());
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        String timeStamp = dateFormat.format(new Date(getRecordingStartTimeMs()));
        sb.append("\nStart date and time = " + timeStamp + " (" + getRecordingStartTimeMs() + " ms)");
        sb.append("\nPatient identification = " + getPatientIdentification());
        sb.append("\nRecording identification = " + getRecordingIdentification());
        sb.append("\nDuration of DataRecords = " + getDurationOfDataRecord());
        sb.append("\nNumber of signals = " + signalsCount());
        for (int i = 0; i < signalsCount(); i++) {
            sb.append("\n  " + i + " getLabel: " + getLabel(i)
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

    /**
     * Unit Test. Usage Example.
     * <p>
     * Create and print default Edf and Bdf HeaderConfig
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        int numberOfSignals = 3;
        EdfHeader headerConfigEdf = new EdfHeader(DataFormat.EDF_16BIT, numberOfSignals);
        EdfHeader headerConfigBdf = new EdfHeader(DataFormat.BDF_24BIT, numberOfSignals);

        // set startRecording date and time for Bdf HeaderConfig
        headerConfigBdf.setRecordingStartDateTime(1972, 6, 23, 23, 23, 50);
        // print header info
        System.out.println(headerConfigEdf);
        System.out.println(headerConfigBdf);
    }
}
