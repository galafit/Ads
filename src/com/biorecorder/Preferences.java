package com.biorecorder;

import com.biorecorder.bdfrecorder.BdfRecorderConfig;

/**
 * Created by gala on 11/05/17.
 */
public interface Preferences {
    public AppConfig getConfig();
    public void saveConfig(AppConfig appConfig);
}
