package com.biorecorder.gui;

import com.biorecorder.AppConfig;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


/**
 * Created by galafit on 8/6/18.
 */
public class ChannelFields {
    private static final int NAME_LENGTH = 10;
    private static final Icon ICON_CONNECTED = new ImageIcon("img/greenBall.png");
    private static final Icon ICON_DISCONNECTED = new ImageIcon("img/redBall.png");
    private static final Icon ICON_DISABLED = new ImageIcon("img/grayBall.png");


    private JComboBox frequencyField;
    private JComboBox gainField;
    private JComboBox commutatorField;
    private JCheckBox isEnabledField;
    private JCheckBox is50HzFilterEnableField;
    private JTextField nameField;
    private ColoredMarker loffPositiveField;
    private ColoredMarker loffNegativeField;

    private int channelNumber;

    public ChannelFields(AppConfig config, int channelNumber) {
        this.channelNumber = channelNumber;
        frequencyField = new JComboBox(AppConfig.getChannelsAvailableSampleRates(config.getSampleRate()));
        frequencyField.setSelectedItem(config.getChannelSampleRate(channelNumber));
        gainField = new JComboBox(AppConfig.getChannelsAvailableGains());
        gainField.setSelectedItem(config.getChannelGain(channelNumber));
        commutatorField = new JComboBox(AppConfig.getChannelsAvailableCommutators());
        commutatorField.setSelectedItem(config.getChannelCommutator(channelNumber));

        is50HzFilterEnableField = new JCheckBox();
        is50HzFilterEnableField.setSelected(config.is50HzFilterEnabled(channelNumber));
        nameField = new JTextField(NAME_LENGTH);
        nameField.setDocument(new FixSizeDocument(NAME_LENGTH));
        nameField.setText(config.getChannelName(channelNumber));

        loffPositiveField = new ColoredMarker(ICON_DISABLED);
        loffNegativeField = new ColoredMarker(ICON_DISABLED);
        isEnabledField = new JCheckBox();
        isEnabledField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnabledField.isSelected());
            }
        });
        isEnabledField.setSelected(config.isChannelEnabled(channelNumber));

    }

    /**
     * enable/disable all fields EXCLUDING isEnabledField
     * @param isEnabled
     */
    public void setEnabled(boolean isEnabled) {
        frequencyField.setEnabled(isEnabled);
        gainField.setEnabled(isEnabled);
        commutatorField.setEnabled(isEnabled);
        is50HzFilterEnableField.setEnabled(isEnabled);
        nameField.setEnabled(isEnabled);
    }

    /**
     * enable/disable all fields INCLUDING isEnabledField
     * @param isEnabled
     */
    public void setFullyEnabled(boolean isEnabled) {
        setEnabled(isEnabled);
        isEnabledField.setEnabled(isEnabled);
    }

    public void setLoffStatus(Boolean loffPositive, Boolean loffNegative) {
        if (loffNegative == null) {
            loffNegativeField.setIcon(ICON_DISABLED);
        } else if (loffNegative == true) {
            loffNegativeField.setIcon(ICON_DISCONNECTED);
        } else {
            loffNegativeField.setIcon(ICON_CONNECTED);
        }

        if (loffPositive == null) {
            loffPositiveField.setIcon(ICON_DISABLED);
        } else if (loffPositive == true) {
            loffPositiveField.setIcon(ICON_DISCONNECTED);
        } else {
            loffPositiveField.setIcon(ICON_CONNECTED);
        }

    }

    public void updateFrequencyField(int sampleRate) {
        frequencyField.setModel(new DefaultComboBoxModel(AppConfig.getChannelsAvailableSampleRates(sampleRate)));
    }

    public void addToPanel(JPanel channelsPanel) {
        channelsPanel.add(new JLabel(new Integer(channelNumber + 1).toString()));
        channelsPanel.add(isEnabledField);
        channelsPanel.add(nameField);
        channelsPanel.add(frequencyField);
        channelsPanel.add(gainField);
        channelsPanel.add(commutatorField);
        JPanel loffPanel = new JPanel();
        loffPanel.add(loffPositiveField);
        loffPanel.add(loffNegativeField);
        channelsPanel.add(loffPanel);
        channelsPanel.add(new JLabel(" "));
        channelsPanel.add(is50HzFilterEnableField);
    }

    public boolean isEnable() {
        return isEnabledField.isSelected();
    }

    public int getFrequency() {
        return (Integer) frequencyField.getSelectedItem();
    }

    public int getGain() {
        return (Integer) gainField.getSelectedItem();
    }

    public String getCommutator() {
        return (String) commutatorField.getSelectedItem();
    }

    public boolean is50HzFilterEnable() {
        return is50HzFilterEnableField.isSelected();
    }

    public String getName() {
        return nameField.getText();
    }
}
