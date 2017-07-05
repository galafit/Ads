package com.biorecorder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class JsonPreferences implements Preferences {
    private static final Log log = LogFactory.getLog(JsonPreferences.class);
    private static final String fileName = "config.json";

    public AppConfig getConfig() {
        File propertyFile = getPropertyFileInProjectDir();
        if(!propertyFile.exists() || ! propertyFile.isFile()) {
            propertyFile = getPropertyFileInHomeDir();
        }

        if (propertyFile.exists() && propertyFile.isFile()) {
            JsonProperties properties = new JsonProperties(propertyFile);
            try {
                AppConfig appConfig = (AppConfig) properties.getConfig(AppConfig.class);
                return appConfig;
            } catch (IOException e) {
                log.error("Error during property file reading: " + propertyFile, e);
            }
        }
        return new AppConfig();
    }

    @Override
    public void saveConfig(AppConfig appConfig) {
        File propertyFile = getPropertyFileInProjectDir();
        if(!propertyFile.exists() || ! propertyFile.isFile()) {
            try {
                propertyFile.createNewFile();
            } catch (IOException e) { // if we can´t create file in project dir, we create it in home dir
                propertyFile = getPropertyFileInHomeDir();
                if(!propertyFile.exists() || ! propertyFile.isFile()) {
                    try {
                        propertyFile.createNewFile();
                    } catch (IOException e1) {
                        String errMsg = "Error during creating property file: " + propertyFile;
                        log.error(errMsg, e1);
                    }
                }

            }
        }
        try {
            JsonProperties properties = new JsonProperties(propertyFile);
            properties.saveCongfig(appConfig);

        } catch (IOException e) {
            String errMsg = "Error during saving app config in json file: " + propertyFile;
            log.error(errMsg, e);
        }
    }

    private File getPropertyFileInProjectDir() {
        return new File(getProjectDir(), fileName);
    }


    private File getPropertyFileInHomeDir() {
        String projectDir = getProjectDir();
        String homeDir = getUserHomeDir();
        String separator = File.separator;
        // to avoid file names matches in user home dir we include "projectDir"
        // as a part of the name of property file
        StringBuilder fullFileName = new StringBuilder(projectDir.replace(separator.charAt(0),'_'));
        fullFileName.append("_").append(fileName);
        File propertyFile = new File(homeDir, fullFileName.toString());

        // on windows file is made hidden in different way
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") >= 0) { // windows
            Path path = Paths.get(propertyFile.toString());
            try {
                Files.setAttribute(path, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
            } catch (IOException e) {
                log.error("Error during making property file hidden");
            }
        }  else {
            // start file with '.' to make file hidden on mac and unix system
            fullFileName.setCharAt(0, '.');
            propertyFile = new File(homeDir, fullFileName.toString());
        }

        return  propertyFile;
    }


    private String getProjectDir() {
        return System.getProperty("user.dir");
    }

    private String getUserHomeDir() {
        return System.getProperty("user.home");
    }
}
