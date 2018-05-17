package com.biorecorder.bdfrecorder.edflib;

import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
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
 */
public class HeaderRecord {
    private static Charset ASCII = Charset.forName("US-ASCII");
    private static String failedToReadHeaderErrMsg = "Failed to read header record.";

    private static final int VERSION_LENGTH = 8;
    private static final int PATIENT_ID_LENGTH = 80;
    private static final int RECORD_ID_LENGTH = 80;
    private static final int STARTDATE_LENGTH = 8;
    private static final int STARTTIME_LENGTH = 8;
    private static final int NUMBER_OF_BYTES_IN_HEADER_LENGTH = 8;
    private static final int RESERVED_LENGTH = 44;
    private static final int NUMBER_Of_DATARECORDS_LENGTH = 8;
    private static final int DURATION_OF_DATARECORD_LENGTH = 8;
    private static final int NUMBER_OF_SIGNALS_LENGTH = 4;

    private static final int SIGNAL_LABEL_LENGTH = 16;
    private static final int SIGNAL_TRANSDUCER_TYPE_LENGTH = 80;
    private static final int SIGNAL_PHYSICAL_DIMENSION_LENGTH = 8;
    private static final int SIGNAL_PHYSICAL_MIN_LENGTH = 8;
    private static final int SIGNAL_PHYSICAL_MAX_LENGTH = 8;
    private static final int SIGNAL_DIGITAL_MIN_LENGTH = 8;
    private static final int SIGNAL_DIGITAL_MAX_LENGTH = 8;
    private static final int SIGNAL_PREFILTERING_LENGTH = 80;
    private static final int SIGNAL_NUMBER_OF_SAMPLES_LENGTH = 8;
    private static final int SIGNAL_RESERVED_LENGTH = 32;

    private static final int PATIENT_ID_OFFSET = VERSION_LENGTH;
    private static final int RECORD_ID_OFFSET = PATIENT_ID_OFFSET + PATIENT_ID_LENGTH;
    private static final int STARTDATE_OFFSET = RECORD_ID_OFFSET + RECORD_ID_LENGTH;
    private static final int STARTTIME_OFFSET = STARTDATE_OFFSET + STARTDATE_LENGTH;
    private static final int NUMBER_OF_BYTES_IN_HEADER_OFFSET = STARTTIME_OFFSET + STARTTIME_LENGTH;
    private static final int RESERVED_OFFSET = NUMBER_OF_BYTES_IN_HEADER_OFFSET + NUMBER_OF_BYTES_IN_HEADER_LENGTH;
    private static final int NUMBER_Of_DATARECORDS_OFFSET = RESERVED_OFFSET + RESERVED_LENGTH;
    private static final int DURATION_OF_DATARECORD_OFFSET = NUMBER_Of_DATARECORDS_OFFSET + NUMBER_Of_DATARECORDS_LENGTH;
    private static final int NUMBER_OF_SIGNALS_OFFSET = DURATION_OF_DATARECORD_OFFSET + DURATION_OF_DATARECORD_LENGTH;

    private static final int SIGNALS_OFFSET = NUMBER_OF_SIGNALS_OFFSET + NUMBER_OF_SIGNALS_LENGTH;

    private static final int SIGNAL_TRANSDUCER_TYPE_OFFSET = SIGNAL_LABEL_LENGTH;
    private static final int SIGNAL_PHYSICAL_DIMENSION_OFFSET = SIGNAL_TRANSDUCER_TYPE_OFFSET + SIGNAL_TRANSDUCER_TYPE_LENGTH;
    private static final int SIGNAL_PHYSICAL_MIN_OFFSET = SIGNAL_PHYSICAL_DIMENSION_OFFSET + SIGNAL_PHYSICAL_DIMENSION_LENGTH;
    private static final int SIGNAL_PHYSICAL_MAX_OFFSET = SIGNAL_PHYSICAL_MIN_OFFSET + SIGNAL_PHYSICAL_MIN_LENGTH;
    private static final int SIGNAL_DIGITAL_MIN_OFFSET = SIGNAL_PHYSICAL_MAX_OFFSET + SIGNAL_PHYSICAL_MAX_LENGTH;
    private static final int SIGNAL_DIGITAL_MAX_OFFSET = SIGNAL_DIGITAL_MIN_OFFSET + SIGNAL_DIGITAL_MIN_LENGTH;
    private static final int SIGNAL_PREFILTERING_OFFSET = SIGNAL_DIGITAL_MAX_OFFSET + SIGNAL_DIGITAL_MAX_LENGTH;
    private static final int SIGNAL_NUMBER_OF_SAMPLES_OFFSET = SIGNAL_PREFILTERING_OFFSET + SIGNAL_PREFILTERING_LENGTH;
    private static final int SIGNAL_RESERVED_OFFSET = SIGNAL_NUMBER_OF_SAMPLES_OFFSET + SIGNAL_NUMBER_OF_SAMPLES_LENGTH;

