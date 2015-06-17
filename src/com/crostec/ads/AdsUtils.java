package com.crostec.ads;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AdsUtils {
    /**
     * returns dividers list for all active channels including 3 accelerometer channels
     */
    public static List<Integer> getDividersForActiveChannels(AdsConfiguration adsConfiguration) {
        List<Integer> dividersList = new ArrayList<Integer>();
        for (AdsChannelConfiguration channelConfiguration : adsConfiguration.getAdsChannels()) {
            if (channelConfiguration.isEnabled()) {
                dividersList.add(channelConfiguration.getDivider().getValue());
            }
        }
        for (int i = 0; i < 3; i++) {
            if (adsConfiguration.isAccelerometerEnabled()) {
                dividersList.add(adsConfiguration.getAccelerometerDivider().getValue());
            }
        }
        if (adsConfiguration.isBatteryVoltageMeasureEnabled()) {
                dividersList.add(10);
        }
        return dividersList;
    }

     public static int getDecodedFrameSize(AdsConfiguration adsConfiguration) {
        int frameSize = 0;
        for (Integer divider : getDividersForActiveChannels(adsConfiguration)) {
            frameSize += adsConfiguration.getDeviceType().getMaxDiv().getValue() / divider;
        }
        return frameSize + 2; // 2 values for device specific information (counter of loff status);
    }

    /**
     * convert int data format to 24 bit (3 bytes) data format valid for Bdf and
     * change Big_endian (java)  to Little_endian (for bdf)
     */
    public static byte[] to24BitLittleEndian(int value) {
        int sizeOfInt = 4;
        ByteBuffer byteBuffer = ByteBuffer.allocate(sizeOfInt).putInt(value);
        byte[] result = new byte[3];
        result[0] = byteBuffer.get(sizeOfInt - 1);
        result[1] = byteBuffer.get(sizeOfInt - 2);
        result[2] = byteBuffer.get(sizeOfInt - 3);
        return result;
    }

    /* Java int BIG_ENDIAN, Byte order: LITTLE_ENDIAN  */
    public static int bytesToSignedInt(byte... b) {
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
     * if the String.length() is more then the given length we cut the String
     * if the String.length() is less then the given length we append spaces to the end of the String
     */
    public static String adjustLength(String text, int length) {
        StringBuilder sB = new StringBuilder(text);
        if (text.length() > length) {
            sB.delete((length + 1), text.length());
        } else {
            for (int i = text.length(); i < length; i++) {
                sB.append(" ");
            }
        }
        return sB.toString();
    }
}
