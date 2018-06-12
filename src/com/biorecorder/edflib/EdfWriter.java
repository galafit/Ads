package com.biorecorder.edflib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EdfWriter permits to write digital or physical samples
 * from multiple measuring channels to  EDF or BDF File.
 * Every channel (signal) has its own sample frequency.
 * <p>
 * This class is  partially thread safe! It means that all methods
 * writing data must be called from the same thread.
 * But method close() may be called from a different (usually GUI) thread!
 * <p>
 * If the file does not exist it will be created.
 * Already existing file with the same name
 * will be silently overwritten without advance warning!!
 * <p>
 * When we write <b>physical</b>  samples
 * every physical (floating point) sample
 * is converted to the corresponding digital (int) one
 * using physical maximum, physical minimum, digital maximum and digital minimum of the signal.
 * <p>
 * Every digital (int) value is converted
 * to 2 LITTLE_ENDIAN ordered bytes (16 bits) for EDF files or
 * to 3 LITTLE_ENDIAN ordered bytes (24 bits) for BDF files
 * and in this form written to the file.
 */
public class EdfWriter {
    private final String CLOSED_MSG = "File was closed. Data can not be written";
    private final String NUMBER_OF_SIGNALS_ZERO = "Number of signals is 0. Data can not be written";
    protected final String RECORD_INCOMPLETE = "Last data record is incomplete. Incorrect use of method: writeDigitalSamples/writePhysicalSamples.";

    private final EdfHeader header;
    private final File file;
    private volatile long firstRecordTime;
    private volatile long lastRecordTime;
    private volatile boolean isDurationOfDataRecordsComputable = false;
    private volatile boolean isClosed = false;
    private volatile boolean isWriting = false;
    private volatile long sampleCount;

    private final boolean isStartTimeUndefined;
    private final FileOutputStream fileOutputStream;
    private final int recordSize; // helper field to avoid unnecessary calculations
    private int signalWritePosition;

    /**
     * Creates EdfWriter to write data samples to the file represented by
     * the specified File object. EdfHeader object specifies the type of the file
     * (EDF_16BIT or BDF_24BIT) and provides all necessary information for the file header record.
     *
     * @param file   the file to be opened for writing
     * @param header object containing all necessary information for the header record
     * @throws FileNotFoundException if the file exists but is a directory rather
     * than a regular file, does not exist but cannot be created,
     * or cannot be opened for any other reason
     */
    public EdfWriter(File file, EdfHeader header) throws FileNotFoundException {
        this.header = header;
        this.file = file;
        fileOutputStream = new FileOutputStream(file);
        recordSize = header.getDataRecordSize();
        this.header.setNumberOfDataRecords(-1);
        if(header.getRecordingStartTimeMs() < 0) {
            isStartTimeUndefined = true;
        } else {
            isStartTimeUndefined = false;
        }
    }

    public File getFile() {
        return file;
    }

    /**
     * If true the average duration of DataRecords during writing process will be calculated
     * and the result will be written to the file header.
     * <p>
     * Average duration of DataRecords = (time of coming last DataRecord - time of coming first DataRecord) / total number of DataRecords
     *
     * @param isComputable - if true duration of DataRecords will be calculated
     */
    public void setDurationOfDataRecordsComputable(boolean isComputable) {
        this.isDurationOfDataRecordsComputable = isComputable;
    }

