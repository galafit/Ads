package com.biorecorder.gui;

import com.biorecorder.ads.*;
import com.biorecorder.ads.exceptions.AdsConnectionRuntimeException;
import com.biorecorder.bdfrecorder.BdfRecorder;
import com.biorecorder.bdfrecorder.BdfRecorderConfig;
import com.biorecorder.bdfrecorder.NotificationListener;
import com.biorecorder.bdfrecorder.exceptions.UserInfoRuntimeException;
import com.biorecorder.gui.file_gui.FileToSaveUI;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;


/**
 *
 */
public class SettingsWindow extends JFrame  {

    private BdfRecorder bdfRecorder;

    private String patientIdentificationLabel = "Patient";
    private String recordingIdentificationLabel = "Record";
    private String spsLabel = "Maximum Frequency (Hz)";
    private JComboBox spsField;
    private JComboBox[] channelFrequency;
    private JComboBox[] channelGain;
    private JComboBox[] channelCommutatorState;
    private JCheckBox[] channelEnable;
    private JCheckBox[] channel50Hz;
    private JTextField[] channelName;
    private JComboBox accelerometerCommutator;
    private int CHANNEL_NAME_LENGTH = 16;
    private int IDENTIFICATION_LENGTH = 80;

    private JComboBox accelerometerFrequency;
    private JTextField accelerometerName = new JTextField("Accelerometer");
    private JCheckBox accelerometerEnable;
    private JTextField patientIdentification;
    private JTextField recordingIdentification;

    private String start = "Start";
    private String stop = "Stop";
    private JButton startButton = new JButton(start);
    private JButton stopButton = new JButton(stop);

    private String comPortLabel = "ComPort:";
    private ButtonComboBox comport;

    private String deviceTypeLabel = "Device:";
    private JComboBox deviceTypeField;

    private FileToSaveUI fileToSaveUI;

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
    private JCheckBox[] channelLoffEnable;
    private String title = "EDF Recorder";
    private JComponent[] channelsHeaders = {new JLabel("Number"), new JLabel("Enable"), new JLabel("Name"), new JLabel("Frequency (Hz)"),
            new JLabel("Gain"), new JLabel("Commutator State"), new JLabel("Lead Off Detection"), new JLabel(" "),new JLabel("50 Hz Filter")};


    public SettingsWindow(BdfRecorder bdfRecorder) {
        this.bdfRecorder = bdfRecorder;
        stopButton.setVisible(false);
        init();
        setVisible(true);
        try {
            String comportName = bdfRecorder.getBdfRecorderConfig().getAdsConfig().getComPortName();
            if(comportName != null && !comportName.isEmpty()) {
               // bdfRecorder.connect();
            }
        } catch (UserInfoRuntimeException e) {
            JOptionPane.showMessageDialog(SettingsWindow.this, e.getMessage());
        }
    }


    private void init() {
        createFields();
        arrangeForm();
        loadDataFromModel();
        setActions();
    }

