package com.biorecorder.gui;

import com.biorecorder.AppConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


/**
 * Created by galafit on 8/6/18.
 */
public class ChannelFields {
    private static final int NAME_LENGTH = 16;
    private static final Icon ICON_CONNECTED = new ImageIcon("img/greenBall.png");
    private static final Icon ICON_DISCONNECTED = new ImageIcon("img/redBall.png");
    private static final Icon ICON_DISABLED = new ImageIcon("img/grayBall.png");


    private JComboBox frequencyField;
    private JComboBox gainField;
    private JComboBox commutatorField;
    private JCheckBox isEnableField;
    private JCheckBox is50HzFilterEnableField;
    private JTextField nameField;
    private MarkerLabel loffPositiveField;
    private MarkerLabel loffNegativeField;

    private Integer channelNumber;

    public ChannelFields(AppConfig config, int channelNumber) {
        boolean isChannelEnable = config.isChannelEnabled(channelNumber);
        this.channelNumber = channelNumber;
        frequencyField = new JComboBox(config.getChannelsAvailableSampleRates());
        frequencyField.setSelectedItem(config.getChannelSampleRate(channelNumber));
        gainField = new JComboBox(AppConfig.getAvailableGains());
        gainField.setSelectedItem(config.getChannelGain(channelNumber));
        commutatorField = new JComboBox(AppConfig.getAvailableRecordingModes());
        commutatorField.setSelectedItem(config.getChannelCommutator(channelNumber));
        isEnableField = new JCheckBox();
        isEnableField.setSelected(isChannelEnable);
        isEnableField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnableField.isSelected());
            }
        });

        is50HzFilterEnableField = new JCheckBox();
        is50HzFilterEnableField.setSelected(config.is50HzFilterEnabled(channelNumber));
        nameField = new JTextField(config.getChannelName(channelNumber), NAME_LENGTH);
        nameField.setDocument(new FixSizeDocument(NAME_LENGTH));

        loffPositiveField = new MarkerLabel(ICON_DISABLED);
        loffNegativeField = new MarkerLabel(ICON_DISABLED);
        setEnabled(isChannelEnable);
    }

    public void setEnabled(boolean isEnabled) {
        frequencyField.setEnabled(isEnabled);
        gainField.setEnabled(isEnabled);
        commutatorField.setEnabled(isEnabled);
        is50HzFilterEnableField.setEnabled(isEnabled);
        nameField.setEnabled(isEnabled);
    }

    public void setLoffStatus(Boolean loffPositive, Boolean loffNegative) {
        if(loffNegative == null) {
            loffNegativeField.setIcon(ICON_DISABLED);
        } else if(loffNegative == true) {
            loffNegativeField.setIcon(ICON_DISCONNECTED);
        } else {
            loffNegativeField.setIcon(ICON_CONNECTED);
        }

        if(loffPositive == null) {
            loffPositiveField.setIcon(ICON_DISABLED);
        } else if(loffPositive == true) {
            loffPositiveField.setIcon(ICON_DISCONNECTED);
        } else {
            loffPositiveField.setIcon(ICON_CONNECTED);
        }

    }


    public void addToPanel(JPanel channelsPanel) {
        channelsPanel.add(new JLabel(channelNumber.toString()));
        channelsPanel.add(isEnableField);
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
        return isEnableField.isSelected();
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

    public Dimension getCommutatorFieldPreferredSize() {
        return commutatorField.getPreferredSize();
    }
}
