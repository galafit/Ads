package com.biorecorder.ads;

import java.util.ArrayList;
import java.util.List;

public class AdsConfigurator8Ch implements AdsConfigurator {
    public static final int NUMBER_OF_ADS_CHANNELS = 8;

    @Override
    public byte[] getAdsConfigurationCommand(AdsConfig adsConfig) {
        //-----------------------------------------
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte)51);       //длина пакета

        result.add((byte)0xF0);     //ads1292 command
        result.add((byte)0x11);     //ads1292 stop continuous

        result.add((byte)0xF1);     //запись регистров ads1298
        result.add((byte)0x01);     //адрес первого регистра
        result.add((byte)0x17);     //количество регистров

        result.add((byte) getRegister_1Value(adsConfig));  //register 0x01   set SPS
        result.add((byte)testSignalEnabledBits(adsConfig)); //register 0x02   example signal
        result.add((byte)0xCC);      //register 0x03
        boolean isLoffEnabled = adsConfig.isLeadOffEnabled();
        result.add((byte)(isLoffEnabled? 0x13 : 0x00));  //register 0x04
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            result.add((byte) getChanelRegisterValue(adsConfig, i)); //registers 0x05 - 0x0C
        }
        int rlsSensBits = getRLDSensBits(adsConfig);
        result.add((byte)rlsSensBits);  //RLD sens positive              register 0x0D
        result.add((byte)rlsSensBits);  //RLD sens negative              register 0x0E

        int loffSensBits = getLoffSensRegisterValue(adsConfig);
        result.add((byte)loffSensBits); //loff sens positive             //register 0x0F
        result.add((byte)loffSensBits); //loff sens negative             //register 0x10
        result.add((byte)0x00);                                          //register 0x11
        result.add((byte)0x00);                                          //register 0x12
        result.add((byte)0x00);                                          //register 0x13
        result.add((byte)0x0F);                                          //register 0x14
        result.add((byte)0x00);                                          //register 0x15
        result.add((byte)0x20);                                          //register 0x16
        result.add((byte)(isLoffEnabled? 0x02 : 0x00));                  //register 0x17

        result.add((byte)0xF2);     //делители частоты для 8 каналов ads1298  возможные значения 0,1,2,5,10;
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            int divider = adsConfig.isAdsChannelEnabled(i) ? adsConfig.getAdsChannelDivider(i) : 0;
            result.add((byte)divider);
        }

        result.add((byte)0xF3);     //accelerometer mode: 0 - disabled, 1 - enabled
        int accelerometerMode = adsConfig.isAccelerometerEnabled() ? 1 : 0;
        result.add((byte)accelerometerMode);

        result.add((byte)0xF4);     //send battery voltage data: 0 - disabled, 1 - enabled
        int batteryMeasure = adsConfig.isBatteryVoltageMeasureEnabled() ? 1 : 0;
        result.add((byte)batteryMeasure);

        result.add((byte)0xF5);     //передача данных loff статуса: 0 - disabled, 1 - enabled
        result.add((byte)(isLoffEnabled ? 1 : 0));

        result.add((byte)0xF6);     //reset timeout. In seconds
        result.add((byte)20);

        result.add((byte)0xF0);     //ads1292 command
        result.add((byte)0x10);     //ads1292 start continuous

        result.add((byte)0xFE);     //start recording

        result.add((byte)0x55);     //footer1
        result.add((byte)0x55);     //footer1
       /* for (int i = 0; i < result.size(); i++) {
            System.out.printf("i=%d; val=%x \n",i, result.get(i));
        }*/
        byte[] resultArr = new byte[result.size()];
        for(int i = 0; i < resultArr.length; i++) {
            resultArr[i] = result.get(i);
        }
        return resultArr;
    }

    private int getRegister_1Value(AdsConfig adsConfig) {
        int registerValue = 0;
        //if (adsConfig.isHighResolutionMode()) {
        switch (adsConfig.getSampleRate()) {
                /*case S250:
                    registerValue = 0x06;//switch to low power mode
                    break;*/
            case S500:
                registerValue = 0x86;
                break;
            case S1000:
                registerValue = 0x85;
                break;
            case S2000:
                registerValue = 0x84;
                break;
            //  }
        } /*else {
            switch (adsConfig.getSps()) {
                case S250:
                    registerValue = 0x06;
                    break;
                case S500:
                    registerValue = 0x05;
                    break;
                case S1000:
                    registerValue = 0x04;
                    break;
                case S2000:
                    registerValue = 0x03;
                    break;
            }
        }*/
        return registerValue;
    }
    //--------------------------------

    private int getChanelRegisterValue(AdsConfig adsConfig, int channelNumber) {
        if (adsConfig.isAdsChannelEnabled(channelNumber)) {
            return adsConfig.getAdsChannelGain(channelNumber).getRegisterBits()  + adsConfig.getAdsChannelCommutatorState(channelNumber).getRegisterBits();
        }
        return 0x81;   //channel disabled
    }

    private int testSignalEnabledBits(AdsConfig adsConfig) {
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
           if(adsConfig.isAdsChannelEnabled(i) && adsConfig.getAdsChannelCommutatorState(i).equals(CommutatorState.TEST_SIGNAL)) {
               return 0x10;
           }
        }

        return 0x00;
    }

    private int getLoffSensRegisterValue(AdsConfig adsConfig){
        int result = 0;
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            result += (adsConfig.isAdsChannelEnabled(i) && adsConfig.isAdsChannelLeadOffEnable(i)) ? Math.pow(2, i) : 0;
        }
        return result;
    }

    private int getRLDSensBits(AdsConfig adsConfig) {
        int result = 0;
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            result += adsConfig.isAdsChannelRldSenseEnabled(i) ? Math.pow(2, i) : 0;
        }
        return result;
    }
}