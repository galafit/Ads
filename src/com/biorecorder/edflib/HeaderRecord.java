package com.biorecorder.edflib;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This class is wrapper around array of byte[]
 * storing header info that gives easy access to all fields.
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
 */
public class HeaderRecord {
    private static Charset ASCII = Charset.forName("US-ASCII");

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
    private byte[] headerBuffer;

    public HeaderRecord(File file) throws FileNotFoundException,  IOException, HeaderException {
        FileInputStream inputStream = new FileInputStream(file);
        numberOfSignals = 0;
        headerBuffer = readHeader(inputStream, numberOfSignals);
        try {
          int realNumberOfSignals = Integer.valueOf(getNumberOfSignals(headerBuffer));
            if(realNumberOfSignals > 0) {
                headerBuffer = readHeader(inputStream, realNumberOfSignals);
                numberOfSignals = realNumberOfSignals;
            }
        } catch (NumberFormatException ex) {
           // do nothing
        }

    }

    byte[] getBytes() {
        return headerBuffer;
    }

    private byte[] readHeader(InputStream inputStream, int numberOfSignals) throws  IOException, HeaderException {
        byte[] buffer = new byte [numberOfBytesInHeader(numberOfSignals)];
        if(inputStream.read(buffer) < buffer.length) {
            throw new HeaderException(HeaderException.TYPE_HEADER_NOT_COMPLETE);
        }
        return buffer;
    }

    public HeaderRecord(EdfHeader edfHeader) {
        // convert this HeaderConfig object to byte array
        String startDateOfRecording = new SimpleDateFormat("dd.MM.yy").format(new Date(edfHeader.getRecordingStartTimeMs()));
        String startTimeOfRecording = new SimpleDateFormat("HH.mm.ss").format(new Date(edfHeader.getRecordingStartTimeMs()));

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append(adjustLength(edfHeader.getDataFormat().getVersion(), VERSION_LENGTH - 1));  // -1 because first non ascii byte (or "0" for edf) we will add later
        headerBuilder.append(adjustLength(edfHeader.getPatientIdentification(), PATIENT_ID_LENGTH));
        headerBuilder.append(adjustLength(edfHeader.getRecordingIdentification(), RECORD_ID_LENGTH));
        headerBuilder.append(startDateOfRecording);
        headerBuilder.append(startTimeOfRecording);
        headerBuilder.append(adjustLength(Integer.toString(edfHeader.getNumberOfBytesInHeaderRecord()), NUMBER_OF_BYTES_IN_HEADER_LENGTH));
        headerBuilder.append(adjustLength(edfHeader.getDataFormat().getFirstReserved(), RESERVED_LENGTH));
        headerBuilder.append(adjustLength(Integer.toString(edfHeader.getNumberOfDataRecords()), NUMBER_Of_DATARECORDS_LENGTH));
        headerBuilder.append(adjustLength(double2String(edfHeader.getDurationOfDataRecord()), DURATION_OF_DATARECORD_LENGTH));
        headerBuilder.append(adjustLength(Integer.toString(edfHeader.signalsCount()), NUMBER_OF_SIGNALS_LENGTH));


        StringBuilder labels = new StringBuilder();
        StringBuilder transducerTypes = new StringBuilder();
        StringBuilder physicalDimensions = new StringBuilder();
        StringBuilder physicalMinimums = new StringBuilder();
        StringBuilder physicalMaximums = new StringBuilder();
        StringBuilder digitalMinimums = new StringBuilder();
        StringBuilder digitalMaximums = new StringBuilder();
        StringBuilder preFilterings = new StringBuilder();
        StringBuilder samplesNumbers = new StringBuilder();
        StringBuilder reservedForChannels = new StringBuilder();

        for (int i = 0; i < edfHeader.signalsCount(); i++) {
            labels.append(adjustLength(edfHeader.getLabel(i), SIGNAL_LABEL_LENGTH));
            transducerTypes.append(adjustLength(edfHeader.getTransducer(i), SIGNAL_TRANSDUCER_TYPE_LENGTH));
            physicalDimensions.append(adjustLength(edfHeader.getPhysicalDimension(i), SIGNAL_PHYSICAL_DIMENSION_LENGTH));
            physicalMinimums.append(adjustLength(double2String(edfHeader.getPhysicalMin(i)), SIGNAL_PHYSICAL_MIN_LENGTH));
            physicalMaximums.append(adjustLength(double2String(edfHeader.getPhysicalMax(i)), SIGNAL_PHYSICAL_MAX_LENGTH));
            digitalMinimums.append(adjustLength(String.valueOf(edfHeader.getDigitalMin(i)), SIGNAL_DIGITAL_MIN_LENGTH));
            digitalMaximums.append(adjustLength(String.valueOf(edfHeader.getDigitalMax(i)), SIGNAL_DIGITAL_MAX_LENGTH));
            preFilterings.append(adjustLength(edfHeader.getPrefiltering(i), SIGNAL_PREFILTERING_LENGTH));
            samplesNumbers.append(adjustLength(Integer.toString(edfHeader.getNumberOfSamplesInEachDataRecord(i)), SIGNAL_NUMBER_OF_SAMPLES_LENGTH));
            reservedForChannels.append(adjustLength("", SIGNAL_RESERVED_LENGTH));
        }

        headerBuilder.append(labels);
        headerBuilder.append(transducerTypes);
        headerBuilder.append(physicalDimensions);
        headerBuilder.append(physicalMinimums);
        headerBuilder.append(physicalMaximums);
        headerBuilder.append(digitalMinimums);
        headerBuilder.append(digitalMaximums);
        headerBuilder.append(preFilterings);
        headerBuilder.append(samplesNumbers);
        headerBuilder.append(reservedForChannels);
        // reserve space for first byte
        ByteBuffer byteBuffer = ByteBuffer.allocate(headerBuilder.length() + 1);
        byteBuffer.put(edfHeader.getDataFormat().getFirstByte());
        byteBuffer.put(stringToBytesASCII(headerBuilder.toString()));
        headerBuffer = byteBuffer.array();
    }

