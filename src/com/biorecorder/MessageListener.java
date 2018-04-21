package com.biorecorder;

/**
 * Created by galafit on 2/6/17.
 */
public interface MessageListener {
    public void showMessage(String message);
    public boolean askConfirmation(String message);
}
