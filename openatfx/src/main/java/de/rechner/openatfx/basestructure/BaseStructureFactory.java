package de.rechner.openatfx.basestructure;

import java.util.HashMap;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.BaseStructure;
import org.omg.CORBA.ORB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory object used to retrieve <code>org.asam.ods.BaseStructure</code> objects.
 * 
 * @author Markus Renner
 */
public class BaseStructureFactory {
    private static final Logger LOG = LoggerFactory.getLogger(BaseStructureFactory.class);

    /** The singleton instance */
    private static BaseStructureFactory instance;

    /** The base structure cache. */
    private final Map<String, BaseStructure> baseStructureCache;

    /**
     * Non visible constructor.
     */
    private BaseStructureFactory() {
        this.baseStructureCache = new HashMap<String, BaseStructure>();
    }
    
    /**
     * Returns the ASAM ODS base structure object for given base model version.
     * <p>
     * The base structure objects will be cached for each base model version.
     * 
     * @param orb The ORB.
     * @param baseModelVersionString The base model version.
     * @return The base structure.
     * @throws AoException Error getting base structure.
     */
    public BaseStructure getBaseStructure(ORB orb, String baseModelVersionString) throws AoException {
        LOG.debug("Received baseModel version " + baseModelVersionString + " to initialize base model");
        // tolerate base model version being specified as two digit number
        String baseModelVersion = baseModelVersionString.trim();
        if (baseModelVersion.length() == 2) {
            baseModelVersion = "asam" + baseModelVersion;
        }
        // tolerate upper case string
        baseModelVersion = baseModelVersion.toLowerCase();

        BaseStructure baseStructure = this.baseStructureCache.get(baseModelVersion);
        if (baseStructure != null) {
            return baseStructure;
        }
        
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
        long start = System.currentTimeMillis();
        baseStructure = modelReader.getBaseModel(orb, baseModelVersion);
        LOG.info("Read base model '" + baseModelVersion + "' in " + (System.currentTimeMillis() - start) + "ms");
        
        this.baseStructureCache.put(baseModelVersion, baseStructure);
        return baseStructure;
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static BaseStructureFactory getInstance() {
        if (instance == null) {
            instance = new BaseStructureFactory();
        }
        return instance;
    }
}
