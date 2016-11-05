package com.biorecorder.comport;


import com.biorecorder.gui.comport_gui.ComportDataProvider;

/**
 * Created by gala on 29/10/16.
 */
public class ComportFacade implements ComportDataProvider {
    @Override
    public String[] getAvailableComports() {
        return ComPort.getportNames();
    }
}