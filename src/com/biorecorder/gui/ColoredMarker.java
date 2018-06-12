package com.biorecorder.gui;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class ColoredMarker extends JLabel {

    private Color  backgroundColor;
    private Dimension defaultDimension = new Dimension(10,10);

    public ColoredMarker(Color backgroundColor) {
        setPreferredSize(defaultDimension);
        setOpaque(true);
        setBackground(backgroundColor);
    }

    public ColoredMarker() {
        setPreferredSize(defaultDimension);
    }


    public ColoredMarker(Dimension dimension) {
        setPreferredSize(dimension);
    }

    public ColoredMarker(Icon icon) {
        setIcon(icon);
        if(icon != null){
            setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        }
    }

    public void setIcon(Icon icon) {
        super.setIcon(icon);
    }

    public void setColor(Color color) {
        setBackground(color);
        setIcon(null);
    }
}
