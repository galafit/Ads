package com.biorecorder.comport;

/**
 * Created by galafit on 21/6/18.
 */
public interface Comport {
    String getComportName();

    boolean isOpened();

    boolean close();

    boolean writeBytes(byte[] bytes) throws IllegalStateException;

    boolean writeByte(byte b) throws IllegalStateException;

    void addListener(ComportListener comportListener);

    void removeListener();
}
