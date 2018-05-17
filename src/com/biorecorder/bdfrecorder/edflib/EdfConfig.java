package com.biorecorder.bdfrecorder.edflib;

/**
 * EDF/BDF data record (data package) is array of integers that
 * contains data from multiple measuring channel (signals) during
 * some time (duration of  data records). Signals may have different sample frequency.
 * Every data record has the following structure:
 * <br>samples belonging to signal 0,
 * <br>samples belonging to signal 1,
 * <br>...
 * <br>samples belonging to  signal n
 * <p>
 * SWhere number of samples for every signal:
 * <br>n_i = (sample frequency of the signal_i) * (duration of DataRecord).
 * <p>
 * This class describes the structure of EDF/BDF data records/packages
 * and provides all necessary information to extract data such as:
 * <br> Duration of data records
 * <br> Number of signals (method {@link #getSignalsCount())
 * <br> and the information about every measuring channel (signal):
 * <ul>
 * <li>signal label</li>
 * <li>transducer type (e.g. AgAgCI electrode)</li>
 * <li>physical dimension(e.g. uV or degree C)</li>
 * <li>physical minimum (e.g. -500 or 34)</li>
 * <li>physical maximum (e.g. 500 or 40)</li>
 * <li>digital minimum (e.g. -2048)</li>
 * <li>digital maximum (e.g. 2047)</li>
 * <li>prefiltering (e.g. HP:0.1Hz LP:75Hz)</li>
 * <li>number of samples in each data record</li>
 * </ul>
 * </p>
 * EDF/BDF format assumes a linear relationship between physical and digital values.
 * Every digital value (int) from data record belongin to some signal
 * can be converted to corresponding physical value (double) using its
 * <b> physical minimum and maximum</b> and the corresponding
 * <b> digital minimum and maximum </b>:
 * <br>(physValue - physMin) / (digValue - digMin)  = constant [Gain] = (physMax - physMin) / (digMax - digMin)
 * <p>
 *  So for every channel:
 * <br>digValue = (physValue / calculateGain) - Offset;
 * <br>physValue = (digValue + calculateOffset) * Gain
 * <br>
 * Where scaling factors:
 * <br>Gain = (physMax - physMin) / (digMax - digMin)
 * <br>Offset = physMax / Gain) - digMax;
 * <p>
 * In general "Gain" refers to multiplication of a signal
 * and "Offset"  refer to addition to a signal, i.e. out = (in + Offset) * Gain
 * <p>
 * Detailed information about EDF/BDF format:
 * <a href="http://www.edfplus.info/specs/edf.html">European Data Format. Full specification of EDF</a>
 * <a href="https://www.biosemi.com/faq/file_format.htm">BioSemi or BDF file format</a>
 * <p>
 */
public interface EdfConfig {

    /**
     * Return the number of measuring channels (signals).
     *
     * @return the number of measuring channels
     */
    public  int getSignalsCount();

    /**
     * Gets duration of DataRecords (data packages).
     *
     * @return duration of DataRecords in seconds
     */
    public  double getDurationOfDataRecord();

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
    public  int getNumberOfSamplesInEachDataRecord(int signalNumber);


    /**
     * Gets the label of the signal
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return label of the signal
     */
    public  String getLabel(int signalNumber);

    /**
     * Get transducer(electrodes) name ("AgAgCl cup electrodes", etc)
     * used for measuring data belonging to the signal).
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing transducer (electrodes) used for measuring
     */
    public  String getTransducer(int signalNumber);

    /**
     * Get the filters names that were applied to the samples belonging to the signal ("HP:0.1Hz", "LP:75Hz N:50Hz", etc.).
     *
     * @param signalNumber number of the signal (channel). Numeration starts from 0
     * @return String describing filters that were applied to the signal
     */
    public  String getPrefiltering(int signalNumber);

    /**
     * Get physical dimension (units) of the signal ("uV", "BPM", "mA", "Degr.", etc.).
     * @param signalNumber      number of the signal (channel). Numeration starts from 0
     * @return String describing physical dimension of the signal ("uV", "BPM", "mA", "Degr.", etc.)
     */
    public  String getPhysicalDimension(int signalNumber);

    public  int getDigitalMin(int signalNumber);

    public  int getDigitalMax(int signalNumber);

    public  double getPhysicalMin(int signalNumber);

    public  double getPhysicalMax(int signalNumber);

}
