package com.biorecorder;

import com.biorecorder.gui.file_gui.FileToSaveUI;
import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TimerTask;

/**
 * Created by galafit on 28/5/17.
 */
public class Test {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        frame.add(new FileToSaveUI());
        frame.pack();
        frame.setVisible(true);

    }
}