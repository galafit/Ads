package com.biorecorder.ads;


import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.List;

class FrameDecoder implements ComPortListener {

    private static final byte START_FRAME_MARKER = (byte) (0xAA & 0xFF);
    private static final byte MESSAGE_MARKER = (byte) (0xA5 & 0xFF);
    private static final byte STOP_FRAME_MARKER = (byte) (0x55 & 0xFF);

    private static final byte MESSAGE_HARDWARE_CONFIG_MARKER = (byte) (0xA4 & 0xFF);
    private static final byte MESSAGE_2CH_MARKER = (byte) (0x02  & 0xFF);
    private static final byte MESSAGE_8CH_MARKER = (byte) (0x08  & 0xFF);
    private static final byte MESSAGE_HELLO_MARKER = (byte) (0xA0 & 0xFF);
    private static final byte MESSAGE_STOP_RECORDING_MARKER = (byte) (0xA5 & 0xFF);
    private static final byte MESSAGE_FIRMWARE_MARKER = (byte) (0xA1 & 0xFF);

    private int frameIndex;
    private int frameSize;
    private int dataRecordSize;
    private int numberOf3ByteSamples;
    private int decodedFrameSize;
    private byte[] rawFrame;
    AdsConfig adsConfig;
    private int previousFrameCounter = -1;
    private int[] accPrev = new int[3];
    List<AdsDataListener> dataListeners = new ArrayList<AdsDataListener>();
    List<MessageListener> messageListeners = new ArrayList<MessageListener>();
    private int MAX_MESSAGE_SIZE = 7;


    FrameDecoder(@Nullable AdsConfig configuration) {
        adsConfig = configuration;
        numberOf3ByteSamples = getNumberOf3ByteSamples();
        dataRecordSize = getRawFrameSize();
        decodedFrameSize = getDecodedFrameSize();
        rawFrame = new byte[Math.max(dataRecordSize, MAX_MESSAGE_SIZE)];
       // log.info("Com port frame size: " + dataRecordSize + " bytes");
    }

    public int getDataRecordSize() {
        return dataRecordSize;
    }

    public void addDataListener(AdsDataListener l) {
        dataListeners.add(l);
    }

