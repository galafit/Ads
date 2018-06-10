package com.biorecorder.dataformat;

/**
 * This interface describes data format similar to the European Data Format (EDF)
 * which is a standard for exchange and storage of multichannel biological
 * and physical signals.
 * <p>
 * Tha base idea is that all samples received from multiple measuring channels (signals)
 * within the specified time interval
 * are placed in one data record (or package) as follows:
 * <br>n_0 samples belonging to signal 0,
 * <br>n_1 samples belonging to signal 1,
 * <br>...
 * <br>n_k samples belonging to  signal k
 * <p>
 * Every signal may have its own sample frequency!
 * <br>The number of samples n_i = (sample frequency of the signal_i) * (time interval).
 * <p>
 * In our case data record/package is just an array of integers. Every measured
 * digital sample is saved as one integer. A linear relationship between
 * digital (integer) values stored in data record/package and the corresponding
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
 * Full specification of EDF:
 * <a href="http://www.edfplus.info/specs/edf.html">European Data Format (EDF)</a>
  *
 */
public interface DataConfig {
    /**
     * Gets the measuring time interval or duration of data records (data package)
     * in seconds
     *
     * @return duration of data record in seconds
     */
    public  double getDurationOfDataRecord();

    /**
     * Gets the number of measuring channels (signals).
     *
     * @return the number of measuring signals
     */
    public  int signalsCount();


    /*****************************************************************
     *                   Signals Info                                *
     *****************************************************************/

    /**
     * Gets the number of samples n_i belonging to the given signal_i
     * in each data record (data package).
     * n_i = (sample frequency of the signal_i) * (duration of data record)
     * <p>
     * When duration of data record = 1 sec (default):
     * n_i = sample frequency of the signal_i
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return number of samples belonging to the given signal
     * in each data record
     */
    public  int getNumberOfSamplesInEachDataRecord(int signalNumber);


    /**
     * Gets the getLabel/name of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return getLabel of the signal
     */
    public  String getLabel(int signalNumber);

    /**
     * Gets getTransducer(electrodes) name ("AgAgCl cup electrodes", etc)
     * used for measuring data belonging to the signal.
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing getTransducer (electrodes) used for measuring
     */
    public  String getTransducer(int signalNumber);

    /**
     * Gets the filters names that were applied to the samples belonging to the signal
     * ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing filters that were applied
     */
    public  String getPrefiltering(int signalNumber);

    /**
     * Gets physical dimension or units of physical values
     * of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     * @param signalNumber     number of the signal (channel). Numeration starts from 0
     * @return String describing units of physical values of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public  String getPhysicalDimension(int signalNumber);

    /**
     * Specify the extreme minimum value of the samples belonging to the given
     * signal that can occur in data records.
     * These often are the extreme minimum output value of the A/D converter.
     * @param signalNumber  number of the signal (channel). Numeration starts from 0
     * @return minimum value of the samples belonging to the
     * signal that can occur in data records
     */
    public  int getDigitalMin(int signalNumber);

    /**
     * Specify the extreme maximum value of the samples belonging to the given
     * signal that can occur in data records.
     * These often are the extreme maximum output value of the A/D converter.
     * @param signalNumber  number of the signal (channel). Numeration starts from 0
     * @return maximum value of the samples belonging to the
     * signal that can occur in data records
     */
    public  int getDigitalMax(int signalNumber);

    /**
     * Specify the physical (usually also physiological) minimum
     * of the signal that corresponds to its digital minimum.
     * Physical minimum should be expressed in the physical dimension
     * (physical units) of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return physical minimum of the signal expressed in physical units
     * that corresponds to its digital minimum
     */
    public  double getPhysicalMin(int signalNumber);

    /**
     * Specify the physical (usually also physiological) maximum
     * of the signal that corresponds to its digital maximum.
     * Physical  maximum should be expressed in the physical dimension
     * (physical units) of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return physical  maximum of the signal expressed in physical units
     * that corresponds to its digital  maximum
     */
    public  double getPhysicalMax(int signalNumber);

    public static double gain(DataConfig dataConfig, int signalNumber) {
        return (dataConfig.getPhysicalMax(signalNumber) - dataConfig.getPhysicalMin(signalNumber)) / (dataConfig.getDigitalMax(signalNumber) - dataConfig.getDigitalMin(signalNumber));
    }

    public static double offset(DataConfig dataConfig, int signalNumber) {
        return dataConfig.getPhysicalMax(signalNumber) / gain(dataConfig, signalNumber) - dataConfig.getDigitalMax(signalNumber);
    }

    public static int physicalToDigital(DataConfig dataConfig, int signalNumber, double physValue) {
        return (int) (physValue / gain(dataConfig, signalNumber) - offset(dataConfig, signalNumber));
    }

    public static double digitalToPysical(DataConfig dataConfig, int signalNumber, int digValue) {
        return (digValue + offset(dataConfig, signalNumber)) * gain(dataConfig, signalNumber);
    }


}