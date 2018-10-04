package com.biorecorder;

import com.biorecorder.dataformat.RecordConfig;
import com.biorecorder.dataformat.RecordStream;
import com.biorecorder.edflib.DataFormat;
import com.biorecorder.edflib.EdfHeader;
import com.biorecorder.edflib.EdfWriter;
import com.biorecorder.filters.RecordsJoiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by galafit on 4/10/18.
 */
public class EdfStream implements RecordStream {
    private static final Log log = LogFactory.getLog(EdfStream.class);

    private int numberOfRecordsToJoin;
    private EdfWriter edfWriter;
    private String patientIdentification;
    private String recordIdentification;
    private final File file;
    private RecordStream fileStream;

    public EdfStream(File edfFile, int numberOfRecordsToJoin, String patientIdentification, String recordIdentification) {
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        file = edfFile;
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

        if(numberOfRecordsToJoin > 1) {
            // join DataRecords
            fileStream = new RecordsJoiner(fileStream, numberOfRecordsToJoin);
        }
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
}
