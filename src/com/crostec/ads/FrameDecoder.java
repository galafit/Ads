package com.crostec.ads;

import comport.ComPortListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

abstract class FrameDecoder implements ComPortListener {

    public static final int START_FRAME_MARKER = 0xAA;
    public static final int STOP_FRAME_MARKER = 0x55;
    private int frameIndex;
    private int rawFrameSize;
    private int numberOf3ByteSamples;
    private int decodedFrameSize;
    private int[] rawFrame;
    AdsConfiguration adsConfiguration;
    private static final Log log = LogFactory.getLog(FrameDecoder.class);
    private int previousFrameCounter = -1;

    public FrameDecoder(AdsConfiguration configuration) {
        adsConfiguration = configuration;
        numberOf3ByteSamples = getNumberOf3ByteSamples(configuration);
        rawFrameSize = getRawFrameSize(configuration);
        decodedFrameSize = getDecodedFrameSize(configuration);
        rawFrame = new int[rawFrameSize];
        log.info("Com port frame size: " + rawFrameSize + " bytes");
    }

    @Override
    public void onByteReceived(byte b) {
        int inByte = (b & 0xFF);
        if (frameIndex == 0 && inByte == START_FRAME_MARKER) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == 1 && inByte == START_FRAME_MARKER) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex > 1 && frameIndex < (rawFrameSize - 1)) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == (rawFrameSize - 1)) {
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
        int counter = ((rawFrame[3] << 24) + ((rawFrame[2]) << 16)) / 65536;

        int[] decodedFrame = new int[decodedFrameSize];
        int rawFrameOffset = 4;
        int decodedFrameOffset = 0;
        for (int i = 0; i < numberOf3ByteSamples; i++) {
            decodedFrame[decodedFrameOffset++] = (((rawFrame[2 +rawFrameOffset] << 24) + ((rawFrame[1 + rawFrameOffset]) << 16) + (rawFrame[rawFrameOffset] << 8)) / 256);
            rawFrameOffset += 3;
        }

        if (adsConfiguration.isAccelerometerEnabled()) {
            for (int i = 0; i < 3; i++) {
                decodedFrame[decodedFrameOffset++] = (((rawFrame[rawFrameOffset + 1] << 24) + ((rawFrame[rawFrameOffset]) << 16)) / 65536);
                rawFrameOffset += 2;
            }
        }

        int numberOfLostFrames = getNumberOfLostFrames(counter);
         for (int i = 0; i < numberOfLostFrames; i++) {
            // byte[] lostFrame = new byte[decodedFrameSize];
            // System.arraycopy( decodedFrame, 0, lostFrame, 0, decodedFrameSize);
            // decodedFrame[decodedFrameSize - 1] = 1;
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
        if (adsConfiguration.isLoffEnabled()) {
            result += 1;
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
