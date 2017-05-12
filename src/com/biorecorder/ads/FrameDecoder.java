package com.biorecorder.ads;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

class FrameDecoder implements ComPortListener {
    private static final byte START_FRAME_MARKER = (byte) (0xAA & 0xFF);
    private static final byte MESSAGE_MARKER = (byte) (0xA5 & 0xFF);
    private static final byte HARDWARE_CONFIG_MARKER = (byte) (0xA4 & 0xFF);
    private static final byte CH2_MARKER = (byte) (0x02  & 0xFF);
    private static final byte CH8_MARKER = (byte) (0x02  & 0xFF);
    private static final byte STOP_FRAME_MARKER = (byte) (0x55 & 0xFF);


    private int frameIndex;
    private int frameSize;
    private int dataRecordSize;
    private int numberOf3ByteSamples;
    private int decodedFrameSize;
    private byte[] rawFrame;
    AdsConfig adsConfig;
    private static final Log log = LogFactory.getLog(FrameDecoder.class);
    private int previousFrameCounter = -1;
    private int[] accPrev = new int[3];
    List<DataFrameListener> dataFrameListeners = new ArrayList<DataFrameListener>(1);

    FrameDecoder(AdsConfig configuration) {
        adsConfig = configuration;
        numberOf3ByteSamples = getNumberOf3ByteSamples();
        dataRecordSize = getRawFrameSize();
        decodedFrameSize = getDecodedFrameSize();
        rawFrame = new byte[dataRecordSize];
        log.info("Com port frame size: " + dataRecordSize + " bytes");
    }

    public void addDataFrameListener(DataFrameListener l) {
        dataFrameListeners.add(l);
    }


