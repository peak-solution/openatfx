package com.peaksolution.openatfx.api.corba;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.AoSessionHelper;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.api.ApiFactory;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.basestructure.BaseStructureFactory;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Object for reading ATFX files.
 * 
 * @author Christian Rechner
 */
public class CorbaAtfxReader {
    private static final Logger LOG = LoggerFactory.getLogger(CorbaAtfxReader.class);

    /** cached model information for faster parsing */
    private final Map<String, String> documentation;
    private final Map<String, String> files;
    private final Map<String, ApplicationElement> applElems;
    private final Map<String, Map<String, ApplicationAttribute>> applAttrs; // aeName, aaName, aa
    private final Map<String, Map<String, ApplicationRelation>> applRels; // aeName, relName, rel
    
    private POA modelPOA;
    private boolean isExtendedCompatiblityMode;
    private OpenAtfxAPIImplementation api;
    private AoSessionImpl sessionImpl;
    private AoSession session;
    
    public CorbaAtfxReader() {
        this.documentation = new HashMap<>();
        this.files = new HashMap<>();
        this.applElems = new HashMap<>();
        this.applAttrs = new HashMap<>();
        this.applRels = new HashMap<>();
        
        System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");
    }
    
    public OpenAtfxAPIImplementation getApi() {
        return api;
    }
    
    public AoSession getSession() {
        return session;
    }
    
    protected AoSessionImpl getSessionImpl() {
        return sessionImpl;
    }
    
    /**
     * @param orb
     * @param fileHandler
     * @param path
     * @param extraContext
     * @throws AoException
     */
    public synchronized void init(ORB orb, IFileHandler fileHandler, Path path, NameValue...extraContext) throws AoException {
        if (session == null) {
            sessionImpl = createSessionImplForATFX(orb, fileHandler, path, extraContext);
            try {
                session = AoSessionHelper.narrow(getModelPOA(orb).servant_to_reference(sessionImpl));
            } catch (ServantNotActive | WrongPolicy  e) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            }
        }
    }

    /**
     * Returns the ASAM ODS AoSession for an ATFX file.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler for file system abstraction.
     * @param path The full path to the file to open.
     * @param extraContext additional optional context values to set in the session.
     * @return The aoSession object.
     * @throws AoException Error getting aoSession.
     */
    synchronized AoSessionImpl createSessionImplForATFX(ORB orb, IFileHandler fileHandler, Path path, NameValue...extraContext)
            throws AoException {
      long start = System.currentTimeMillis();
      
      Properties properties = new Properties();
      for (NameValue nv : extraContext) {
          if (org.asam.ods.DataType.DT_STRING == nv.value.u.discriminator()) {
              properties.put(nv.valName, nv.value.u.stringVal());
          } else if (org.asam.ods.DataType.DT_LONG == nv.value.u.discriminator()) {
              properties.put(nv.valName, String.valueOf(nv.value.u.longVal()));
          } else if (org.asam.ods.DataType.DT_BOOLEAN == nv.value.u.discriminator()) {
              properties.put(nv.valName, String.valueOf(nv.value.u.booleanVal()));
          } else {
              throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                    "Support for context NameValue with dataType "
                                            + ODSHelper.dataType2String(nv.value.u.discriminator()) + " not implemented, yet.");
          }
      }
      
      ApiFactory apiFactory = new ApiFactory();
      api = apiFactory.getApiForExistingFile(fileHandler, path, properties);
      
      try {
          BaseStructure bs = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
          AoSessionImpl aoSessionImpl = new AoSessionImpl(getModelPOA(orb), fileHandler, api, path, bs, isExtendedCompatiblityMode);
          getModelPOA(orb).activate_object(aoSessionImpl);
          
          LOG.info("Read ATFX in {}ms", System.currentTimeMillis() - start);
          return aoSessionImpl;
      } catch (ServantAlreadyActive | WrongPolicy e) {
          throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
      } finally {
          this.documentation.clear();
          this.files.clear();
          this.applElems.clear();
          this.applAttrs.clear();
          this.applRels.clear();
      }
    }
    
    /**
     * Creates and caches a new POA for all elements of the application structure for the session.
     * 
     * @param orb The ORB object.
     * @return The POA.
     * @throws AoException Error creating POA.
     */
    POA getModelPOA(ORB orb) throws AoException {
        if (modelPOA == null) {
            modelPOA = ODSHelper.createModelPOA(orb);
        }
        return modelPOA;
    }
}
