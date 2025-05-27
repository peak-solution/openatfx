package com.peaksolution.openatfx;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoFactoryHelper;
import org.asam.ods.AoSession;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import com.peaksolution.openatfx.api.corba.AoEmptySessionFactory;
import com.peaksolution.openatfx.api.corba.CorbaAtfxReader;


/**
 * Factory object to connect to ATFX services.
 * 
 * @author Christian Rechner, Markus Renner
 */
public class AoServiceFactory {

    /** The singleton instance */
    private static AoServiceFactory instance;

    /**
     * Non visible constructor.
     */
    private AoServiceFactory() {
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static synchronized AoServiceFactory getInstance() {
        if (instance == null) {
            instance = new AoServiceFactory();
        }
        return instance;
    }

    /**
     * Creates a new <code>org.asam.ods.AoFactory</code> object.
     * 
     * @param orb The ORB.
     * @return The AoFactory object.
     * @throws AoException Error creating AoFactory.
     */
    public AoFactory newAoFactory(ORB orb) throws AoException {
        try {
            System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");
            System.setProperty("jacorb.buffermanager.expansionpolicy",
                               "org.jacorb.orb.buffermanager.DoubleExpansionPolicy");

            // get reference to rootpoa & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            AoFactoryImpl aoFactoryImpl = new AoFactoryImpl(orb, new LocalFileHandler());
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(aoFactoryImpl);
            return AoFactoryHelper.narrow(ref);
        } catch (InvalidName | AdapterInactive | ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Creates a new <code>org.asam.ods.AoFactory</code> object.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler, must not be null.
     * @return The AoFactory object.
     * @throws AoException Error creating AoFactory.
     */
    public AoFactory newAoFactory(ORB orb, IFileHandler fileHandler) throws AoException {
        try {
            System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");
            System.setProperty("jacorb.buffermanager.expansionpolicy",
                               "org.jacorb.orb.buffermanager.DoubleExpansionPolicy");

            // get reference to rootpoa & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            AoFactoryImpl aoFactoryImpl = new AoFactoryImpl(orb, fileHandler);
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(aoFactoryImpl);
            return AoFactoryHelper.narrow(ref);
        } catch (InvalidName | AdapterInactive | ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> with the content of given ATFX file.
     * <p>
     * The session will contain the complete application structure and all found instances.
     * 
     * @param orb The ORB.
     * @param atfxFile The ATFX file.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public synchronized AoSession newAoSession(ORB orb, File atfxFile) throws AoException {
        return newAoSession(orb, new LocalFileHandler(), atfxFile.toPath().toAbsolutePath());
    }

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> with the content of given ATFX file.
     * <p>
     * The session will contain the complete application structure and all found instances.
     * 
     * @param orb The ORB.
     * @param atfxFile The ATFX file.
     * @param extraContext additional optional context values to set in the session.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public synchronized AoSession newAoSession(ORB orb, File atfxFile, NameValue... extraContext) throws AoException {
        return newAoSession(orb, new LocalFileHandler(), atfxFile.toPath().toAbsolutePath(), extraContext);
    }

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> with the content of given ATFX file at a remote location.
     * <p>
     * The session will contain the complete application structure and all found instances.
     * <p>
     * If a remote file location is used the ATFX file will be readonly.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler, must not be null.
     * @param path The path to the ATFX file.
     * @param extraContext additional optional context values to set in the session.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public synchronized AoSession newAoSession(ORB orb, IFileHandler fileHandler, String path,
            NameValue... extraContext) throws AoException {
        return newAoSession(orb, fileHandler, Paths.get(path), extraContext);
    }

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> with the content of given ATFX file at a remote location.
     * <p>
     * The session will contain the complete application structure and all found instances.
     * <p>
     * If a remote file location is used the ATFX file will be readonly.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler, must not be null.
     * @param path The {@link Path} denoting the ATFX file.
     * @param extraContext additional optional context values to set in the session.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public synchronized AoSession newAoSession(ORB orb, IFileHandler fileHandler, Path path, NameValue... extraContext)
            throws AoException {
        return newCorbaReader(orb, fileHandler, path, extraContext).getSession();
    }

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
        return new AoEmptySessionFactory().newEmptyAoSession(orb, atfxFile, baseModelVersion);
    }

    /**
     * Creates a CorbaAtfxReader, providing the OpenAtfx Java API as well as the ODS AoSession.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler, must not be null.
     * @param path The {@link Path} denoting the ATFX file.
     * @param extraContext additional optional context values to set in the session.
     * @return The created {@link CorbaAtfxReader}.
     * @throws AoException Error connecting atfx file.
     */
    public synchronized CorbaAtfxReader newCorbaReader(ORB orb, IFileHandler fileHandler, Path path,
            NameValue... extraContext) throws AoException {
        CorbaAtfxReader reader = new CorbaAtfxReader();
        reader.init(orb, fileHandler, path, extraContext);
        return reader;
    }
}
