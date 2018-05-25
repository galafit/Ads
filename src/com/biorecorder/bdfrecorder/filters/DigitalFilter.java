package com.biorecorder.bdfrecorder.filters;

/**
 * Any LINEAR transformation
 */
public interface DigitalFilter {
    double filteredValue(double inputValue);
}