    @Override
    public void onByteReceived(byte inByte) {
        if (frameIndex == 0 && inByte == START_FRAME_MARKER) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == 1 && inByte == START_FRAME_MARKER) {  //receiving data record
            rawFrame[frameIndex] = inByte;
            frameSize = dataRecordSize;
            frameIndex++;
        } else if (frameIndex == 1 && inByte == MESSAGE_MARKER) {  //receiving message
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == 2) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
            if (rawFrame[1] == MESSAGE_MARKER) {   //message length
                frameSize = inByte & 0xFF;
            }
        } else if (frameIndex > 2 && frameIndex < (frameSize - 1)) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == (frameSize - 1)) {
            rawFrame[frameIndex] = inByte;
            if (inByte == STOP_FRAME_MARKER) {
                onFrameReceived();
            }
            frameIndex = 0;
        } else {
            log.warn("Lost Frame. Frame index = " + frameIndex + " inByte = " + inByte);
            frameIndex = 0;
        }
    }

    private void onFrameReceived() {
        // Frame = \xAA\xAA... => frame[0] and frame[1] = START_FRAME_MARKER - data
        if (rawFrame[1] == START_FRAME_MARKER) {
            onDataRecordReceived();
        }
        // Frame = \xAA\xA5... => frame[0] = START_FRAME_MARKER and frame[1] = MESSAGE_MARKER - massage
        if (rawFrame[1] == MESSAGE_MARKER) {
            onMessageReceived();
        }
    }

    private void onMessageReceived() {
     // xAA|xA5|x07|xA4|x02|x01|x55 =>
     // START_FRAME|MESSAGE_MARKER|number_of_bytes|HARDWARE_CONFIG|number_of_ads_channels|???|STOP_FRAME

        if(rawFrame[3] == HARDWARE_CONFIG_MARKER) {
            if(rawFrame[4] == CH2_MARKER) {
                System.out.println("ads 2ch");
            }
            if(rawFrame[4] == CH8_MARKER) {
                System.out.println("ads 8ch");
            }
            log.info("Hardware message received");
        }
        else if (((rawFrame[3] & 0xFF) == 0xA3) && ((rawFrame[5] & 0xFF) == 0x01)) {
            log.info("Low battery message received");
        } else if ((rawFrame[3] & 0xFF) == 0xA0) {
            log.info("Hello message received");
        } else if ((rawFrame[3] & 0xFF) == 0xA1) {
            log.info("Firmware version message received");
        } else if (((rawFrame[3] & 0xFF) == 0xA2) && ((rawFrame[5] & 0xFF) == 0x04)) {
            log.info("TX fail message received");
        } else if ((rawFrame[3] & 0xFF) == 0xA5) {
            log.info("Stop recording message received");
        }else {
            System.out.println("Unknown message received: ");
            for (int i = 0; i < rawFrame[2]; i++) {
                int val = rawFrame[i] & 0xFF;
                System.out.printf("i=%d; val=%x \n", i, val);
            }
        }
    }

    private void onDataRecordReceived() {
        int counter = bytesToSignedInt(rawFrame[2], rawFrame[3]);

        int[] decodedFrame = new int[decodedFrameSize];
        int rawFrameOffset = 4;
        int decodedFrameOffset = 0;
        for (int i = 0; i < numberOf3ByteSamples; i++) {
            decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1], rawFrame[rawFrameOffset + 2])/ adsConfig.getNoiseDivider();
            rawFrameOffset += 3;
        }

        if (adsConfig.isAccelerometerEnabled()) {
            int[] accVal = new int[3];
            int accSum = 0;
            for (int i = 0; i < 3; i++) {
//                decodedFrame[decodedFrameOffset++] = AdsUtils.littleEndianBytesToInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                accVal[i] =  bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            }
            if(adsConfig.isAccelerometerOneChannelMode()){
                for (int i = 0; i < accVal.length; i++) {
                    accSum += Math.abs(accVal[i] - accPrev[i]);
                    accPrev[i] = accVal[i];
                }
                decodedFrame[decodedFrameOffset++] = accSum;
            }else {
                 for (int i = 0; i < accVal.length; i++) {
                    decodedFrame[decodedFrameOffset++] = accVal[i];
                }
            }
        }

        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
            rawFrameOffset += 2;
        }

        if (adsConfig.isLoffEnabled()) {
            if (adsConfig.getNumberOfAdsChannels() == 8) {
                decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            }
            else {
                decodedFrame[decodedFrameOffset++] = rawFrame[rawFrameOffset];
                rawFrameOffset += 1;
            }
        }

        int numberOfLostFrames = getNumberOfLostFrames(counter);
        for (int i = 0; i < numberOfLostFrames; i++) {
            notifyDataFrameListeners(decodedFrame);
        }
        notifyDataFrameListeners(decodedFrame);
    }

    private int getRawFrameSize() {
        int result = 2;//маркер начала фрейма
        result += 2; // счечик фреймов
        result += 3 * getNumberOf3ByteSamples();
        if (adsConfig.isAccelerometerEnabled()) {
            result += 6;
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            result += 2;
        }
        if (adsConfig.isLoffEnabled()) {
            if (adsConfig.getNumberOfAdsChannels() == 8) {
                result += 2;
            } else {
                result += 1;
            }
        }
        result += 1;//footer
        return result;
    }

    private int getDecodedFrameSize() {
        int result = 0;
        result += getNumberOf3ByteSamples();
        if (adsConfig.isAccelerometerEnabled()) {
            result = result + (adsConfig.isAccelerometerOneChannelMode() ? 1 : 3);
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            result += 1;
        }
        if(adsConfig.isLoffEnabled()) {
            if (adsConfig.getNumberOfAdsChannels() == 8) {
                result += 2;
            } else {
                result += 1;
            }
        }

        return result;
    }

    private int getNumberOf3ByteSamples() {
        int result = 0;
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            AdsChannelConfig adsChannelConfig = adsConfig.getAdsChannel(i);
            if (adsChannelConfig.isEnabled) {
                int divider = adsChannelConfig.getDivider().getValue();
                int maxDiv = adsConfig.getMaxDiv();
                result += (maxDiv / divider);
            }
        }
        return result;
    }

    private int getNumberOfLostFrames(int frameCounter) {
        if (previousFrameCounter == -1) {
            previousFrameCounter = frameCounter;
            return 0;
        }
        int result = frameCounter - previousFrameCounter;
        result = result > 0 ? result : (result + 256);
        previousFrameCounter = frameCounter;
        return result - 1;
    }

    private void notifyDataFrameListeners(int[] decodedFrame) {
        for (DataFrameListener dataFrameListener : dataFrameListeners) {
            dataFrameListener.onDataFrameReceived(decodedFrame);
        }
    }

    /* Java int BIG_ENDIAN, Byte order: LITTLE_ENDIAN  */
    private static int bytesToSignedInt(byte... b) {
        switch (b.length) {
            case 1:
                return b[0];
            case 2:
                return (b[1] << 8) | (b[0] & 0xFF);
            case 3:
                return (b[2] << 16) | (b[1] & 0xFF) << 8 | (b[0] & 0xFF);
            default:
                return (b[3] << 24) | (b[2] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[0] & 0xFF);
        }
    }
}
