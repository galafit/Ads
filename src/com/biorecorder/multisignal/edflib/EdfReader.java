package com.biorecorder.multisignal.edflib;

import com.biorecorder.multisignal.recordformat.DataHeader;

import java.io.*;

// TODO make it  partially thread safe like EdfWriter!!!

/**
 * Permits to read data samples from EDF or BDF file. Also it
 * reads information from the file header and saves it
 * in special {@link DataHeader} object, that we
 * can get by method {@link #getHeader()}
 * <p>
 * This class is NOT thread safe!
 * <p>
 * EDF/BDF files contains "row" digital (int) data but they can be converted to corresponding
 * real physical floating point data on the base of header information (physical maximum and minimum
 * and digital maximum and minimum specified for every channel (signal)).
 * So we can "read" both digital or physical values.
 */
public class EdfReader {
    private DataHeader header;
    private FileInputStream fileInputStream;
    private File file;
    private long[] samplesPositionList;
    private long recordPosition = 0;
    private final int recordSize;
    private int numberOfBytesInHeaderRecord;

    /**
     * Creates EdfFileReader to read data from the file represented by the specified
     * File object.
     *
     * @param file Edf or Bdf file to be opened for reading
     * @throws FileNotFoundException if the file does not exist,
     *                               is a directory rather than a regular file,
     *                               or for some other reason cannot be opened for reading.
     * @throws HeaderException       if the the file is not valid EDF/BDF file
     *                               due to some errors in its header record
     * @throws IOException           if an I/O error occurs
     */
    public EdfReader(File file) throws FileNotFoundException, HeaderException, IOException {
        this.file = file;
        fileInputStream = new FileInputStream(file);
        HeaderRecord headerRecord = new HeaderRecord(file);
        header = headerRecord.getHeaderInfo();
        numberOfBytesInHeaderRecord = headerRecord.getNumberOfBytes();
        samplesPositionList = new long[header.numberOfSignals()];
        recordSize = header.getRecordSize();
    }

    /**
     * Set the sample position indicator of the given channel (signal)
     * to the given new position. The position is measured in samples.
     * <p>
     * Note that every signal has it's own independent sample position indicator and
     * setSamplePosition() affects only one of them.
     * Methods {@link #readSamples(int, int, int[])} and
     * {@link #readPhysicalSamples(int, int, double[])}  will start reading
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
     * The position is measured in DataRecords.
     * Methods: {@link #readDataRecords(int, int[])}
     * will start reading from the specified position.
     *
     * @param newPosition the new position, a non-negative integer counting
     *                    the number of data records from the beginning of the file
     */
    public void setRecordPosition(long newPosition) {
        recordPosition = newPosition;
    }

    /**
     * Puts DataRecord position indicator and sample position indicators of all signals to 0.
     */
    public void reset() {
        recordPosition = 0;
        for (int i = 0; i < samplesPositionList.length; i++) {
            samplesPositionList[i] = 0;
        }
    }


    /**
     * Read n samples belonging to the  signal
     * starting from the current sample position indicator.
     * The values are the "raw" digital (integer) values.
     * <p>
     * The sample position indicator of that channel will be increased
     * with the amount of samples read (this can be less than n or zero!)
     *
     * @param signal channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param n      number of samples to read
     * @return the amount of really read samples that can be less than n or zero
     * @throws IOException if an I/O error occurs
     */
    public int readSamples(int signal, int n, int[] buffer) throws IOException {
        return readSamples(signal, n, buffer, null);
    }

    /**
     * Read n samples belonging to the  signal
     * starting from the current sample position indicator.
     * Converts the read samples
     * to their physical values (e.g. microVolts, beats per minute, etc).
     * <p>
     * The sample position indicator of that channel will be increased
     * with the amount of samples read (this can be less than n or zero!)
     *
     * @param signal channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param n      number of samples to read
     * @return the amount of really read samples that can be less than n or zero
     * @throws IOException if an I/O error occurs
     */
    public int readPhysicalSamples(int signal, int n, double[] buffer) throws IOException {
        return readSamples(signal, n, null, buffer);
    }


