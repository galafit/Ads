package com.biorecorder;

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
     * @throws IOException
     */
    public void saveConfig(Object object) throws IOException {
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();

        ow.writeValue(jsonFile, object);
    }

    /**
     * Read AdsConfig object from JSON file
     * @throws IOException
     */
    public Object getConfig(Class objectClass) throws IOException {
        return mapper.readValue(jsonFile, objectClass);
    }
}
