package com.biorecorder.gui;

import com.biorecorder.AppConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by galafit on 8/6/18.
 */
public class AccelerometerFields {
    private static final int NAME_LENGTH = 16;
    private static  final String[] COMMUTATORS = {"1 Channel", "3 Channels"};
    private JComboBox commutatorField = new JComboBox(COMMUTATORS);
    private JTextField nameField = new JTextField("Accelerometer", NAME_LENGTH);
    private JCheckBox isEnabledField = new JCheckBox();
    private JLabel frequencyField;

    public AccelerometerFields(AppConfig config) {
        nameField.setEnabled(false);
        isEnabledField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnabledField.isSelected());
            }
        });
        frequencyField = new JLabel(new Integer(config.getAccelerometerSampleRate()).toString());
        if (config.isAccelerometerOneChannelMode()) {
            commutatorField.setSelectedIndex(0);
        } else {
            commutatorField.setSelectedIndex(1);
        }
        setEnabled(config.isAccelerometerEnabled());
    }

    public void setEnabled(boolean isEnabled) {
        commutatorField.setEnabled(isEnabled);
    }

    public void addToPanel(JPanel channelsPanel) {
        channelsPanel.add(new JLabel(" "));
        channelsPanel.add(isEnabledField);
        channelsPanel.add(nameField);
        channelsPanel.add(frequencyField);
        channelsPanel.add(new JLabel(" "));
        channelsPanel.add(commutatorField);
     }

    public void setCommutatorFieldPreferredSize(Dimension preferredSize) {
        commutatorField.setPreferredSize(preferredSize);
    }

    public boolean isOneChannelMode() {
        if(commutatorField.getSelectedItem() == COMMUTATORS[0]) {
            return true;
        }
        return false;
    }

    public String getName() {
        return nameField.getText();
    }

    public boolean isEnabled() {
        return isEnabledField.isSelected();
    }
}
