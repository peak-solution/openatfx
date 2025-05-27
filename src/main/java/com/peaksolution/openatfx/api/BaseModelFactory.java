package com.peaksolution.openatfx.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        AtfxBaseModel baseModel = this.baseModelCache.get(baseModelVersion);
        if (baseModel != null) {
            return baseModel;
        }
        
        BaseModelReader modelReader = getBaseModelReader(baseModelVersion);
        long start = System.currentTimeMillis();
        baseModel = modelReader.getBaseModel(baseModelVersion);
        LOG.info("Read base model '" + baseModelVersion + "' in " + (System.currentTimeMillis() - start) + "ms");
        
        this.baseModelCache.put(baseModelVersion, baseModel);
        return baseModel;
    }
    
    /**
     * @param baseModelVersionString
     * @return
     */
    private String prepareBaseModelVersionString(String baseModelVersionString) {
        LOG.debug("Received baseModel version " + baseModelVersionString + " to initialize base model");
        // tolerate base model version being specified as two digit number
        String baseModelVersion = baseModelVersionString.trim();
        if (baseModelVersion.length() == 2) {
            baseModelVersion = "asam" + baseModelVersion;
        }
        // tolerate upper case string
        return baseModelVersion.toLowerCase();
    }
    
    /**
     * @param baseModelVersion
     * @return
     */
    private BaseModelReader getBaseModelReader(String baseModelVersion) {
        // transform version to number for easier check 
        int versionNumber = 0;
        if (baseModelVersion.length() == 6) {
            String versionNumberString = baseModelVersion.substring(baseModelVersion.length() - 2);
            try {
                versionNumber = Integer.valueOf(versionNumberString);
            } catch (NumberFormatException ex) {
                // tolerate if last two characters are no number
            }
        }

        // check model version to be 29 or higher
        // use the model with the best compatibility as fallback, in case a lower model as supported was configured,
        // ODS should be backward compatible
        if (versionNumber > 0 && versionNumber < 29) {
            LOG.warn("Configured base model " + baseModelVersion + " is not supported, trying to fallback to version 30!");
            LOG.info("Base model version " + versionNumber
                    + " was configured, but as fallback the most compatible version 30 will be used!");
            baseModelVersion = "asam30.xml";
            versionNumber = 30;
        }
        
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
