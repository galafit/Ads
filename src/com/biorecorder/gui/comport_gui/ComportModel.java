package com.biorecorder.gui.comport_gui;

import javax.swing.*;

/**
 * Created by gala on 29/10/16.
 */
class ComportModel extends DefaultComboBoxModel {
    private ComportDataProvider comportDataProvider;
    private String defaultComPort;
    String[] availableComports;

    public ComportModel(ComportDataProvider comportDataProvider, String defaultComPort) {
        this.comportDataProvider = comportDataProvider;
        this.defaultComPort = defaultComPort;
        availableComports = comportDataProvider.getAvailableComports();
        if(defaultComPort != null) {
            setSelectedItem(defaultComPort);
        }
        else if(getSize() > 0) {
            setSelectedItem(getElementAt(0));
        }
    }

    public boolean isComPortAvailable(String comPort) {
        availableComports = comportDataProvider.getAvailableComports();
        if(comPort != null) {
            for(String port :  availableComports){
                if ( port.equalsIgnoreCase(comPort)) {
                    return true;
                }
            }
        }
        return false;
    }



    public void update() {
        availableComports = comportDataProvider.getAvailableComports();
        fireContentsChanged(this, 0, getSize());
    }


    @Override
    public Object getElementAt(int index) {
        if(defaultComPort != null && ! isComPortAvailable(defaultComPort)) {
            if(index == 0) {
                return defaultComPort;
            }
            else {
                return (availableComports[index - 1]);
            }
        }

        return (availableComports[index]);
    }

    @Override
    public int getSize() {
        int availableComportNumber = 0;
        if(availableComports != null) {
            availableComportNumber = availableComports.length;
        }

        if(defaultComPort != null & !isComPortAvailable(defaultComPort)) {
            availableComportNumber++;
        }

        return availableComportNumber;
    }
}