    private int numberOfSignals;
    private char[] headerBuffer;

    public HeaderRecord(File file) throws FileNotFoundException,  FailedReadHeaderException {
        Reader reader = new InputStreamReader(new FileInputStream(file), ASCII);
        long fileSize = file.length();
        numberOfSignals = 0;
        headerBuffer = readHeader(reader, numberOfSignals, fileSize);
        try {
          int realNumberOfSignals = Integer.valueOf(getNumberOfSignals(headerBuffer));
            if(realNumberOfSignals > 0) {
                headerBuffer = readHeader(reader, realNumberOfSignals, fileSize);
                numberOfSignals = realNumberOfSignals;
            }
        } catch (NumberFormatException ex) {
           // do nothing
        }
    }

    private char[] readHeader(Reader reader, int numberOfSignals, long fileSize) throws  FailedReadHeaderException {
        char[] buffer = new char [numberOfBytesInHeader(numberOfSignals)];
        int numberCharactersRead = 0;
        try {
            numberCharactersRead = reader.read(headerBuffer);
        } catch (IOException e) {
            throw new FailedReadHeaderException(failedToReadHeaderErrMsg, e);
        }
        if(numberCharactersRead < headerBuffer.length) {
            throw new FailedReadHeaderException(failedToReadHeaderErrMsg + " file size: "+fileSize+", header record size: "+ buffer.length);
        }
        return buffer;
    }

    private static int numberOfBytesInHeader(int numberOfSignals) {
        return 256 * (1 + numberOfSignals);
    }

    private static String getNumberOfSignals(char[] buffer) {
        return new String(buffer, NUMBER_OF_SIGNALS_OFFSET, NUMBER_OF_SIGNALS_LENGTH).trim();
    }

    private DataFormat getDataFormat() throws HeaderFormatException {
        DataFormat dataFormat;
        String version = new String(headerBuffer, 0, 8);
        char firstChar = headerBuffer[0];
        if ((Character.getNumericValue(firstChar) & 0xFF) == 255) { // BDF
            String version_ = new String(headerBuffer, 1, VERSION_LENGTH - 1);
            String expectedVersion = "BIOSEMI";
            if(version_.equals(expectedVersion)) {
                dataFormat = DataFormat.BDF_24BIT;
            } else {
                throw new HeaderFormatException(HeaderFormatException.TYPE_VERSION_FORMAT_INVALID, );
            }
        } else { // EDF
            String expectedVersion = adjustLength("0", VERSION_LENGTH);
            if(version.equals(expectedVersion)) {
                dataFormat = DataFormat.EDF_16BIT;
            } else {
                throw new HeaderFormatException(HeaderFormatException.TYPE_VERSION_FORMAT_INVALID, );
            }
        }
        return dataFormat;
    }

    public EdfConfig getHeaderInfo() throws HeaderFormatException {
        /**
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
         */
        String numberOfSignalsString = numberOfSignals();
        int realNumberOfSignals;
        try {
            realNumberOfSignals = Integer.valueOf(numberOfSignals());
        } catch (NumberFormatException ex) {
            throw new HeaderFormatException(HeaderFormatException.TYPE_NUMBER_OF_SIGNALS_NAN, numberOfSignalsString);
        }
        if(realNumberOfSignals < 0) {
            throw new HeaderFormatException(HeaderFormatException.TYPE_NUMBER_OF_SIGNALS_NEGATIVE, numberOfSignalsString);
        }

        String dateString = recordingStartDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
        //if we want that a date object strictly matches the pattern, lenient has to be false
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new HeaderFormatException(HeaderFormatException.TYPE_DATE_FORMAT_INVALID, dateString);
        }

        String timeString = recordingStartTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH.mm.ss");
        timeFormat.setLenient(false);
        try {
            timeFormat.parse(timeString);
        } catch (ParseException e) {
            throw new HeaderFormatException(HeaderFormatException.TYPE_TIME_FORMAT_INVALID, timeString);
        }

        long startingDateTime;
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yy HH.mm.ss");
        String dateTimeString = dateString + " " + timeString;
        try {
            startingDateTime = dateTimeFormat.parse(dateTimeString).getTime();
        } catch (ParseException e) {
            // This situation never should take place. If happens it is an error
            // and we should detect it apart
            new RuntimeException("DateTime parsing failed: "+dateTimeString+ ". Expected: "+"dd.MM.yy HH.mm.ss");
        }

        String numberOfDataRecordsString = numberOfDataRecords();
        long numberOfDataRecords = ;



