package com.biorecorder.filters;

import com.biorecorder.dataformat.DataRecordConfig;
import com.biorecorder.dataformat.DataRecordSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permits to  add digital filters to any signal and realize corresponding
 * transformation  with the data samples belonging to the signals
 */
public class SignalsFilter extends FilterDataRecordRecordSender {
    private Map<Integer, List<NamedFilter>> filters = new HashMap<Integer, List<NamedFilter>>();
    private int inRecordSize;
    private double[] offsets;

    public SignalsFilter(DataRecordSender in) {
        super(in);
        for (int i = 0; i < this.in.dataConfig().signalsCount(); i++) {
            inRecordSize += this.in.dataConfig().getNumberOfSamplesInEachDataRecord(i);
        }
        DataRecordConfig inConfig = in.dataConfig();
        offsets = new double[inConfig.signalsCount()];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = DataRecordConfig.offset(inConfig, i);
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
    public DataRecordConfig dataConfig() {
        return new DataRecordConfigWrapper(in.dataConfig()) {
            @Override
            public String getPrefiltering(int signalNumber) {
                if(inConfig.getPrefiltering(signalNumber) != null && ! inConfig.getPrefiltering(signalNumber).isEmpty()) {
                    return inConfig.getPrefiltering(signalNumber) + ";" +getSignalFiltersName(signalNumber);
                }
                return getSignalFiltersName(signalNumber);
            }
        };
    }

    @Override
    protected void filterData(int[] inputRecord)  {
        int[] resultantRecord = new int[inRecordSize];
        int signalNumber = 0;
        int signalStartSampleNumber = 0;
        for (int i = 0; i < inRecordSize; i++) {
            if(i >= signalStartSampleNumber + in.dataConfig().getNumberOfSamplesInEachDataRecord(signalNumber)) {
                signalStartSampleNumber += in.dataConfig().getNumberOfSamplesInEachDataRecord(signalNumber);
                signalNumber++;
            }

            List<NamedFilter> signalFilters = filters.get(signalNumber);
            if(signalFilters != null) {
                // for filtering we use (digValue + offset) that is proportional physValue !!!
                double digValue = inputRecord[i] + offsets[signalNumber];
                for (DigitalFilter filter : signalFilters) {
                    digValue = filter.filteredValue(digValue);
                }
                resultantRecord[i] = new Double(digValue - offsets[signalNumber]).intValue();
            } else {
                resultantRecord[i] = inputRecord[i];
            }

        }
        sendDataToListeners(resultantRecord);
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