    /**
     * Writes n "raw" digital (integer) samples belonging to one signal.
     * The number of written samples : n = (sample frequency of the signal) * (duration of DataRecord).
     * <p>
     * Call this method for every signal (channel) in the file. The order is important!
     * When there are 4 signals,  the order of calling this method must be:
     * <br>samples belonging to signal 0, samples belonging to signal 1, samples belonging to signal 2, samples belonging to  signal 3,
     * <br>samples belonging to signal 0, samples belonging to signal 1, samples belonging to signal 2, samples belonging to  signal 3,
     * <br> ... etc.
     * @param digitalSamples data array with digital samples belonging to one signal
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if file was close,
     * or number of signals for that file is 0.
     */
    public void writeDigitalSamples(int[] digitalSamples) throws IOException, IllegalStateException {
        if(header.signalsCount() == 0) {
            throw new IllegalStateException(NUMBER_OF_SIGNALS_ZERO);
        }
        int sn = header.getNumberOfSamplesInEachDataRecord(signalWritePosition);
        int digMin = header.getDigitalMin(signalWritePosition);
        int digMax = header.getDigitalMax(signalWritePosition);
        for (int i = 0; i < sn; i++) {
            if(digitalSamples[i] < digMin) {
                digitalSamples[i] = digMin;
            }
            if(digitalSamples[i] > digMax) {
                digitalSamples[i] = digMax;
            }
        }
        writeDataToFile(digitalSamples, sn);
        signalWritePosition++;
        if(signalWritePosition == header.signalsCount()) {
            signalWritePosition = 0;
        }
    }

    /**
     * Writes the entire DataRecord (data pack) containing "raw" digital samples from all signals
     * starting with n_0 samples of signal 0, n_1 samples of signal 1, n_2 samples of signal 2, etc.
     * <br>
     * Where number of samples of signal i: n_i = (sample frequency of the signal_i) * (duration of DataRecord).
     * @param digitalDataRecord array with digital (int) samples from all signals
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if file was close,
     * if number of signals for that file is 0,
     * or last data record is incomplete (due to
     * the fact that samples from some channels were not recorded by methods
     * writeDigitalSamples/writePhysicalSamples).
     */
    public void writeDigitalRecord(int[] digitalDataRecord) throws IOException, IllegalStateException {
        if(header.signalsCount() == 0) {
            throw new IllegalStateException(NUMBER_OF_SIGNALS_ZERO);
        }
        if(signalWritePosition != 0) {
            throw new IllegalStateException(RECORD_INCOMPLETE);
        }
        int counter = 0;
        for (int signal = 0; signal < header.signalsCount(); signal++) {
            int sn = header.getNumberOfSamplesInEachDataRecord(signal);
            int digMin = header.getDigitalMin(signal);
            int digMax = header.getDigitalMax(signal);
            for (int i = 0; i < sn; i++) {
                if(digitalDataRecord[counter] < digMin) {
                    digitalDataRecord[counter] = digMin;
                }
                if(digitalDataRecord[counter] > digMax) {
                    digitalDataRecord[counter] = digMax;
                }
                counter++;
            }
        }
        writeDataToFile(digitalDataRecord, recordSize);
    }


    /**
     * Writes n physical samples (uV, mA, Ohm) belonging to one signal.
     * The number of written samples : n = (sample frequency of the signal) * (duration of DataRecord).
     * <p>
     * The physical samples will be converted to digital samples using the
     * values of physical maximum, physical minimum, digital maximum and digital minimum.
     * <p>
     * Call this method for every signal (channel) in the file. The order is important!
     * When there are 4 signals,  the order of calling this method must be:
     * <br>samples belonging to signal 0, samples belonging to signal 1, samples belonging to signal 2, samples belonging to  signal 3,
     * <br>samples belonging to signal 0, samples belonging to signal 1, samples belonging to signal 2, samples belonging to  signal 3,
     * <br> ... etc.
     * @param physicalSamples data array with physical (double) samples belonging to one signal
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if file was close,
     * or number of signals for that file is 0
     */
    public void writePhysicalSamples(double[] physicalSamples) throws IOException, IllegalStateException {
        int ns = header.getNumberOfSamplesInEachDataRecord(signalWritePosition);
        int digSamples[] = new int[ns];
        for (int i = 0; i < ns; i++) {
            digSamples[i] = header.physicalValueToDigital(signalWritePosition, physicalSamples[i]);
        }
        writeDigitalSamples(digSamples);
    }

