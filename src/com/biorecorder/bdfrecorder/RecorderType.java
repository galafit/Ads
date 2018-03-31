package com.biorecorder.bdfrecorder;


/**
 * Created by galafit on 30/3/18.
 */
public enum RecorderType {
    RECORDER_2(2),
    RECORDER_8(8);

    private int channelsCount;

    private RecorderType(int channelsCount) {
        this.channelsCount = channelsCount;
    }

    public int getChannelsCount() {
        return channelsCount;
    }

    public static RecorderType valueOf(int channelsCount) throws IllegalArgumentException {
        for (RecorderType recorderType : RecorderType.values()) {
            if(recorderType.getChannelsCount() == channelsCount) {
                return recorderType;
            }

        }
        String msg = "Invalid channels count: "+channelsCount+ ". Number of channels should be 2 or 8";
        throw new IllegalArgumentException(msg);
    }

}
