package com.biorecorder.gui.file_gui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

/**
 * Created by gala on 01/11/16.
 */
public class DirectoryField extends JComboBox {

    private JFileChooser fileChooser;

    public DirectoryField() {
        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a directory to save");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories";
            }
        });

        for (Component component : getComponents()) {
            // cancel the popup menu opening from MouseClicks
            for (MouseListener listener : component.getMouseListeners()) {
                component.removeMouseListener(listener);

            }
            // instead open JFileChooser
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    int fileChooserState = fileChooser.showOpenDialog(DirectoryField.this);
                    if (fileChooserState == JFileChooser.APPROVE_OPTION) {
                        String dir = fileChooser.getSelectedFile().getAbsolutePath();
                        //parent.setDirectoryToSave(dir);
                        DirectoryField.this.removeAllItems();
                        DirectoryField.this.addItem(fileChooser.getSelectedFile());
                    }
                }
            });

        }

        // disable the popup menu opening from Key-Event Alt-Down
        // which opens the popup menu even if remove all the key listeners and clear the action map
        setFocusable(false);
    }

    public DirectoryField(String dirName) {
        this();
        setDirectory(dirName);
    }



    public void setDirectory(String dirName) {
        if(dirName!=null) {
            File directory = new File(dirName);
            if(directory.isFile()) {
                dirName = directory.getParent();
            }
            else {
                dirName = directory.getPath();
            }
            addItem(dirName);
            setSelectedItem(dirName);
            fileChooser.setCurrentDirectory(new File(dirName));
        }
    }


    public String getDirectory() {
        if(getSelectedItem()!= null) {
            return getSelectedItem().toString();
        }
        return null;
    }

    public void setLength(int length) {
        char[] prototype = new char[length];
        for(int i = 0; i < length; i++) {
            prototype[i] = 255;
        }
        setPrototypeDisplayValue(new String(prototype));
    }

}
