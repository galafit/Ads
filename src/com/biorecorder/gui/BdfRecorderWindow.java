package com.biorecorder.gui;

import com.biorecorder.*;
import com.biorecorder.gui.file_gui.FileToSaveUI;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ArrayList;


/**
 *
 */
public class BdfRecorderWindow extends JFrame implements NotificationListener, MessageListener {
    private static final String DIR_CREATION_CONFIRMATION_MSG = "Directory: {0}\ndoes not exist. Do you want to create it?";


    private static final int IDENTIFICATION_LENGTH = 80;
    private static final int PATIENT_LENGTH = 28;
    private static final int RECORDING_LENGTH = 28;

    Color COLOR_CONNECTED = Color.GREEN;
    Color COLOR_DISCONNECTED = Color.GRAY;

    private static final Icon BATTERY_ICON_1 = new ImageIcon("img/battery_1_small.png");
    private static final Icon BATTERY_ICON_2 = new ImageIcon("img/battery_2_small.png");
    private static final Icon BATTERY_ICON_3 = new ImageIcon("img/battery_3_small.png");
    private static final Icon BATTERY_ICON_4 = new ImageIcon("img/battery_4_small.png");
    private static final Icon BATTERY_ICON_5 = new ImageIcon("img/battery_5_small.png");

    private final BdfRecorderApp recorder;

    private ChannelFields[] channels;
    private AccelerometerFields accelerometer;

    private JLabel spsLabel = new JLabel("Max Frequency (Hz)");
    private JComboBox spsField;

    private JLabel patientIdentificationLabel = new JLabel("Patient");
    private JLabel recordingIdentificationLabel = new JLabel("Record");
    private JTextField patientIdentificationField;
    private JTextField recordingIdentificationField;

    private JLabel comportLabel = new JLabel("ComPort");
    private JComboBox comportField;

    private JComboBox deviceTypeField;

    private FileToSaveUI fileToSaveUI;

    private JButton startButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");

    private JButton checkImpedanceButton = new JButton("Impedance");

    private ColoredMarker stateMarker = new ColoredMarker(COLOR_DISCONNECTED);
    private JLabel stateField = new JLabel("Disconnected");

    private ColoredMarker batteryIcon = new ColoredMarker(new Dimension(45, 16));
    private JLabel batteryLevel = new JLabel();

    private String title = "BioRecorder";
    private JComponent[] channelsHeaders = {new JLabel(" "), new JLabel("Enable"), new JLabel("Name"), new JLabel("Frequency (Hz)"),
            new JLabel("Gain"), new JLabel("Commutator State"), new JLabel("Impedance"), new JLabel(" "), new JLabel("50 Hz Filter")};


