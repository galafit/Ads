package com.biorecorder.multisignal.edflib.example;

import com.biorecorder.multisignal.edflib.EdfReader;
import com.biorecorder.multisignal.edflib.EdfWriter;
import com.biorecorder.multisignal.recordformat.DataHeader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This example program opens the EDF-file records/ekg.edf
 * (that contains data from two measuring channels - cardiogram and accelerometer) and
 * copy its data to new file
 */
public class EdfExample {
    public static void main(String[] args) {
        String originalFilename = "ekg.bdf";
        File recordsDir = new File(System.getProperty("user.dir"), "records");
        File originalFile = new File(recordsDir, originalFilename);

        EdfReader originalFileReader = null;
        try {
            originalFileReader = new EdfReader(originalFile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        DataHeader header = originalFileReader.getHeader();
        // Print some header info from original file
        System.out.println(header);


/*****************************************************************************************
 *    Read «DIGITAL» DataRecords from file and write them to the new file ekgcopy1.bdf as it is
 *****************************************************************************************/
        File resultantFile1 = new File(recordsDir, "copy1"+originalFilename);
        try {
            EdfWriter fileWriter1 = new EdfWriter(resultantFile1, header);
            int originalDataRecordLength = header.getRecordSize();
            int[] intBuffer = new int[originalDataRecordLength];
            while (originalFileReader.readDataRecords(1, intBuffer) > 0) {
                // read digital DataRecord from the original file

                // write digital DataRecord to the new file
                fileWriter1.writeDataRecord(intBuffer);
            }
            fileWriter1.close();

            System.out.println("Test1: simple copy file record by record.");

            FileInputStream fs1 = new FileInputStream(originalFile);
            FileInputStream fs2 = new FileInputStream(resultantFile1);
            byte[] data1 = new byte[fs1.available()];
            byte[] data2 = new byte[fs2.available()];

            if(data1.length != data2.length) {
                throw new RuntimeException(" Test1 failed: original file length: "+ data1.length + ", resultant file length:" + data2.length);
            }
            for (int i = 0; i < data1.length; i++) {
                if(data1[i] != data2[i]) {
                    throw new RuntimeException(i + " Test1: original and resultant files are not equals");
                }
            }
            System.out.println("Test1 done! \n");

        } catch (Throwable e) {
            e.printStackTrace();
        }

/*****************************************************************************************
 *     Read data by samples (from both channels) and
 *     write them to the new file ekgcopy2.bdf
 *****************************************************************************************/
        File resultantFile2 = new File(recordsDir, "copy2"+originalFilename);
        try {
            EdfWriter fileWriter2 = new EdfWriter(resultantFile2, header);
            // set DataRecord and signals positions to 0;
            originalFileReader.reset();
            int signals = header.numberOfSignals();
            List<double[]> buffers = new ArrayList<>(signals);


            for (int i = 0; i < signals; i++) {
                buffers.add(new double[header.getNumberOfSamplesInEachDataRecord(i)]);
            }

            boolean isFinished = false;
            int counter = 0;

            while (!isFinished) {
                for (int i = 0; i < signals; i++) {
                    double[] buffer = buffers.get(i);
                    // read physical samples belonging to signal i from the original file
                    int numberOfReadBytes = originalFileReader.readPhysicalSamples(i, buffer.length, buffer);
                    if(numberOfReadBytes > 0) {
                        // write physical samples to the new file
                        fileWriter2.writePhysicalSamples(buffer);
                    } else {
                        isFinished = true;
                        break;
                    }
                }
            }
            fileWriter2.close();

            System.out.println("Test2: read data by samples (from both channels) and write them to new file");
            FileInputStream fs1 = new FileInputStream(originalFile);
            FileInputStream fs2 = new FileInputStream(resultantFile2);
            byte[] data1 = new byte[fs1.available()];
            byte[] data2 = new byte[fs2.available()];

            if(data1.length != data2.length) {
                throw new RuntimeException(" Test2 failed: original file length: "+ data1.length + ", resultant file length:" + data2.length);
            }
            for (int i = 0; i < data1.length; i++) {
                if(data1[i] != data2[i]) {
                    throw new RuntimeException(i + " Test2: original and resultant files are not equals");
                }
            }
            System.out.println("Test2 done! \n");

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
