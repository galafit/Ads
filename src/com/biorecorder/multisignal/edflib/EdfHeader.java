package com.biorecorder.multisignal.edflib;

import com.biorecorder.multisignal.recordformat.FormatVersion;
import com.biorecorder.multisignal.recordformat.RecordConfig;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
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
public class EdfHeader extends RecordConfig {
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long recordingStartTime = 0;
    private int numberOfDataRecords = -1;

    public EdfHeader(FormatVersion sampleSize, int numberOfSignals) throws IllegalArgumentException {
        super(sampleSize, numberOfSignals);
    }

    /**
     * Constructor to make a copy of the given header
     *
     * @param header EdfHeader instance that will be copied
     */
    public EdfHeader(EdfHeader header) {
        super(header);
        patientIdentification = header.patientIdentification;
        recordingIdentification = header.recordingIdentification;
        numberOfDataRecords = header.numberOfDataRecords;
        recordingStartTime = header.recordingStartTime;
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



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //  sb.append(super.toString());
        sb.append("Format version = " + getFormatVersion());
        sb.append("\nNumber of DataRecords = " + getNumberOfDataRecords());
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        String timeStamp = dateFormat.format(new Date(getRecordingStartTimeMs()));
        sb.append("\nStart date and time = " + timeStamp + " (" + getRecordingStartTimeMs() + " ms)");
        sb.append("\nPatient identification = " + getPatientIdentification());
        sb.append("\nRecording identification = " + getRecordingIdentification());
        sb.append("\nDuration of DataRecords = " + getDurationOfDataRecord());
        sb.append("\nNumber of signals = " + signalsCount());
        for (int i = 0; i < signalsCount(); i++) {
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

    /**
     * Unit Test. Usage Example.
     * <p>
     * Create and print default Edf and Bdf HeaderConfig
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        int numberOfSignals = 3;
        EdfHeader headerConfigEdf = new EdfHeader(FormatVersion.EDF_16BIT, numberOfSignals);
        EdfHeader headerConfigBdf = new EdfHeader(FormatVersion.BDF_24BIT, numberOfSignals);

        // set startRecording date and time for Bdf HeaderConfig
        headerConfigBdf.setRecordingStartDateTime(1972, 6, 23, 23, 23, 50);
        // print header info
        System.out.println(headerConfigEdf);
        System.out.println(headerConfigBdf);
    }
}
