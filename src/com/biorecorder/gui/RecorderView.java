package com.biorecorder.gui;

import com.biorecorder.*;
import com.biorecorder.gui.file_gui.FileToSaveUI;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ArrayList;


/**
 *
 */
public class RecorderView extends JFrame implements ProgressListener, StateChangeListener, AvailableComportsListener {
    private static final String DIR_CREATION_CONFIRMATION_MSG = "Directory: {0}\ndoes not exist. Do you want to create it?";


    private static final int IDENTIFICATION_LENGTH = 80;
    private static final int PATIENT_LENGTH = 20;
    private static final int RECORDING_LENGTH = 20;

    private static final int FILENAME_LENGTH = 12;
    private static final int DIRNAME_LENGTH = 45;

    private static final Color COLOR_CONNECTED = new Color(79, 245, 42);
    private static final Color COLOR_DISCONNECTED = Color.GRAY;
    private static final Color COLOR_TITLE = new Color(10, 10, 120);
    private static final Icon BATTERY_ICON_1 = new ImageIcon("img/battery_1_small.png");
    private static final Icon BATTERY_ICON_2 = new ImageIcon("img/battery_2_small.png");
    private static final Icon BATTERY_ICON_3 = new ImageIcon("img/battery_3_small.png");
    private static final Icon BATTERY_ICON_4 = new ImageIcon("img/battery_4_small.png");
    private static final Icon BATTERY_ICON_5 = new ImageIcon("img/battery_5_small.png");

    private static final String TITLE_CHANNELS = "Channels";
    private static final String TITLE_SAVE_AS = "Save as";
    private static final String TITLE_IDENTIFICATION = "Identification";

    private String[] availableComports;

    private final RecorderViewModel recorder;
    private RecorderSettings settings;

    private ChannelFields[] channels;
    private AccelerometerFields accelerometer;

    private JLabel maxFrequencyLabel = new JLabel("Max Frequency (Hz)", SwingConstants.CENTER);
    private JComboBox maxFrequencyField;

    private JLabel patientIdentificationLabel = new JLabel("Patient");
    private JLabel recordingIdentificationLabel = new JLabel("Record");
    private JTextField patientIdentificationField;
    private JTextField recordingIdentificationField;

    private JLabel comportLabel = new JLabel("Comport", SwingConstants.CENTER);
    private JComboBox comportField;
    private JLabel deviceTypeLabel = new JLabel("Device", SwingConstants.CENTER);
    private JComboBox deviceTypeField;

    private FileToSaveUI fileToSaveUI;

    private JButton startRecordingButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");
    private JButton checkContactsButton = new JButton("Check contacts");

    private ColoredMarker stateMarker = new ColoredMarker(COLOR_DISCONNECTED);
    private JLabel progressField = new JLabel("Disconnected");

    private ColoredMarker batteryIcon = new ColoredMarker(new Dimension(45, 16));
    private JLabel batteryLevel = new JLabel();

    private String title = "BioRecorder";

    private String filter50Hz = "Filter50Hz";
    private String contacts = "Contacts";
    private JLabel filterOrContactsLabel = new JLabel(filter50Hz, SwingConstants.CENTER);

    private JComponent[] channelsHeaders = {new JLabel(" "), new JLabel(" "), new JLabel("Name", SwingConstants.CENTER), new JLabel("Frequency", SwingConstants.CENTER),
            new JLabel("Mode", SwingConstants.CENTER), new JLabel("Gain", SwingConstants.CENTER), filterOrContactsLabel};


