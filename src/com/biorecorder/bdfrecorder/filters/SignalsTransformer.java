package com.biorecorder.bdfrecorder.filters;

import com.biorecorder.bdfrecorder.dataformat.DataProducer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permits to  add digital filters to any signal and realize corresponding
 * transformation  with the data samples belonging to the signals
 */
public class SignalsTransformer extends DataPackageFilter {
    private Map<Integer, List<NamedFilter>> filters = new HashMap<Integer, List<NamedFilter>>();

    public SignalsTransformer(DataProducer input) {
        super(input);
    }

    /**
     * Indicate that the given filter should be applied to the samples
     * belonging to the given signal in DataRecords
     *
     * @param signalFilter digital filter that will be applied to the samples
     * @param signalNumber number of the channel (signal) to whose samples
     *                     the filter should be applied to. Numbering starts from 0.
     */
    public void addSignalFilter(int signalNumber, DigitalFilter signalFilter, String filterName) {
        List<NamedFilter> signalFilters = filters.get(signalNumber);
        if(signalFilters == null) {
            signalFilters = new ArrayList<NamedFilter>();
            filters.put(signalNumber, signalFilters);
        }
        signalFilters.add(new NamedFilter(signalFilter, filterName));
    }

    public void addSignalFilter(int signalNumber, DigitalFilter signalFilter) {
        addSignalFilter(signalNumber, signalFilter, "");
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
    public int getSignalsCount() {
        return input.getSignalsCount();
    }

    @Override
    public double getDurationOfDataRecord() {
        return input.getDurationOfDataRecord();
    }

    @Override
    public int getNumberOfSamplesInEachDataRecord(int signalNumber) {
        return input.getNumberOfSamplesInEachDataRecord(signalNumber);
    }

    @Override
    protected void filterData(int[] inputRecord)  {
        int inputRecordLength = recordSize(input);
        int[] resultantRecord = new int[inputRecordLength];
        int signalNumber = 0;
        int signalStartSampleNumber = 0;
        for (int i = 0; i < inputRecordLength; i++) {
            if(i >= signalStartSampleNumber + input.getNumberOfSamplesInEachDataRecord(signalNumber)) {
                signalStartSampleNumber += input.getNumberOfSamplesInEachDataRecord(signalNumber);
                signalNumber++;
            }

            List<NamedFilter> signalFilters = filters.get(signalNumber);
            if(signalFilters != null) {
                double filteredValue = inputRecord[i];
                for (DigitalFilter filter : signalFilters) {
                    filteredValue = filter.filteredValue(filteredValue);
                }
                resultantRecord[i] = new Double(filteredValue).intValue();
            } else {
                resultantRecord[i] = inputRecord[i];
            }

        }
        dataListener.onDataReceived(resultantRecord);
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

}