    private static int numberOfBytesInHeader(int numberOfSignals) {
        return 256 * (1 + numberOfSignals);
    }

    private static String getNumberOfSignals(byte[] buffer) {
        return bytesToStringASCII(buffer, NUMBER_OF_SIGNALS_OFFSET, NUMBER_OF_SIGNALS_LENGTH).trim();
    }


    public EdfHeader getHeaderInfo() throws HeaderException {

/******************** VERSION OF DATA FORMAT *********************************************/
        String versionString = bytesToStringASCII(headerBuffer, 0, 8);
        DataFormat dataFormat;
        byte firstByte = headerBuffer[0];
        if ((firstByte & 0xFF) == 255) { // BDF
            String version_ = bytesToStringASCII(headerBuffer, 1, VERSION_LENGTH - 1);
            String expectedVersion = "BIOSEMI";
            if(version_.equals(expectedVersion)) {
                dataFormat = DataFormat.BDF_24BIT;
            } else {
                throw new HeaderException(HeaderException.TYPE_VERSION_FORMAT_INVALID,versionString );
            }
        } else { // EDF
            String expectedVersion = adjustLength("0", VERSION_LENGTH);
            if(versionString.equals(expectedVersion)) {
                dataFormat = DataFormat.EDF_16BIT;
            } else {
                throw new HeaderException(HeaderException.TYPE_VERSION_FORMAT_INVALID, versionString);
            }
        }

/******************** NUMBER OF SIGNALS *********************************************/
        String numberOfSignalsString = numberOfSignals();
        int realNumberOfSignals;
        try {
            realNumberOfSignals = Integer.valueOf(numberOfSignalsString);
        } catch (NumberFormatException ex) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_SIGNALS_INVALID, numberOfSignalsString);
        }
        if(realNumberOfSignals < 0) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_SIGNALS_INVALID, numberOfSignalsString);
        }
/******************** START DATE AND TIME *********************************************/
        String dateString = recordingStartDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
        //if we want that a date object strictly matches the pattern, lenient has to be false
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new HeaderException(HeaderException.TYPE_DATE_FORMAT_INVALID, dateString);
        }

        String timeString = recordingStartTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH.mm.ss");
        timeFormat.setLenient(false);
        try {
            timeFormat.parse(timeString);
        } catch (ParseException e) {
            throw new HeaderException(HeaderException.TYPE_TIME_FORMAT_INVALID, timeString);
        }

        long startingDateTime = 0;
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yy HH.mm.ss");
        String dateTimeString = dateString + " " + timeString;
        try {
            startingDateTime = dateTimeFormat.parse(dateTimeString).getTime();
        } catch (ParseException e) {
            // This situation never should take place. If happens it is an error
            // and we should detect it apart
            new RuntimeException("DateTime parsing failed: "+dateTimeString+ ". Expected: "+"dd.MM.yy HH.mm.ss");
        }
