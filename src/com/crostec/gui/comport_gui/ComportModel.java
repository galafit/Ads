package com.crostec.gui.comport_gui;

import javax.swing.*;

/**
 * Created by gala on 29/10/16.
 */
class ComportModel extends DefaultComboBoxModel {
    private ComportDataProvider comportDataProvider;
    private String defaultComPort;

    public ComportModel(ComportDataProvider comportDataProvider, String defaultComPort) {
        this.comportDataProvider = comportDataProvider;
        this.defaultComPort = defaultComPort;
        if(defaultComPort != null) {
            setSelectedItem(defaultComPort);
        }
        else if(getSize() > 0) {
            setSelectedItem(getElementAt(0));
        }

    }

    public boolean isComPortAvailable(String comPort) {
        if(comPort != null) {
            for(String port : comportDataProvider.getAvailableComports()){
                if ( port.equals(comPort)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void update() {
        fireContentsChanged(this, 0, getSize());
    }


    @Override
    public Object getElementAt(int index) {
        if(defaultComPort != null && ! isComPortAvailable(defaultComPort)) {
            if(index == 0) {
                return defaultComPort;
            }
            else {
                return (comportDataProvider.getAvailableComports()[index - 1]);
            }
        }

        return (comportDataProvider.getAvailableComports()[index]);
    }

    @Override
    public int getSize() {
        int availableComportNumber = 0;
        if(comportDataProvider.getAvailableComports() != null) {
            availableComportNumber = comportDataProvider.getAvailableComports().length;
        }

        if(defaultComPort != null & !isComPortAvailable(defaultComPort)) {
            availableComportNumber++;
        }

        return availableComportNumber;
    }
}

