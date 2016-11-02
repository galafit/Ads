package com.crostec.gui.comport_gui;


/**
 * Created by gala on 29/10/16.
 */
public class ComportDataProviderMock implements ComportDataProvider {
    int i = 0;
    @Override
    public String[] getAvailableComports() {
        i++;
        if(i%2 == 0) {
            String[] values = {"one", "two", "tree"};
            return values;
        }
        String[] values = {"раз", "два", "три"};
        return values;
    }
}
