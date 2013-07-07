package com.crostec.bdfrecorder;

import com.crostec.ads.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 *
 */
public class SettingsWindow extends JFrame implements AdsDataListener {

    private Controller controller;
    private BdfHeaderData bdfHeaderData;
    private int adsDataFrameSize;
    private int adsDataFrameCounter;
    private int adsDataFrameFrequency;
//    public static final String FILENAME_PATTERN = "dd-mm-yyyy_hh-mm.bdf";
    private String patientIdentificationLabel = "Patient";
    private String recordingIdentificationLabel = "Record";
    private String spsLabel = "Sampling Frequency (Hz)";
    private String comPortLabel = "Com Port";
    private JComboBox spsField;
    private JTextField comPortName;
    private JComboBox[] channelFrequency;
    private JCheckBox[] channelEnable;
    private JTextField[] channelName;
    private JCheckBox[] channelDrlEnabled;
    private JCheckBox[] channelLoffEnable;
    private JTextField[] channelElectrodeType;

    private JComboBox accelerometerFrequency;
    private JTextField accelerometerName;
    private JCheckBox accelerometerEnable;
    private JTextField patientIdentification;
    private JTextField recordingIdentification;

    private JTextField fileToSave;

    private boolean isAdvanced = false;
    private String start = "Start";
    private String stop = "Stop";
    private JButton startButton = new JButton(start);

    private String advancedLabel = "Advanced";
    private JButton advancedButton = new JButton();

    private Color colorProcess = Color.GREEN;
    private Color colorProblem = Color.RED;
    private Color colorInfo = Color.GRAY;
    private MarkerLabel markerLabel = new MarkerLabel();
    private JLabel reportLabel = new JLabel(" ");

    Icon iconShow = new ImageIcon("img/arrow-open.png");
    Icon iconHide = new ImageIcon("img/arrow-close.png");
    Icon iconConnected = new ImageIcon("img/greenBall.png");
    Icon iconDisconnected = new ImageIcon("img/redBall.png");
    Icon iconDisabled = new ImageIcon("img/grayBall.png");
    private MarkerLabel[] channelLoffStatPositive;
    private MarkerLabel[] channelLoffStatNegative;
    private String title = "EDF Recorder";
    private JComponent[] channelsHeaders = {new JLabel("Number"), new JLabel("Enable"), new JLabel("Name"), new JLabel("Frequency (Hz)"), new JLabel("Lead Off Detection"), new JLabel("DRL"), new JLabel("Lead Off"), advancedButton};


    public SettingsWindow(Controller controller, BdfHeaderData bdfHeaderData) {
        this.controller = controller;
        this.bdfHeaderData = bdfHeaderData;
        init();
        arrangeForm();
        setActions();
        loadDataFromModel();
        setVisible(true);
    }

    private void init() {
        int adsChannelsNumber = bdfHeaderData.getAdsConfiguration().getAdsChannels().size();
        advancedButton.setIcon(iconShow);

        spsField = new JComboBox(Sps.values());
        spsField.setSelectedItem(bdfHeaderData.getAdsConfiguration().getSps());
        int textFieldLength = 5;
        comPortName = new JTextField(textFieldLength);

        textFieldLength = 25;
        patientIdentification = new JTextField(textFieldLength);
        recordingIdentification = new JTextField(textFieldLength);

        textFieldLength = 55;
        fileToSave = new JTextField(textFieldLength);

        channelFrequency = new JComboBox[adsChannelsNumber];
        channelEnable = new JCheckBox[adsChannelsNumber];
        channelName = new JTextField[adsChannelsNumber];
        channelElectrodeType = new JTextField[adsChannelsNumber];
        channelLoffStatPositive = new MarkerLabel[adsChannelsNumber];
        channelLoffStatNegative = new MarkerLabel[adsChannelsNumber];
        channelDrlEnabled = new JCheckBox[adsChannelsNumber];
        channelLoffEnable = new JCheckBox[adsChannelsNumber];
        textFieldLength = 16;
        for (int i = 0; i < adsChannelsNumber; i++) {
            channelFrequency[i] = new JComboBox();
            channelEnable[i] = new JCheckBox();
            channelName[i] = new JTextField(textFieldLength);
            // channelElectrodeType[i] = new JTextField(textFieldLength);
            channelDrlEnabled[i] = new JCheckBox();
            channelLoffEnable[i] = new JCheckBox();
            channelLoffStatPositive[i] = new MarkerLabel(iconDisabled);
            channelLoffStatNegative[i] = new MarkerLabel(iconDisabled);
        }
        accelerometerEnable = new JCheckBox();
        accelerometerName = new JTextField(textFieldLength);
        accelerometerFrequency = new JComboBox();
        setAdvanced();
    }

