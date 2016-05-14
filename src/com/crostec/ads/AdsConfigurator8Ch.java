package com.crostec.ads;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AdsConfigurator8Ch implements AdsConfigurator {
    public static final int NUMBER_OF_ADS_CHANNELS = 8;

    @Override
    public List<Byte> writeAdsConfiguration(AdsConfiguration adsConfiguration) {
        //-----------------------------------------
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte)51);       //длина пакета

        result.add((byte)0xF0);     //ads1292 command
        result.add((byte)0x11);     //ads1292 stop continuous

        result.add((byte)0xF1);     //запись регистров ads1298
        result.add((byte)0x01);     //адрес первого регистра
        result.add((byte)0x17);     //количество регистров

        result.add((byte) getRegister_1Value(adsConfiguration));         //register 0x01   set SPS
        result.add((byte)testSignalEnabledBits(adsConfiguration));       //register 0x02   test signal
        result.add((byte)0xCC);                                          //register 0x03
        boolean isLoffEnabled = adsConfiguration.isLoffEnabled();
        result.add((byte)(isLoffEnabled? 0x13 : 0x00));                  //register 0x04
        for (int i = 0; i < 8; i++) {
            result.add((byte) getChanelRegisterValue(adsConfiguration.getAdsChannels().get(i)));//registers 0x05 - 0x0C
        }
         int rlsSensBits = getRLDSensBits(adsConfiguration.getAdsChannels());
        result.add((byte)rlsSensBits);  //RLD sens positive              register 0x0D
        result.add((byte)rlsSensBits);  //RLD sens negative              register 0x0E

        int loffSensBits = getLoffSensRegisterValue(adsConfiguration.getAdsChannels());
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
        for (int i = 0; i < NUMBER_OF_ADS_CHANNELS; i++) {
            AdsChannelConfiguration adsChannelConfiguration = adsConfiguration.getAdsChannels().get(i);
            int divider = adsChannelConfiguration.isEnabled ? adsChannelConfiguration.getDivider().getValue() : 0;
            result.add((byte)divider);
        }

        result.add((byte)0xF3);     //accelerometer mode: 0 - disabled, 1 - enabled
        int accelerometerMode = adsConfiguration.isAccelerometerEnabled() ? 1 : 0;
        result.add((byte)accelerometerMode);

        result.add((byte)0xF4);     //send battery voltage data: 0 - disabled, 1 - enabled
        int batteryMeasure = adsConfiguration.isBatteryVoltageMeasureEnabled() ? 1 : 0;
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
        for (int i = 0; i < result.size(); i++) {
             System.out.printf("i=%d; val=%x \n",i, result.get(i));
        }
        return result;
    }

    private int getRegister_1Value(AdsConfiguration adsConfiguration) {
        int registerValue = 0;
        //if (adsConfiguration.isHighResolutionMode()) {
            switch (adsConfiguration.getSps()) {
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
            switch (adsConfiguration.getSps()) {
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

    private int getChanelRegisterValue(AdsChannelConfiguration channelConfiguration) {
        if (channelConfiguration.isEnabled()) {
            return channelConfiguration.getGain().getRegisterBits() + channelConfiguration.getCommutatorState().getRegisterBits();
        }
        return 0x81;   //channel disabled
    }

    private int testSignalEnabledBits(AdsConfiguration configuration) {
        int result = 0x00;
        for (AdsChannelConfiguration adsChannelConfiguration : configuration.getAdsChannels()) {
            if (adsChannelConfiguration.isEnabled() && adsChannelConfiguration.getCommutatorState().equals(CommutatorState.TEST_SIGNAL)) {
                result = 0x10;
            }
        }
        return result;
    }

    private int getLoffSensRegisterValue(List<AdsChannelConfiguration> channelConfigurationList){
        int result = 0;
        for (int i = 0; i < channelConfigurationList.size(); i++) {
            AdsChannelConfiguration adsChannelConfiguration = channelConfigurationList.get(i);
            result += (adsChannelConfiguration.isEnabled && adsChannelConfiguration.isLoffEnable()) ? Math.pow(2, i) : 0;
        }
        return result;
    }

    private int getRLDSensBits(List<AdsChannelConfiguration> channelConfigurationList) {
        int result = 0;
        for (int i = 0; i < channelConfigurationList.size(); i++) {
            AdsChannelConfiguration adsChannelConfiguration = channelConfigurationList.get(i);
            result += adsChannelConfiguration.isRldSenseEnabled() ? Math.pow(2, i) : 0;
        }
        return result;
    }
}
