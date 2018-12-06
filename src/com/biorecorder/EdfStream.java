package com.biorecorder;

import com.biorecorder.multisignal.recordformat.DataHeader;
import com.biorecorder.multisignal.recordformat.DataRecordStream;
import com.biorecorder.multisignal.edflib.EdfWriter;
import com.biorecorder.multisignal.recordfilter.RecordsJoiner;
import com.biorecorder.multisignal.recordfilter.SignalFrequencyReducer;
import com.biorecorder.recorder.RecordingInfo;
import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class writes data records to the edf/bdf file. But before do
 * some transformation with income data records:
 * <ul>
 * <li>join data records</li>
 * <li>reduce signal frequencies if it was specified</li>
 * </ul>
 */
public class EdfStream implements DataRecordStream {
    private static final Log log = LogFactory.getLog(EdfStream.class);

    private final int numberOfRecordsToJoin;
    private final String patientIdentification;
    private final String recordIdentification;
    private final boolean isDurationOfDataRecordComputable;
    private final File file;
    private final Map<Integer, Integer> extraDividers;

    private volatile EdfWriter edfWriter;
    private volatile DataRecordStream DataStream;
    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);


    public EdfStream(File edfFile, int numberOfRecordsToJoin, Map<Integer, Integer> extraDividers, String patientIdentification, String recordIdentification, boolean isDurationOfDataRecordComputable) {
        this.numberOfRecordsToJoin = numberOfRecordsToJoin;
        this.file = edfFile;
        this.extraDividers = extraDividers;
        this.patientIdentification = patientIdentification;
        this.recordIdentification = recordIdentification;
        this.isDurationOfDataRecordComputable = isDurationOfDataRecordComputable;
    }


    @Override
    public void setHeader(DataHeader header) throws FileNotFoundRuntimeException {
        DataStream = new DataRecordStream() {
            @Override
            public void setHeader(DataHeader header) throws FileNotFoundRuntimeException {
                DataHeader edfHeader = new DataHeader(header);
                edfHeader.setPatientIdentification(patientIdentification);
                edfHeader.setRecordingIdentification(recordIdentification);

                try {
                    edfWriter = new EdfWriter(file, edfHeader);
                } catch (FileNotFoundException e) {
                    throw new FileNotFoundRuntimeException(e);
                }
            }

            @Override
            public void writeDataRecord(int[] dataRecord) {
                edfWriter.writeDataRecord(dataRecord);
                numberOfWrittenDataRecords.incrementAndGet();
            }

            @Override
            public void close() {
                try {
                    edfWriter.close();
                    if (edfWriter.getNumberOfReceivedDataRecords() == 0) {
                        edfWriter.getFile().delete();
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        };

        // reduce signals frequencies
        if (!extraDividers.isEmpty()) {
            SignalFrequencyReducer edfFrequencyDivider = new SignalFrequencyReducer(DataStream);
            for (Integer signal : extraDividers.keySet()) {
                edfFrequencyDivider.addDivider(signal, extraDividers.get(signal));
            }

            DataStream = edfFrequencyDivider;
        }

        // join DataRecords
        if(numberOfRecordsToJoin > 1) {
            DataStream = new RecordsJoiner(DataStream, numberOfRecordsToJoin);
        }


        DataStream.setHeader(header);
    }

    @Override
    public void writeDataRecord(int[] dataRecord) throws IORuntimeException {
        DataStream.writeDataRecord(dataRecord);
    }

    @Override
    public void close() throws IORuntimeException {
        close(null);
    }

    public void close(@Nullable RecordingInfo recordingInfo) throws IORuntimeException {
        if(recordingInfo != null) {
            DataHeader dataHeader = edfWriter.getHeader();
            dataHeader.setRecordingStartTimeMs(recordingInfo.getStartRecordingTime());
            if(isDurationOfDataRecordComputable) {
                dataHeader.setDurationOfDataRecord(recordingInfo.getDurationOfDataRecord() * numberOfRecordsToJoin);
            }
            edfWriter.setHeader(dataHeader);
        }
        DataStream.close();
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
