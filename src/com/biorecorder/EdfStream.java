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
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private final boolean isDurationOfDataRecordComputable;
    private volatile DataRecordStream DataStream;
    private final File file;
    private DataHeader header;
    private AtomicLong numberOfWrittenDataRecords = new AtomicLong(0);
    private String writingInfo;

    public EdfStream(File edfFile, int numberOfRecordsToJoin, Map<Integer, Integer> extraDividers,  boolean isDurationOfDataRecordComputable) throws FileNotFoundRuntimeException  {
        this.isDurationOfDataRecordComputable = isDurationOfDataRecordComputable;
        this.file = edfFile;

        DataStream = new FilteredFileStream(file);

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
    }

    @Override
    public void setHeader(DataHeader header) {
        this.header = header;
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
            header.setRecordingStartTimeMs(recordingInfo.getStartRecordingTime());
            if(isDurationOfDataRecordComputable) {
                header.setDurationOfDataRecord(recordingInfo.getDurationOfDataRecord());
            }
            DataStream.setHeader(header);
        }
        DataStream.close();
    }

    /**
     * Gets some info about file writing process: startRecording recording time, stop recording time,
     * number of written data records, average duration of data records.
     *
     * @return string with some info about writing process
     */
    public String getWritingInfo() {
      return writingInfo;
    }

    public File getFile() {
        return file;
    }

    public long getNumberOfWrittenDataRecords() {
        return numberOfWrittenDataRecords.get();
    }

    class FilteredFileStream implements DataRecordStream {
        EdfWriter edfWriter;

        public FilteredFileStream(File file) throws FileNotFoundRuntimeException {
            try {
                edfWriter = new EdfWriter(file);
            } catch (FileNotFoundException e) {
                throw new FileNotFoundRuntimeException(e);
            }
        }

        @Override
        public void setHeader(DataHeader header) {
            edfWriter.setHeader(header);
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
                DataHeader fileHeader = edfWriter.getHeader();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Start recording time = "  + dateFormat.format(new Date(fileHeader.getRecordingStartTimeMs())) + "\n");
                stringBuilder.append("Duration of data records(sec) = " + fileHeader.getDurationOfDataRecord()+ "\n");
                stringBuilder.append("Number of data records = " + fileHeader.getNumberOfDataRecords());

                writingInfo = stringBuilder.toString();


                if (edfWriter.getNumberOfReceivedDataRecords() == 0) {
                    file.delete();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
