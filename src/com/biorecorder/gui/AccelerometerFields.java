package com.biorecorder.gui;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by galafit on 8/6/18.
 */
public class AccelerometerFields {
    private JComboBox commutatorField;
    private JLabel nameField;
    private JCheckBox isEnabledField;
    private JComboBox frequencyField;
    private Integer number;

    public AccelerometerFields(RecorderSettings settings) {
        number = settings.getChannelsCount() + 1;
        nameField = new JLabel(settings.getAccelerometerName());
        Integer[] frequencies = {settings.getAccelerometerFrequency()};
        frequencyField = new JComboBox(frequencies);
        commutatorField = new JComboBox(settings.getAccelerometerAvailableModes());
        commutatorField.setSelectedItem(settings.getAccelerometerMode());
        isEnabledField = new JCheckBox();
        isEnabledField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnabledField.isSelected());
            }
        });
        isEnabledField.setSelected(settings.isAccelerometerEnabled());
    }

    public String getMode() {
        return (String)commutatorField.getSelectedItem();
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
