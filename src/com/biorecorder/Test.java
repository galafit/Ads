package com.biorecorder;

import jssc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by galafit on 28/5/17.
 */
public class Test {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(500,500);
        frame.getContentPane().setLayout(new FlowLayout());
        String[] items = {"one"};
        JComboBox box = new JComboBox(items);
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("action "+box.getSelectedItem());
            }
        });
        frame.add(box);
        frame.setVisible(true);
        try {
            Thread.sleep(1000);
            System.out.println("set item one ");
            box.setSelectedItem("one");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    public void test() {
        //String[] names = SerialPortList.getPortNames();
        String[]  names = {"one", "two"};
        try {
            SerialPort comPort1 = new SerialPort(names[0]);
            comPort1.openPort();
            comPort1.setParams(460800,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);


            SerialPort   comPort2 = new SerialPort(names[0]);
            comPort2.openPort();

        } catch (SerialPortException e) {
            closeApplication();
        }
    }

    public static void closeApplication() {
        System.out.println("close app");
        System.exit(1);
    }
}
