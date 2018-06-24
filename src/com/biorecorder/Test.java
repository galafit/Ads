package com.biorecorder;

import com.biorecorder.ads.Ads;
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
        System.out.println(Ads.lithiumBatteryIntToPercentage(7800));
        System.out.println(Ads.lithiumBatteryIntToPercentage(7500));
        System.out.println(Ads.lithiumBatteryIntToPercentage(7240));
        System.out.println(Ads.lithiumBatteryIntToPercentage(6540));
    }
}