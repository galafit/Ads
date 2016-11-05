package com.biorecorder.gui.comport_gui;

import com.sun.istack.internal.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

/**
 * Created by gala on 29/10/16.
 */
public class ComportUI extends JPanel {
    private ComportModel comportModel;
    private JComboBox comportComboBox;
    private String labelText = "ComPort:  ";

    public ComportUI(ComportDataProvider comportDataProvider, @Nullable  String defaultComport) {
        this.comportModel = new ComportModel(comportDataProvider, defaultComport);
        int hgap = 0;
        int vgap = 0;
        setLayout(new FlowLayout(FlowLayout.LEFT, hgap, vgap));

        comportComboBox = new JComboBox(comportModel);

// чтобы отследить подключение новых приборов каждый раз при открытии ComboBox
// он перерисовывается чтобы отобразить список всех доступных портов
        comportComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // obligate comportComboBox to synchronize with its model
                // to show  available comports at the current moment
                comportModel.update();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        add(new Label(labelText));
        add(comportComboBox);

    }


    public String getCurrentComport() {
        return (String) comportComboBox.getSelectedItem();
    }

    public boolean isCurrentComportAvailable() {
        return comportModel.isComPortAvailable(getCurrentComport());
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        comportComboBox.setEnabled(isEnabled);
    }
}