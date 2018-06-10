package com.biorecorder.ads;

import com.sun.istack.internal.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class FrameDecoder implements ComportListener {
    private static final Log log = LogFactory.getLog(FrameDecoder.class);
    private static final byte START_FRAME_MARKER = (byte) (0xAA & 0xFF);
    private static final byte MESSAGE_MARKER = (byte) (0xA5 & 0xFF);
    private static final byte STOP_FRAME_MARKER = (byte) (0x55 & 0xFF);

    private static final byte MESSAGE_HARDWARE_CONFIG_MARKER = (byte) (0xA4 & 0xFF);
    private static final byte MESSAGE_2CH_MARKER = (byte) (0x02 & 0xFF);
    private static final byte MESSAGE_8CH_MARKER = (byte) (0x08 & 0xFF);
    private static final byte MESSAGE_HELLO_MARKER = (byte) (0xA0 & 0xFF);
    private static final byte MESSAGE_STOP_RECORDING_MARKER = (byte) (0xA5 & 0xFF);
    private static final byte MESSAGE_FIRMWARE_MARKER = (byte) (0xA1 & 0xFF);

    private int MAX_MESSAGE_SIZE = 7;
    /*******************************************************************
     * these fields we need to restore  data records numbers
     *  from short (sent by ads in 2 bytes) to long
     *******************************************************************/
    private static int SHORT_MAX = 65535; // max value of unsigned short
    private static int HALF_SHORT_MAX = SHORT_MAX / 2;
    private int durationOfShortMaxRecordsMs;
    private int durationOfHalfShortMaxRecordsMs;
    private int previousRecordShortNumber = -1;
    private long previousRecordTime;
    private int startRecordNumber;
    private int shortBlocksCount;
    /***************************************************************/


    private int frameIndex;
    private int frameSize;
    private int rowFrameSizeInBytes;
    private int numberOf3ByteSamples;
    private int decodedFrameSizeInInts;
    private byte[] rawFrame;
    private int[] accPrev = new int[3];
    private final AdsConfig adsConfig;
    private volatile NumberedDataListener dataListener = new NullDataListener();
    private volatile MessageListener messageListener = new NullMessageListener();

    FrameDecoder(@Nullable AdsConfig configuration) {
        if (configuration != null) {
            durationOfShortMaxRecordsMs = (int) (configuration.getDurationOfDataRecord() * 1000 * SHORT_MAX);
            durationOfHalfShortMaxRecordsMs = durationOfShortMaxRecordsMs / 2;
        }
        adsConfig = configuration;
        numberOf3ByteSamples = getNumberOf3ByteSamples();
        rowFrameSizeInBytes = getRawFrameSize();
        decodedFrameSizeInInts = getDecodedFrameSize();
        rawFrame = new byte[Math.max(rowFrameSizeInBytes, MAX_MESSAGE_SIZE)];
        log.info("Com port frame size: " + decodedFrameSizeInInts + " bytes");
    }

    /**
     * Frame decoder permits to add only ONE DataListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addDataListener(NumberedDataListener l) {
        if (l != null) {
            dataListener = l;
        }
    }

    /**
     * Frame decoder permits to add only ONE MessageListener! So if a new listener added
     * the old one are automatically removed
     */
    public void addMessageListener(MessageListener l) {
        if (l != null) {
            messageListener = l;
        }
    }

    public void removeDataListener() {
        dataListener = new NullDataListener();
    }

    public void removeMessageListener() {
        messageListener = new NullMessageListener();
    }

    @Override
    public void onByteReceived(byte inByte) {
        if (frameIndex == 0 && inByte == START_FRAME_MARKER) {
            rawFrame[frameIndex] = inByte;
            frameIndex++;
        } else if (frameIndex == 1 && inByte == START_FRAME_MARKER) {  //receiving data record
            rawFrame[frameIndex] = inByte;
            frameSize = rowFrameSizeInBytes;
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
                if (msg_size <= MAX_MESSAGE_SIZE) {
                    frameSize = msg_size;
                } else {
                    String infoMsg = "Message frame broken. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
                    notifyMessageListeners(AdsMessageType.FRAME_BROKEN, infoMsg);
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
            } else {
                String infoMsg = "Frame broken. No stop frame marker. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
                notifyMessageListeners(AdsMessageType.FRAME_BROKEN, infoMsg);
            }
            frameIndex = 0;
        } else {
            // if adsConfig == null data frames can not be correctly detected
            if(adsConfig != null) { // frameSize != 0
                String infoMsg = "Frame broken. Frame index = " + frameIndex + " received byte = " + byteToHexString(inByte);
                notifyMessageListeners(AdsMessageType.FRAME_BROKEN, infoMsg);
            }
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

        // stop recording message: \xAA\xA5\x05\xA5\x55
        // hello message: \xAA\xA5\x05\xA0\x55
        AdsMessageType adsMessageType = null;
        String info = "";
        if (rawFrame[3] == MESSAGE_HELLO_MARKER) {
            adsMessageType = AdsMessageType.HELLO;
            info = "Hello message received";
        } else if (rawFrame[3] == MESSAGE_STOP_RECORDING_MARKER) {
            adsMessageType = AdsMessageType.STOP_RECORDING;
            info = "Stop recording message received";
        } else if (rawFrame[3] == MESSAGE_FIRMWARE_MARKER) {
            adsMessageType = AdsMessageType.FIRMWARE;
            info = "Firmware version message received";
        } else if (rawFrame[3] == MESSAGE_HARDWARE_CONFIG_MARKER && rawFrame[4] == MESSAGE_2CH_MARKER) {
            adsMessageType = AdsMessageType.ADS_2_CHANNELS;
            info = "Ads_2channel message received";
        } else if (rawFrame[3] == MESSAGE_HARDWARE_CONFIG_MARKER && rawFrame[4] == MESSAGE_8CH_MARKER) {
            adsMessageType = AdsMessageType.ADS_8_CHANNELS;
            info = "Ads_8channel message received";
        } else if (((rawFrame[3] & 0xFF) == 0xA3) && ((rawFrame[5] & 0xFF) == 0x01)) {
            adsMessageType = AdsMessageType.LOW_BATTERY;
            info = "Low battery message received";
        } else if (((rawFrame[3] & 0xFF) == 0xA2) && ((rawFrame[5] & 0xFF) == 0x04)) {
            info = "TX fail message received";
            adsMessageType = AdsMessageType.TX_FAIL;
        } else {
            StringBuilder sb = new StringBuilder("Unknown message received: ");
            for (int i = 0; i < rawFrame[2]; i++) {
                sb.append(byteToHexString(rawFrame[i]));
            }
            log.info(sb.toString());
            return;
        }
        notifyMessageListeners(adsMessageType, info);
    }

    private void onDataRecordReceived() {
        int[] decodedFrame = new int[decodedFrameSizeInInts];
        int rawFrameOffset = 4;
        int decodedFrameOffset = 0;
        for (int i = 0; i < numberOf3ByteSamples; i++) {
            decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1], rawFrame[rawFrameOffset + 2]) / adsConfig.getNoiseDivider();
            rawFrameOffset += 3;
        }

        if (adsConfig.isAccelerometerEnabled()) {
            int[] accVal = new int[3];
            int accSum = 0;
            for (int i = 0; i < 3; i++) {
//                decodedFrame[decodedFrameOffset++] = AdsUtils.littleEndianBytesToInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                accVal[i] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            }
            if (adsConfig.isAccelerometerOneChannelMode()) {
                for (int i = 0; i < accVal.length; i++) {
                    accSum += Math.abs(accVal[i] - accPrev[i]);
                    accPrev[i] = accVal[i];
                }
                decodedFrame[decodedFrameOffset++] = accSum;
            } else {
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
            if (adsConfig.getAdsChannelsCount() == 8) {
                // 2 bytes for 8 channels
                decodedFrame[decodedFrameOffset++] = bytesToSignedInt(rawFrame[rawFrameOffset], rawFrame[rawFrameOffset + 1]);
                rawFrameOffset += 2;
            } else {
                // 1 byte for 2 channels
                decodedFrame[decodedFrameOffset++] = rawFrame[rawFrameOffset];
                rawFrameOffset += 1;
            }
        }

        int recordShortNumber = bytesToUnsignedInt(rawFrame[2], rawFrame[3]);
        notifyDataListeners(decodedFrame, recordShortNumberToInt(recordShortNumber));
    }


    private int recordShortNumberToInt(int recordShortNumber) {
        long time = System.currentTimeMillis();

        if (previousRecordShortNumber == -1) {
            previousRecordShortNumber = recordShortNumber;
            previousRecordTime = time;
            startRecordNumber = recordShortNumber;
            return 0;
        }
        int recordsDistance = recordShortNumber - previousRecordShortNumber;
        if (recordsDistance <= 0) {
            shortBlocksCount++;
            recordsDistance += SHORT_MAX;
        }
        if (time - previousRecordTime > durationOfHalfShortMaxRecordsMs) {
            long blocks = Math.round(((double) (time - previousRecordTime)) / durationOfShortMaxRecordsMs);
            if (recordsDistance > HALF_SHORT_MAX / 2) {
                blocks--;
            }
            shortBlocksCount += blocks;
        }

        previousRecordTime = time;
        previousRecordShortNumber = recordShortNumber;
        return shortBlocksCount * SHORT_MAX + recordShortNumber - startRecordNumber;
    }


    private int getRawFrameSize() {
        if (adsConfig == null) {
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
            if (adsConfig.getAdsChannelsCount() == 8) {
                result += 2;
            } else {
                result += 1;
            }
        }
        result += 1;//footer
        return result;
    }

    private int getDecodedFrameSize() {
        if (adsConfig == null) {
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
        if (adsConfig.isLeadOffEnabled()) {
            result += 1;
        }

        return result;
    }

    private int getNumberOf3ByteSamples() {
        if (adsConfig == null) {
            return 0;
        }
        int result = 0;
        int maxDiv = Divider.getMaxDivider().getValue();
        for (int i = 0; i < adsConfig.getAdsChannelsCount(); i++) {
            if (adsConfig.isAdsChannelEnabled(i)) {
                int divider = adsConfig.getAdsChannelDivider(i);
                result += (maxDiv / divider);
            }
        }
        return result;
    }

    private void notifyDataListeners(int[] dataRecord, int recordNumber) {
        dataListener.onDataReceived(dataRecord, recordNumber);

    }

    private void notifyMessageListeners(AdsMessageType adsMessageType, String additionalInfo) {
        messageListener.onMessage(adsMessageType, additionalInfo);
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

    /**
     * Convert given LITTLE_ENDIAN ordered bytes to BIG_ENDIAN 32-bit UNSIGNED int.
     * Available number of input bytes: 4, 3, 2 or 1.
     *
     * @param bytes 4, 3, 2 or 1 bytes (LITTLE_ENDIAN ordered) to be converted to int
     * @return 32-bit UNSIGNED int (BIG_ENDIAN)
     */

    public static int bytesToUnsignedInt(byte... bytes) {
        switch (bytes.length) {
            case 1:
                return (bytes[0] & 0xFF);
            case 2:
                return (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF);
            case 3:
                return (bytes[2] & 0xFF) << 16 | (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF);
            case 4:
                return (bytes[3] & 0xFF) << 24 | (bytes[2] & 0xFF) << 16 | (bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF);
            default:
                String errMsg = "Wrong «number of bytes» = " + bytes.length +
                        "! Available «number of bytes per int»: 4, 3, 2 or 1.";
                throw new IllegalArgumentException(errMsg);
        }
    }


    private static String byteToHexString(byte b) {
        return String.format("%02X ", b);
    }

    class NullMessageListener implements MessageListener {
        @Override
        public void onMessage(AdsMessageType messageType, String message) {
            // do nothing;
        }
    }

    class NullDataListener implements NumberedDataListener {
        @Override
        public void onDataReceived(int[] dataRecord, int dataRecordNumber) {
            // do nothing;
        }
    }
}
