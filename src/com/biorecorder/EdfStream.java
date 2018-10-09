package com.biorecorder;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordStream;
import com.biorecorder.edflib.DataFormat;
import com.biorecorder.edflib.EdfHeader;
import com.biorecorder.edflib.EdfWriter;
import com.biorecorder.filters.RecordsJoiner;
import com.biorecorder.filters.SignalFrequencyReducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class write data records to the edf/bdf file. But before do
 * some transformation with income data records:
 * <ul>
 * <li>join data records</li>
 * <li>reduce signal frequencies if it was specified</li>
 * </ul>
 */
public class EdfStream implements RecordStream {
    private static final Log log = LogFactory.getLog(EdfStream.class);

    private final int numberOfRecordsToJoin;
    private final String patientIdentification;
    private final String recordIdentification;
    private final File file;
    private final Map<Integer, Integer> extraDividers;

    private volatile EdfWriter edfWriter;
    private volatile RecordStream fileStream;
    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);


    public EdfStream(File edfFile, int numberOfRecordsToJoin, Map<Integer, Integer> extraDividers,  String patientIdentification, String recordIdentification) {
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        this.file = edfFile;
        this.extraDividers = extraDividers;
        this.patientIdentification = patientIdentification;
        this.recordIdentification = recordIdentification;
    }

    @Override
    public void setRecordConfig(RecordConfig recordConfig) throws FileNotFoundRuntimeException {
        fileStream = new RecordStream() {
            @Override
            public void setRecordConfig(RecordConfig recordConfig) throws FileNotFoundRuntimeException {
                // copy data from recordConfig to the EdfHeader
                EdfHeader edfHeader = new EdfHeader(DataFormat.BDF_24BIT, recordConfig.signalsCount());
                edfHeader.setPatientIdentification(patientIdentification);
                edfHeader.setRecordingIdentification(recordIdentification);
                edfHeader.setDurationOfDataRecord(recordConfig.getDurationOfDataRecord());
                for (int i = 0; i < recordConfig.signalsCount(); i++) {
                    edfHeader.setNumberOfSamplesInEachDataRecord(i, recordConfig.getNumberOfSamplesInEachDataRecord(i));
                    edfHeader.setPrefiltering(i, recordConfig.getPrefiltering(i));
                    edfHeader.setTransducer(i, recordConfig.getTransducer(i));
                    edfHeader.setLabel(i, recordConfig.getLabel(i));
                    edfHeader.setDigitalRange(i, recordConfig.getDigitalMin(i), recordConfig.getDigitalMax(i));
                    edfHeader.setPhysicalRange(i, recordConfig.getPhysicalMin(i), recordConfig.getPhysicalMax(i));
                    edfHeader.setPhysicalDimension(i, recordConfig.getPhysicalDimension(i));
                }

                try {
                    edfWriter = new EdfWriter(file, edfHeader);
                } catch (FileNotFoundException e) {
                    throw new FileNotFoundRuntimeException(e);
                }
            }

            @Override
            public void writeRecord(int[] dataRecord) {
               try {
                    edfWriter.writeDigitalRecord(dataRecord);
                    numberOfWrittenDataRecords.incrementAndGet();
                } catch (IOException e) {
                    throw new IORuntimeException(e);
                }

            }

            @Override
            public void close() {
                try {
                    edfWriter.close();
                    if (edfWriter.getNumberOfReceivedDataRecords() == 0) {
                        edfWriter.getFile().delete();
                    }
                } catch (IOException e) {
                    throw new IORuntimeException(e);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        };

        // reduce signals frequencies
        if (!extraDividers.isEmpty()) {
            SignalFrequencyReducer edfFrequencyDivider = new SignalFrequencyReducer(fileStream);
            for (Integer signal : extraDividers.keySet()) {
                edfFrequencyDivider.addDivider(signal, extraDividers.get(signal));
            }

            fileStream = edfFrequencyDivider;
        }

        // join DataRecords
        if(numberOfRecordsToJoin > 1) {
            fileStream = new RecordsJoiner(fileStream, numberOfRecordsToJoin);
        }


        fileStream.setRecordConfig(recordConfig);
    }

    @Override
    public void writeRecord(int[] dataRecord) throws IORuntimeException {
        fileStream.writeRecord(dataRecord);
    }

    @Override
    public void close() throws IORuntimeException {
        fileStream.close();
    }

    public void setStartRecordingTime(long time) {
        edfWriter.setStartRecordingTime(time);
    }

    public void setDurationOfDataRecords(double durationOfDataRecord) {
        edfWriter.setDurationOfDataRecords(durationOfDataRecord * numberOfRecordsToJoin);
    }

    public long getNumberOfWrittenRecords() {
        return edfWriter.getNumberOfReceivedDataRecords();
    }

    public String getWritingInfo() {
        return edfWriter.getWritingInfo();
    }

    public File getFile() {
        return file;
    }

    public long getNumberOfWrittenDataRecords() {
        return numberOfWrittenDataRecords.get();
    }
}
