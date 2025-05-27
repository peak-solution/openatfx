package com.peaksolution.openatfx;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.asam.ods.ErrorCode;

import com.peaksolution.openatfx.api.ApiFactory;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.api.OpenAtfxException;


/**
 * Main entry point of openAtfx library for non-Corba use cases.
 * 
 * @author Markus Renner
 */
public class OpenAtfx {

    private final Properties properties;

    public OpenAtfx() {
        this(new Properties());
    }

    public OpenAtfx(Properties properties) {
        this.properties = properties;
    }

    /**
     * Adds the given configuration property.
     * 
     * @param key 
     * @param value
     */
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Loads the given atfx file and parses its data. Returns the OpenAtfxAPI to access this file.
     * 
     * @param path the path to the atfx file to load, has to be a valid existing file path!
     * @return the initialized OpenAtfxAPI instance.
     */
    public OpenAtfxAPI openFile(String path) {
        return openFile(Paths.get(path));
    }

    /**
     * Loads the given atfx file and parses its data. Returns the OpenAtfxAPI to access this file.
     * 
     * @param path the path to the atfx file to load, has to be a valid existing file path!
     * @return the initialized OpenAtfxAPI instance.
     */
    public OpenAtfxAPI openFile(Path path) {
        return openFile(new LocalFileHandler(), path);
    }
    
    /**
     * Accepts an external file handler (e.g. from openatfx mdf library), loads the given atfx file
     * and parses its data. Returns the OpenAtfxAPI to access this file.
     * 
     * @param fileHandler the IFileHandler implementation, which may provide a filestream of a different
     * file to read, than it actually specifies as root and filename.
     * @param path the path to the file to load, has to be a valid existing file path!
     * @return the initialized OpenAtfxAPI instance.
     */
    public OpenAtfxAPI openFile(IFileHandler fileHandler, Path path) {
        ApiFactory apiFactory = new ApiFactory();
        return apiFactory.getApiForExistingFile(fileHandler, path, properties);
    }

    /**
     * Initializes an empty atfx file with the given file path and the given ODS base model version number. Returns the
     * OpenAtfxAPI to access this file.
     * 
     * @param path the path to the atfx file to create. The file will be created if not existing.
     * @param baseModelVersionNr any ODS model version number between 29 and the latest version. If any other number is
     *            provided, default fallback to latest supported ODS base model version.
     * @return the initialized OpenAtfxAPI instance.
     */
    public OpenAtfxAPI createNewFile(Path path, int baseModelVersionNr) {
        // make sure the file exists before initializing API on it
        try {
            path.toFile().getParentFile().mkdirs();
            boolean created = path.toFile().createNewFile();
            if (!created) {
                throw new OpenAtfxException(ErrorCode.AO_CONNECT_FAILED, "The new atfx file '" + path
                        + "' could not be created, because it already exists!");
            }
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Error trying to create the file " + path);
        }

        ApiFactory apiFactory = new ApiFactory();
        return apiFactory.getApiForNewFile(path, properties, baseModelVersionNr);
    }
}