        EdfHeader edfHeader = new EdfHeader(getDataFormat(), realNumberOfSignals);
        edfHeader.setPatientIdentification(patientIdentification());
        edfHeader.setRecordingIdentification(recordingIdentification());


    }

    public char[] dataFormatVersion() {
        return Arrays.copyOf(headerBuffer, VERSION_LENGTH);
    }

    public String patientIdentification() {
        return new String(headerBuffer, PATIENT_ID_OFFSET, PATIENT_ID_LENGTH).trim();
    }

    public String recordingIdentification() {
        return new String(headerBuffer, RECORD_ID_OFFSET, RECORD_ID_LENGTH).trim();
    }

    public String recordingStartDate() {
        return new String(headerBuffer, STARTDATE_OFFSET, STARTDATE_LENGTH);
    }

    public String recordingStartTime() {
            return new String(headerBuffer, STARTTIME_OFFSET, STARTTIME_LENGTH);
    }

    public String numberOfBytesInHeader() throws NumberFormatException {
        return new String(headerBuffer, NUMBER_OF_BYTES_IN_HEADER_OFFSET, NUMBER_OF_BYTES_IN_HEADER_LENGTH).trim();
    }

    public String reserved() {
         return new String(headerBuffer, RESERVED_OFFSET, RESERVED_LENGTH).trim();
    }

    public String numberOfDataRecords() throws NumberFormatException {
         return new String(headerBuffer, NUMBER_Of_DATARECORDS_OFFSET, NUMBER_Of_DATARECORDS_LENGTH).trim();
    }

    public String durationOfDataRecord() throws NumberFormatException {
            return new String(headerBuffer, DURATION_OF_DATARECORD_OFFSET, DURATION_OF_DATARECORD_LENGTH).trim();
    }

    public String numberOfSignals() throws NumberFormatException {
           return getNumberOfSignals(headerBuffer);
    }

    public String signalLabel(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_LABEL_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_LABEL_LENGTH).trim();
    }

    public String signalTransducer(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_TRANSDUCER_TYPE_OFFSET * numberOfSignals + SIGNAL_TRANSDUCER_TYPE_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_TRANSDUCER_TYPE_LENGTH).trim();
    }

    public String signalPhysicalDimension(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_DIMENSION_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_DIMENSION_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_PHYSICAL_DIMENSION_LENGTH).trim();
    }

    public String signalPhysicalMin(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_MIN_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_MIN_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_PHYSICAL_MIN_LENGTH).trim();
    }

    public String signalPhysicalMax(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_MAX_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_MAX_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_PHYSICAL_MAX_LENGTH).trim();
    }

    public String signalDigitalMin(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_DIGITAL_MIN_OFFSET * numberOfSignals + SIGNAL_DIGITAL_MIN_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_DIGITAL_MIN_LENGTH).trim();
    }

    public String signalDigitalMax(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_DIGITAL_MAX_OFFSET * numberOfSignals + SIGNAL_DIGITAL_MAX_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_DIGITAL_MAX_LENGTH).trim();
    }

    public String signalPrefiltering(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_PREFILTERING_OFFSET * numberOfSignals + SIGNAL_PREFILTERING_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_PREFILTERING_LENGTH).trim();
    }

    public String signalNumberOfSamplesInDataRecord(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_NUMBER_OF_SAMPLES_OFFSET * numberOfSignals + SIGNAL_NUMBER_OF_SAMPLES_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_NUMBER_OF_SAMPLES_LENGTH).trim();
    }

    public String signalReserved(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_RESERVED_OFFSET * numberOfSignals + SIGNAL_RESERVED_LENGTH * signalNumber;
        return new String(headerBuffer, offset, SIGNAL_RESERVED_LENGTH).trim();
    }


    /**
     * Convert String to int
     *
     * @param str string to convert
     * @return resultant int
     * @throws NumberFormatException - if the string does not contain a parsable integer.
     */
    private static Integer stringToInt(String str) throws NumberFormatException {
        str = str.trim();
        return Integer.valueOf(str);
    }

    /**
     * Convert String to double
     *
     * @param str string to convert
     * @return resultant double
     * @throws NumberFormatException - if the string does not contain a parsable double.
     */
    private static Double stringToDouble(String str) throws NumberFormatException {
        str = str.trim();
        return Double.valueOf(str);
    }


    /**
     * if the String.length() is more then the given length cut the String to the given length
     * if the String.length() is less then the given length append spaces to the end of the String
     *
     * @param text   - string which length should be adjusted
     * @param length - desired length
     * @return resultant string with the given length
     */
    private static String adjustLength(String text, int length) {
        StringBuilder sB = new StringBuilder(text);
        if (text.length() > length) {
            sB.delete(length, text.length());
        } else {
            for (int i = text.length(); i < length; i++) {
                sB.append(" ");
            }
        }
        return sB.toString();
    }

    /**
     * Convert double to the string with format valid for EDF and BDF header - "%.6f".
     *
     * @param value double that should be converted to the string
     * @return resultant string with format valid for EDF and BDF header
     */
    private static String double2String(double value) {
        return String.format("%.6f", value).replace(",", ".");
    }



}
