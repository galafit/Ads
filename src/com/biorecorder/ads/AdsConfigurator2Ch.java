package com.biorecorder.ads;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class AdsConfigurator2Ch implements AdsConfigurator{

    public static final int NUMBER_OF_ADS_CHANNELS = 2;
    public static final int NUMBER_OF_ACCELEROMETER_CHANNELS = 3;


    @Override
    public byte[] getAdsConfigurationCommand(AdsConfig adsConfiguration) {
        List<Byte> result = new ArrayList<Byte>();
        result.add((byte)32);       //длина пакета

        result.add((byte)0xF0);     //ads1292 command
        result.add((byte)0x11);     //ads1292 stopRecording continuous

        result.add((byte)0xF1);     //запись регистров ads1292
        result.add((byte)0x01);     //адрес первого регистра
        result.add((byte)0x0A);     //количество регистров

        int config1RegisterValue = adsConfiguration.getSampleRate().getRegisterBits();
        result.add((byte)config1RegisterValue);

        int config2RegisterValue = 0xA0 + loffComparatorEnabledBit(adsConfiguration) + testSignalEnabledBits(adsConfiguration);
        result.add((byte)config2RegisterValue);

         //reg 0x03
        result.add((byte)0x10);
         //reg 0x04
        result.add((byte)getChanelRegisterValue(adsConfiguration, 0));      //reg 0x04 Set Channel 1 to example
         //reg 0x05
        result.add((byte)getChanelRegisterValue(adsConfiguration, 1));     //reg 0x05 Set Channel 2 to Input Short and disable
        //reg 0x06 Turn on Drl.
        result.add((byte)0x20);

        //reg 0x07
        int loffSensRegisterValue = 0;
         if(adsConfiguration.isAdsChannelLeadOffEnable(0)){
            loffSensRegisterValue += 0x03;
        }
        if(adsConfiguration.isAdsChannelLeadOffEnable(1)){
            loffSensRegisterValue += 0x0C;
        }
        result.add((byte)loffSensRegisterValue);     //reg 0x07

        result.add((byte)0x40);     //reg 0x08 clock divider Fclc/16 2048mHz external clock
        result.add((byte)0x02);     //reg 0x09 Set mandatory bit. 
        result.add((byte)0x01);     //reg 0x0A  RLDREF_INT disabled
        //result.add((byte)0x03);     //reg 0x0A Set RLDREF_INT

        result.add((byte)0xF2);     //делители частоты для 2х каналов ads1292  возможные значения 0,1,2,5,10;
        for (int i = 0; i < NUMBER_OF_ADS_CHANNELS; i++) {
            int divider = adsConfiguration.isAdsChannelEnabled(i) ? adsConfiguration.getAdsChannelDivider(i) : 0;
            result.add((byte)divider);
        }

        result.add((byte)0xF3);     //accelerometer mode: 0 - disabled, 1 - enabled
        int accelerometerMode = adsConfiguration.isAccelerometerEnabled() ? 1 : 0;
        result.add((byte)accelerometerMode);

        result.add((byte)0xF4);     //send battery voltage data: 0 - disabled, 1 - enabled
        int batteryMeasure = adsConfiguration.isBatteryVoltageMeasureEnabled() ? 1 : 0;
        result.add((byte)batteryMeasure);

        result.add((byte)0xF5);     //передача данных loff статуса: 0 - disabled, 1 - enabled
        int loffEnabled = adsConfiguration.isLeadOffEnabled() ? 1 : 0;
        result.add((byte)loffEnabled);

        result.add((byte)0xF6);     //reset timeout. In seconds
        result.add((byte)20);

        result.add((byte)0xF0);     //ads1292 command
        result.add((byte)0x10);     //ads1292 connect continuous

        result.add((byte)0xFE);     //connect recording

        result.add((byte)0x55);     //footer1
        result.add((byte)0x55);     //footer1

        byte[] resultArr = new byte[result.size()];
        for(int i = 0; i < resultArr.length; i++) {
            resultArr[i] = result.get(i);
        }
        return resultArr;
    }

    private int getChanelRegisterValue(AdsConfig configuration, int adsChannelNumber) {
        int result = 0x80;   //channel disabled
        if (configuration.isAdsChannelEnabled(adsChannelNumber)) {
            result = 0x00;
        }
        return result + configuration.getAdsChannelGain(adsChannelNumber).getRegisterBits() + configuration.getAdsChannelCommutatorState(adsChannelNumber).getRegisterBits();
    }

    private int loffComparatorEnabledBit(AdsConfig configuration) {
        int result = 0x00;
        for (int i = 0; i < configuration.getAdsChannelsCount(); i++) {
            if (configuration.isAdsChannelLeadOffEnable(i)) {
                result = 0x40;
            }
        }
        return result;
    }

    private int testSignalEnabledBits(AdsConfig configuration) {
        int result = 0x00;
        for (int i = 0; i < configuration.getAdsChannelsCount(); i++) {
            if (configuration.getAdsChannelCommutatorState(i).equals(Commutator.TEST_SIGNAL)) {
                result = 0x03;
            }
        }
        return result;
    }
}
