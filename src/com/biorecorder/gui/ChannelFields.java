package com.biorecorder.gui;

import javax.swing.*;
import java.awt.*;
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
    private JComboBox modeField;
    private JCheckBox isEnabledField;
    private JCheckBox is50HzFilterEnableField;
    private JTextField nameField;
    private ColoredMarker contactPositiveField;
    private ColoredMarker contactNegativeField;

    private int channelNumber;

    public ChannelFields(RecorderSettings settings, int channelNumber) {
        this.channelNumber = channelNumber;
        frequencyField = new JComboBox(settings.getChannelsAvailableFrequencies());
        frequencyField.setSelectedItem(settings.getChannelSampleRate(channelNumber));
        gainField = new JComboBox(settings.getChannelsAvailableGains());
        gainField.setSelectedItem(settings.getChannelGain(channelNumber));
        modeField = new JComboBox(settings.getChannelsAvailableModes());
        modeField.setSelectedItem(settings.getChannelMode(channelNumber));

        is50HzFilterEnableField = new JCheckBox();
        is50HzFilterEnableField.setSelected(settings.is50HzFilterEnabled(channelNumber));
        nameField = new JTextField(NAME_LENGTH);
        nameField.setDocument(new FixSizeDocument(NAME_LENGTH));
        nameField.setText(settings.getChannelName(channelNumber));

        contactNegativeField = new ColoredMarker(ICON_DISABLED);
        contactPositiveField = new ColoredMarker(ICON_DISABLED);
        setContactsVisible(false);

        isEnabledField = new JCheckBox();
        isEnabledField.setSelected(settings.isChannelEnabled(channelNumber));
        setEnabled(settings.isChannelEnabled(channelNumber));
        isEnabledField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(isEnabledField.isSelected());
            }
        });

    }

    /**
     * enable/disable all fields EXCLUDING isEnabledField
     *
     * @param isEnabled
     */
    public void setEnabled(boolean isEnabled) {
        frequencyField.setEnabled(isEnabled);
        gainField.setEnabled(isEnabled);
        modeField.setEnabled(isEnabled);
        is50HzFilterEnableField.setEnabled(isEnabled);
        nameField.setEnabled(isEnabled);
    }

    /**
     * enable/disable all fields INCLUDING isEnabledField
     *
     * @param isEnabled
     */
    public void setFullyEnabled(boolean isEnabled) {
        setEnabled(isEnabled);
        isEnabledField.setEnabled(isEnabled);
    }

    public void setContacts(Boolean contactPositive, Boolean contactNegative) {
        if (contactNegative == null) {
            contactNegativeField.setIcon(ICON_DISABLED);
        } else if (contactNegative == true) {
            contactNegativeField.setIcon(ICON_DISCONNECTED);
        } else {
            contactNegativeField.setIcon(ICON_CONNECTED);
        }

        if (contactPositive == null) {
            contactPositiveField.setIcon(ICON_DISABLED);
        } else if (contactPositive == true) {
            contactPositiveField.setIcon(ICON_DISCONNECTED);
        } else {
            contactPositiveField.setIcon(ICON_CONNECTED);
        }
    }

    public void setContactsVisible(boolean isVisible) {
        if (!isVisible) {
            contactNegativeField.setIcon(ICON_DISABLED);
            contactPositiveField.setIcon(ICON_DISABLED);
        }
        contactPositiveField.setVisible(isVisible);
        contactNegativeField.setVisible(isVisible);
        is50HzFilterEnableField.setVisible(!isVisible);
    }


    public void addToPanel(JPanel channelsPanel) {
        channelsPanel.add(new JLabel(new Integer(channelNumber + 1).toString()));
        channelsPanel.add(isEnabledField);
        channelsPanel.add(nameField);
        channelsPanel.add(frequencyField);
        channelsPanel.add(modeField);
        channelsPanel.add(gainField);

        int hgap = 5;
        int vgap = 0;
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        panel.add(is50HzFilterEnableField);
        panel.add(contactPositiveField);
        panel.add(contactNegativeField);
        channelsPanel.add(panel);
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

    public String getMode() {
        return (String) modeField.getSelectedItem();
    }

    public boolean is50HzFilterEnable() {
        return is50HzFilterEnableField.isSelected();
    }

    public String getName() {
        return nameField.getText();
    }
}
