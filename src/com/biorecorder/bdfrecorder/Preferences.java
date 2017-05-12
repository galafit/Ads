package com.biorecorder.bdfrecorder;

/**
 * Created by gala on 11/05/17.
 */
public interface Preferences {
    public BdfRecorderConfig getConfig();
    public void saveConfig(BdfRecorderConfig bdfRecorderConfig);
}
