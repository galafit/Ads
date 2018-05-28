package com.biorecorder.edflib;

import java.io.*;
import java.util.Arrays;

/**
 * Permits to read data samples from EDF or BDF file. Also it
 * reads information from the file header and saves it
 * in special {@link EdfHeader} object, that we
 * can get by method {@link #getHeader()}
 * <p>
 * EDF/BDF files contains "row" digital (int) data but they can be converted to corresponding
 * real physical floating point data on the base of header information (physical maximum and minimum
 * and digital maximum and minimum specified for every channel (signal)).
 * So we can "read" both digital or physical values.
 * See: {@link #readDigitalSamples(int, int[], int, int)}, {@link #readPhysicalSamples(int, double[], int, int)}
 * {@link #readDigitalDataRecord(int[])}, {@link #readPhysicalDataRecord(double[])}.
 */
public class EdfReader {
    private EdfHeader header;
    private FileInputStream fileInputStream;
    private File file;
    private long[] samplesPositionList;
    private long recordPosition = 0;
    private final int recordSize;

    /**
     * Creates EdfFileReader to read data from the file represented by the specified
     * File object.
     *
     * @param file Edf or Bdf file to be opened for reading
     * @throws FileNotFoundException if the file does not exist,
     *                               is a directory rather than a regular file,
     *                               or for some other reason cannot be opened for reading.
     * @throws HeaderException if the the file is not valid EDF/BDF file
     *                               due to some errors in its header record
     * @throws IOException if an I/O error occurs
     */
    public EdfReader(File file) throws FileNotFoundException, HeaderException, IOException {
        this.file = file;
        fileInputStream = new FileInputStream(file);
        header = new HeaderRecord(file).getHeaderInfo();
        samplesPositionList = new long[header.signalsCount()];
        recordSize = header.getDataRecordSize();
    }

    /**
     * Set the sample position indicator of the given channel (signal)
     * to the given new position. The position is measured in samples.
     * <p>
     * Note that every signal has it's own independent sample position indicator and
     * setSamplePosition() affects only one of them.
     * Methods {@link #readDigitalSamples(int, int[], int, int)} and
     * {@link #readPhysicalSamples(int, double[], int, int)} will start reading
     * samples belonging to a channel from the specified for that channel position.
     *
     * @param signalNumber channel (signal) number whose sample position we change. Numbering starts from 0!
     * @param newPosition  the new sample position, a non-negative integer counting
     *                     the number of samples belonging to the specified
     *                     channel from the beginning of the file
     */
    public void setSamplePosition(int signalNumber, long newPosition) {
        samplesPositionList[signalNumber] = newPosition;
    }

    /**
     * Return the current sample position  of the given channel (signal).
     * The position is measured in samples.
     *
     * @param signalNumber channel (signal) number whose position we want to get. Numbering starts from 0!
     * @return current sample position, a non-negative integer counting
     * the number of samples belonging to the given
     * channel from the beginning of the file
     */
    public long getSamplePosition(int signalNumber) {
        return samplesPositionList[signalNumber];
    }


    /**
     * Return the current data record position.
     * The position is measured in DataRecords.
     *
     * @return current DataRecord position, a non-negative integer counting
     * the number of DataRecords from the beginning of the file
     */
    public long getRecordPosition() {
        return recordPosition;
    }

    /**
     * Set the DataRecords position indicator to the given new position.
     * The position is measured in DataRecords. Methods {@link #readDigitalDataRecord(int[])} and
     * {@link #readPhysicalDataRecord(double[])} will start reading from the specified position.
     *
     * @param newPosition the new position, a non-negative integer counting
     *                    the number of data records from the beginning of the file
     */
    public void setRecordPosition(long newPosition) {
        recordPosition = newPosition;
    }



