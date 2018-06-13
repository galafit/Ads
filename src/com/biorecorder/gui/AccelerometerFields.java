package com.biorecorder.gui;

import com.biorecorder.AppConfig;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by galafit on 8/6/18.
 */
public class AccelerometerFields {
    private static final int NAME_LENGTH = 16;
    private JComboBox commutatorField;
    private JLabel nameField;
    private JCheckBox isEnabledField;
    private JComboBox frequencyField;
    private Integer number;

    public AccelerometerFields(AppConfig config) {
        number = config.getChannelsCount() + 1;
        nameField = new JLabel(config.getAccelerometerName());
        Integer[] frequencies = {AppConfig.getAccelerometerSampleRate(config.getSampleRate())};
        frequencyField = new JComboBox(frequencies);
        commutatorField = new JComboBox(AppConfig.getAccelerometerAvailableCommutators());
        commutatorField.setSelectedItem(config.getAccelerometerCommutator());
        isEnabledField = new JCheckBox();
        isEnabledField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnabledField.isSelected());
            }
        });
        isEnabledField.setSelected(config.isAccelerometerEnabled());
    }

    public String getCommutator() {
        return (String)commutatorField.getSelectedItem();
    }

    public void updateFrequencyField(int sampleRate) {
        Integer[] frequencies = {AppConfig.getAccelerometerSampleRate(sampleRate)};
        frequencyField.setModel(new DefaultComboBoxModel(frequencies));

    }

    /**
     * enable/disable all fields EXCLUDING isEnabledField
     * @param isEnabled
     */
    public void setEnabled(boolean isEnabled) {
        commutatorField.setEnabled(isEnabled);
        frequencyField.setEnabled(isEnabled);
    }

    /**
     * enable/disable all fields INCLUDING isEnabledField
     * @param isEnabled
     */
    public void setFullyEnabled(boolean isEnabled) {
        setEnabled(isEnabled);
        isEnabledField.setEnabled(isEnabled);
    }

    public void addToPanel(JPanel channelsPanel) {
        channelsPanel.add(new JLabel(number.toString()));
        channelsPanel.add(isEnabledField);
        channelsPanel.add(nameField);
        channelsPanel.add(frequencyField);
        channelsPanel.add(commutatorField);
     }

    public boolean isEnabled() {
        return isEnabledField.isSelected();
    }
}
