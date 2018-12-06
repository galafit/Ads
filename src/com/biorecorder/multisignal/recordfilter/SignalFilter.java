package com.biorecorder.multisignal.recordfilter;

import com.biorecorder.digitalfilter.DigitalFilter;
import com.biorecorder.digitalfilter.MovingAverageFilter;
import com.biorecorder.multisignal.recordformat.DataHeader;
import com.biorecorder.multisignal.recordformat.DataRecordStream;
import com.biorecorder.multisignal.recordformat.FormatVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permits to  add digital filters to any signal and realize corresponding
 * transformation  with the data samples belonging to the signals
 */
public class SignalFilter extends FilterRecordStream {
    private Map<Integer, List<NamedFilter>> filters = new HashMap<Integer, List<NamedFilter>>();
    private double[] offsets; // gain and offsets to convert dig value to phys one

    public SignalFilter(DataRecordStream outStream) {
        super(outStream);
    }

    @Override
    public void setHeader(DataHeader header) {
        super.setHeader(header);
        offsets = new double[header.numberOfSignals()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = header.offset(i);
        }
    }

    /**
     * Indicates that the given filter should be applied to the samples
     * belonging to the given signal. This method can be called only
     * before adding a listener!
     *
     * @param signalFilter digital filter that will be applied to the samples
     * @param signalNumber number of the signal to whose samples
     *                     the filter should be applied to. Numbering starts from 0.
     */
    public void addSignalFilter(int signalNumber, DigitalFilter signalFilter, String filterName) {
        List<NamedFilter> signalFilters = filters.get(signalNumber);
        if(signalFilters == null) {
            signalFilters = new ArrayList<NamedFilter>();
            filters.put(signalNumber, signalFilters);
        }
        signalFilters.add(new NamedFilter(signalFilter, filterName));
        if(inConfig != null) {
            outStream.setHeader(getOutConfig());
        }
    }


    public String getSignalFiltersName(int signalNumber) {
        StringBuilder name = new StringBuilder("");
        List<NamedFilter> signalFilters = filters.get(signalNumber);
        if(signalFilters != null) {
            for (NamedFilter filter : signalFilters) {
                name.append(filter.getFilterName()).append(";");
            }
        }
        return name.toString();
    }

    @Override
    public DataHeader getOutConfig() {
        DataHeader outConfig = new DataHeader(inConfig);
        for (int i = 0; i < outConfig.numberOfSignals(); i++) {
            String prefilter = getSignalFiltersName(i);
            if(inConfig.getPrefiltering(i) != null && ! inConfig.getPrefiltering(i).isEmpty()) {
                prefilter = inConfig.getPrefiltering(i) + ";" +getSignalFiltersName(i);
            }
            outConfig.setPrefiltering(i, prefilter);
        }
        return outConfig;
    }

    @Override
    public void writeDataRecord(int[] inputRecord)  {
        int[] outRecord = new int[inputRecord.length];
        int signalNumber = 0;
        int signalStartSampleNumber = 0;
        for (int i = 0; i < inRecordSize; i++) {

            if(i >= signalStartSampleNumber + inConfig.getNumberOfSamplesInEachDataRecord(signalNumber)) {
                signalStartSampleNumber += inConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
                signalNumber++;
            }

            List<NamedFilter> signalFilters = filters.get(signalNumber);
            if(signalFilters != null) {
                // for filtering we use (digValue + offset) that is proportional physValue !!!
                double digValue = inputRecord[i] + offsets[signalNumber];
                for (DigitalFilter filter : signalFilters) {
                    digValue = filter.filteredValue(digValue);
                }
                outRecord[i] = (int)(digValue - offsets[signalNumber]);
            } else {
                outRecord[i] = inputRecord[i];
            }

        }
        outStream.writeDataRecord(outRecord);
    }

    class NamedFilter implements DigitalFilter {
        private DigitalFilter filter;
        private String filterName;

        public NamedFilter(DigitalFilter filter, String filterName) {
            this.filter = filter;
            this.filterName = filterName;
        }

        @Override
        public double filteredValue(double inputValue) {
            return filter.filteredValue(inputValue);
        }

        public String getFilterName() {
            return filterName;
        }
    }

    /**
     * Unit Test. Usage Example.
     */
    public static void main(String[] args) {

        // 0 channel 1 sample, 1 channel 6 samples, 2 channel 2 samples
        int[] dataRecord = {1,  2,4,8,6,0,8,  3,5};

        DataHeader dataConfig = new DataHeader(FormatVersion.BDF_24BIT, 3);

        dataConfig.setNumberOfSamplesInEachDataRecord(0, 1);
        dataConfig.setNumberOfSamplesInEachDataRecord(1, 6);
        dataConfig.setNumberOfSamplesInEachDataRecord(2, 2);


        // Moving average filter to channel 1

        // expected dataRecords
        int[] expectedDataRecord1 = {1,  2,3,6,7,3,4,  3,5};
        int[] expectedDataRecord2 = {1,  5,3,6,7,3,4,  3,5};
        List<int[]> expectedRecords = new ArrayList<>(2);
        expectedRecords.add(expectedDataRecord1);
        expectedRecords.add(expectedDataRecord2);

        SignalFilter recordFilter = new SignalFilter(new TestStream(expectedRecords));
        recordFilter.addSignalFilter(1, new MovingAverageFilter(2), "movAvg:2");
        recordFilter.setHeader(dataConfig);

        // send 4 records and get 4 resultant records
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
        recordFilter.writeDataRecord(dataRecord);
    }

}