    public BdfRecorderWindow(BdfRecorderApp recorder) {
        this.recorder = recorder;
        setTitle(title);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                recorder.closeApplication(saveData());
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveData();
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
                OperationResult actionResult = recorder.startRecording(saveData(), false);
                if (!actionResult.isMessageEmpty()) {
                    showMessage(actionResult.getMessage());
                }
            }
        });

        checkImpedanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveData();
                OperationResult actionResult = recorder.startRecording(saveData(), true);
                if (!actionResult.isMessageEmpty()) {
                    showMessage(actionResult.getMessage());
                }
            }
        });


        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                OperationResult actionResult = recorder.stop();
                if (!actionResult.isMessageEmpty()) {
                    showMessage(actionResult.getMessage());
                }
            }
        });
        stopButton.setVisible(false);

        init(recorder.getConfig());
        arrangeForm();
        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);
    }


    private void init(AppConfig config) {
        deviceTypeField = new JComboBox(AppConfig.getAvailableDeviseTypes());
        deviceTypeField.setSelectedItem(config.getDeviceType());

        comportField = new JComboBox(recorder.getAvailableComports());
        String comportName = config.getComportName();
        if (comportName != null && !comportName.isEmpty()) {
            comportField.setSelectedItem(comportName);
        }

        spsField = new JComboBox(AppConfig.getAvailableSampleRates());
        spsField.setSelectedItem(config.getSampleRate());

        channels = new ChannelFields[config.getChannelsCount()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelFields(config, i);
        }
        accelerometer = new AccelerometerFields(config);

        patientIdentificationField = new JTextField(PATIENT_LENGTH);
        recordingIdentificationField = new JTextField(RECORDING_LENGTH);
        patientIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        recordingIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        patientIdentificationField.setText(config.getPatientIdentification());
        recordingIdentificationField.setText(config.getRecordingIdentification());

        fileToSaveUI = new FileToSaveUI();
        fileToSaveUI.setDirectory(config.getDirToSave());

        deviceTypeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recorder.changeDeviceType((String)deviceTypeField.getSelectedItem());
                AppConfig config = recorder.getConfig();
                init(config);
                arrangeForm();
                pack();
            }
        });

        // init available comportField list every time we "open" JComboBox (mouse over «arrow button»)
        comportField.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                comportField.setModel(new DefaultComboBoxModel(recorder.getAvailableComports()));
                BdfRecorderWindow.this.pack();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });

        comportField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!recorder.setComportName((String) comportField.getSelectedItem())) {
                    comportField.setSelectedItem(recorder.getComportName());
                }
            }
        });

        spsField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int sps = (Integer) spsField.getSelectedItem();
                for (ChannelFields channel : channels) {
                    channel.updateFrequencyField(sps);
                }
                accelerometer.updateFrequencyField(sps);
                arrangeForm();
                pack();
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

    }

    private boolean confirm(String message) {
        int answer = JOptionPane.showConfirmDialog(BdfRecorderWindow.this, message, null, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(BdfRecorderWindow.this, msg);
    }

    @Override
    public void onMessage(String message) {
        showMessage(message);
    }

    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Here, we can safely update the GUI
                // because we'll be called from the
                // event dispatch thread
                Color stateColor = COLOR_DISCONNECTED;
                if (recorder.isActive()) {
                    stateColor = COLOR_CONNECTED;
                }
                if (recorder.isRecording()) {
                    stateColor = COLOR_CONNECTED;
                    stopButton.setVisible(true);
                    checkImpedanceButton.setVisible(false);
                    startButton.setVisible(false);
                    disableFields();
                } else {
                    stopButton.setVisible(false);
                    startButton.setVisible(true);
                    checkImpedanceButton.setVisible(true);
                    enableFields();
                }
                updateLeadOffStatus(recorder.getLeadOffMask());
                Integer level = recorder.getBatteryLevel();
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
                    batteryLevel.setText(level + "%  ");
                }
                setReport(recorder.getStateReport(), stateColor);
            }
        });
    }

    private void arrangeForm() {
        getContentPane().removeAll();
        int hgap = 5;
        int vgap = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        buttonPanel.add(startButton);
        // stopButton.setPreferredSize(startButton.getPreferredSize());
        buttonPanel.add(stopButton);
        buttonPanel.add(checkImpedanceButton);

        hgap = 3;
        vgap = 0;
        JPanel spsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        spsPanel.add(spsLabel);
        spsPanel.add(spsField);

        JPanel comportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        comportPanel.add(comportLabel);
        comportPanel.add(comportField);


        hgap = 15;
        vgap = 15;
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        topPanel.add(deviceTypeField);
        topPanel.add(comportPanel);
        topPanel.add(spsPanel);
        topPanel.add(buttonPanel);


        hgap = 9;
        vgap = 0;
        TableLayout tableLayout = new TableLayout(channelsHeaders.length, new TableOption(TableOption.FILL, TableOption.CENTRE), hgap, vgap);
        JPanel channelsPanel = new JPanel(tableLayout);

        // add headers
        for (JComponent component : channelsHeaders) {
            channelsPanel.add(component, new TableOption(TableOption.CENTRE, TableOption.CENTRE));
        }

        // add channels
        for (int i = 0; i < channels.length; i++) {
            channels[i].addToPanel(channelsPanel);
        }

        // Add accelerometer
        accelerometer.addToPanel(channelsPanel);


        hgap = 0;
        vgap = 10;
        JPanel channelsBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        channelsBorderPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        channelsBorderPanel.add(channelsPanel);

        hgap = 3;
        vgap = 0;
        JPanel patientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        patientPanel.add(patientIdentificationLabel);
        patientPanel.add(patientIdentificationField);

        JPanel recordingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        recordingPanel.add(recordingIdentificationLabel);
        recordingPanel.add(recordingIdentificationField);

        hgap = 5;
        vgap = 0;
        JPanel identificationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        identificationPanel.add(patientPanel);
        identificationPanel.add(recordingPanel);

        hgap = 0;
        vgap = 5;
        JPanel identificationBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        identificationBorderPanel.setBorder(BorderFactory.createTitledBorder("Identification"));
        identificationBorderPanel.add(identificationPanel);

        hgap = 5;
        vgap = 5;
        JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        reportPanel.add(stateMarker);
        reportPanel.add(stateField);

        hgap = 5;
        JPanel batteryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        batteryPanel.add(batteryIcon);
        batteryPanel.add(batteryLevel);

        hgap = 5;
        vgap = 5;
        JPanel statePanel = new JPanel(new BorderLayout(hgap, vgap));
        statePanel.add(reportPanel, BorderLayout.CENTER);
        statePanel.add(batteryPanel, BorderLayout.EAST);

        hgap = 0;
        vgap = 5;
        JPanel adsPanel = new JPanel(new BorderLayout(hgap, vgap));
        adsPanel.add(channelsBorderPanel, BorderLayout.NORTH);
        adsPanel.add(identificationBorderPanel, BorderLayout.CENTER);
        adsPanel.add(fileToSaveUI, BorderLayout.SOUTH);

        // Root Panel of the BdfRecorderWindow
        add(topPanel, BorderLayout.NORTH);
        add(adsPanel, BorderLayout.CENTER);
        add(statePanel, BorderLayout.SOUTH);

        // set the same size for identificationPanel and  saveAsPanel
        int height = Math.max(identificationBorderPanel.getPreferredSize().height, fileToSaveUI.getPreferredSize().height);
        int width = Math.max(identificationBorderPanel.getPreferredSize().width, fileToSaveUI.getPreferredSize().width);
        fileToSaveUI.setPreferredSize(new Dimension(width, height));
        identificationBorderPanel.setPreferredSize(new Dimension(width, height));
    }


    private void disableFields() {
        boolean isEnable = false;

        deviceTypeField.setEnabled(isEnable);
        spsField.setEnabled(isEnable);
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
        spsField.setEnabled(isEnable);
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

    private void setReport(String report, Color markerColor) {
        int rowLength = 100;
        String htmlReport = convertToHtml(report, rowLength);
        stateField.setText(htmlReport);
        stateMarker.setColor(markerColor);
    }

    private AppConfig saveData() {
        AppConfig config = new AppConfig();
        config.setDeviceType((String) deviceTypeField.getSelectedItem());
        config.setComportName((String) comportField.getSelectedItem());
        config.setPatientIdentification(patientIdentificationField.getText());
        config.setRecordingIdentification(recordingIdentificationField.getText());
        int[] adsChannelsFrequencies = new int[config.getChannelsCount()];

        for (int i = 0; i < channels.length; i++) {
            config.setChannelName(i, channels[i].getName());
            config.setChannelEnabled(i, channels[i].isEnable());
            config.set50HzFilterEnabled(i, channels[i].is50HzFilterEnable());
            config.setChannelGain(i, channels[i].getGain());
            config.setChannelCommutator(i, channels[i].getCommutator());
            adsChannelsFrequencies[i] = channels[i].getFrequency();
        }
        int spsValue = (Integer) spsField.getSelectedItem();
        config.setSampleRates(spsValue, adsChannelsFrequencies);
        config.setAccelerometerEnabled(accelerometer.isEnabled());
        config.setAccelerometerCommutator(accelerometer.getCommutator());
        config.setFileName(fileToSaveUI.getFilename());
        config.setDirToSave(fileToSaveUI.getDirectory());
        return config;
    }


    private void updateLeadOffStatus(Boolean[] leadOffDetectionMask) {
        if (leadOffDetectionMask != null) {
            for (int i = 0; i < channels.length; i++) {
                Boolean loffPositive = leadOffDetectionMask[2 * i];
                Boolean loffNegative = leadOffDetectionMask[2 * i + 1];
                channels[i].setLoffStatus(loffPositive, loffNegative);
            }
        } else {
            for (int i = 0; i < channels.length; i++) {
                channels[i].setLoffStatus(null, null);
            }
        }
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