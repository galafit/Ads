package com.biorecorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

public class JsonPreferences implements Preferences {
    private static final Log log = LogFactory.getLog(JsonPreferences.class);
    File propertyFile = new File(System.getProperty("user.dir"), "config.json");


    public AppConfig getConfig() {
        if (propertyFile.exists() && propertyFile.isFile()) {
            JsonProperties properties = new JsonProperties(propertyFile);
            try {
                AppConfig appConfig = (AppConfig) properties.getConfig(AppConfig.class);
                return appConfig;
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Error during property file reading: " + propertyFile + "! " + e.getMessage());
            }
        }
        return new AppConfig();
    }

    @Override
    public void saveConfig(AppConfig appConfig) {
        try {
            JsonProperties properties = new JsonProperties(propertyFile);
            properties.saveCongfig(appConfig);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
