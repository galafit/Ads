package com.crostec.ads;


abstract class JoinFramesUtility implements AdsDataListener {

    private AdsConfiguration adsConfiguration;
    private int numberOfFramesToJoin;
    private int[] joinedFrame;
    private int inputFramesCounter;

    protected JoinFramesUtility(AdsConfiguration adsConfiguration) {
        this.numberOfFramesToJoin = adsConfiguration.getSps().getValue() / AdsChannelConfiguration.MAX_DIV.getValue(); // 1 second duration of a data record in bdf file
        this.adsConfiguration = adsConfiguration;
        joinedFrame = new int[getJoinedFrameSize(numberOfFramesToJoin, adsConfiguration)];
    }

    @Override
    public void onAdsDataReceived(int[] dataFrame) {
        int channelPosition = 0;
        for (int divider : AdsUtils.getDividersForActiveChannels(adsConfiguration)) {
            int channelSampleNumber = AdsChannelConfiguration.MAX_DIV.getValue() / divider;
            for (int j = 0; j < channelSampleNumber; j++) {
                joinedFrame[channelPosition * numberOfFramesToJoin + inputFramesCounter * channelSampleNumber + j] = dataFrame[channelPosition + j];
            }
            channelPosition += channelSampleNumber;
        }
        inputFramesCounter++;
        if (inputFramesCounter == numberOfFramesToJoin) {  // when edfFrame is ready
            inputFramesCounter = 0;
            notifyListeners(joinedFrame);
        }
    }

    private int getJoinedFrameSize(int numberOfFramesToJoin, AdsConfiguration adsConfiguration) {
        int result = 0;
        for (int divider : AdsUtils.getDividersForActiveChannels(adsConfiguration)) {
            result += AdsChannelConfiguration.MAX_DIV.getValue() / divider;
        }
        return result * numberOfFramesToJoin;
    }

    public abstract void notifyListeners(int[] joinedFrame);
}
