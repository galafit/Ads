package com.biorecorder;

import com.biorecorder.bdfrecorder.BdfRecorderConfig;
import com.biorecorder.bdfrecorder.BdfRecorderConfig1;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by galafit on 30/3/18.
 */
public class AppConfig1 {
    private BdfRecorderConfig1 bdfRecorderConfig = new BdfRecorderConfig1();
    private String comportName;
    private String dirToSave;
    @JsonIgnore
    private String fileName;

    public BdfRecorderConfig1 getBdfRecorderConfig() {
        return bdfRecorderConfig;
    }

    public void setBdfRecorderConfig(BdfRecorderConfig1 bdfRecorderConfig) {
        this.bdfRecorderConfig = bdfRecorderConfig;
    }

    public String getComportName() {
        return comportName;
    }

    public void setComportName(String comportName) {
        this.comportName = comportName;
    }

    public String getDirToSave() {
        return dirToSave;
    }

    public void setDirToSave(String dirToSave) {
        this.dirToSave = dirToSave;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
