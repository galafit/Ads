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
    private static final int PATIENT_LENGTH = 30;
    private static final int RECORDING_LENGTH = 30;

    private static final Color COLOR_PROCESS = Color.GREEN;
    private static final Color COLOR_PROBLEM = Color.RED;
    private static final Color COLOR_INFO = Color.GRAY;

    private static final Icon ICON_SHOW = new ImageIcon("img/arrow-open.png");
    private static final Icon ICON_HIDE = new ImageIcon("img/arrow-close.png");


    private final BdfRecorderApp recorder;

    private ChannelFields[] channels;
    private AccelerometerFields accelerometer;

    private JLabel spsLabel = new JLabel("Maximum Frequency (Hz)");
    private JComboBox spsField = new JComboBox(AppConfig.getAvailableSampleRates());

    private JLabel patientIdentificationLabel = new JLabel("Patient");
    private JLabel recordingIdentificationLabel = new JLabel("Record");
    private JTextField patientIdentificationField = new JTextField(PATIENT_LENGTH);
    private JTextField recordingIdentificationField = new JTextField(RECORDING_LENGTH);

    private JLabel comportLabel = new JLabel("ComPort:");
    private JComboBox comportField = new JComboBox();

    private JLabel deviceTypeLabel = new JLabel("Device:");
    private JComboBox deviceTypeField = new JComboBox(AppConfig.getAvailableDeviseTypes());

    private FileToSaveUI fileToSaveUI = new FileToSaveUI();

    private JButton startButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");

    private JButton checkImpedanceButton = new JButton("Check impedance");

    private MarkerLabel markerLabel = new MarkerLabel();
    private JLabel reportLabel = new JLabel(" ");
    private String title = "EDF Recorder";
    private JComponent[] channelsHeaders = {new JLabel(" "), new JLabel("Enable"), new JLabel("Name"), new JLabel("Frequency (Hz)"),
            new JLabel("Gain"), new JLabel("Commutator State"), new JLabel("Impedance"), new JLabel(" "), new JLabel("50 Hz Filter")};


    public BdfRecorderWindow(BdfRecorderApp recorder) {
        this.recorder = recorder;

        setTitle(title);

        stopButton.setVisible(false);
        patientIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        recordingIdentificationField.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        loadData(recorder.getConfig());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                recorder.closeApplication(saveData());
            }
        });

        deviceTypeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               /* recorder.changeDeviceType((String)deviceTypeField.getSelectedItem());
                AppConfig config = recorder.getConfig();
                createChannelsAndAccelerometerFields(config);
                arrangeForm();
                pack();*/
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
                System.out.println("set comportField: "+ comportField.getSelectedItem());
            }
        });


        comportField.setModel(new DefaultComboBoxModel(recorder.getAvailableComports()));
        String selectedComport = recorder.getComportName();
        if (selectedComport != null && !selectedComport.isEmpty()) {
            comportField.setSelectedItem(selectedComport);
        } else if (comportField.getItemCount() > 0) {
            comportField.setSelectedIndex(0);
        }


        spsField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                createChannelsAndAccelerometerFields(saveData());
                arrangeForm();
                pack();
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

        arrangeForm();
        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadData(AppConfig config) {
        comportField.setModel(new DefaultComboBoxModel(recorder.getAvailableComports()));
        String selectedComport = recorder.getComportName();
        if (selectedComport != null && !selectedComport.isEmpty()) {
            comportField.setSelectedItem(selectedComport);
        } else if (comportField.getItemCount() > 0) {
            comportField.setSelectedIndex(0);
        }

        spsField.setSelectedItem(config.getSampleRate());
        deviceTypeField.setSelectedItem(config.getDeviceType());
        fileToSaveUI.setDirectory(config.getDirToSave());
        patientIdentificationField.setText(config.getPatientIdentification());
        recordingIdentificationField.setText(config.getRecordingIdentification());
        createChannelsAndAccelerometerFields(config);

    }

    private void createChannelsAndAccelerometerFields(AppConfig config) {
        channels = new ChannelFields[config.getChannelsCount()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelFields(config, i);
        }
        accelerometer = new AccelerometerFields(config);
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
                updateLeadOffStatus(recorder.getLeadOffMask());
                Color activeColor = Color.GREEN;
                Color nonActiveColor = Color.GRAY;
                Color stateColor = nonActiveColor;
                if (recorder.isActive()) {
                    stateColor = activeColor;
                }
                if (recorder.isRecording()) {
                    stateColor = activeColor;
                    stopButton.setVisible(true);
                    if (recorder.isLoffDetecting()) {
                        checkImpedanceButton.setVisible(false);
                    } else {
                        startButton.setVisible(false);
                    }
                    disableFields();
                } else {
                    stopButton.setVisible(false);
                    startButton.setVisible(true);
                    checkImpedanceButton.setVisible(true);
                    enableFields();
                }
                setReport(recorder.getStateReport(), stateColor);
            }
        });
    }


    private void arrangeForm() {
        getContentPane().removeAll();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        // stopButton.setPreferredSize(startButton.getPreferredSize());
        buttonPanel.add(stopButton);
        buttonPanel.add(checkImpedanceButton);


        int hgap = 5;
        int vgap = 0;
        JPanel spsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        spsPanel.add(spsLabel);
        spsPanel.add(spsField);


        hgap = 5;
        vgap = 0;
        JPanel comportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        comportPanel.add(comportLabel);
        comportPanel.add(comportField);

        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        devicePanel.add(deviceTypeLabel);
        devicePanel.add(deviceTypeField);


        hgap = 20;
        vgap = 5;
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        topPanel.add(devicePanel);
        topPanel.add(comportPanel);
        topPanel.add(spsPanel);
        topPanel.add(buttonPanel);


        hgap = 9;
        vgap = 0;
        JPanel channelsPanel = new JPanel(new TableLayout(channelsHeaders.length, new TableOption(TableOption.CENTRE, TableOption.CENTRE), hgap, vgap));

        // add headers
        for (JComponent component : channelsHeaders) {
            channelsPanel.add(component);
        }

        // add channels
        for (int i = 0; i < channels.length; i++) {
            channels[i].addToPanel(channelsPanel);
        }

        // Add accelerometer
        accelerometer.addToPanel(channelsPanel);
        accelerometer.setCommutatorFieldPreferredSize(channels[0].getCommutatorFieldPreferredSize());

        hgap = 0;
        vgap = 10;
        JPanel channelsBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        channelsBorderPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        channelsBorderPanel.add(channelsPanel);

        hgap = 5;
        vgap = 0;
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

        hgap = 15;
        vgap = 5;
        JPanel identificationBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        identificationBorderPanel.setBorder(BorderFactory.createTitledBorder("Identification"));
        identificationBorderPanel.add(identificationPanel);

        hgap = 10;
        vgap = 5;
        JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        reportPanel.add(markerLabel);
        reportPanel.add(reportLabel);

        hgap = 0;
        vgap = 5;
        JPanel adsPanel = new JPanel(new BorderLayout(hgap, vgap));
        adsPanel.add(channelsBorderPanel, BorderLayout.NORTH);
        adsPanel.add(identificationBorderPanel, BorderLayout.CENTER);
        adsPanel.add(fileToSaveUI, BorderLayout.SOUTH);

        // Root Panel of the BdfRecorderWindow
        add(topPanel, BorderLayout.NORTH);
        add(adsPanel, BorderLayout.CENTER);
        add(reportPanel, BorderLayout.SOUTH);

        // set the same size for identificationPanel and  saveAsPanel
        int height = Math.max(identificationBorderPanel.getPreferredSize().height, fileToSaveUI.getPreferredSize().height);
        int width = Math.max(identificationBorderPanel.getPreferredSize().width, fileToSaveUI.getPreferredSize().width);
        fileToSaveUI.setPreferredSize(new Dimension(width, height));
        identificationBorderPanel.setPreferredSize(new Dimension(width, height));
    }


    private void disableFields() {
        boolean isEnable = false;

        spsField.setEnabled(isEnable);
        patientIdentificationField.setEnabled(isEnable);
        recordingIdentificationField.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comportField.setEnabled(isEnable);

        accelerometer.setEnabled(isEnable);
        for (int i = 0; i < channels.length; i++) {
            channels[i].setEnabled(isEnable);
        }
    }

    private void enableFields() {
        boolean isEnable = true;
        spsField.setEnabled(isEnable);
        patientIdentificationField.setEnabled(isEnable);
        recordingIdentificationField.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comportField.setEnabled(isEnable);

        if(accelerometer.isEnabled()) {
            accelerometer.setEnabled(true);
        } else {
            accelerometer.setEnabled(false);
        }

        for (int i = 0; i < channels.length; i++) {
            if(channels[i].isEnable()) {
                channels[i].setEnabled(true);
            } else {
                channels[i].setEnabled(false);
            }
        }
    }

    private void setReport(String report, Color markerColor) {
        int rowLength = 100;
        String htmlReport = convertToHtml(report, rowLength);
        reportLabel.setText(htmlReport);
        markerLabel.setColor(markerColor);
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
        config.setAccelerometerOneChannelMode(accelerometer.isOneChannelMode());
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