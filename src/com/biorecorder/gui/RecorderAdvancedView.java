package com.biorecorder.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * http://www.java2s.com/Code/Java/Swing-JFC/NumericTextField.htm
 */
public class RecorderAdvancedView extends JDialog {
    private JCheckBox writeBatteryVoltageToFileField = new JCheckBox();
    private JCheckBox labStreamingField =  new JCheckBox();
    private JCheckBox adjustDurationOfDataRecordField =  new JCheckBox();

    private final Double[] AVAILABLE_DURATIONS = {1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
    private JComboBox durationOfDataRecordField = new JComboBox(AVAILABLE_DURATIONS);
    private JButton okButton = new JButton("Ok");

    private RecorderSettings settings;

    public RecorderAdvancedView(RecorderSettings settings, Frame owner) {
        super(owner, true);
        setTitle("Advanced settings");
        this.settings = settings;

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new MigLayout("fill, insets 5", "left", "center"));

        innerPanel.add(new JLabel("Duration of data records (sec):"));
        innerPanel.add(durationOfDataRecordField, "wrap");

        innerPanel.add(new JLabel("Adjust duration of data records:"));
        innerPanel.add(adjustDurationOfDataRecordField, "wrap");

        innerPanel.add(new JLabel("Write battery voltage to the file:"));
        innerPanel.add(writeBatteryVoltageToFileField, "wrap");

        innerPanel.add(new JLabel("Stream to lab:"));
        innerPanel.add(labStreamingField, "wrap");

        innerPanel.add(okButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
                dispose();
            }
        });
        int hgap = 20;
        int vgap = 15;
        getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        add(innerPanel);
        loadData();
        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadData() {
        writeBatteryVoltageToFileField.setSelected(!settings.isBatteryVoltageChannelDeletingEnable());
        labStreamingField.setSelected(settings.isLabStreamingEnabled());
        adjustDurationOfDataRecordField.setSelected(settings.isDurationOfDataRecordAdjustable());
        durationOfDataRecordField.setSelectedItem(settings.getDataRecordDuration());
    }

    private void saveData() {
        settings.setBatteryVoltageChannelDeletingEnable(!writeBatteryVoltageToFileField.isSelected());
        settings.setLabStreamingEnabled(labStreamingField.isSelected());
        settings.setDurationOfDataRecordAdjustable(adjustDurationOfDataRecordField.isSelected());
        settings.setDataRecordDuration((Double)durationOfDataRecordField.getSelectedItem());
    }
}
