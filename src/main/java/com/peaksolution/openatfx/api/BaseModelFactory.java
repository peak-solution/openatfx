package com.peaksolution.openatfx.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BaseModelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(BaseModelFactory.class);

    /** The singleton instance */
    private static BaseModelFactory instance;
    
    private final Map<String, AtfxBaseModel> baseModelCache;
    
    /**
     * Non visible constructor.
     */
    private BaseModelFactory() {
        this.baseModelCache = new HashMap<>();
    }
    
    public AtfxBaseModel getBaseModel(String baseModelVersionString) {
        String baseModelVersion = prepareBaseModelVersionString(baseModelVersionString);
        int versionNumber = getVersionNumberFromString(baseModelVersion);

        AtfxBaseModel baseModel = this.baseModelCache.get(baseModelVersion);
        if (baseModel != null) {
            return baseModel;
        }

        int modelVersion = checkBaseModelVersion(versionNumber);
        String correctedModelVersionString = baseModelVersion.replace(String.valueOf(versionNumber), String.valueOf(modelVersion));
        BaseModelReader modelReader = getBaseModelReader(modelVersion);
        long start = System.currentTimeMillis();
        LOG.info("Reading base model for version {}...", correctedModelVersionString);
        baseModel = modelReader.getBaseModel(correctedModelVersionString);
        LOG.info("Read base model '{}' in {}ms", correctedModelVersionString, System.currentTimeMillis() - start);
        
        this.baseModelCache.put(baseModelVersion, baseModel);
        return baseModel;
    }

    /**
     * @param baseModelVersionString
     * @return
     */
    private String prepareBaseModelVersionString(String baseModelVersionString) {
        LOG.debug("Received baseModel version {} to initialize base model", baseModelVersionString);
        // tolerate base model version being specified as two digit number
        String baseModelVersion = baseModelVersionString.trim();
        if (baseModelVersion.length() == 2) {
            baseModelVersion = "asam" + baseModelVersion;
        }
        // tolerate upper case string
        return baseModelVersion.toLowerCase();
    }

    private int getVersionNumberFromString(String baseModelVersion) {
        int versionNumber = 0;
        if (baseModelVersion.length() == 6) {
            String versionNumberString = baseModelVersion.substring(baseModelVersion.length() - 2);
            try {
                versionNumber = Integer.parseInt(versionNumberString);
            } catch (NumberFormatException ex) {
                // tolerate if last two characters are no number
            }
        }
        return versionNumber;
    }

    private int checkBaseModelVersion(int baseModelVersion) {
        // check model version to be 29 or higher
        // use the model with the best compatibility as fallback, in case a lower model as supported was configured,
        // ODS should be backward compatible
        int versionNumber = baseModelVersion;
        if (versionNumber > 0 && versionNumber < 29) {
            LOG.warn("Configured base model {} is not supported, trying to fallback to version 30!", versionNumber);
            LOG.info("Base model version {} was configured, but as fallback the most compatible version 30 will be used!", versionNumber);
            versionNumber = 30;
        }
        return versionNumber;
    }
    
    /**
     * @param versionNumber
     * @return
     */
    private BaseModelReader getBaseModelReader(int versionNumber) {
        // use respective reader to parse the base model from file
        BaseModelReader modelReader = null;
        if (versionNumber >= 34) {
            modelReader = new NewBaseModelReader();
        } else {
            modelReader = new OldBaseModelReader();
        }
        return modelReader;
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static BaseModelFactory getInstance() {
        if (instance == null) {
            instance = new BaseModelFactory();
        }
        return instance;
    }
}
