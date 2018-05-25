package com.biorecorder.bdfrecorder.dataformat;

/**
 * This class describes the data structure similar to the structure
 * of data records (data packages) used in the European Data Format (EDF)
 * which is a standard for exchange and storage of multichannel biological
 * and physical signals.
 * <p>
 * Tha base idea is that all samples received from multiple measuring channels (signals)
 * within the specified time interval (duration of  data record)
 * are written (packed) in one array as follows:
 * <br>n_0 samples belonging to signal 0,
 * <br>n_1 samples belonging to signal 1,
 * <br>...
 * <br>k samples belonging to  signal k
 * <p>
 * Where number of samples n_i = (sample frequency of the signal_i) * (duration of  data record).
 * <br> <b>Every signal may have its own sample frequency!</b>
 * <p>
 * A linear relationship between digital (integer) values stored
 * in data record/package and the corresponding measuring physical values are assumed.
 * For every signal its <b>digital minimum and maximum</b>
 * and the corresponding <b> physical minimum and maximum</b> must be specified.
 * It is supposed that:
 * <p>
 * (physValue - physMin) / (digValue - digMin)  = constant [Gain] = (physMax - physMin) / (digMax - digMin)
 * <p>
 * So for every signal it is easy to convert stored digital value to
 * the corresponding physical one and vice versa:
 * <br>digValue = (physValue / Gain) - Offset;
 * <br>physValue = (digValue + Offset) * Gain
 * <br>
 * Where scaling factors:
 * <br>Gain = (physMax - physMin) / (digMax - digMin)
 * <br>Offset = physMax / Gain) - digMax;
 * <p>
 * In general "Gain" refers to multiplication of a signal
 * and "Offset"  refer to addition to a signal, i.e. out = (in + Offset) * Gain
 * <p>
 * Detailed information about 16 bit European Data Format (EDF) and its 24 bit version -
 * BioSemi data format (BDF) can be viewed on the following links:
 * <br><a href="http://www.edfplus.info/specs/edf.html">European Data Format (EDF). Full specification</a>
 * <br><a href="https://www.biosemi.com/faq/file_format.htm">BioSemi file format (BDF)</a>
 * <br><a href="https://www.teuniz.net/edfbrowser/bdfplus%20format%20description.html">Differences between BDF+ and EDF+</a>
 *
 */
public interface DataConfig {
    /**
     * Gets duration of DataRecords (data packages).
     *
     * @return duration of DataRecords in seconds
     */
    public  double durationOfDataRecord();

    /**
     * Return the number of measuring channels (signals).
     *
     * @return the number of measuring channels
     */
    public  int signalsCount();


    /*****************************************************************
     *                   Signals Info                                *
     *****************************************************************/

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
    public  int numberOfSamplesInEachDataRecord(int signalNumber);


    /**
     * Gets the label of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return label of the signal
     */
    public  String label(int signalNumber);

    /**
     * Get transducer(electrodes) name ("AgAgCl cup electrodes", etc)
     * used for measuring data belonging to the signal).
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing transducer (electrodes) used for measuring
     */
    public  String transducer(int signalNumber);

    /**
     * Get the filters names that were applied to the samples belonging to the signal ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing filters that were applied to the signal
     */
    public  String prefiltering(int signalNumber);

    /**
     * Get physical dimension (units) of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     * @param signalNumber      number of the signal (channel). Numeration starts from 0
     * @return String describing physical dimension of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public  String physicalDimension(int signalNumber);

    public  int digitalMin(int signalNumber);

    public  int digitalMax(int signalNumber);

    public  double physicalMin(int signalNumber);

    public  double physicalMax(int signalNumber);

}