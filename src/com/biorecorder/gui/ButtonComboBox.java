package com.biorecorder.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Permits to  attach action Events on the JComboBox «arrow JButton»
 * <br><a href="http://stackoverflow.com/questions/5057439/attach-action-event-on-jcombobox-arrow-jbutton">Attach action Event on JComboBox arrow JButton</a>
 */
public class ButtonComboBox<E> extends JComboBox<E> {
    private JButton arrButton;
    public ButtonComboBox(E[] items) {
        super(items);
        arrButton = getButtonSubComponent(this);
    }


    public JButton getButton() {
        return arrButton;
    }

    /**
     * extract the JButton component from an existing JComboBox (Container)
     */
    private static JButton getButtonSubComponent(Container container) {
        if (container instanceof JButton) {
            return (JButton) container;
        } else {
            Component[] components = container.getComponents();
            for (Component component : components) {
                if (component instanceof Container) {
                    return getButtonSubComponent((Container)component);
                }
            }
        }
        return null;
    }
}
