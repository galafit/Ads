package com.biorecorder;

/**
 * Created by galafit on 26/3/18.
 */
public class TestExeptions {
    public static void main(String[] args) {
        System.out.println("point 1");
        boolean b = true;
        if(b) {
            throw new RuntimeException("test exception");
        }
        System.out.println("point 2");
    }
}
