package com.crostec.ads;

import java.util.ArrayList;
import java.util.List;

class AdsConfigurator {

    private static final int BYTE_0_MARKER = 0x00;
    private static final int BYTE_1_MARKER = 0x10;
    private static final int BYTE_2_MARKER = 0x20;
    private static final int BYTE_3_MARKER = 0x30;
    private static final int START_PIN_HI_CODE = 0xF0;
    private static final int START_PIN_LO_CODE = 0xF1;
    private static final int WRITE_COMMAND_CODE = 0xF2;
    private static final int WRITE_REGISTER_CODE = 0xF3;
    private static final int SET_CHANEL_DIVIDER_CODE = 0xF4;
    private static final int SET_ACCELEROMETER_ENABLED_CODE = 0xF5;
    private static final int CONFIG_DATA_RECEIVED_CODE = 0xF6;

    public static final int NUMBER_OF_ADS_CHANNELS = 2;
    public static final int NUMBER_OF_ACCELEROMETER_CHANNELS = 3;

    private List<Byte> write(int code) {
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte) code);
        return result;
    }

    private List<Byte> write(int param, int code) {
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte) (BYTE_0_MARKER | (param >> 4)));
        result.add((byte) (BYTE_1_MARKER | (param & 0x0F)));
        result.add((byte) code);
        return result;
    }

    private List<Byte> write(int param1, int param2, int code) {
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte) (BYTE_0_MARKER | (param1 >> 4)));
        result.add((byte) (BYTE_1_MARKER | (param1 & 0x0F)));
        result.add((byte) (BYTE_2_MARKER | (param2 >> 4)));
        result.add((byte) (BYTE_3_MARKER | (param2 & 0x0F)));
        result.add((byte) code);
        return result;
    }

    public List<Byte> startPinHi() {
        return write(START_PIN_HI_CODE);
    }

    public List<Byte> startPinLo() {
        return write(START_PIN_LO_CODE);
    }

    public List<Byte> writeCommand(int command) {
        return write(command, WRITE_COMMAND_CODE);
    }

    public List<Byte> writeRegister(int address, int value) {
        return write(address, value, WRITE_REGISTER_CODE);
    }

    public List<Byte> writeDividerForChannel(int chanelNumber, int divider) {
        return write(chanelNumber, divider, SET_CHANEL_DIVIDER_CODE);
    }

    public List<Byte> writeAccelerometerEnabled(boolean isAccelerometerEnabled) {
        int isEnabled = isAccelerometerEnabled ? 1 : 0;
        return write(isEnabled, SET_ACCELEROMETER_ENABLED_CODE);
    }

    public List<Byte> writeConfigDataReceivedCode() {
        return write(CONFIG_DATA_RECEIVED_CODE);
    }

    public List<Byte> writeAdsConfiguration(AdsConfiguration adsConfiguration) {
        List<Byte> result = new ArrayList<Byte>();
        result.addAll(startPinLo());
        result.addAll(writeCommand(0x11));  //stop continious
        for (int i = 0; i < NUMBER_OF_ADS_CHANNELS; i++) {
            AdsChannelConfiguration adsChannelConfiguration = adsConfiguration.getAdsChannels().get(i);
            int divider = adsChannelConfiguration.isEnabled ? adsChannelConfiguration.getDivider().getValue() : 0;
            result.addAll(writeDividerForChannel(i, divider));
        }
        for (int i = NUMBER_OF_ADS_CHANNELS; i < NUMBER_OF_ACCELEROMETER_CHANNELS + NUMBER_OF_ADS_CHANNELS; i++) {
            int divider = adsConfiguration.isAccelerometerEnabled() ? adsConfiguration.getAccelerometerDivider().getValue() : 0;
            result.addAll(writeDividerForChannel(i,divider ));
        }
        result.addAll(writeAccelerometerEnabled(adsConfiguration.isAccelerometerEnabled()));
        int config1RegisterValue = adsConfiguration.getSps().getRegisterBits();
        result.addAll(writeRegister(0x41, config1RegisterValue));  //set SPS

        int config2RegisterValue = 0xA0 + loffComparatorEnabledBit(adsConfiguration) + testSignalEnabledBits(adsConfiguration);
        result.addAll(writeRegister(0x42, config2RegisterValue));

        result.addAll(writeRegister(0x43, 0x10));//Loff comparator threshold

        result.addAll(writeRegister(0x44, getChanelRegisterValue(adsConfiguration.getAdsChannels().get(0))));
        result.addAll(writeRegister(0x45, getChanelRegisterValue(adsConfiguration.getAdsChannels().get(1))));

        int rldSensRegisterValue = 0x20;
        if(adsConfiguration.getAdsChannels().get(0).isRldSenseEnabled()){
            rldSensRegisterValue += 0x03;
        }
        if(adsConfiguration.getAdsChannels().get(1).isRldSenseEnabled()){
            rldSensRegisterValue += 0x0C;
        }
        result.addAll(writeRegister(0x46, rldSensRegisterValue));

        int loffSensRegisterValue = 0;
         if(adsConfiguration.getAdsChannels().get(0).isLoffEnable()){
            loffSensRegisterValue += 0x03;
        }
        if(adsConfiguration.getAdsChannels().get(1).isLoffEnable()){
            loffSensRegisterValue += 0x0C;
        }
        result.addAll(writeRegister(0x47, loffSensRegisterValue));

        result.addAll(writeConfigDataReceivedCode());
        return result;
    }

    private int getChanelRegisterValue(AdsChannelConfiguration channelConfiguration) {
        int result = 0x80;   //channel disabled
        if (channelConfiguration.isEnabled()) {
            result = 0x00;
        }
        return result + channelConfiguration.getGain().getRegisterBits() + channelConfiguration.getCommutatorState().getRegisterBits();
    }

    private int loffComparatorEnabledBit(AdsConfiguration configuration) {
        int result = 0x00;
        for (AdsChannelConfiguration adsChannelConfiguration : configuration.getAdsChannels()) {
            if (adsChannelConfiguration.isLoffEnable()) {
                result = 0x40;
            }
        }
        return result;
    }

    private int testSignalEnabledBits(AdsConfiguration configuration) {
        int result = 0x00;
        for (AdsChannelConfiguration adsChannelConfiguration : configuration.getAdsChannels()) {
            if (adsChannelConfiguration.getCommutatorState().equals(CommutatorState.TEST_SIGNAL)) {
                result = 0x03;
            }
        }
        return result;
    }

}