    private void createFields() {
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int adsChannelsNumber = adsConfig.getNumberOfAdsChannels();

        spsField = new JComboBox(Sps.values());
        comport = new ButtonComboBox(bdfRecorder.getComportNames());
        deviceTypeField = new JComboBox(DeviceType.values());
        fileToSaveUI = new FileToSaveUI();

        int textFieldLength = 30;
        patientIdentification = new JTextField(textFieldLength);
        patientIdentification.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        recordingIdentification = new JTextField(textFieldLength);
        recordingIdentification.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));

        channelFrequency = new JComboBox[adsChannelsNumber];
        channelGain = new JComboBox[adsChannelsNumber];
        channelCommutatorState = new JComboBox[adsChannelsNumber];
        channelEnable = new JCheckBox[adsChannelsNumber];
        channel50Hz = new JCheckBox[adsChannelsNumber];
        channelName = new JTextField[adsChannelsNumber];
        channelLoffStatPositive = new MarkerLabel[adsChannelsNumber];
        channelLoffStatNegative = new MarkerLabel[adsChannelsNumber];
        channelLoffEnable = new JCheckBox[adsChannelsNumber];
        textFieldLength = CHANNEL_NAME_LENGTH;
        for (int i = 0; i < adsChannelsNumber; i++) {
            channelFrequency[i] = new JComboBox();
            channelGain[i] = new JComboBox();
            channelCommutatorState[i] = new JComboBox();
            channelEnable[i] = new JCheckBox();
            channel50Hz[i] = new JCheckBox();
            channelName[i] = new JTextField(textFieldLength);
            channelName[i].setDocument(new FixSizeDocument(CHANNEL_NAME_LENGTH));
            channelLoffStatPositive[i] = new MarkerLabel(iconDisabled);
            channelLoffStatNegative[i] = new MarkerLabel(iconDisabled);
            channelLoffEnable[i] = new JCheckBox();
        }
        accelerometerEnable = new JCheckBox();
        accelerometerFrequency = new JComboBox();
        accelerometerCommutator = new JComboBox();
        accelerometerName.setPreferredSize(channelName[0].getPreferredSize());
        accelerometerName.setEnabled(false);
    }




    private void setActions() {
        final AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();

        deviceTypeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adsConfig.setDeviceType(getDeviceType());
                init();
            }
        });

        bdfRecorder.addNotificationListener(new NotificationListener() {
            @Override
            public void update() {
                int recordsNumber = bdfRecorder.getNumberOfWrittenDataRecords();
                if(recordsNumber > 0) {
                    setProcessReport("Recording... " + recordsNumber + " data records");
                }
            }
        });
        // init available comport list every time we "open" JComboBox (mouse over «arrow button»)
       comport.addPopupMenuListener(new PopupMenuListener() {
           @Override
           public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
               comport.setModel(new DefaultComboBoxModel(bdfRecorder.getComportNames()));
               SettingsWindow.this.pack();
           }

           @Override
           public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

           }

           @Override
           public void popupMenuCanceled(PopupMenuEvent e) {

           }
       });
       // another way to do the same
        /*JButton comportButton = comport.getButton();
        comportButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                comport.setModel(new DefaultComboBoxModel(bdfRecorder.getComportNames()));
                SettingsWindow.this.pack();
              }
        });*/


        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
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
                BdfRecorderConfig bdfRecorderConfig = saveDataToModel();
                bdfRecorder.setBdfRecorderConfig(bdfRecorderConfig);
                try {
                    bdfRecorder.startRecording();
                    stopButton.setVisible(true);
                    startButton.setVisible(false);
                    setProcessReport("Connecting...");
                    disableFields();
                } catch (UserInfoRuntimeException e) {
                    JOptionPane.showMessageDialog(SettingsWindow.this, e.getMessage());
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                bdfRecorder.stopRecording();
                stopButton.setVisible(false);
                startButton.setVisible(true);
                enableFields();
                setProcessReport("Saved to file: " + bdfRecorder.getSavedFile());
            }
        });


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
                BdfRecorderConfig bdfRecorderConfig = saveDataToModel();
                bdfRecorder.closeApplication(bdfRecorderConfig);
            }
        });
    }


    private void arrangeForm() {
        setTitle(title);
        getContentPane().removeAll();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        stopButton.setPreferredSize(startButton.getPreferredSize());
        buttonPanel.add(stopButton);

        JButton testButton = new JButton("test");
        buttonPanel.add(testButton);
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDataToModel();
               bdfRecorder.test();
            }
        });

        int hgap = 5;
        int vgap = 0;
        JPanel spsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        spsPanel.add(new JLabel(spsLabel));
        spsPanel.add(spsField);


        hgap = 5;
        vgap = 0;
        JPanel comportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        comportPanel.add(new JLabel(comPortLabel));
        comportPanel.add(comport);

        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        devicePanel.add(new JLabel(deviceTypeLabel));
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

        for (JComponent component : channelsHeaders) {
            channelsPanel.add(component);
        }

        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            channelsPanel.add(new JLabel(" " + (i + 1) + " "));
            channelsPanel.add(channelEnable[i]);
            channelsPanel.add(channelName[i]);
            channelsPanel.add(channelFrequency[i]);
            channelsPanel.add(channelGain[i]);
            channelsPanel.add(channelCommutatorState[i]);
            JPanel loffPanel = new JPanel();
            loffPanel.add(channelLoffEnable[i]);
            loffPanel.add(channelLoffStatPositive[i]);
            loffPanel.add(channelLoffStatNegative[i]);
            channelsPanel.add(loffPanel);
            channelsPanel.add(new JLabel(" "));
                        channelsPanel.add(channel50Hz[i]);
        }

        // Add line of accelerometer
        channelsPanel.add(new JLabel(" " + (1 + adsConfig.getNumberOfAdsChannels()) + " "));
        channelsPanel.add(accelerometerEnable);
        channelsPanel.add(accelerometerName);
        channelsPanel.add(accelerometerFrequency);
        /*JComboBox accGain = new JComboBox();
        channelsPanel.add(accGain);
        accGain.addItem("1");
        accGain.setSelectedIndex(0);
        accGain.setPreferredSize(channelGain[0].getPreferredSize());
        accGain.setEditable(false);*/
        channelsPanel.add(new JLabel(" "));

        channelsPanel.add(accelerometerCommutator);

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
//        identificationPanel.add(new Label("    "));
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

        // Root Panel of the SettingsWindow
        add(topPanel, BorderLayout.NORTH);
        add(adsPanel, BorderLayout.CENTER);
        add(reportPanel, BorderLayout.SOUTH);

        // set the same size for identificationPanel and  saveAsPanel
        int height = Math.max(identificationBorderPanel.getPreferredSize().height, fileToSaveUI.getPreferredSize().height);
        int width = Math.max(identificationBorderPanel.getPreferredSize().width, fileToSaveUI.getPreferredSize().width);
        fileToSaveUI.setPreferredSize(new Dimension(width, height));
        identificationBorderPanel.setPreferredSize(new Dimension(width, height));


        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
    }

    private void disableEnableFields(boolean isEnable) {
        spsField.setEnabled(isEnable);
        patientIdentification.setEnabled(isEnable);
        recordingIdentification.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comport.setEnabled(isEnable);
        accelerometerEnable.setEnabled(isEnable);
        accelerometerFrequency.setEnabled(isEnable);
        accelerometerCommutator.setEnabled(isEnable);

        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            channelEnable[i].setEnabled(isEnable);
            channel50Hz[i].setEnabled(isEnable);
            channelName[i].setEnabled(isEnable);
            channelFrequency[i].setEnabled(isEnable);
            channelGain[i].setEnabled(isEnable);
            channelCommutatorState[i].setEnabled(isEnable);
            channelLoffEnable[i].setEnabled(isEnable);
        }
    }


    private void disableFields() {
        boolean isEnable = false;
        disableEnableFields(isEnable);


    }


    private void enableFields() {
        boolean isEnable = true;
        disableEnableFields(isEnable);
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            if (!isChannelEnable(i)) {
                enableAdsChannel(i, false);
            }
        }
        if (!adsConfig.isAccelerometerEnabled()) {
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

    private void loadDataFromModel() {
        BdfRecorderConfig bdfRecorderConfig = bdfRecorder.getBdfRecorderConfig();
        AdsConfig adsConfig = bdfRecorderConfig.getAdsConfig();
        spsField.setSelectedItem(adsConfig.getSps());
        System.out.println("data");
        deviceTypeField.setSelectedItem(adsConfig.getDeviceType());
        fileToSaveUI.setDirectory(bdfRecorderConfig.getDirToSave().getName());
//        fileToSave.setText(FILENAME_PATTERN);
        patientIdentification.setText(bdfRecorderConfig.getPatientIdentification());
        recordingIdentification.setText(bdfRecorderConfig.getRecordingIdentification());
        int numberOfAdsChannels = adsConfig.getNumberOfAdsChannels();
        for (int i = 0; i < numberOfAdsChannels; i++) {
            AdsChannelConfig channel = adsConfig.getAdsChannel(i);
            channelName[i].setText(channel.getName());
            channelEnable[i].setSelected(channel.isEnabled());
            if (!channel.isEnabled()) {
                enableAdsChannel(i, false);
            }
            channel50Hz[i].setSelected(bdfRecorderConfig.is50HzFilterEnabled(i));
            channelLoffEnable[i].setSelected(channel.isLoffEnable());
        }

        accelerometerEnable.setSelected(adsConfig.isAccelerometerEnabled());
        if (!adsConfig.isAccelerometerEnabled()) {
            enableAccelerometer(false);
        }
        setChannelsFrequencies(adsConfig.getSps());
        setChannelsGain();
        setChannelsCommutatorState();
        setAccelerometerCommutator();
    }


    private BdfRecorderConfig saveDataToModel() {
        BdfRecorderConfig bdfRecorderConfig = bdfRecorder.getBdfRecorderConfig();
        AdsConfig adsConfig = bdfRecorderConfig.getAdsConfig();
        adsConfig.setSps(getSps());
        adsConfig.setDeviceType(getDeviceType());
        adsConfig.setComPortName(getComPortName());
        bdfRecorderConfig.setPatientIdentification(getPatientIdentification());
        bdfRecorderConfig.setRecordingIdentification(getRecordingIdentification());
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            AdsChannelConfig channel = adsConfig.getAdsChannel(i);
            channel.setName(getChannelName(i));
            channel.setDivider(getChannelDivider(i));
            channel.setEnabled(isChannelEnable(i));
            bdfRecorderConfig.setIs50HzFilterEnabled(i, is50HzFilterEnable(i));
            channel.setGain(getChannelGain(i));
            channel.setCommutatorState(getChannelCommutatorState(i));
            channel.setLoffEnable(channelLoffEnable[i].isSelected());
        }
        adsConfig.setAccelerometerEnabled(isAccelerometerEnable());
        adsConfig.setAccelerometerDivider(getAccelerometerDivider());
        adsConfig.setAccelerometerOneChannelMode(getAccelerometerCommutator());
        bdfRecorderConfig.setFileNameToSave(getFilename());
        bdfRecorderConfig.setDirectoryToSave(new File(getDirectory()));
        return bdfRecorderConfig;
    }

   private DeviceType getDeviceType() {
        return (DeviceType) deviceTypeField.getSelectedItem();
   }

    private String getDirectory() {
       return fileToSaveUI.getDirectory();
    }
    private String getFilename() {
        return fileToSaveUI.getFilename();
    }

    private void setChannelsFrequencies(Sps sps) {
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int numberOfAdsChannels = adsConfig.getNumberOfAdsChannels();
        Divider[] adsChannelsDividers = adsConfig.getChannelsAvailableDividers();
        // set available frequencies
        for (int i = 0; i < numberOfAdsChannels; i++) {
            channelFrequency[i].removeAllItems();
            for (Divider divider : adsChannelsDividers) {
                channelFrequency[i].addItem(sps.getValue()/divider.getValue());
            }
            // select channel frequency
            AdsChannelConfig channel = adsConfig.getAdsChannel(i);
            Integer frequency = sps.getValue() / channel.getDivider().getValue();
            channelFrequency[i].setSelectedItem(frequency);
        }
        Divider[] accelerometerAvailableDividers = adsConfig.getGetAccelerometerAvailableDividers();
        accelerometerFrequency.removeAllItems();
        for (Divider divider : accelerometerAvailableDividers) {
            accelerometerFrequency.addItem(sps.getValue()/divider.getValue());
        }
        // select channel frequency
        Integer frequency = sps.getValue() / adsConfig.getAccelerometerDivider().getValue();
        accelerometerFrequency.setSelectedItem(frequency);
        if (numberOfAdsChannels > 0) {
            // put the size if field   accelerometerFrequency equal to the size of fields  channelFrequency
            accelerometerFrequency.setPreferredSize(channelFrequency[0].getPreferredSize());
        }
    }

    private void setChannelsGain(){
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int numberOfAdsChannels = adsConfig.getNumberOfAdsChannels();
        for (int i = 0; i < numberOfAdsChannels; i++) {
            channelGain[i].removeAllItems();
            for (Gain gain : Gain.values()) {
                channelGain[i].addItem(gain.getValue());
            }
            AdsChannelConfig channel = adsConfig.getAdsChannel(i);
            channelGain[i].setSelectedItem(channel.getGain().getValue());
        }
    }

    private void setChannelsCommutatorState(){
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int numberOfAdsChannels = adsConfig.getNumberOfAdsChannels();
        for (int i = 0; i < numberOfAdsChannels; i++) {
            channelCommutatorState[i].removeAllItems();
            for (CommutatorState commutatorState : CommutatorState.values()) {
                channelCommutatorState[i].addItem(commutatorState.toString());
            }
            AdsChannelConfig channel = adsConfig.getAdsChannel(i);
            channelCommutatorState[i].setSelectedItem(channel.getCommutatorState().toString());
        }
    }

    private void setAccelerometerCommutator(){
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        accelerometerCommutator.addItem("1 Channel");
        accelerometerCommutator.addItem("3 Channels");
        if(adsConfig.isAccelerometerOneChannelMode()){
            accelerometerCommutator.setSelectedIndex(0);
        }else {
            accelerometerCommutator.setSelectedIndex(1);
        }
        accelerometerCommutator.setPreferredSize(channelCommutatorState[0].getPreferredSize());
    }

    private void enableAdsChannel(int channelNumber, boolean isEnable) {
        channel50Hz[channelNumber].setEnabled(isEnable);
        channelFrequency[channelNumber].setEnabled(isEnable);
        channelGain[channelNumber].setEnabled(isEnable);
        channelCommutatorState[channelNumber].setEnabled(isEnable);
        channelName[channelNumber].setEnabled(isEnable);
        channelLoffEnable[channelNumber].setEnabled(isEnable);
        channelLoffStatPositive[channelNumber].setIcon(iconDisabled);
        channelLoffStatNegative[channelNumber].setIcon(iconDisabled);
    }


    private void enableAccelerometer(boolean isEnable) {
        accelerometerFrequency.setEnabled(isEnable);
        accelerometerCommutator.setEnabled(isEnable);
    }

    private Divider getChannelDivider(int channelNumber) {
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int divider = adsConfig.getSps().getValue() / getChannelFrequency(channelNumber);
        return Divider.valueOf(divider);
    }

    private Divider getAccelerometerDivider() {
        AdsConfig adsConfig = bdfRecorder.getBdfRecorderConfig().getAdsConfig();
        int divider = adsConfig.getSps().getValue() / getAccelerometerFrequency();
        return Divider.valueOf(divider);
    }

    private boolean getAccelerometerCommutator(){
        return (accelerometerCommutator.getSelectedIndex() == 0)? true :false;
    }


    private int getChannelFrequency(int channelNumber) {
        return (Integer) channelFrequency[channelNumber].getSelectedItem();
    }

    private Gain getChannelGain(int channelNumber) {
        return Gain.valueOf(((Integer)channelGain[channelNumber].getSelectedItem()));
    }

    private CommutatorState getChannelCommutatorState(int channelNumber) {
        return CommutatorState.valueOf(((String) channelCommutatorState[channelNumber].getSelectedItem()));
    }

    private boolean isChannelEnable(int channelNumber) {
        return channelEnable[channelNumber].isSelected();
    }

    private boolean is50HzFilterEnable(int channelNumber) {
        return channel50Hz[channelNumber].isSelected();
    }

    private String getChannelName(int channelNumber) {
        return channelName[channelNumber].getText();
    }

    private String getComPortName() {
        return (String) comport.getSelectedItem();
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