    public RecorderView(RecorderViewModel recorder) {
        this.recorder = recorder;
        startRecordingButton.setForeground(COLOR_TITLE);
        stopButton.setForeground(COLOR_TITLE);
        checkContactsButton.setForeground(COLOR_TITLE);

        settings = recorder.getInitialSettings();
        availableComports = settings.getAvailableComports();
        setTitle(title);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                recorder.closeApplication(saveSettings());
            }
        });

        startRecordingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
               startRecording();
            }
        });

        checkContactsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
              checkContacts();
            }
        });


        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
               stop();
            }
        });
        stopButton.setVisible(false);

        loadData(settings);

        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);
    }


    private void loadData(RecorderSettings settings) {
        deviceTypeField = new JComboBox(settings.getAvailableDeviseTypes());
        deviceTypeField.setSelectedItem(settings.getDeviceType());

        comportField = new JComboBox(settings.getAvailableComports());
        String comportName = settings.getComportName();
        if (comportName != null && !comportName.isEmpty()) {
            comportField.setSelectedItem(comportName);
        }

        maxFrequencyField = new JComboBox(settings.getAvailableMaxFrequencies());
        maxFrequencyField.setSelectedItem(settings.getMaxFrequency());

        channels = new ChannelFields[settings.getChannelsCount()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelFields(settings, i);
        }
        accelerometer = new AccelerometerFields(settings);

        patientIdentificationField = new JTextField(PATIENT_LENGTH);
        recordingIdentificationField = new JTextField(RECORDING_LENGTH);
        patientIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        recordingIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        patientIdentificationField.setText(settings.getPatientIdentification());
        recordingIdentificationField.setText(settings.getRecordingIdentification());

        fileToSaveUI = new FileToSaveUI(FILENAME_LENGTH, DIRNAME_LENGTH);
        fileToSaveUI.setDirectory(settings.getDirToSave());

        deviceTypeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              changeDeviceType();
            }
        });

        comportField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeComport();
            }
        });

        // init available comport list every time we "open" JComboBox (mouse over «arrow button»)
     /*   comportField.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                comportField.setModel(new DefaultComboBoxModel(availableComports));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });*/

        maxFrequencyField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                changeMaxFrequency();
            }
        });

        patientIdentificationField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                patientIdentificationField.selectAll();
            }
        });


        recordingIdentificationField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                recordingIdentificationField.selectAll();
            }
        });

        arrangeFields();
        pack();

    }

    private void arrangeFields() {
        getContentPane().removeAll();

        JPanel topPanel = new JPanel(new MigLayout("fill, insets 5", "center", "center"));
        topPanel.add(deviceTypeLabel);
        topPanel.add(comportLabel);
        topPanel.add(maxFrequencyLabel, "wrap");
        topPanel.add(deviceTypeField);
        topPanel.add(comportField);
        topPanel.add(maxFrequencyField);

        LC layoutConstraints = new LC();
        layoutConstraints.fill();
        layoutConstraints.setWrapAfter(7);
        UnitValue[] insets = {new UnitValue(0), new UnitValue(0), new UnitValue(0), new UnitValue(0) };
        layoutConstraints.setInsets(insets);

        AC columnConstraints = new AC();
        columnConstraints.align("left");
        columnConstraints.fill();
        columnConstraints.gap("10");

        AC rowConstraints = new AC();
        rowConstraints.align("center");

        MigLayout tableLayout = new MigLayout(layoutConstraints,columnConstraints, rowConstraints);

        JPanel channelsPanel = new JPanel(tableLayout);

        // add headers
        for (JComponent component : channelsHeaders) {
            channelsPanel.add(component, "center");
        }

        // add channels
        for (int i = 0; i < channels.length; i++) {
            channels[i].addToPanel(channelsPanel);
        }

        // Add accelerometer
        accelerometer.addToPanel(channelsPanel);

        JPanel channelsBorderPanel = new JPanel(new MigLayout("fill, insets 10"));

        TitledBorder titledBorder = BorderFactory.createTitledBorder(TITLE_CHANNELS);
        titledBorder.setTitleColor(COLOR_TITLE);
        channelsBorderPanel.setBorder(titledBorder);
        channelsBorderPanel.add(channelsPanel, "grow");

        int hgap = 5;
        int vgap = 0;
        JPanel patientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        patientPanel.add(patientIdentificationLabel);
        patientPanel.add(patientIdentificationField);

        JPanel recordingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        recordingPanel.add(recordingIdentificationLabel);
        recordingPanel.add(recordingIdentificationField);

        hgap = 0;
        vgap = 0;
        JPanel identificationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        identificationPanel.add(patientPanel);
        identificationPanel.add(recordingPanel);

        hgap = 0;
        vgap = 5;
        JPanel identificationBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        titledBorder = BorderFactory.createTitledBorder(TITLE_IDENTIFICATION);
        titledBorder.setTitleColor(COLOR_TITLE);
        identificationBorderPanel.setBorder(titledBorder);
        identificationBorderPanel.add(identificationPanel);

        hgap = 0;
        vgap = 5;
        JPanel saveAsBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        titledBorder = BorderFactory.createTitledBorder(TITLE_SAVE_AS);
        titledBorder.setTitleColor(COLOR_TITLE);
        saveAsBorderPanel.setBorder(titledBorder);
        saveAsBorderPanel.add(fileToSaveUI);

        hgap = 0;
        vgap = 10;
        JPanel batteryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        batteryPanel.add(batteryIcon);
        batteryPanel.add(batteryLevel);

        hgap = 10;
        vgap = 0;
        JPanel batteryWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        batteryWrapperPanel.add(batteryPanel);

        hgap = 5;
        vgap = 5;
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        progressPanel.add(stateMarker);
        progressPanel.add(progressField);

        hgap = 10;
        vgap = 5;
        JPanel progressWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        progressWrapperPanel.add(progressPanel);

        hgap = 5;
        vgap = 5;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        buttonPanel.add(startRecordingButton);
        stopButton.setPreferredSize(checkContactsButton.getPreferredSize());
        startRecordingButton.setPreferredSize(checkContactsButton.getPreferredSize());
        buttonPanel.add(stopButton);
        buttonPanel.add(checkContactsButton);

        hgap = 5;
        vgap = 0;
        JPanel statePanel = new JPanel(new BorderLayout(hgap, vgap));
        statePanel.add(batteryWrapperPanel, BorderLayout.WEST);
        statePanel.add(progressWrapperPanel, BorderLayout.CENTER);
        statePanel.add(buttonPanel, BorderLayout.EAST);

        MigLayout migLayout = new MigLayout("fill, wrap 1, insets 5", "center, fill", "center");

        JPanel mainPanel = new JPanel(migLayout);
        mainPanel.add(topPanel);
        mainPanel.add(channelsBorderPanel);
        mainPanel.add(identificationBorderPanel);
        mainPanel.add(saveAsBorderPanel);
        mainPanel.add(statePanel);

        // Root Panel of the RecorderView
        add(mainPanel, BorderLayout.CENTER);
    }

    private boolean confirm(String message) {
        int answer = JOptionPane.showConfirmDialog(RecorderView.this, message, null, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(RecorderView.this, msg);
    }

    @Override
    public void onAvailableComportsChanged(String[] comports) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                availableComports = comports;
                String comportName = (String) comportField.getSelectedItem();
                ActionListener[] listeners = comportField.getActionListeners();
                if(!comportField.isPopupVisible()) {
                    for (ActionListener listener : listeners) {
                        comportField.removeActionListener(listener);
                    }

                    comportField.setModel(new DefaultComboBoxModel(comports));
                    if (comportName != null && !comportName.isEmpty()) {
                        comportField.setSelectedItem(comportName);
                    } else {
                        String selectedComport = (String) comportField.getSelectedItem();
                        if(selectedComport != null && !selectedComport.isEmpty()) {
                            recorder.changeComport(selectedComport);
                        }
                    }
                    comportField.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            changeComport();
                        }
                    });
                }
            }
        });

    }

    @Override
    public void onStateChanged(StateChangeReason changeReason) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (recorder.isRecording()) {
                    stopButton.setVisible(true);
                    checkContactsButton.setVisible(false);
                    startRecordingButton.setVisible(false);
                    disableFields();
                } else {
                    stopButton.setVisible(false);
                    startRecordingButton.setVisible(true);
                    checkContactsButton.setVisible(true);
                    enableFields();
                }

                if(recorder.isCheckingContacts()) {
                    setContactsVisible(true);
                } else {
                    setContactsVisible(false);
                }

                if(!changeReason.isMessageEmpty()) {
                    showMessage(changeReason.getMessage());
                }
            }
        });
    }

    @Override
    public void onProgress() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // update progress info
                Color stateColor = COLOR_DISCONNECTED;
                if (recorder.isActive()) {
                    stateColor = COLOR_CONNECTED;
                }

                progressField.setText(recorder.getProgressInfo());
                stateMarker.setColor(stateColor);

                updateBatteryLevel(recorder.getBatteryLevel());

                updateContacts(recorder.getDisconnectionMask());
            }
        });
    }

    private void startRecording() {
        String dirToSave = fileToSaveUI.getDirectory();

        if (!recorder.isDirectoryExist(dirToSave)) {
            String confirmMsg = MessageFormat.format(DIR_CREATION_CONFIRMATION_MSG, dirToSave);
            if (confirm(confirmMsg)) {
                OperationResult actionResult = recorder.createDirectory(dirToSave);
                if (!actionResult.isMessageEmpty()) {
                    showMessage(actionResult.getMessage());
                }
                if (!actionResult.isSuccess()) {
                    return;
                }
            } else {
                return;
            }
        }
        OperationResult actionResult = recorder.startRecording(saveSettings());
        if (!actionResult.isMessageEmpty()) {
            showMessage(actionResult.getMessage());
        }
    }

    private void stop() {
        OperationResult actionResult = recorder.stop();
        if (!actionResult.isMessageEmpty()) {
            showMessage(actionResult.getMessage());
        }
    }

    private void changeDeviceType() {
        settings = recorder.changeDeviceType(saveSettings());
        loadData(settings);
    }

    private void changeMaxFrequency() {
        settings = recorder.changeMaxFrequency(saveSettings());
        loadData(settings);
    }

    private void changeComport() {
        recorder.changeComport((String) comportField.getSelectedItem());
    }

    private void updateBatteryLevel(Integer level) {
        if(level != null) {
            if(level < 20) {
                batteryIcon.setIcon(BATTERY_ICON_1);
            } else if(level < 40) {
                batteryIcon.setIcon(BATTERY_ICON_2);
            } else if(level < 60) {
                batteryIcon.setIcon(BATTERY_ICON_3);
            } else if(level < 80) {
                batteryIcon.setIcon(BATTERY_ICON_4);
            } else {
                batteryIcon.setIcon(BATTERY_ICON_5);
            }
            String levelText = Integer.toString(level);
            if(level <  10) {
                levelText = "< 10";
            }
            batteryLevel.setText(levelText + "%");
        }
    }

    private void updateContacts(Boolean[] contactsMask) {
        if (contactsMask != null) {
            for (int i = 0; i < channels.length; i++) {
                Boolean contactPositive = contactsMask[2 * i];
                Boolean contactNegative = contactsMask[2 * i + 1];
                channels[i].setContacts(contactPositive, contactNegative);
            }
        }
    }

    private void setContactsVisible(boolean isVisible) {
        if(isVisible) {
            filterOrContactsLabel.setText(contacts);
        } else {
            filterOrContactsLabel.setText(filter50Hz);
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i].setContactsVisible(isVisible);
        }
    }

    private void checkContacts() {
        OperationResult actionResult = recorder.checkContacts(saveSettings());
        if (!actionResult.isMessageEmpty()) {
            showMessage(actionResult.getMessage());
        }
    }


    private void disableFields() {
        boolean isEnable = false;

        deviceTypeField.setEnabled(isEnable);
        maxFrequencyField.setEnabled(isEnable);
        patientIdentificationField.setEnabled(isEnable);
        recordingIdentificationField.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comportField.setEnabled(isEnable);

        accelerometer.setFullyEnabled(isEnable);
        for (int i = 0; i < channels.length; i++) {
            channels[i].setFullyEnabled(isEnable);
        }
    }

    private void enableFields() {
        boolean isEnable = true;

        deviceTypeField.setEnabled(isEnable);
        maxFrequencyField.setEnabled(isEnable);
        patientIdentificationField.setEnabled(isEnable);
        recordingIdentificationField.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comportField.setEnabled(isEnable);

        accelerometer.setFullyEnabled(true);
        if(!accelerometer.isEnabled()) {
            accelerometer.setEnabled(false);
        }

        for (int i = 0; i < channels.length; i++) {
            channels[i].setFullyEnabled(true);
            if(!channels[i].isEnable()) {
                channels[i].setEnabled(false);
            }
        }
    }


    private RecorderSettings saveSettings() {
        settings.setDeviceType((String) deviceTypeField.getSelectedItem());
        settings.setComportName((String) comportField.getSelectedItem());
        settings.setPatientIdentification(patientIdentificationField.getText());
        settings.setRecordingIdentification(recordingIdentificationField.getText());
        for (int i = 0; i < channels.length; i++) {
            settings.setChannelName(i, channels[i].getName());
            settings.setChannelEnabled(i, channels[i].isEnable());
            settings.set50HzFilterEnabled(i, channels[i].is50HzFilterEnable());
            settings.setChannelGain(i, channels[i].getGain());
            settings.setChannelMode(i, channels[i].getMode());
            settings.setChannelFrequency(i, channels[i].getFrequency());
        }

        settings.setMaxFrequency((Integer) maxFrequencyField.getSelectedItem());
        settings.setAccelerometerEnabled(accelerometer.isEnabled());
        settings.setAccelerometerMode(accelerometer.getMode());
        settings.setFileName(fileToSaveUI.getFilename());
        settings.setDirToSave(fileToSaveUI.getDirectory());
        return settings;
    }

    private String convertToHtml(String text, int rowLength) {
        StringBuilder html = new StringBuilder("<html>");
        String[] givenRows = text.split("\n");
        for (String givenRow : givenRows) {
            String[] splitRows = split(givenRow, rowLength);
            for (String row : splitRows) {
                html.append(row);
                html.append("<br>");
            }
        }
        html.append("</html>");
        return html.toString();
    }

    // split input string to the  array of strings with length() <= rowLength
    private String[] split(String text, int rowLength) {
        ArrayList<String> resultRows = new ArrayList<String>();
        StringBuilder row = new StringBuilder();
        String[] words = text.split(" ");
        for (String word : words) {
            if ((row.length() + word.length()) < rowLength) {
                row.append(word);
                row.append(" ");
            } else {
                resultRows.add(row.toString());
                row = new StringBuilder(word);
                row.append(" ");
            }
        }
        resultRows.add(row.toString());
        String[] resultArray = new String[resultRows.size()];
        return resultRows.toArray(resultArray);
    }
}