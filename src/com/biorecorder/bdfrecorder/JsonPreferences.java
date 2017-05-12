package com.biorecorder.bdfrecorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

public class JsonPreferences implements Preferences {
    private static final Log log = LogFactory.getLog(JsonPreferences.class);
    File propertyFile = new File(System.getProperty("user.dir"), "config.json");


    @Override
    public BdfRecorderConfig getConfig() {
        if (propertyFile.exists() && propertyFile.isFile()) {
            JsonProperties properties = new JsonProperties(propertyFile);
            try {
                BdfRecorderConfig bdfRecorderConfig = (BdfRecorderConfig) properties.getConfig(BdfRecorderConfig.class);
                return bdfRecorderConfig;
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Problem with property file reading: " + propertyFile + "! " + e.getMessage());
            }
        }
        return new BdfRecorderConfig();
    }

    @Override
    public void saveConfig(BdfRecorderConfig bdfRecorderConfig) {
        try {
            JsonProperties properties = new JsonProperties(propertyFile);
            properties.saveCongfig(bdfRecorderConfig);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
