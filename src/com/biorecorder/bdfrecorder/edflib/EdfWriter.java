package com.biorecorder.bdfrecorder.edflib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * This class permits to write digital or physical samples
 * from multiple measuring channels to  EDF or BDF File.
 * Every channel (signal) has its own sample frequency.
 * <p>
 * If the file does not exist it will be created.
 * Already existing file with the same name
 * will be silently overwritten without advance warning!!
 * <p>
 * We may write <b>digital</b> or <b>physical</b>  samples.
 * Every physical (floating point) sample
 * will be converted to the corresponding digital (int) one
 * using physical maximum, physical minimum, digital maximum and digital minimum of the signal.
 * <p>
 * Every digital (int) value will be converted
 * to 2 LITTLE_ENDIAN ordered bytes (16 bits) for EDF files or
 * to 3 LITTLE_ENDIAN ordered bytes (24 bits) for BDF files
 * and in this form written to the file.
 */
public class EdfWriter {
    private volatile long recordsCount;
    private int signalNumber;
    private File file;
    private long startTime;
    private long stopTime;
    private double actualDurationOfDataRecord;
    private boolean isDurationOfDataRecordsComputable;
    private FileOutputStream fileOutputStream;
    private volatile boolean isClosed = false;
    private volatile EdfHeader header;

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

    }


    /**
     * Gets the Edf/Bdf file where data are saved
     * @return Edf/Bdf file
     */
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
     */
    public void writeDigitalSamples(int[] digitalSamples) {



    }


    /**
     *
     * @param digitalSamples data array with digital samples
     * @param offset the start calculateOffset in the data.
     * @param length the number of bytes to write.
     * @throws IOException  if an I/O  occurs while writing data to the file
     */
    public void writeDigitalSamples(int[] digitalSamples, int offset, int length) throws EdfRuntimeException, IllegalStateException {
        if(isClosed) {
            return;
        }
        try {
            HeaderConfig config = (HeaderConfig) this.header;
            if (sampleCounter == 0) {
                // 1 second = 1000 msec
                startTime = System.currentTimeMillis() - (long) config.getDurationOfDataRecord() * 1000;
                // setRecordingStartDateTimeMs делаем только если bdfHeader.getRecordingStartDateTimeMs == -1
                // если например идет копирование данных из файла в файл и
                // bdfHeader.getRecordingStartDateTimeMs имеет нормальное значение то изменять его не нужно
                if (config.getRecordingStartDateTimeMs() < 0) {
                    config.setRecordingStartDateTimeMs(startTime);
                }
                config.setNumberOfDataRecords(-1);
                fileOutputStream.write(config.createFileHeader());
            }

            int numberOfBytesPerSample = config.getFileType().getNumberOfBytesPerSample();
            byte[] byteArray = new byte[numberOfBytesPerSample * length];
            EndianBitConverter.intArrayToLittleEndianByteArray(digitalSamples, offset, byteArray, 0, length, numberOfBytesPerSample);
            fileOutputStream.write(byteArray);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Error while writing data to the file: {0}. Check available HD space.", file);
            throw new EdfRuntimeException(errMsg, e);
        }
        sampleCounter += digitalSamples.length;
        stopTime = System.currentTimeMillis();
        if (getNumberOfReceivedDataRecords() > 0) {
            actualDurationOfDataRecord = (stopTime - startTime) * 0.001 / getNumberOfReceivedDataRecords();
        }
    }

    /**
     * Writes the entire DataRecord (data pack) containing "raw" digital samples from all signals
     * starting with n_0 samples of signal 0, n_1 samples of signal 1, n_2 samples of signal 2, etc.
     * <br>
     * Where number of samples of signal i: n_i = (sample frequency of the signal_i) * (duration of DataRecord).
     * @param digitalDataRecord array with digital (int) samples from all signals
     */
    public void writeDigitalRecord(int[] digitalDataRecord) {
        writeDigitalSamples(digitalDataRecord, 0, edfConfig.getDataRecordLength());
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
     */
    public void writePhysicalSamples(double[] physicalSamples) throws IllegalStateException {
        if(edfConfig == null) {
            throw new IllegalStateException("Recording configuration info is not specified! EdfConfig = "+ edfConfig);
        }
        int[] digSamples = new int[physicalSamples.length];
        int signalNumber;
        for (int i = 0; i < physicalSamples.length; i++) {
            signalNumber = edfConfig.sampleNumberToSignalNumber(sampleCounter + i + 1);
            digSamples[i] = edfConfig.physicalValueToDigital(signalNumber, physicalSamples[i]);
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
     */
    public void writePhysicalRecord(double[] physicalDataRecord) {

    }


    /**
     * Gets the number of received data records (data packages).
     * @return number of received data records
     */
    public int getNumberOfReceivedDataRecords() {
        if(edfConfig == null || edfConfig.getDataRecordLength()== 0) {
            return 0;
        }
        return (int) (sampleCounter / edfConfig.getDataRecordLength());
    }



    /**
     * Closes this Edf/Bdf file for writing DataRecords and releases any system resources associated with
     * it. This method MUST be called after finishing writing DataRecords.
     * Failing to do so will cause unnessesary memory usage and corrupted and incomplete data writing.

     * @throws IOException  if an I/O  occurs while writing data to the file
     */
    public void close() throws IOException {
        if(isClosed) {
            return;
        }
        isClosed = true;
        HeaderConfig config = (HeaderConfig) this.header;
        if (config.getNumberOfDataRecords() == -1) {
            config.setNumberOfDataRecords(getNumberOfReceivedDataRecords());
        }
        if (isDurationOfDataRecordsComputable && actualDurationOfDataRecord > 0) {
            config.setDurationOfDataRecord(actualDurationOfDataRecord);
        }
        FileChannel channel = fileOutputStream.getChannel();
        try {
            channel.position(0);
            fileOutputStream.write(config.createFileHeader());
            fileOutputStream.close();
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Error while closing the file: {0}.", file);
            new EdfRuntimeException(errMsg, e);
        }
    }

    /**
     * Gets some info about file writing process: start recording time, stop recording time,
     * number of written DataRecords, average duration of DataRecords.
     *
     * @return string with some info about writing process
     */
    public String getWritingInfo() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        StringBuilder stringBuilder = new StringBuilder("\n");
        stringBuilder.append("Start recording time = " + startTime + " (" + dateFormat.format(new Date(startTime)) + ") \n");
        stringBuilder.append("Stop recording time = " + stopTime + " (" + dateFormat.format(new Date(stopTime)) + ") \n");
        stringBuilder.append("Number of data records = " + getNumberOfReceivedDataRecords() + "\n");
        stringBuilder.append("Actual duration of a data record = " + actualDurationOfDataRecord);
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
        HeaderConfig headerConfig = new HeaderConfig(2, DataFormat.EDF_16BIT);
        // Signal numbering starts from 0!
        // configure signal (channel) number 0
        headerConfig.setSampleFrequency(0, channel0Frequency);
        headerConfig.setLabel(0, "first channel");
        headerConfig.setPhysicalRange(0, -500, 500);
        headerConfig.setDigitalRange(0, -2048, -2047);
        headerConfig.setPhysicalDimension(0, "uV");

        // configure signal (channel) number 1
        headerConfig.setSampleFrequency(1, channel1Frequency);
        headerConfig.setLabel(1, "second channel");
        headerConfig.setPhysicalRange(1, 100, 300);

        // create file
        File recordsDir = new File(System.getProperty("user.dir"), "records");
        File file = new File(recordsDir, "test.edf");

        // create EdfFileWriter to write edf data to that file
        EdfFileWriter fileWriter = new EdfFileWriter(file, headerConfig);

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
            fileWriter.writeDigitalSamples(samplesFromChannel0);
            fileWriter.writeDigitalSamples(samplesFromChannel1);
        }

        // close EdfFileWriter. Always must be called after finishing writing DataRecords.
        fileWriter.close();

        // print header info
        System.out.println(headerConfig);
        System.out.println();
        // print writing info
        System.out.println(fileWriter.getWritingInfo());

    }

}