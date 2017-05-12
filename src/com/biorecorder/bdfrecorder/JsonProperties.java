package com.biorecorder.bdfrecorder;

import com.biorecorder.ads.AdsChannelConfig;
import com.biorecorder.ads.AdsConfig;
import com.biorecorder.ads.Divider;
import com.biorecorder.ads.Sps;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;

/**
 * Created by gala on 04/05/17.
 */
public class JsonProperties {
    private File jsonFile;
    private ObjectMapper mapper = new ObjectMapper();

    public JsonProperties(File jsonFile) {
        this.jsonFile = jsonFile;
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
    }

    /**
     * Converts AdsConfig object to JSON and saves it in file
     * @param object
     * @throws IOException
     */
    public void saveCongfig(Object object) throws IOException {
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        ow.writeValue(jsonFile, object);


    }

    /**
     * Read AdsConfig object from JSON file
     * @return
     * @throws IOException
     */
    public Object getConfig(Class objectClass) throws IOException {
        return mapper.readValue(jsonFile, objectClass);
    }

    /**
     * Unit Test. Usage Example.
     * <p>
     * <br>1) Create AdsConfig object and write it to the JSON file.
     * <br>2) Read the resultant JSON file and create AdsConfig object from it
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        File jsonFile = new File(System.getProperty("user.dir"), "config.json");
        JsonProperties jsonProperties = new JsonProperties(jsonFile);

        // save BdfRecorderConfig object to JSON file
        try {
            AdsConfig adsConfig = new AdsConfig();
            adsConfig.setSps(Sps.S1000);
            AdsChannelConfig adsChannelConfig1 = adsConfig.getAdsChannel(0);
            AdsChannelConfig adsChannelConfig2 = adsConfig.getAdsChannel(1);
            adsChannelConfig1.setName("Channel_Name_1");
            adsChannelConfig1.setDivider(Divider.D2);
            adsChannelConfig2.setName("Channel_Name_2");
            adsChannelConfig2.setDivider(Divider.D5);

            BdfRecorderConfig bdfRecorderConfig = new BdfRecorderConfig();
            bdfRecorderConfig.setAdsConfig(adsConfig);

            jsonProperties.saveCongfig(bdfRecorderConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get BdfRecorderConfig object from JSON file
        try {
            BdfRecorderConfig bdfRecorderConfig = (BdfRecorderConfig) jsonProperties.getConfig(BdfRecorderConfig.class);
            AdsConfig adsConfig = bdfRecorderConfig.getAdsConfig();

            // print some info from resultant AdsConfig object
            System.out.println("Ads Config from file: ");
            System.out.println("sps = "+adsConfig.getSps().getValue());
            System.out.println("number of channels = "+adsConfig.getNumberOfAdsChannels());
            for(int i = 0; i < adsConfig.getNumberOfAdsChannels(); i++) {
                AdsChannelConfig adsChannel = adsConfig.getAdsChannel(i);
                System.out.println("  " + adsChannel.getName() + " divider = "+ adsChannel.getDivider().getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
