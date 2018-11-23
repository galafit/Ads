package com.biorecorder.edflib.example;

import com.biorecorder.edflib.EdfHeader;
import com.biorecorder.edflib.EdfReader;
import com.biorecorder.edflib.EdfWriter;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * This example program opens the EDF-file records/ekg.edf
 * (that contains data from two measuring channels - cardiogram and accelerometer) and
 * copy its data to new file
 */
public class EdfExample {
    public static void main(String[] args) {
        File recordsDir = new File(System.getProperty("user.dir"), "records");
        File originalFile = new File(recordsDir, "ekg.bdf");

        EdfReader originalFileReader = null;
        try {
            originalFileReader = new EdfReader(originalFile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        EdfHeader header = originalFileReader.getHeader();
        // Print some header info from original file
        System.out.println(header);


/*****************************************************************************************
 *    Read «DIGITAL» DataRecords from file and write them to the new file ekgcopy1.bdf as it is
 *****************************************************************************************/
        File resultantFile1 = new File(recordsDir, "ekgcopy1.bdf");
        try {
            EdfWriter fileWriter1 = new EdfWriter(resultantFile1, header);
            int originalDataRecordLength = header.getDataRecordSize();
            int[] intBuffer = new int[originalDataRecordLength];
            while (originalFileReader.readDigitalRecords(1, intBuffer) > 0) {
                // read digital DataRecord from the original file

                // write digital DataRecord to the new file
                fileWriter1.writeDigitalRecord(intBuffer);
            }
            fileWriter1.close();

            System.out.println("Test1: simple copy file record by record.");

            FileInputStream fs1 = new FileInputStream(originalFile);
            FileInputStream fs2 = new FileInputStream(resultantFile1);
            byte[] data1 = new byte[fs1.available()];
            byte[] data2 = new byte[fs2.available()];

            if (!Arrays.equals(data1, data2)) {
                throw new RuntimeException("Test1: original and resultant files are not equals.");
            } else {
                System.out.println("Test1 done! \n");
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

/*****************************************************************************************
 *     Read data by samples (from both channels) and
 *     write them to the new file ekgcopy2.bdf
 *****************************************************************************************/
        File resultantFile2 = new File(recordsDir, "ekgcopy2.bdf");
        try {
            EdfWriter fileWriter2 = new EdfWriter(resultantFile2, header);
            // set DataRecord and signals positions to 0;
            originalFileReader.reset();
            int samples0 = header.getNumberOfSamplesInEachDataRecord(0);
            int samples1 = header.getNumberOfSamplesInEachDataRecord(1);
            double[] buffer0 = new double[samples0];
            double[] buffer1 = new double[samples1];

            while (originalFileReader.readPhysicalSamples(0, samples0, buffer0) > 0) {

                // read physical samples belonging to signal 1 from the original file
                originalFileReader.readPhysicalSamples(1, samples1, buffer1);
                // write physical samples to the new file
                fileWriter2.writePhysicalSamples(buffer0);
                fileWriter2.writePhysicalSamples(buffer1);
            }
            fileWriter2.close();

            System.out.println("Test2: read data by samples (from both channels) and write them to new file");
            FileInputStream fs1 = new FileInputStream(originalFile);
            FileInputStream fs2 = new FileInputStream(resultantFile1);
            byte[] data1 = new byte[fs1.available()];
            byte[] data2 = new byte[fs2.available()];
            if (!Arrays.equals(data1, data2)) {
                throw new RuntimeException("Test2: original and resultant files are not equals.");
            } else {
                System.out.println("Test2 done! \n");
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
