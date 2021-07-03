package com.biorecorder.test;

import com.biorecorder.ads.ComportRuntimeException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;


public class ComportTest {
    public static void main(String[] args) {
        int COMPORT_SPEED = 460800;
        byte HELLO_REQUEST = (byte) (0xFD & 0xFF);
        SerialPort serialPort = new SerialPort("/dev/tty.usbserial-AH0604FV");
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(COMPORT_SPEED,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
            serialPort.addEventListener(new SerialPortEventListener() {
                @Override
                public void serialEvent(SerialPortEvent event) {
                    System.out.println("port event ");
                    if (event.isRXCHAR() && event.getEventValue() > 0) {
                        try {
                            byte[] buffer = serialPort.readBytes();
                            System.out.println("\nbuffer length "+buffer.length);
                            for (int i = 0; i < buffer.length; i++) {
                                System.out.println(buffer[i]);
                            }
                        } catch (SerialPortException ex) {
                            String errMsg = "Error during receiving serial port data: " + ex.getMessage();

                            throw new ComportRuntimeException(errMsg, ex);
                        }
                    }
                }
            });
            for (int i = 0; i < 10; i++) {
                serialPort.writeByte(HELLO_REQUEST);//Write data to port
                System.out.println("byte written to port:  " + i);
                Thread.sleep(1000);
            }

            /*
            byte[] buffer = serialPort.readBytes(1);//Read 1 bytes from serial port
            for (byte b : buffer) {
                System.out.println("byte" + b);
            }*/
            serialPort.closePort();//Close serial port
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
