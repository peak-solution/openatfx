package com.peaksolution.openatfx.api.corba;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.AoSessionHelper;
import org.asam.ods.BaseStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.api.ApiFactory;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.basestructure.BaseStructureFactory;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Factory object to connect to ATFX services.
 * 
 * @author Markus Renner
 */
public class AoEmptySessionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AoEmptySessionFactory.class);

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> without having an application model and instance elements.
     * 
     * @param orb The ORB.
     * @param atfxFile The target ATFX file. Caution: On session commit this file will be created or overwritten.
     * @param baseModelVersion The base model version string, e.g. 'asam30'.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public synchronized AoSession newEmptyAoSession(ORB orb, File atfxFile, String baseModelVersion)
            throws AoException {
        try {
            System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");

            // extract base model version number
            Pattern baseModelPattern = Pattern.compile("asam(\\d{2})");
            Matcher m = baseModelPattern.matcher(baseModelVersion);
            String versionNumber = "36";
            if (m.matches()) {
                versionNumber = m.group(1);
            }

            // create new atfx file
            Path path = atfxFile.toPath();
            ApiFactory apiFactory = new ApiFactory();
            OpenAtfxAPIImplementation api = apiFactory.getApiForNewFile(path, new Properties(),
                                                                        Integer.parseInt(versionNumber));

            // create POA
            POA modelPOA = ODSHelper.createModelPOA(orb);
            LOG.debug("Created session POA");

            // create AoSession object
            BaseStructure bs = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
            IFileHandler fileHandler = new LocalFileHandler();
            AoSessionImpl aoSessionImpl = new AoSessionImpl(modelPOA, fileHandler, api, path, bs, false);
            modelPOA.activate_object(aoSessionImpl);
            return AoSessionHelper.narrow(modelPOA.servant_to_reference(aoSessionImpl));
        } catch (ServantNotActive | WrongPolicy | ServantAlreadyActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }
}