    /**
     * Read n samples belonging to the  signal
     * starting from the current sample position indicator.
     * The values are the "raw" digital (integer) values.
     * <p>
     * The sample position indicator of that channel will be increased
     * with the amount of samples read (this can be less than n or zero!)
     * Return the array with samples read.
     * @param signal channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param n       number of samples to read
     * @return array with digital samples read.
     * The amount of read samples can be less than n or zero
     * @throws IOException  if an I/O error occurs
     */
    public int[] readDigitalSamples(int signal, int n) throws IOException {
        int[] samples = new int[n];
        int bytesPerSample = header.getDataFormat().getNumberOfBytesPerSample();
        int samplesPerRecord = header.getNumberOfSamplesInEachDataRecord(signal);

        long recordNumber = samplesPositionList[signal] / samplesPerRecord;
        int signalStartPositionInRecord = 0;
        for (int i = 0; i < signal; i++) {
            signalStartPositionInRecord += header.getNumberOfSamplesInEachDataRecord(i);
        }
        int sampleStartOffset = (int)(samplesPositionList[signal] % samplesPerRecord);
        long fileReadPosition = header.getNumberOfBytesInHeaderRecord() + (recordNumber * recordSize + signalStartPositionInRecord + sampleStartOffset) * bytesPerSample;

        // set file start reading position and read
        fileInputStream.getChannel().position(fileReadPosition);
        byte[] byteData = new byte[samplesPerRecord * bytesPerSample];
        int totalReadBytes = 0;
        int bytesToRead = Math.min((samplesPerRecord - sampleStartOffset) * bytesPerSample, n * bytesPerSample - totalReadBytes) ;

        while (totalReadBytes < n * bytesPerSample) {
            int readBytes = fileInputStream.read(byteData, 0, bytesToRead);
            EndianBitConverter.littleEndianByteArrayToIntArray(byteData, 0, samples, totalReadBytes/samplesPerRecord, readBytes/bytesPerSample, bytesPerSample);
            totalReadBytes += readBytes;
            if(readBytes < bytesToRead) { // end of file
                break;
            }
            fileInputStream.skip((recordSize - samplesPerRecord) * bytesPerSample);
            bytesToRead = Math.min(samplesPerRecord * bytesPerSample, n * bytesPerSample - totalReadBytes) ;
        }
        int readSamples = totalReadBytes/bytesPerSample;
        samplesPositionList[signal] += readSamples;
        if(readSamples == n) {
            return samples;
        } else {
            return Arrays.copyOfRange(samples, 0, readSamples);
        }
    }


    /**
     * Read n samples belonging to the  signal
     * starting from the current sample position indicator.
     * Converts the read samples
     * to their physical values (e.g. microVolts, beats per minute, etc).
     * <p>
     * The sample position indicator of that channel will be increased
     * with the amount of samples read (this can be less than n or zero!).
     * Return the array with physical samples read
     * @param signalNumber channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param n       number of samples to read
     * @return array with physical samples read.
     * The amount of samples read can be less than n or zero
     * @throws IOException  if an I/O error occurs
     */
    public double[] readPhysicalSamples(int signalNumber, int n) throws IOException {
        int[] digSamples =  readDigitalSamples(signalNumber, n);
        double [] physSamples = new double[digSamples.length];
        for (int i = 0; i < digSamples.length; i++) {
            physSamples[i] = header.digitalValueToPhysical(signalNumber, digSamples[i]);
        }
        return physSamples;
    }