    private void setActions() {

        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            channelEnable[i].addActionListener(new AdsChannelEnableListener(i));
        }

        accelerometerEnable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JCheckBox checkBox = (JCheckBox) actionEvent.getSource();
                if (checkBox.isSelected()) {
                    enableAccelerometer(true);
                } else {
                    enableAccelerometer(false);
                }
            }
        });


        spsField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox comboBox = (JComboBox) actionEvent.getSource();
                Sps sps = (Sps) comboBox.getSelectedItem();
                setChannelsFrequencies(sps);
            }
        });


        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (controller.isRecording()) {
                    controller.stopRecording();
                    startButton.setText(start);
                    enableFields();
                    setProcessReport("Saved to file: " + bdfHeaderData.getFileNameToSave());  //todo enter file name
                } else {
                    startButton.setText(stop);
                    comPortName.setEnabled(false);
                    disableFields();
                    saveDataToModel();
                    adsDataFrameSize = AdsUtils.getDecodedFrameSize(bdfHeaderData.getAdsConfiguration());
                    adsDataFrameCounter = 0;
                    adsDataFrameFrequency = bdfHeaderData.getAdsConfiguration().getSps().getValue() / AdsChannelConfiguration.MAX_DIV.getValue();
                    setProcessReport("Connecting...");
                    controller.startRecording(bdfHeaderData);
                }
            }
        });

        advancedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setAdvanced();
            }
        });

        advancedButton.setToolTipText(advancedLabel);

        patientIdentification.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                patientIdentification.selectAll();
            }
        });


        recordingIdentification.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                recordingIdentification.selectAll();
            }
        });


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                saveDataToModel();
                controller.closeApplication(bdfHeaderData);
            }
        });
    }


    private void arrangeForm() {
        setTitle(title);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);

        int hgap = 5;
        int vgap = 0;
        JPanel spsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        spsPanel.add(new JLabel(spsLabel));
        spsPanel.add(spsField);

        JPanel comPortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        comPortPanel.add(new Label(comPortLabel));
        comPortPanel.add(comPortName);

        hgap = 60;
        vgap = 15;
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        topPanel.add(comPortPanel);
        topPanel.add(spsPanel);
        topPanel.add(buttonPanel);


        hgap = 20;
        vgap = 5;
        JPanel channelsPanel = new JPanel(new TableLayout(channelsHeaders.length, new TableOption(TableOption.CENTRE, TableOption.CENTRE), hgap, vgap));

        for (JComponent component : channelsHeaders) {
            channelsPanel.add(component);
        }

        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            channelsPanel.add(new JLabel(" " + (i+1) + " "));
            channelsPanel.add(channelEnable[i]);
            channelsPanel.add(channelName[i]);
            channelsPanel.add(channelFrequency[i]);
            JPanel loffPanel = new JPanel();
            loffPanel.add(channelLoffStatPositive[i]);
            loffPanel.add(channelLoffStatNegative[i]);
            channelsPanel.add(loffPanel);
            // channelsPanel.add(channelElectrodeType[i]);
            channelsPanel.add(channelDrlEnabled[i]);
            channelsPanel.add(channelLoffEnable[i]);
            channelsPanel.add(new JLabel(" "));
        }

        // Add line of accelerometer
        channelsPanel.add(new JLabel(" " + (1 + bdfHeaderData.getAdsConfiguration().getAdsChannels().size()) + " "));
        channelsPanel.add(accelerometerEnable);
        channelsPanel.add(accelerometerName);
        channelsPanel.add(accelerometerFrequency);

        hgap = 0;
        vgap = 10;
        JPanel channelsBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        channelsBorderPanel.setBorder(BorderFactory.createTitledBorder("Channels"));
        channelsBorderPanel.add(channelsPanel);

        hgap = 5;
        vgap = 0;
        JPanel patientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        patientPanel.add(new JLabel(patientIdentificationLabel));
        patientPanel.add(patientIdentification);

        JPanel recordingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        recordingPanel.add(new JLabel(recordingIdentificationLabel));
        recordingPanel.add(recordingIdentification);

        hgap = 0;
        vgap = 0;
        JPanel identificationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        identificationPanel.add(patientPanel);
        identificationPanel.add(new Label("    "));
        identificationPanel.add(recordingPanel);

        hgap = 15;
        vgap = 5;
        JPanel identificationBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        identificationBorderPanel.setBorder(BorderFactory.createTitledBorder("Identification"));
        identificationBorderPanel.add(identificationPanel);


        hgap = 5;
        vgap = 0;
        JPanel saveAsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        saveAsPanel.add(fileToSave);

        hgap = 15;
        vgap = 5;
        JPanel saveAsBorderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        saveAsBorderPanel.setBorder(BorderFactory.createTitledBorder("Save As"));
        saveAsBorderPanel.add(saveAsPanel);

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
        adsPanel.add(saveAsBorderPanel, BorderLayout.SOUTH);

        // Root Panel of the SettingsWindow
        add(topPanel, BorderLayout.NORTH);
        add(adsPanel, BorderLayout.CENTER);
        add(reportPanel, BorderLayout.SOUTH);

        // set the same size for identificationPanel and  saveAsPanel
        int height = Math.max(identificationPanel.getPreferredSize().height, saveAsPanel.getPreferredSize().height);
        int width = Math.max(identificationPanel.getPreferredSize().width, saveAsPanel.getPreferredSize().width);
        saveAsPanel.setPreferredSize(new Dimension(width, height));
        identificationPanel.setPreferredSize(new Dimension(width, height));


        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
    }

    private void disableEnableFields(boolean isEnable) {
        spsField.setEnabled(isEnable);
        patientIdentification.setEnabled(isEnable);
        recordingIdentification.setEnabled(isEnable);
        fileToSave.setEnabled(isEnable);

        accelerometerName.setEnabled(isEnable);
        accelerometerEnable.setEnabled(isEnable);
        accelerometerFrequency.setEnabled(isEnable);

        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            channelEnable[i].setEnabled(isEnable);
            channelName[i].setEnabled(isEnable);
            channelFrequency[i].setEnabled(isEnable);
            channelDrlEnabled[i].setEnabled(isEnable);
            channelLoffEnable[i].setEnabled(isEnable);
            // channelElectrodeType[i].setEnabled(isEnable);
        }
    }


    private void disableFields() {
        boolean isEnable = false;
        disableEnableFields(isEnable);


    }


    private void enableFields() {
        boolean isEnable = true;
        disableEnableFields(isEnable);
        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            if (!isChannelEnable(i)) {
                enableAdsChannel(i, false);
            }
        }
        if (!bdfHeaderData.getAdsConfiguration().isAccelerometerEnabled()) {
            enableAccelerometer(false);
        }
    }

    private void setReport(String report, Color markerColor) {
        int rowLength = 100;
        String htmlReport = convertToHtml(report, rowLength);
        reportLabel.setText(htmlReport);
        markerLabel.setColor(markerColor);
    }

    public void setProcessReport(String report) {
        setReport(report, colorProcess);
    }

    public void setProblemReport(String report) {
        setReport(report, colorProblem);
    }

    public void setReport(String report) {
        setReport(report, colorInfo);
    }

    private void loadDataFromModel() {
        spsField.setSelectedItem(bdfHeaderData.getAdsConfiguration().getSps());
        comPortName.setText(bdfHeaderData.getAdsConfiguration().getComPortName());
//        fileToSave.setText(FILENAME_PATTERN);
        patientIdentification.setText(bdfHeaderData.getPatientIdentification());
        recordingIdentification.setText(bdfHeaderData.getRecordingIdentification());
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getAdsChannels().size();
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfiguration channel = bdfHeaderData.getAdsConfiguration().getAdsChannels().get(i);
            channelName[i].setText(bdfHeaderData.getAdsChannelNames().get(i));
            channelEnable[i].setSelected(channel.isEnabled());
            channelDrlEnabled[i].setSelected(channel.isRldSenseEnabled());
            channelLoffEnable[i].setSelected(channel.isLoffEnable());
            //channelElectrodeType[i].setText(channel.getElectrodeType());
            if (!channel.isEnabled()) {
                enableAdsChannel(i, false);
            }
        }

        accelerometerName.setText("Accelerometer");
        accelerometerEnable.setSelected(bdfHeaderData.getAdsConfiguration().isAccelerometerEnabled());
        if (!bdfHeaderData.getAdsConfiguration().isAccelerometerEnabled()) {
            enableAccelerometer(false);
        }
        setChannelsFrequencies(bdfHeaderData.getAdsConfiguration().getSps());
    }

    public void updateLoffStatus(int loffStatusRegisterValue) {
        if ((loffStatusRegisterValue & 8) == 0) {
            channelLoffStatPositive[0].setIcon(iconConnected);
        } else {
            channelLoffStatPositive[0].setIcon(iconDisconnected);
        }
        if ((loffStatusRegisterValue & 16) == 0) {
            channelLoffStatNegative[0].setIcon(iconConnected);
        } else {
            channelLoffStatNegative[0].setIcon(iconDisconnected);
        }
        if ((loffStatusRegisterValue & 32) == 0) {
            channelLoffStatPositive[1].setIcon(iconConnected);
        } else {
            channelLoffStatPositive[1].setIcon(iconDisconnected);
        }
        if ((loffStatusRegisterValue & 64) == 0) {
            channelLoffStatNegative[1].setIcon(iconConnected);
        } else {
            channelLoffStatNegative[1].setIcon(iconDisconnected);
        }
    }

    private void saveDataToModel() {
        bdfHeaderData.getAdsConfiguration().setSps(getSps());
        bdfHeaderData.getAdsConfiguration().setComPortName(getComPortName());
        bdfHeaderData.setPatientIdentification(getPatientIdentification());
        bdfHeaderData.setRecordingIdentification(getRecordingIdentification());
        bdfHeaderData.getAdsChannelNames().clear();
        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            AdsChannelConfiguration channel = bdfHeaderData.getAdsConfiguration().getAdsChannels().get(i);
            bdfHeaderData.getAdsChannelNames().add(getChannelName(i));
            channel.setDivider(getChannelDivider(i));
            channel.setEnabled(isChannelEnable(i));
            channel.setLoffEnable(isChannelLoffEnable(i));
            channel.setRldSenseEnabled(isChannelDrlEnabled(i));
        }
        bdfHeaderData.getAdsConfiguration().setAccelerometerEnabled(isAccelerometerEnable());
        bdfHeaderData.getAdsConfiguration().setAccelerometerDivider(getAccelerometerDivider());
        bdfHeaderData.setFileNameToSave(new SimpleDateFormat("dd-MM-yyyy_HH-mm").format(new Date(System.currentTimeMillis())) + fileToSave.getText() + ".bdf");
    }

    private void setChannelsFrequencies(Sps sps) {
        int numberOfAdsChannels = bdfHeaderData.getAdsConfiguration().getAdsChannels().size();
        Integer[] channelsAvailableFrequencies = sps.getChannelsAvailableFrequencies();
        // set available frequencies
        for (int i = 0; i < numberOfAdsChannels; i++) {
            channelFrequency[i].removeAllItems();
            for (Integer frequency : channelsAvailableFrequencies) {
                channelFrequency[i].addItem(frequency);
            }
            // select channel frequency
            AdsChannelConfiguration channel = bdfHeaderData.getAdsConfiguration().getAdsChannels().get(i);
            Integer frequency = sps.getValue() / channel.getDivider().getValue();
            channelFrequency[i].setSelectedItem(frequency);
        }
        Integer[] accelerometerAvailableFrequencies = sps.getAccelerometerAvailableFrequencies();
        accelerometerFrequency.removeAllItems();
        for (Integer frequency : accelerometerAvailableFrequencies) {
            accelerometerFrequency.addItem(frequency);
        }
        // select channel frequency
        Integer frequency = sps.getValue() / bdfHeaderData.getAdsConfiguration().getAccelerometerDivider().getValue();
        accelerometerFrequency.setSelectedItem(frequency);
        if (numberOfAdsChannels > 0) {
            // put the size if field   accelerometerFrequency equal to the size of fields  channelFrequency
            accelerometerFrequency.setPreferredSize(channelFrequency[0].getPreferredSize());

        }
    }

    private void enableAdsChannel(int channelNumber, boolean isEnable) {
        channelFrequency[channelNumber].setEnabled(isEnable);
        channelName[channelNumber].setEnabled(isEnable);
        channelDrlEnabled[channelNumber].setEnabled(isEnable);
        channelLoffEnable[channelNumber].setEnabled(isEnable);
        // channelElectrodeType[channelNumber].setEnabled(isEnable);
    }


    private void enableAccelerometer(boolean isEnable) {
        accelerometerName.setEnabled(isEnable);
        accelerometerFrequency.setEnabled(isEnable);

    }


    private void showAdvanced(boolean isVisible) {
        channelsHeaders[channelsHeaders.length - 2].setVisible(isVisible);
        channelsHeaders[channelsHeaders.length - 3].setVisible(isVisible);
        for (int i = 0; i < bdfHeaderData.getAdsConfiguration().getAdsChannels().size(); i++) {
            // channelElectrodeType[i].setVisible(isVisible);
            channelDrlEnabled[i].setVisible(isVisible);
            channelLoffEnable[i].setVisible(isVisible);
        }
        pack();
    }

    private void setAdvanced() {
        if (isAdvanced) {
            advancedButton.setIcon(iconHide);
            showAdvanced(true);
            isAdvanced = false;
        } else {
            advancedButton.setIcon(iconShow);
            showAdvanced(false);
            isAdvanced = true;
        }
    }


    private Divider getChannelDivider(int channelNumber) {
        int divider = bdfHeaderData.getAdsConfiguration().getSps().getValue() / getChannelFrequency(channelNumber);
        return Divider.valueOf(divider);
    }

    private Divider getAccelerometerDivider() {
        int divider = bdfHeaderData.getAdsConfiguration().getSps().getValue() / getAccelerometerFrequency();
        return Divider.valueOf(divider);
    }


    private int getChannelFrequency(int channelNumber) {
        return (Integer) channelFrequency[channelNumber].getSelectedItem();
    }


    private boolean isChannelEnable(int channelNumber) {
        return channelEnable[channelNumber].isSelected();
    }

    private boolean isChannelLoffEnable(int channelNumber) {
        return channelLoffEnable[channelNumber].isSelected();
    }

    private boolean isChannelDrlEnabled(int channelNumber) {
        return channelDrlEnabled[channelNumber].isSelected();
    }

    private String getChannelName(int channelNumber) {
        return channelName[channelNumber].getText();
    }

    private String getComPortName() {
        return comPortName.getText();
    }

    private String getPatientIdentification() {
        return patientIdentification.getText();
    }

    private String getRecordingIdentification() {
        return recordingIdentification.getText();
    }

    private boolean isAccelerometerEnable() {
        return accelerometerEnable.isSelected();
    }

    private int getAccelerometerFrequency() {
        return (Integer) accelerometerFrequency.getSelectedItem();
    }

    private Sps getSps() {
        return (Sps) spsField.getSelectedItem();
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

    @Override
    public void onAdsDataReceived(final int[] dataFrame) {
        //update GUI every second
        adsDataFrameCounter++;
        if (adsDataFrameCounter % adsDataFrameFrequency  == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateLoffStatus(dataFrame[adsDataFrameSize - 1]);
                    setProcessReport("Recording... " + adsDataFrameCounter / adsDataFrameFrequency + " data records");
                }
            });
        }
    }


    private class AdsChannelEnableListener implements ActionListener {
        private int channelNumber;

        private AdsChannelEnableListener(int channelNumber) {
            this.channelNumber = channelNumber;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JCheckBox checkBox = (JCheckBox) actionEvent.getSource();
            if (checkBox.isSelected()) {
                enableAdsChannel(channelNumber, true);
            } else {
                enableAdsChannel(channelNumber, false);
            }
        }
    }
}