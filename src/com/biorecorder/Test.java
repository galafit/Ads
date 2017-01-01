package com.biorecorder;

import com.biorecorder.edflib.filters.SignalAveragingFilter;
import com.biorecorder.filter.MovingAveragePreFilter;

import java.util.Random;

public class Test {
    public static void main(String[] args) {
        Random r = new Random();
        int randomInt = r.nextInt();
        MovingAveragePreFilter sasha = new MovingAveragePreFilter(10);
        SignalAveragingFilter gala = new SignalAveragingFilter(10);
        for (int i = 0; i < 1000; i++) {
            randomInt = r.nextInt();
            if (sasha.getFilteredValue(randomInt) != gala.getFilteredValue(randomInt)) {
                System.out.println("bug");
            }
        }
        System.out.println(sasha.getFilteredValue(randomInt) +"  "+ gala.getFilteredValue(randomInt));
    }
}
