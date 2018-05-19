package com.biorecorder.bdfrecorder.edflib;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * This class permits to store the information of the header record
 * of EDF/BDF file that permits correctly extract data from the file.
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
 * <br>ns * 16 ascii : ns * label (e.g. EEG Fpz-Cz or Body temp) (mind item 9 of the additional EDF+ specs)
 * <br>ns * 80 ascii : ns * transducer type (e.g. AgAgCl electrode)
 * <br>ns * 8 ascii : ns * physical dimension (e.g. uV or degreeC)
 * <br>ns * 8 ascii : ns * physical minimum (e.g. -500 or 34)
 * <br>ns * 8 ascii : ns * physical maximum (e.g. 500 or 40)
 * <br>ns * 8 ascii : ns * digital minimum (e.g. -2048)
 * <br>ns * 8 ascii : ns * digital maximum (e.g. 2047)
 * <br>ns * 80 ascii : ns * prefiltering (e.g. HP:0.1Hz LP:75Hz)
 * <br>ns * 8 ascii : ns * nr of samples in each data record
 * <br>ns * 32 ascii : ns * reserved
 * <p>
 * More info see {@link EdfConfig}
 * <br>Detailed information about EDF/BDF format:
 * <a href="http://www.edfplus.info/specs/edf.html">European Data Format. Full specification of EDF</a>
 * <a href="https://www.biosemi.com/faq/file_format.htm">BioSemi or BDF file format</a>
 * <p>
 */
public class EdfHeader extends DefaultEdfConfig{
    private String patientIdentification = "Default patient";
    private String recordingIdentification = "Default record";
    private long recordingStartTime = -1;
    private int numberOfDataRecords = -1;
    private DataFormat dataFormat = DataFormat.EDF_16BIT;
    private int dataRecordLength;
    private int[] dataRecord;


    /**
     * This constructor creates a EdfHeader instance that specifies the
     * the type of the of the file where data records will be written: EDF_16BIT or BDF_24BIT
     * and the number of measuring channels (signals)
     *
     * @param dataFormat  EDF_16BIT or BDF_24BIT
     * @param numberOfSignals number of signals in data records
     * @throws IllegalArgumentException if numberOfSignals <= 0
     */
    public EdfHeader(DataFormat dataFormat, int numberOfSignals) throws IllegalArgumentException {
        super(numberOfSignals);
        this.dataFormat = dataFormat;
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
     * between the recording start time
     * and midnight, January 1, 1970 UTC.
     */
    public long getRecordingStartDateTimeMs() {
        return recordingStartTime;
    }


    /**
     * Sets recording start date and time.
     * If not called, EdfFileWriter will use the system date and time at runtime
     * since midnight, January 1, 1970 UTC.
     *
     * @param year   1970 - 3000
     * @param month  1 - 12
     * @param day    1 - 31
     * @param hour   0 - 23
     * @param minute 0 - 59
     * @param second 0 - 59
     */
    public void setRecordingStartDateTime(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        // in java month indexing from 0
        calendar.set(year, month - 1, day, hour, minute, second);
        this.recordingStartTime = calendar.getTimeInMillis();
    }


    /**
     * Sets recording start date and time measured in milliseconds,
     * since midnight, January 1, 1970 UTC.
     *
     * @param recordingStartTime the difference, measured in milliseconds,
     *                           between the recording start time
     *                           and midnight, January 1, 1970 UTC.
     */
    public void setRecordingStartDateTimeMs(long recordingStartTime) {
        this.recordingStartTime = recordingStartTime;
    }



    /**
     * Get the number of bytes in the EDF/BDF header record (when we will create it on the base of this HeaderConfig)
     *
     * @return number of bytes in EDF/BDF header = (number of signals + 1) * 256
     */
    public int getNumberOfBytesInHeaderRecord() {
        return 256 + (getSignalsCount() * 256);
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
     * Normally EdfFileWriter calculate and sets the number of DataRecords automatically when
     * we finish to write data to the EdfWileWriter and close it
     *
     * @param numberOfDataRecords number of DataRecords (data packages) in Edf/Bdf file
     */
    public void setNumberOfDataRecords(int numberOfDataRecords) {
        this.numberOfDataRecords = numberOfDataRecords;
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //  sb.append(super.toString());
        sb.append("file type = " + getDataFormat());
        sb.append("\nNumber of DataRecords = " + getNumberOfDataRecords());
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss");
        String timeStamp = dateFormat.format(new Date(getRecordingStartDateTimeMs()));
        sb.append("\nStart date and time = " + timeStamp + " (" + getRecordingStartDateTimeMs() + " ms)");
        sb.append("\nPatient identification = " + getPatientIdentification());
        sb.append("\nRecording identification = " + getRecordingIdentification());
        sb.append("\nDuration of DataRecords = " + getDurationOfDataRecord());
        sb.append("\nNumber of signals = " + getSignalsCount());
        for (int i = 0; i < getSignalsCount(); i++) {
            sb.append("\n  " + i + " label: " + getLabel(i)
                    + "; number of samples: " + getNumberOfSamplesInEachDataRecord(i)
                    + "; frequency: " + Math.round(getSampleFrequency(i))
                    + "; dig min: " + getDigitalMin(i) + "; dig max: " + getDigitalMax(i)
                    + "; phys min: " + getPhysicalMin(i) + "; phys max: " + getPhysicalMax(i)
                    + "; prefiltering: " + getPrefiltering(i)
                    + "; transducer: " + getTransducer(i)
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

        // set start date and time for Bdf HeaderConfig
        headerConfigBdf.setRecordingStartDateTime(1972, 6, 23, 23, 23, 50);
        // print header info
        System.out.println(headerConfigEdf);
        System.out.println(headerConfigBdf);
    }
}