    private int readSamples(int signal, int n, int[] digBuffer, double[] physBuffer) throws IOException {
        int bytesPerSample = header.getFormatVersion().getNumberOfBytesPerSample();
        int samplesPerRecord = header.getNumberOfSamplesInEachDataRecord(signal);

        long recordNumber = samplesPositionList[signal] / samplesPerRecord;
        int signalStartPositionInRecord = 0;
        for (int i = 0; i < signal; i++) {
            signalStartPositionInRecord += header.getNumberOfSamplesInEachDataRecord(i);
        }
        int sampleStartOffset = (int) (samplesPositionList[signal] % samplesPerRecord);




        long fileReadPosition = numberOfBytesInHeaderRecord +
                (recordNumber * recordSize + signalStartPositionInRecord + sampleStartOffset) * bytesPerSample;

        // set file start reading position and read
        fileInputStream.getChannel().position(fileReadPosition);
        byte[] byteData = new byte[samplesPerRecord * bytesPerSample];
        int totalReadBytes = 0;
        int bytesToRead = Math.min((samplesPerRecord - sampleStartOffset) * bytesPerSample, n * bytesPerSample - totalReadBytes);
        int sampleCount = 0;
        while (totalReadBytes < n * bytesPerSample) {
            int readBytes = fileInputStream.read(byteData, 0, bytesToRead);
            int maxOffset = readBytes - bytesPerSample;
            for (int offset = 0; offset < maxOffset; offset += bytesPerSample) {
                if (physBuffer != null) {
                    physBuffer[sampleCount] = header.digitalValueToPhysical(signal, EndianBitConverter.littleEndianBytesToInt(byteData, offset, bytesPerSample));
                }
                if (digBuffer != null) {
                    digBuffer[sampleCount] = EndianBitConverter.littleEndianBytesToInt(byteData, offset, bytesPerSample);
                }
                sampleCount++;
            }

            totalReadBytes += readBytes;
            if (readBytes < bytesToRead) { // end of file
                break;
            }
            fileInputStream.skip((recordSize - samplesPerRecord) * bytesPerSample);
            bytesToRead = Math.min(samplesPerRecord * bytesPerSample, n * bytesPerSample - totalReadBytes);
        }
        int readSamples = totalReadBytes / bytesPerSample;
        samplesPositionList[signal] += readSamples;
        return readSamples;
    }


    /**
     * Read n data records
     * starting from the current record position indicator.
     * The values are the "raw" digital (integer) values.
     * <p>
     * The record position indicator will be increased with the amount of data records
     * read (this can be less than n or zero!)
     *
     * @param buffer array where read data will be stored
     * @param n      number of "data records" to read
     * @return the total number of data records read into the buffer,
     * or -1 if there is no more data because the end of the stream has been reached
     * @throws IOException if an I/O error occurs
     */
    public int readDataRecords(int n, int[] buffer) throws IOException {
        int bytesPerSample = header.getFormatVersion().getNumberOfBytesPerSample();
        long fileReadPosition = numberOfBytesInHeaderRecord +
                recordSize * recordPosition * bytesPerSample;
        fileInputStream.getChannel().position(fileReadPosition);


        byte[] byteData = new byte[recordSize * n * bytesPerSample];
        int readBytes = fileInputStream.read(byteData, 0, byteData.length);
        if (readBytes < 0) { // end of file
            return -1;
        }
        int readRecords = readBytes / (recordSize * bytesPerSample);
        recordPosition += readRecords;
        int maxOffset = readBytes - bytesPerSample;
        int sampleCount = 0;
        for (int offset = 0; offset < maxOffset; offset += bytesPerSample) {
            buffer[sampleCount] = EndianBitConverter.littleEndianBytesToInt(byteData, offset, bytesPerSample);
            sampleCount++;
        }
        return readRecords;
    }


    /**
     * Return the information from the file header stored in the HeaderConfig object
     *
     * @return the object containing EDF/BDF header information
     */
    public DataHeader getHeader() {
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
        return (file.length() - numberOfBytesInHeaderRecord) / (recordSize * header.getFormatVersion().getNumberOfBytesPerSample());
    }

    /**
     * Calculate and get the total number of samples of the given signal
     * in the file.
     * <br>numberOfSamples(sampleNumberToSignalNumber) = availableSamples(sampleNumberToSignalNumber) + getSamplePosition(sampleNumberToSignalNumber);
     *
     * @return total number of samples of the given signal in the file
     */
    public long numberOfSamples(int signalNumber) {
        return numberOfRecords() * header.getNumberOfSamplesInEachDataRecord(signalNumber);
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

