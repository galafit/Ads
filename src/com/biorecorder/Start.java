package com.biorecorder;

import com.biorecorder.gui.MainFrame;
import com.biorecorder.gui.RecorderView;
import com.biorecorder.gui.RecorderViewModel;

import java.util.Set;

public class Start {
    public static void main(String[] args) {
      /*  int[] arr1 = new int[500 * 8];
        int[] arr2 = new int[500 * 8];
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            for (int i1 = 0; i1 < arr1.length; i1++) {
                arr2[i1] = arr1[i1];
                int k = i1 + i;
                if(i1 < i) {
                    k++;
                }
                arr2[i1] = k;
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("time ms "+ (endTime - startTime));*/



        JsonPreferences preferences = new JsonPreferences();
        RecorderViewModel bdfRecorder = new RecorderViewModelImpl(new EdfBioRecorderApp(), preferences);
        MainFrame recorderView = new MainFrame(bdfRecorder);
        bdfRecorder.addProgressListener(recorderView);
        bdfRecorder.addAvailableComportsListener(recorderView);
        bdfRecorder.addStateChangeListener(recorderView);

    }
}