/******************** NUMBER OF BYTES IN HEADER *********************************************/
        String numberOfBytesString = numberOfBytesInHeader();
        int numberOfBytes;
        try {
            numberOfBytes = Integer.valueOf(numberOfBytesString);
        } catch (NumberFormatException ex) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_BYTES_INVALID, numberOfBytesString);
        }
        if(numberOfBytes != 256 + 256 * realNumberOfSignals) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_BYTES_INVALID, numberOfBytesString);
        }
/******************** NUMBER OF DATA RECORDS *********************************************/
        String numberOfRecordsString = numberOfDataRecords();
        int numberOfRecords;
        try {
            numberOfRecords = Integer.valueOf(numberOfRecordsString);
        } catch (NumberFormatException ex) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_RECORDS_INVALID, numberOfRecordsString);
        }
        if(numberOfRecords < -1) {
            throw new HeaderException(HeaderException.TYPE_NUMBER_OF_RECORDS_INVALID, numberOfRecordsString);
        }
/******************** DURATION OF DATA RECORDS *********************************************/
        String recordDurationString = numberOfDataRecords();
        double recordDuration;
        try {
            recordDuration = Double.valueOf(recordDurationString);
        } catch (NumberFormatException ex) {
            throw new HeaderException(HeaderException.TYPE_RECORD_DURATION_INVALID, recordDurationString);
        }
        if(recordDuration < 0) {
            throw new HeaderException(HeaderException.TYPE_RECORD_DURATION_INVALID, recordDurationString);
        }

        EdfHeader edfHeader = new EdfHeader(dataFormat, realNumberOfSignals);
        edfHeader.setPatientIdentification(patientIdentification());
        edfHeader.setRecordingIdentification(recordingIdentification());
        edfHeader.setRecordingStartTimeMs(startingDateTime);
        edfHeader.setNumberOfDataRecords(numberOfRecords);
        edfHeader.setDurationOfDataRecord(recordDuration);

/*******************************************************************************
 *                            SIGNAL INFO                                      *
 *******************************************************************************/

        for (int i = 0; i < realNumberOfSignals; i++) {
/******************** PHYSICAL MAX AND MIN *********************************************/
           String physMinString = signalPhysicalMin(i);
           double physMin;
            try {
                physMin = Double.valueOf(physMinString);
            } catch (NumberFormatException ex) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_PHYSICAL_MIN_INVALID, physMinString, i);
            }
            String physMaxString = signalPhysicalMax(i);
            double physMax;
            try {
                physMax = Double.valueOf(physMaxString);
            } catch (NumberFormatException ex) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_PHYSICAL_MAX_INVALID, physMaxString, i);
            }
            if(physMax <= physMin) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_PHYSICAL_MAX_LOWER_OR_EQUAL_MIN, "max: "+physMax + ", min: "+physMin, i);
            }
/******************** DIGITAL MAX AND MIN *********************************************/
            String digMinString = signalDigitalMin(i);
            int digMin;
            try {
                digMin = Integer.valueOf(digMinString);
            } catch (NumberFormatException ex) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_DIGITAL_MIN_INVALID, digMinString, i);
            }
            if(dataFormat == DataFormat.EDF_16BIT && digMin < -32768 || dataFormat == DataFormat.BDF_24BIT && digMin < -8388608) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_DIGITAL_MIN_INVALID, digMinString, i);
            }

            String digMaxString = signalDigitalMax(i);
            int digMax;
            try {
                digMax = Integer.valueOf(digMaxString);
            } catch (NumberFormatException ex) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_DIGITAL_MAX_INVALID, digMaxString, i);
            }
            if(dataFormat == DataFormat.EDF_16BIT && digMax > 32767 || dataFormat == DataFormat.BDF_24BIT && digMax > 8388607) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_DIGITAL_MAX_INVALID, digMaxString, i);
            }

            if(digMax <= digMin) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_DIGITAL_MAX_LOWER_OR_EQUAL_MIN, "max: "+digMax + ", min: "+digMin, i);
            }
