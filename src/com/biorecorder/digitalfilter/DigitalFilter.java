package com.biorecorder.digitalfilter;

/**
 * Any LINEAR transformation
 */
public interface DigitalFilter {
    double filteredValue(double inputValue);
}
