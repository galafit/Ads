package com.biorecorder;

public interface StateChangeListener {
    void onStateChanged(StateChangeReason changeReason);
}