    public void addMessageListener(MessageListener l) {
        messageListeners.add(l);
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
                // create new rowFrame with length = message length
                int msg_size = inByte & 0xFF;
                if(msg_size <= MAX_MESSAGE_SIZE) {
                    frameSize = msg_size;
                } else {
                    String infoMsg = "Message frame broken. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
                    notifyMessageListeners(AdsMessage.FRAME_BROKEN, infoMsg);
                    frameIndex = 0;
                }
            }
        } else if (frameIndex > 2 && frameIndex < (frameSize - 1)) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == (frameSize - 1)) {
            rawFrame[frameIndex] = inByte;
            if (inByte == STOP_FRAME_MARKER) {
                onFrameReceived();
            }else {
                String infoMsg = "Frame broken. No stopRecording frame marker. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
                notifyMessageListeners(AdsMessage.FRAME_BROKEN, infoMsg);
            }
            frameIndex = 0;
        } else {
            String infoMsg = "Frame broken. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
            notifyMessageListeners(AdsMessage.FRAME_BROKEN, infoMsg);
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
     // hardwareConfigMessage: xAA|xA5|x07|xA4|x02|x01|x55 =>
     // START_FRAME|MESSAGE_MARKER|number_of_bytes|HARDWARE_CONFIG|number_of_ads_channels|???|STOP_FRAME
     //  - reserved, power button, 2ADS channels, 1 accelerometer

     // stopRecording recording message: \xAA\xA5\x05\xA5\x55
     // hello message: \xAA\xA5\x05\xA0\x55
        AdsMessage adsMessage = null;
        String info = "";
        if(rawFrame[3] == MESSAGE_HELLO_MARKER) {
            adsMessage = AdsMessage.HELLO;
            info = "Hello message received";
        } else if (rawFrame[3] == MESSAGE_STOP_RECORDING_MARKER) {
            adsMessage = AdsMessage.STOP_RECORDING;
            info = "Stop recording message received";
        } else if (rawFrame[3] == MESSAGE_FIRMWARE_MARKER) {
            adsMessage = AdsMessage.FIRMWARE;
            info = "Firmware version message received";
        } else if(rawFrame[3] == MESSAGE_HARDWARE_CONFIG_MARKER && rawFrame[4] == MESSAGE_2CH_MARKER) {
            adsMessage = AdsMessage.ADS_2_CHANNELS;
            info = "Ads_2channel message received";
        } else if(rawFrame[3] == MESSAGE_HARDWARE_CONFIG_MARKER && rawFrame[4] == MESSAGE_8CH_MARKER) {
            adsMessage = AdsMessage.ADS_8_CHANNELS;
            info = "Ads_8channel message received";
        } else if (((rawFrame[3] & 0xFF) == 0xA3) && ((rawFrame[5] & 0xFF) == 0x01)) {
            adsMessage = AdsMessage.LOW_BATTERY;
            info = "Low battery message received";
        }  else if (((rawFrame[3] & 0xFF) == 0xA2) && ((rawFrame[5] & 0xFF) == 0x04)) {
            info = "TX fail message received";
            adsMessage = AdsMessage.TX_FAIL;
        } else {
            adsMessage = AdsMessage.UNKNOWN;
            StringBuilder sb = new StringBuilder("Unknown message received: ");
            for (int i = 0; i < rawFrame[2]; i++) {
                sb.append(byteToHexString(rawFrame[i]));
            }
            info = sb.toString();
        }
        notifyMessageListeners(adsMessage, info);
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

        if (adsConfig.isLeadOffEnabled()) {
            if (adsConfig.getNumberOfAdsChannels() == 8) {
                // 2 bytes for 8 channels
                decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            }
            else {
                // 1 byte for 2 channels
                decodedFrame[decodedFrameOffset++] = rawFrame[rawFrameOffset];
                rawFrameOffset += 1;
            }
        }

        int numberOfLostFrames = getNumberOfLostFrames(counter);
        for (int i = 0; i < numberOfLostFrames; i++) {
            notifyDataListeners(decodedFrame);
        }
        notifyDataListeners(decodedFrame);
    }

    private int getRawFrameSize() {
        if(adsConfig == null) {
            return 0;
        }
        int result = 2;//маркер начала фрейма
        result += 2; // счечик фреймов
        result += 3 * getNumberOf3ByteSamples();
        if (adsConfig.isAccelerometerEnabled()) {
            result += 6;
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            result += 2;
        }
        if (adsConfig.isLeadOffEnabled()) {
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
        if(adsConfig == null) {
            return 0;
        }
        int result = 0;
        result += getNumberOf3ByteSamples();
        if (adsConfig.isAccelerometerEnabled()) {
            result = result + (adsConfig.isAccelerometerOneChannelMode() ? 1 : 3);
        }
        if (adsConfig.isBatteryVoltageMeasureEnabled()) {
            result += 1;
        }
        if(adsConfig.isLeadOffEnabled()) {
            if (adsConfig.getNumberOfAdsChannels() == 8) {
                result += 2;
            } else {
                result += 1;
            }
        }

        return result;
    }

    private int getNumberOf3ByteSamples() {
        if(adsConfig == null) {
            return 0;
        }
        int result = 0;
        for (int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
            if (adsConfig.isAdsChannelEnabled(i)) {
                int divider = adsConfig.getAdsChannelDivider(i);
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

    private void notifyDataListeners(int[] decodedFrame) {
        for (AdsDataListener l : dataListeners) {
            l.onDataReceived(decodedFrame);
        }
    }

    private void notifyMessageListeners(AdsMessage adsMessage, String additionalInfo) {
        for (MessageListener l : messageListeners) {
            l.onMessageReceived(adsMessage, additionalInfo);
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

    private static String byteToHexString(byte b) {
        return String.format("%02X ", b);
    }
}