    /**
     * Writes the entire DataRecord (data pack) containing physical samples (uV, mA, Ohm) from all signals
     * starting with n_0 samples of signal 0, n_1 samples of signal 1, n_2 samples of signal 2, etc.
     * <br>
     * Where number of samples of signal i: n_i = (sample frequency of the signal_i) * (duration of DataRecord).
     * <p>
     * The physical samples will be converted to digital samples using the
     * values of physical maximum, physical minimum, digital maximum and digital minimum.
     * @param physicalDataRecord array with physical (double) samples from all signals
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if file was close,
     * if number of signals for that file is 0,
     * or last data record is incomplete (due to
     * the fact that samples from some channels were not recorded by methods
     * writeDigitalSamples/writePhysicalSamples).
     */
    public void writePhysicalRecord(double[] physicalDataRecord) throws IOException, IllegalStateException {
        int digSamples[] = new int[recordSize];
        int counter = 0;
        for (int signal = 0; signal < header.signalsCount(); signal++) {
            int sn = header.getNumberOfSamplesInEachDataRecord(signal);
            for (int i = 0; i < sn; i++) {
                digSamples[counter] = header.physicalValueToDigital(signal, physicalDataRecord[counter]);
                counter++;
            }
        }
        writeDigitalRecord(digSamples);
    }

