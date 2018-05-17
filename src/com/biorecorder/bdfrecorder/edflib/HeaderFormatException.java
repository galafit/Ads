package com.biorecorder.bdfrecorder.edflib;


/**
 * This exception tells that Edf/Bdf file header has some sort of error
 * or invalid info. The type of error is described by exceptionType.
 * <p>
 * Idea is that appropriate
 * exception message for the user could be generated on any app level only on the base
 * of exception type,  wrongValues and signalNumber.
 * Message string of the exception serves only for developers,
 * logging and debugging. It should not be shown to final users
 * <p>
 * Идея заключается в том чтобы в идеала соответсвующее сообщение об ошибке
 * могло генериться на любом уровне лишь на основании типа исключения и содержащихся
 * в исключении параметров. Message string служит лишь информацией для разработчиков и не должен
 * выводиться клиенты
 *
 */
public class HeaderFormatException extends Exception {
    public static final String TYPE_VERSION_FORMAT_INVALID = "Version format invalid";
    public static final String TYPE_DATE_FORMAT_INVALID = "Date format invalid. Expected: dd.mm.yy";
    public static final String TYPE_TIME_FORMAT_INVALID = "Time format invalid. Expected: hh.mm.ss";
    public static final String TYPE_RECORD_DURATION_NAN = "Duration of data record can not be converted to double";
    public static final String TYPE_RECORD_DURATION_NEGATIVE = "Duration of data record < 0";
    public static final String TYPE_NUMBER_OF_SIGNALS_NAN = "Number of signals can not be converted to int";
    public static final String TYPE_NUMBER_OF_SIGNALS_NEGATIVE = "Number of signals < 0";
    public static final String TYPE_SIGNAL_PHYSICAL_MIN_NAN = "Physical min can not be converted to double";
    public static final String TYPE_SIGNAL_PHYSICAL_MAX_NAN = "Physical max can not be converted to double";
    public static final String TYPE_SIGNAL_DIGITAL_MIN_NAN = "Digital min can not be converted to int";
    public static final String TYPE_SIGNAL_DIGITAL_MAX_NAN = "Digital max can not be converted to int";
    public static final String TYPE_SIGNAL_PHYSICAL_MAX_LOWER_OR_EQUAL_MIN = "Physical max <= Physical min";
    public static final String TYPE_SIGNAL_DIGITAL_MAX_LOWER_OR_EQUAL_MIN = "Digital max <= Digital min";
    public static final String TYPE_SIGNAL_NUMBER_OF_SAMPLES_IN_RECORD_NAN = "Number of samples in data record can not be converted to int";
    public static final String TYPE_SIGNAL_NUMBER_OF_SAMPLES_IN_RECORD_NEGATIVE = "Number of samples in data record < 0";

    private String exceptionType;
    private String wrongValues;
    private int signalNumber = -1;


    public HeaderFormatException(String exceptionType) {
        super(exceptionType);
        this.exceptionType = exceptionType;
    }

    public HeaderFormatException(String exceptionType, String value) {
        super(exceptionType + ". Read:  ");
        this.exceptionType = exceptionType;
        this.wrongValues = value;
    }

    public HeaderFormatException(String exceptionType, String value, int signalNumber) {
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