/******************** NUMBER OF SAMPLES PER RECORD *********************************************/
            String samplesString = signalNumberOfSamplesInDataRecord(i);
            int samples;
            try {
                samples = Integer.valueOf(samplesString);
            } catch (NumberFormatException ex) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_NUMBER_OF_SAMPLES_IN_RECORD_INVALID, samplesString, i);
            }
            if(samples < 0) {
                throw new HeaderException(HeaderException.TYPE_SIGNAL_NUMBER_OF_SAMPLES_IN_RECORD_INVALID, samplesString, i);
            }
            edfHeader.setNumberOfSamplesInEachDataRecord(i, samples);
            edfHeader.setPhysicalDimension(i, signalPhysicalDimension(i));
            edfHeader.setPhysicalRange(i, physMin, physMax);
            edfHeader.setDigitalRange(i, digMin, digMax);
            edfHeader.setLabel(i,signalLabel(i));
            edfHeader.setTransducer(i, signalTransducer(i));
            edfHeader.setPrefiltering(i, signalPrefiltering(i));
        }

        return edfHeader;
    }

    public byte[] dataFormatVersion() {
        return Arrays.copyOf(headerBuffer, VERSION_LENGTH);
    }

    public String patientIdentification() {
        return bytesToStringASCII(headerBuffer, PATIENT_ID_OFFSET, PATIENT_ID_LENGTH).trim();
    }

    public String recordingIdentification() {
        return bytesToStringASCII(headerBuffer, RECORD_ID_OFFSET, RECORD_ID_LENGTH).trim();
    }

    public String recordingStartDate() {
        return bytesToStringASCII(headerBuffer, STARTDATE_OFFSET, STARTDATE_LENGTH);
    }

    public String recordingStartTime() {
            return bytesToStringASCII(headerBuffer, STARTTIME_OFFSET, STARTTIME_LENGTH);
    }

    public String numberOfBytesInHeader() throws NumberFormatException {
        return bytesToStringASCII(headerBuffer, NUMBER_OF_BYTES_IN_HEADER_OFFSET, NUMBER_OF_BYTES_IN_HEADER_LENGTH).trim();
    }

    public String reserved() {
         return bytesToStringASCII(headerBuffer, RESERVED_OFFSET, RESERVED_LENGTH).trim();
    }

    public String numberOfDataRecords() throws NumberFormatException {
         return bytesToStringASCII(headerBuffer, NUMBER_Of_DATARECORDS_OFFSET, NUMBER_Of_DATARECORDS_LENGTH).trim();
    }

    public String durationOfDataRecord() throws NumberFormatException {
            return bytesToStringASCII(headerBuffer, DURATION_OF_DATARECORD_OFFSET, DURATION_OF_DATARECORD_LENGTH).trim();
    }

    public String numberOfSignals() throws NumberFormatException {
           return getNumberOfSignals(headerBuffer);
    }

    public String signalLabel(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_LABEL_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_LABEL_LENGTH).trim();
    }

    public String signalTransducer(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_TRANSDUCER_TYPE_OFFSET * numberOfSignals + SIGNAL_TRANSDUCER_TYPE_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_TRANSDUCER_TYPE_LENGTH).trim();
    }

    public String signalPhysicalDimension(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_DIMENSION_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_DIMENSION_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_PHYSICAL_DIMENSION_LENGTH).trim();
    }

    public String signalPhysicalMin(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_MIN_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_MIN_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_PHYSICAL_MIN_LENGTH).trim();
    }

    public String signalPhysicalMax(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_PHYSICAL_MAX_OFFSET * numberOfSignals + SIGNAL_PHYSICAL_MAX_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_PHYSICAL_MAX_LENGTH).trim();
    }

    public String signalDigitalMin(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_DIGITAL_MIN_OFFSET * numberOfSignals + SIGNAL_DIGITAL_MIN_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_DIGITAL_MIN_LENGTH).trim();
    }

    public String signalDigitalMax(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_DIGITAL_MAX_OFFSET * numberOfSignals + SIGNAL_DIGITAL_MAX_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_DIGITAL_MAX_LENGTH).trim();
    }

    public String signalPrefiltering(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_PREFILTERING_OFFSET * numberOfSignals + SIGNAL_PREFILTERING_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_PREFILTERING_LENGTH).trim();
    }

    public String signalNumberOfSamplesInDataRecord(int signalNumber) throws NumberFormatException {
        int offset = SIGNALS_OFFSET + SIGNAL_NUMBER_OF_SAMPLES_OFFSET * numberOfSignals + SIGNAL_NUMBER_OF_SAMPLES_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_NUMBER_OF_SAMPLES_LENGTH).trim();
    }

    public String signalReserved(int signalNumber) {
        int offset = SIGNALS_OFFSET + SIGNAL_RESERVED_OFFSET * numberOfSignals + SIGNAL_RESERVED_LENGTH * signalNumber;
        return bytesToStringASCII(headerBuffer, offset, SIGNAL_RESERVED_LENGTH).trim();
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

    // see https://www.javacodegeeks.com/2010/11/java-best-practices-char-to-byte-and.html
    private static byte[] stringToBytesASCII(String str) {
        byte[] b = new byte[str.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) str.charAt(i);
        }
        return b;
    }

    private static String bytesToStringASCII(byte[] b, int offset, int length) {
        return new String(b, offset, length, ASCII);
    }




}