    /**
     * Closes this Edf/Bdf file for writing DataRecords and releases any system resources associated with
     * it. This method MUST be called after finishing writing DataRecords.
     * Failing to do so will cause unnessesary memory usage and corrupted and incomplete data writing.
     *
     * @throws IOException  if an I/O  occurs
     */
    public void close() throws IOException {
        if(isClosed) {
            return;
        }
        isClosed = true;
        while(isWriting) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            writeHeaderToFile();
        } finally {
            fileOutputStream.close();
        }
    }


    /**
     * Gets the number of received data records (data packages).
     * @return number of received data records
     */
    public long getNumberOfReceivedDataRecords() {
        if(recordSize == 0) {
            return 0;
        }
        return (int) (sampleCount / recordSize);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public long getFirstRecordTime() {
        return firstRecordTime;
    }

    public long getLastRecordTime() {
        return lastRecordTime;
    }


    private void writeDataToFile(int[] samples, int length) throws IllegalStateException, IOException {
        isWriting = true;
        if(isClosed) {
            isWriting = false;
            throw new IllegalStateException(CLOSED_MSG);
        }
        try{
            if(sampleCount == 0) {
                firstRecordTime = System.currentTimeMillis();
                lastRecordTime = firstRecordTime;
                writeHeaderToFile();
            }

            sampleCount += length;
            // if number of received records > 1
            if(sampleCount > recordSize && sampleCount % recordSize == 0) {
                lastRecordTime = System.currentTimeMillis();
            }
            int numberOfBytesPerSample = header.getDataFormat().getNumberOfBytesPerSample();
            byte[] byteArray = new byte[numberOfBytesPerSample * length];
            EndianBitConverter.intArrayToLittleEndianByteArray(samples, 0, byteArray, 0, length, numberOfBytesPerSample);
            fileOutputStream.write(byteArray);
        } finally {
            isWriting = false;
        }
    }

    private void writeHeaderToFile() throws IOException {
        Long numberOfReceivedRecords = getNumberOfReceivedDataRecords();
        if(numberOfReceivedRecords > 0 && numberOfReceivedRecords < 100000000) {
            header.setNumberOfDataRecords(numberOfReceivedRecords.intValue());
        }
        if (isDurationOfDataRecordsComputable && numberOfReceivedRecords > 1) {
            double durationOfRecord_ms = (lastRecordTime - firstRecordTime)  /  numberOfReceivedRecords;
            header.setDurationOfDataRecord(durationOfRecord_ms / 1000);
        }
        if(numberOfReceivedRecords > 0 && isStartTimeUndefined) {
            header.setRecordingStartTimeMs(firstRecordTime  - (long) (header.getDurationOfDataRecord() * 1000));
        }
        FileChannel fileChannel = fileOutputStream.getChannel();
        long currentPosition = fileChannel.position();
        fileChannel.position(0);
        fileOutputStream.write(new HeaderRecord(header).getBytes());
        if(currentPosition > 0) {
            fileChannel.position(currentPosition);
        }
    }

    /**
     * Gets some info about file writing process: start recording time, stop recording time,
     * number of written DataRecords, average duration of DataRecords.
     *
     * @return string with some info about writing process
     */
    public String getWritingInfo() {
        long numberOfRecords = getNumberOfReceivedDataRecords();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        StringBuilder stringBuilder = new StringBuilder("\n");
        stringBuilder.append("Start recording time = " + firstRecordTime + " (" + dateFormat.format(new Date(firstRecordTime)) + ") \n");
        stringBuilder.append("Stop recording time = " + lastRecordTime + " (" + dateFormat.format(new Date(lastRecordTime)) + ") \n");
        stringBuilder.append("Number of data records = " + numberOfRecords + "\n");
        if(numberOfRecords > 1) {
            double durationOfRecord = (lastRecordTime - firstRecordTime) * 1000 / numberOfRecords;
            stringBuilder.append("Calculated duration of data records = " + durationOfRecord);
        }
        return stringBuilder.toString();
    }


    /**
     * Unit Test. Usage Example.
     * <p>
     * Create the file: current_project_dir/records/test.edf
     * and write to it 10 data records. Then print some file header info
     * and writing info.
     * <p>
     * Data records has the following structure:
     * <br>duration of data records = 1 sec (default)
     * <br>number of channels = 2;
     * <br>number of samples from channel 0 in each data record (data package) = 50 (sample frequency 50Hz);
     * <br>number of samples from channel 1 in each data record (data package) = 5 (sample frequency 5 Hz);
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        int channel0Frequency = 50; // Hz
        int channel1Frequency = 5; // Hz

        // create header info for the file describing data records structure
        EdfHeader header = new EdfHeader(DataFormat.EDF_16BIT, 2);
        // Signal numbering starts from 0!
        // configure signal (channel) number 0
        header.setSampleFrequency(0, channel0Frequency);
        header.setLabel(0, "first channel");
        header.setPhysicalRange(0, -500, 500);
        header.setDigitalRange(0, -2048, -2047);
        header.setPhysicalDimension(0, "uV");

        // configure signal (channel) number 1
        header.setSampleFrequency(1, channel1Frequency);
        header.setLabel(1, "second channel");
        header.setPhysicalRange(1, 100, 300);

        // create file
        File recordsDir = new File(System.getProperty("user.dir"), "records");
        File file = new File(recordsDir, "test.edf");

        // create EdfWriter to write edf data to that file
        EdfWriter fileWriter = null;
        try {
            fileWriter = new EdfWriter(file, header);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // create and write samples
        int[] samplesFromChannel0 = new int[channel0Frequency];
        int[] samplesFromChannel1 = new int[channel1Frequency];
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            // create random samples for channel 0
            for (int j = 0; j < samplesFromChannel0.length; j++) {
                samplesFromChannel0[j] = rand.nextInt(10000);
            }

            // create random samples for channel 1
            for (int j = 0; j < samplesFromChannel1.length; j++) {
                samplesFromChannel1[j] = rand.nextInt(1000);
            }

            // write samples from both channels to the edf file
            try {
                fileWriter.writeDigitalSamples(samplesFromChannel0);
                fileWriter.writeDigitalSamples(samplesFromChannel1);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        // close EdfFileWriter. Always must be called after finishing writing DataRecords.
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // print header info
        System.out.println(header);
        System.out.println();
        // print writing info
        System.out.println(fileWriter.getWritingInfo());

    }

}