    /**
     * Read n data records
     * starting from the current record position indicator.
     * The values are the "raw" digital (integer) values.
     * <p>
     * The record position indicator will be increased with the amount of data records
     * read (this can be less than n or zero!)
     * Return the array with data records read
     * @param n       number of data records to read
     * @return array with digital data records read.
     * The amount of records read can be less than n or zero
     * @throws IOException  if an I/O error occurs
     */
    public int[] readDigitalRecords(int n) throws IOException {
        int bytesPerSample = header.getDataFormat().getNumberOfBytesPerSample();
        long fileReadPosition = header.getNumberOfBytesInHeaderRecord() +
                recordSize * recordPosition * bytesPerSample;
        fileInputStream.getChannel().position(fileReadPosition);

        int[] records = new int[recordSize * n];
        byte[] byteData = new byte[recordSize * n * bytesPerSample];
        int readBytes = fileInputStream.read(byteData, 0, recordSize * n);
        EndianBitConverter.littleEndianByteArrayToIntArray(byteData, 0, records, 0, readBytes / bytesPerSample, bytesPerSample);
        int readRecords = readBytes / (recordSize * bytesPerSample);
        if(readRecords == n) {
            return records;
        } else {
            return Arrays.copyOfRange(records, 0, readRecords * recordSize);
        }
    }

    /**
     * Read n data records
     * starting from the current record position indicator.
     * Convert the read samples
     * to their physical values (e.g. microVolts, beats per minute, etc).
     * <p>
     * The record position indicator will be increased with the amount
     * of data records read (this can be less than n or zero!).
     * Return the array with data records read.
     * @param n       number of data records to read
     * @return array with physical data records read.
     * The amount of records read can be less than n or zero
     * @throws IOException  if an I/O error occurs
     */
    public double[] readPhysicalDataRecords(int n) throws IOException {
        int[] digRecords =  readDigitalRecords(n);
        double [] physRecords = new double[digRecords.length];
        int signal = 0;
        int counter = 0;
        for (int i = 0; i < digRecords.length; i++) {
            physRecords[i] = header.digitalValueToPhysical(signal, digRecords[i]);
            counter++;
            if(counter == header.getNumberOfSamplesInEachDataRecord(signal)){
                signal++;
                counter = 0;
            }
            if(signal == header.signalsCount()) {
                signal = 0;
            }
        }
        return physRecords;
    }


    /**
     * Return the information from the file header stored in the HeaderConfig object
     *
     * @return the object containing EDF/BDF header information
     */
    public EdfHeader getHeader() {
        return header;
    }


    /**
     * Get the number of data records available for reading (from the current data record position).
     * <br>availableDataRecords() = numberOfRecords() - getDataRecordPosition();
     *
     * @return number of available for reading data records
     */
    public long availableRecords() {
        return numberOfRecords() - recordPosition;
    }

    /**
     * Get the number of samples of the given signal available for reading
     * (from the current sample position set for that signal)
     * <br>availableSamples(sampleNumberToSignalNumber) = numberOfSamples(sampleNumberToSignalNumber) - getSamplePosition(sampleNumberToSignalNumber);
     *
     * @return number of samples of the given signal available for reading
     */
    public long availableSamples(int signalNumber) {
        return numberOfSamples(signalNumber) - samplesPositionList[signalNumber];
    }

    /**
     * Calculate and get the total number of  data records in the file.
     * <br>numberOfRecords() = availableDataRecords() + getDataRecordPosition();
     *
     * @return total number of DataRecords in the file
     */
    public long numberOfRecords() {
        return  (file.length() - header.getNumberOfBytesInHeaderRecord()) / (recordSize * header.getDataFormat().getNumberOfBytesPerSample());
    }

    /**
     * Calculate and get the total number of samples of the given signal
     * in the file.
     * <br>numberOfSamples(sampleNumberToSignalNumber) = availableSamples(sampleNumberToSignalNumber) + getSamplePosition(sampleNumberToSignalNumber);
     *
     * @return total number of samples of the given signal in the file
     */
    public long numberOfSamples(int signalNumber) {
        return  numberOfRecords() *  header.getNumberOfSamplesInEachDataRecord(signalNumber);
    }


    /**
     * Close this reader and releases any system resources associated with
     * it. This method MUST be called after finishing reading data.
     * Failing to do so will cause unnessesary memory usage
     *
     * @throws IOException if an I/O  occurs
     */
    public void close() throws IOException {
        fileInputStream.close();
    }
}

