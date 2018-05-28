package com.biorecorder.filters;

/**
 * Any LINEAR transformation
 */
public interface DigitalFilter {
    double filteredValue(double inputValue);
}
