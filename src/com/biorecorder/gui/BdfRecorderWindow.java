package com.biorecorder.gui;

import com.biorecorder.*;
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
public class BdfRecorderWindow extends JFrame  {

    private BdfRecorderApp bdfRecorder;
    private AppConfig recordingSettings;

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
    private JTextField accelerometerName;
    private JCheckBox accelerometerEnable;
    private JTextField patientIdentification;
    private JTextField recordingIdentification;

    private String start = "Start";
    private String stop = "Stop";
    private JButton startButton;
    private JButton stopButton;

    private String comPortLabel = "ComPort:";
    private JComboBox comport;

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


    public BdfRecorderWindow(BdfRecorderApp bdfRecorder, AppConfig recordingSettings) {
        this.recordingSettings = recordingSettings;
        this.bdfRecorder = bdfRecorder;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                saveDataToModel();
                bdfRecorder.closeApplication(recordingSettings);
            }
        });
        init();
        pack();
        // place the window to the screen center
        setLocationRelativeTo(null);
        setVisible(true);
        selectComport();
    }


    private void init() {
        createFields();
        arrangeForm();
        loadDataFromModel();
        setActions();
    }

    private void createFields() {
        int numberOfAdsChannels = recordingSettings.getNumberOfAdsChannels();

        startButton = new JButton(start);
        stopButton = new JButton(stop);
        stopButton.setVisible(false);

        spsField = new JComboBox();
        comport = new JComboBox();
        deviceTypeField = new JComboBox();
        fileToSaveUI = new FileToSaveUI();

        int textFieldLength = 30;
        patientIdentification = new JTextField(textFieldLength);
        patientIdentification.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));
        recordingIdentification = new JTextField(textFieldLength);
        recordingIdentification.setDocument(new FixSizeDocument(IDENTIFICATION_LENGTH));

        channelFrequency = new JComboBox[numberOfAdsChannels];
        channelGain = new JComboBox[numberOfAdsChannels];
        channelCommutatorState = new JComboBox[numberOfAdsChannels];
        channelEnable = new JCheckBox[numberOfAdsChannels];
        channel50Hz = new JCheckBox[numberOfAdsChannels];
        channelName = new JTextField[numberOfAdsChannels];
        channelLoffStatPositive = new MarkerLabel[numberOfAdsChannels];
        channelLoffStatNegative = new MarkerLabel[numberOfAdsChannels];
        channelLoffEnable = new JCheckBox[numberOfAdsChannels];
        textFieldLength = CHANNEL_NAME_LENGTH;
        for (int i = 0; i < numberOfAdsChannels; i++) {
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
        accelerometerName = new JTextField("Accelerometer");
        accelerometerEnable = new JCheckBox();
        accelerometerFrequency = new JComboBox();
        accelerometerCommutator = new JComboBox();
        accelerometerName.setPreferredSize(channelName[0].getPreferredSize());
        accelerometerName.setEnabled(false);
    }


    private void setActions() {

        deviceTypeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recordingSettings.setDeviceType(getDeviceType());
                bdfRecorder.changeRecorder();
                init();
                pack();
            }
        });

        bdfRecorder.setMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                JOptionPane.showMessageDialog(BdfRecorderWindow.this, message);

            }
            @Override
            public boolean onConfirmationAsked(String message) {
                int answer = JOptionPane.showConfirmDialog(BdfRecorderWindow.this, message, null, JOptionPane.YES_NO_OPTION);
                if(answer == JOptionPane.YES_OPTION) {
                    return true;
                }
                return false;
            }
        });

        bdfRecorder.setNotificationListener(new NotificationListener() {
            @Override
            public void update() {
                updateLeadOffStatus(bdfRecorder.getLeadOfDetectionMask());
                Color activeColor = Color.GREEN;
                Color nonActiveColor = Color.GRAY;
                Color stateColor = nonActiveColor;
                if(bdfRecorder.isActive()) {
                    stateColor = activeColor;
                }
                if(bdfRecorder.isRecording()) {
                    stateColor = activeColor;
                    stopButton.setVisible(true);
                    startButton.setVisible(false);
                    disableFields();
                } else {
                    stopButton.setVisible(false);
                    startButton.setVisible(true);
                    enableFields();
                }
                setReport(bdfRecorder.getStateReport(), stateColor);
            }
        });
        // init available comport list every time we "open" JComboBox (mouse over «arrow button»)
       comport.addPopupMenuListener(new PopupMenuListener() {
           @Override
           public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
               comport.setModel(new DefaultComboBoxModel(bdfRecorder.getComportNames()));
               BdfRecorderWindow.this.pack();
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
                comport.setModel(new DefaultComboBoxModel(bdfRecorder.getAvailableComports()));
                BdfRecorderWindow.this.pack();
              }
        });*/

        comport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bdfRecorder.connect(getComPortName());
            }
        });


        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
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
                int sps = (Integer) comboBox.getSelectedItem();
                setChannelsFrequencies(sps);
            }
        });


        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveDataToModel();
                bdfRecorder.startRecording(recordingSettings);
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                bdfRecorder.stopRecording();
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

    }


    private void arrangeForm() {
        setTitle(title);
        getContentPane().removeAll();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        stopButton.setPreferredSize(startButton.getPreferredSize());
        buttonPanel.add(stopButton);


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

        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
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
        channelsPanel.add(new JLabel(" " + (1 + recordingSettings.getNumberOfAdsChannels()) + " "));
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

    private void disableEnableFields(boolean isEnable) {
        spsField.setEnabled(isEnable);
        patientIdentification.setEnabled(isEnable);
        recordingIdentification.setEnabled(isEnable);
        fileToSaveUI.setEnabled(isEnable);
        comport.setEnabled(isEnable);
        accelerometerEnable.setEnabled(isEnable);
        accelerometerFrequency.setEnabled(isEnable);
        accelerometerCommutator.setEnabled(isEnable);

        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            channelEnable[i].setEnabled(isEnable);
            channel50Hz[i].setEnabled(isEnable);
            channelName[i].setEnabled(isEnable);
            channelFrequency[i].setEnabled(isEnable);
            channelGain[i].setEnabled(isEnable);
            channelCommutatorState[i].setEnabled(isEnable);
            channelLoffEnable[i].setEnabled(isEnable);
        }
    }

    private void selectComport() {
        String comportName = recordingSettings.getComportName();
        if(comportName != null && !comportName.isEmpty()) {
            comport.setSelectedItem(comportName);
        } else if(comport.getItemCount() > 0) {
            comport.setSelectedIndex(0);
        }
    }


    private void disableFields() {
        boolean isEnable = false;
        disableEnableFields(isEnable);


    }


    private void enableFields() {
        boolean isEnable = true;
        disableEnableFields(isEnable);
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            if (!isChannelEnable(i)) {
                enableAdsChannel(i, false);
            }
        }
        if (!recordingSettings.isAccelerometerEnabled()) {
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
        comport.setModel(new DefaultComboBoxModel(bdfRecorder.getComportNames()));
        selectComport();
        spsField.setModel(new DefaultComboBoxModel(recordingSettings.getAvailableSampleRates()));
        spsField.setSelectedItem(recordingSettings.getSampleRate());
        deviceTypeField.setModel(new DefaultComboBoxModel(recordingSettings.getAvailableDeviceTypes()));
        deviceTypeField.setSelectedItem(recordingSettings.getDeviceType());
        fileToSaveUI.setDirectory(recordingSettings.getDirToSave());
        patientIdentification.setText(recordingSettings.getPatientIdentification());
        recordingIdentification.setText(recordingSettings.getRecordingIdentification());
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            channelName[i].setText(recordingSettings.getAdsChannelName(i));
            channelEnable[i].setSelected(recordingSettings.isAdsChannelEnabled(i));
            if (!recordingSettings.isAdsChannelEnabled(i)) {
                enableAdsChannel(i, false);
            }
            channel50Hz[i].setSelected(recordingSettings.is50HzFilterEnabled(i));
            channelLoffEnable[i].setSelected(recordingSettings.isAdsChannelLeadOffEnable(i));
        }

        accelerometerEnable.setSelected(recordingSettings.isAccelerometerEnabled());
        if (!recordingSettings.isAccelerometerEnabled()) {
            enableAccelerometer(false);
        }
        setChannelsFrequencies(recordingSettings.getSampleRate());
        setChannelsGain();
        setChannelsCommutatorState();
        setAccelerometerCommutator();
    }


    private AppConfig saveDataToModel() {
        recordingSettings.setDeviceType(getDeviceType());
        recordingSettings.setComportName(getComPortName());
        recordingSettings.setPatientIdentification(getPatientIdentification());
        recordingSettings.setRecordingIdentification(getRecordingIdentification());
        int[] adsChannelsFrequencies = new int[recordingSettings.getNumberOfAdsChannels()];
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            recordingSettings.setAdsChannelName(i, getChannelName(i));
            recordingSettings.setAdsChannelEnabled(i, isChannelEnable(i));
            recordingSettings.setIs50HzFilterEnabled(i, is50HzFilterEnable(i));
            recordingSettings.setAdsChannelGain(i, getChannelGain(i));
            recordingSettings.setAdsChannelCommutatorState(i, getChannelCommutatorState(i));
            recordingSettings.setAdsChannelLeadOffEnable(i, isChannelLoffEnable(i));
            adsChannelsFrequencies[i] = getChannelFrequency(i);
        }
        recordingSettings.setFrequencies(getSampleRate(), getAccelerometerFrequency(), adsChannelsFrequencies);
        recordingSettings.setAccelerometerEnabled(isAccelerometerEnable());
        recordingSettings.setAccelerometerOneChannelMode(getAccelerometerCommutator());
        recordingSettings.setFileName(getFilename());
        recordingSettings.setDirToSave(getDirectory());
        return recordingSettings;
    }


   private boolean isChannelLoffEnable(int i) {
      return channelLoffEnable[i].isSelected();
   }

   private String getDeviceType() {
        return (String) deviceTypeField.getSelectedItem();
   }

    private String getDirectory() {
       return fileToSaveUI.getDirectory();
    }
    private String getFilename() {
        return fileToSaveUI.getFilename();
    }

    private void setChannelsFrequencies(int sampleRate) {
        Integer[] adsChannelsAvilableFrequencies = recordingSettings.getAdsChannelsAvailableFrequencies(sampleRate);
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            channelFrequency[i].setModel(new DefaultComboBoxModel(adsChannelsAvilableFrequencies));
            channelFrequency[i].setSelectedItem(recordingSettings.getAdsChannelFrequency(i));
        }

        accelerometerFrequency.setModel(new DefaultComboBoxModel(recordingSettings.getAccelerometerAvailableFrequencies(sampleRate)));
        accelerometerFrequency.setSelectedItem(recordingSettings.getAccelerometerFrequency());

        // put the size if field   accelerometerFrequency equal to the size of fields  channelFrequency
        accelerometerFrequency.setPreferredSize(channelFrequency[0].getPreferredSize());
    }

    private void updateLeadOffStatus(Boolean[] leadOffDetectionMask) {
         for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
             if (leadOffDetectionMask[2 * i] == null) {
                 channelLoffStatPositive[i].setIcon(iconDisabled);
             } else if (leadOffDetectionMask[2 * i] == true) {
                 channelLoffStatPositive[i].setIcon(iconDisconnected);
             }else if (leadOffDetectionMask[2 * i] == false) {
                 channelLoffStatPositive[i].setIcon(iconConnected);
             }

             if (leadOffDetectionMask[2 * i + 1] == null) {
                 channelLoffStatNegative[i].setIcon(iconDisabled);
             } else if (leadOffDetectionMask[2 * i +1] == true) {
                 channelLoffStatNegative[i].setIcon(iconDisconnected);
             }else if (leadOffDetectionMask[2 * i + 1] == false) {
                 channelLoffStatNegative[i].setIcon(iconConnected);
             }
         }
    }

    private void setChannelsGain(){
        Integer[] availableGains = recordingSettings.getAvailableGaines();
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            channelGain[i].setModel(new DefaultComboBoxModel(availableGains));
            channelGain[i].setSelectedItem(recordingSettings.getAdsChannelGain(i));
        }
    }

    private void setChannelsCommutatorState(){
        String[] availableStates = recordingSettings.getAvailableCommutatorStates();
        for (int i = 0; i < recordingSettings.getNumberOfAdsChannels(); i++) {
            channelCommutatorState[i].setModel(new DefaultComboBoxModel(availableStates));
            channelCommutatorState[i].setSelectedItem(recordingSettings.getAdsChannelCommutatorState(i));
        }
    }

    private void setAccelerometerCommutator(){
        accelerometerCommutator.addItem("1 Channel");
        accelerometerCommutator.addItem("3 Channels");
        if(recordingSettings.isAccelerometerOneChannelMode()){
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


    private boolean getAccelerometerCommutator(){
        return (accelerometerCommutator.getSelectedIndex() == 0)? true :false;
    }


    private int getChannelFrequency(int channelNumber) {
        return (Integer) channelFrequency[channelNumber].getSelectedItem();
    }

    private int getChannelGain(int channelNumber) {
        return (Integer)channelGain[channelNumber].getSelectedItem();
    }

    private String getChannelCommutatorState(int channelNumber) {
        return (String) channelCommutatorState[channelNumber].getSelectedItem();
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

    private int getSampleRate() {
        return (Integer)spsField.getSelectedItem();
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