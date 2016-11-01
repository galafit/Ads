package com.crostec.gui.comport_gui;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

/**
 * Created by gala on 29/10/16.
 */
public class ComportUI extends JPanel {
    private ComportModel comportModel;
    private JComboBox comPort;
    private String labelText = "ComPort:  ";
    private String currentComPort;


    public ComportUI(ComportModel comportModel) {
        this.comportModel = comportModel;
        int hgap = 0;
        int vgap = 0;
        setLayout(new FlowLayout(FlowLayout.LEFT, hgap, vgap));

        comPort = new JComboBox();

// чтобы отследить подключение новых приборов каждый раз при открытии ComboBox
// обновляется  список всех доступных портов
        comPort.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                loadData();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });

        add(new Label(labelText));
        add(comPort);

        loadData();
    }

    public void setCurrentPort(String comPortName) {
        currentComPort = comPortName;
        loadData();
    }

    public String getComPortName() {
        return (String) comPort.getSelectedItem();
    }


    private void loadData() {
        DefaultComboBoxModel model = new DefaultComboBoxModel(comportModel.getAvailableComports());
        comPort.setModel( model );
        if(currentComPort != null){
            comPort.setSelectedItem(currentComPort);
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        comPort.setEnabled(isEnabled);
    }
}