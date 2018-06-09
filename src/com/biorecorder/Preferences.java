package com.biorecorder;

/**
 * Created by galafit on 30/3/18.
 */
public interface Preferences {
    public void saveConfig(AppConfig appConfig);
    public AppConfig getConfig();
}
