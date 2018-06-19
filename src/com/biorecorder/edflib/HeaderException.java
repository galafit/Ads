package com.biorecorder.edflib;


/**
 * This exception tells that Edf/Bdf file header has some sort of error
 * or invalid info. The type of error is described by exceptionType.
 * <p>
 * Idea is that appropriate
 * exception message for the user could be generated on any app level only on the base
 * of exception type,  wrongValues and signalNumber.
 * StateChangeReason string of the exception serves only for developers,
 * logging and debugging. It should not be shown to final users
 * <p>
 * Идея заключается в том чтобы в идеала соответсвующее сообщение об ошибке
 * могло генериться на любом уровне лишь на основании типа исключения и содержащихся
 * в исключении параметров. StateChangeReason string служит лишь информацией для разработчиков и не должен
 * выводиться клиенты
 *
 */
public class HeaderException extends Exception {
    public static final String TYPE_HEADER_NOT_COMPLETE = "Header record is not complete";
    public static final String TYPE_VERSION_FORMAT_INVALID = "Version format invalid";
    public static final String TYPE_DATE_FORMAT_INVALID = "Date format invalid. Expected: dd.mm.yy";
    public static final String TYPE_TIME_FORMAT_INVALID = "Time format invalid. Expected: hh.mm.ss";
    public static final String TYPE_NUMBER_OF_BYTES_INVALID = "Number of bytes in header invalid. Expected: 256 + 256 * number of signals";
    public static final String TYPE_NUMBER_OF_RECORDS_INVALID = "Number of data records invalid. Expected int >= -1";
    public static final String TYPE_RECORD_DURATION_INVALID = "Duration of data record invalid. Expected double >= 0";
    public static final String TYPE_NUMBER_OF_SIGNALS_INVALID = "Number of signals invalid. Expected int >= 0";
    public static final String TYPE_SIGNAL_PHYSICAL_MIN_INVALID = "Physical min invalid. Expected double";
    public static final String TYPE_SIGNAL_PHYSICAL_MAX_INVALID = "Physical max invalid. Expected double";
    public static final String TYPE_SIGNAL_DIGITAL_MIN_INVALID = "Digital min invalid. Expected int >= -32768 (-8388608) for EDF (BDF)";
    public static final String TYPE_SIGNAL_DIGITAL_MAX_INVALID = "Digital max invalid. Expected int <= 32767 (8388607) for EDF (BDF)";
    public static final String TYPE_SIGNAL_PHYSICAL_MAX_LOWER_OR_EQUAL_MIN = "Physical max <= Physical min";
    public static final String TYPE_SIGNAL_DIGITAL_MAX_LOWER_OR_EQUAL_MIN = "Digital max <= Digital min";
    public static final String TYPE_SIGNAL_NUMBER_OF_SAMPLES_IN_RECORD_INVALID = "Number of samples in data record invalid. Expected int >= 0";

    private String exceptionType;
    private String wrongValues;
    private int signalNumber = -1;


    public HeaderException(String exceptionType) {
        super(exceptionType);
        this.exceptionType = exceptionType;
    }

    public HeaderException(String exceptionType, String value) {
        super(exceptionType + ". Read:  ");
        this.exceptionType = exceptionType;
        this.wrongValues = value;
    }

    public HeaderException(String exceptionType, String value, int signalNumber) {
        super("SignalNumber: "+signalNumber+ " " + exceptionType + ". Read:  ");
        this.exceptionType = exceptionType;
        this.wrongValues = value;
        this.signalNumber = signalNumber;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getWrongValues() {
        return wrongValues;
    }

    public int getSignalNumber() {
        return signalNumber;
    }
}
