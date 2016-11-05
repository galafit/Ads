package com.biorecorder.ads;

import com.biorecorder.comport.ComPortListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

abstract class FrameDecoder implements ComPortListener {

    public static final byte START_FRAME_MARKER = (byte) (0xAA & 0xFF);
    public static final byte STOP_FRAME_MARKER = (byte) (0x55 & 0xFF);
    public static final byte MESSAGE_MARKER = (byte) (0xA5 & 0xFF);
    private int frameIndex;
    private int frameSize;
    private int dataRecordSize;
    private int numberOf3ByteSamples;
    private int decodedFrameSize;
    private byte[] rawFrame;
    AdsConfiguration adsConfiguration;
    private static final Log log = LogFactory.getLog(FrameDecoder.class);
    private int previousFrameCounter = -1;

    public FrameDecoder(AdsConfiguration configuration) {
        adsConfiguration = configuration;
        numberOf3ByteSamples = getNumberOf3ByteSamples(configuration);
        dataRecordSize = getRawFrameSize(configuration);
        decodedFrameSize = getDecodedFrameSize(configuration);
        rawFrame = new byte[dataRecordSize];
        log.info("Com port frame size: " + dataRecordSize + " bytes");
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
        if (rawFrame[1] == START_FRAME_MARKER) {
            onDataRecordReceived();
        }
        if (rawFrame[1] == MESSAGE_MARKER) {
            onMessageReceived();
        }
    }

    private void onMessageReceived() {
        if (((rawFrame[3] & 0xFF) == 0xA3) && ((rawFrame[5] & 0xFF) == 0x01)) {
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
        int counter = AdsUtils.bytesToSignedInt(rawFrame[2], rawFrame[3]);

        int[] decodedFrame = new int[decodedFrameSize];
        int rawFrameOffset = 4;
        int decodedFrameOffset = 0;
        for (int i = 0; i < numberOf3ByteSamples; i++) {
            decodedFrame[decodedFrameOffset++] = AdsUtils.bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1], rawFrame[rawFrameOffset + 2])/adsConfiguration.getNoiseDivider();
            rawFrameOffset += 3;
        }

        if (adsConfiguration.isAccelerometerEnabled()) {
            for (int i = 0; i < 3; i++) {
                decodedFrame[decodedFrameOffset++] = AdsUtils.bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            }
        }

        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
            decodedFrame[decodedFrameOffset++] = AdsUtils.bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
            rawFrameOffset += 2;
        }

        if (adsConfiguration.isLoffEnabled()) {
            decodedFrame[decodedFrameOffset++] = rawFrame[rawFrameOffset];
            rawFrameOffset += 1;
            if (adsConfiguration.getDeviceType().getNumberOfAdsChannels() == 8) {
                decodedFrame[decodedFrameOffset++] = rawFrame[rawFrameOffset];
                rawFrameOffset += 1;
            }
        }

        int numberOfLostFrames = getNumberOfLostFrames(counter);
        for (int i = 0; i < numberOfLostFrames; i++) {
            notifyListeners(decodedFrame);
        }
        notifyListeners(decodedFrame);
    }

    private int getRawFrameSize(AdsConfiguration adsConfiguration) {
        int result = 2;//маркер начала фрейма
        result += 2; // счечик фреймов
        result += 3 * getNumberOf3ByteSamples(adsConfiguration);
        if (adsConfiguration.isAccelerometerEnabled()) {
            result += 6;
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
            result += 2;
        }
        if (adsConfiguration.isLoffEnabled()) {
            if (adsConfiguration.getDeviceType().getNumberOfAdsChannels() == 8) {
                result += 2;
            } else {
                result += 1;
            }
        }
        result += 1;//footer
        return result;
    }

    private int getDecodedFrameSize(AdsConfiguration configuration) {
        int result = 0;
        result += getNumberOf3ByteSamples(configuration);
        if (configuration.isAccelerometerEnabled()) {
            result += 3;
        }
        if (configuration.isBatteryVoltageMeasureEnabled()) {
            result += 1;
        }
        result += 2;
        return result;
    }

    private int getNumberOf3ByteSamples(AdsConfiguration adsConfiguration) {
        int result = 0;
        for (AdsChannelConfiguration adsChannelConfiguration : adsConfiguration.getAdsChannels()) {
            if (adsChannelConfiguration.isEnabled) {
                int divider = adsChannelConfiguration.getDivider().getValue();
                int maxDiv = adsConfiguration.getDeviceType().getMaxDiv().getValue();
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

    public abstract void notifyListeners(int[] decodedFrame);


}
