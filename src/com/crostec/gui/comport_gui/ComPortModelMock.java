package com.crostec.gui.comport_gui;


import com.crostec.ads.AdsConfiguration;

/**
 * Created by gala on 29/10/16.
 */
public class ComPortModelMock implements ComportModel {
    @Override
    public String[] getAvailableComports() {
        String[] values = {"one", "two", "tree"};
        return values;
    }
}
