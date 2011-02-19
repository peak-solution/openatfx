package de.rechner.openatfx;

import java.io.File;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoFactoryHelper;
import org.asam.ods.AoSession;
import org.asam.ods.AoSessionHelper;
import org.asam.ods.BaseStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.basestructure.BaseStructureFactory;
import de.rechner.openatfx.io.DOMatfxReader;



/**
 * Factory object to connect to ATFX services.
 * 
 * @author Christian Rechner
 */
public class AoServiceFactory {

    private static final Log LOG = LogFactory.getLog(AoServiceFactory.class);

    /** The singleton instance */
    private static AoServiceFactory instance;

    /**
     * Non visible constructor.
     */
    private AoServiceFactory() {}

    /**
     * Creates a new <code>org.asam.ods.AoFactory</code> object.
     * 
     * @param orb The ORB.
     * @return The AoFactory object.
     * @throws AoException Error creating AoFactory.
     */
    public AoFactory newAoFactory(ORB orb) throws AoException {
        try {
            // get reference to rootpoa & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            AoFactoryImpl aoFactoryImpl = new AoFactoryImpl(orb);
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(aoFactoryImpl);
            AoFactory aoFactory = AoFactoryHelper.narrow(ref);

            return aoFactory;
        } catch (InvalidName e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterInactive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
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
    public AoSession newAoSession(ORB orb, File atfxFile) throws AoException {
        return DOMatfxReader.getInstance().createSessionForATFX(orb, atfxFile);
    }

    /**
     * Creates a new <code>org.asam.ods.AoSession</code> without having a application model and instance elements.
     * 
     * @param orb The ORB.
     * @param atfxFile The target ATFX file. Caution: On session commit this file will be created or overwritten.
     * @param baseModelVersion The base model version string, e.g. 'asam30'.
     * @return The created session.
     * @throws AoException Error creating session.
     */
    public AoSession newEmptyAoSession(ORB orb, File atfxFile, String baseModelVersion) throws AoException {
        try {
            // read base structure
            BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, baseModelVersion);

            // create session POA
            POA sessionPOA = createSessionPOA(orb);

            // create AoSession object
            AoSessionImpl aoSessionImpl = new AoSessionImpl(sessionPOA, atfxFile, baseStructure);
            AoSession aoSession = AoSessionHelper.narrow(sessionPOA.servant_to_reference(aoSessionImpl));

            return aoSession;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Creates a new POA for all elements for the session.
     * 
     * @param orb The ORB object.
     * @return The POA.
     * @throws AoException Error creating POA.
     */
    private POA createSessionPOA(ORB orb) throws AoException {
        try {
            String poaName = "AoSession.POA." + UUID.randomUUID().toString();
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POA poa = rootPOA.create_POA(poaName,
                                         null,
                                         new Policy[] {
                                                 rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID),
                                                 rootPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
                                                 rootPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                                                 rootPOA.create_implicit_activation_policy(ImplicitActivationPolicyValue.IMPLICIT_ACTIVATION),
                                                 rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
                                                 rootPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
                                                 rootPOA.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL) });
            poa.the_POAManager().activate();
            return poa;
        } catch (InvalidName e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterAlreadyExists e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (InvalidPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterInactive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static AoServiceFactory getInstance() {
        if (instance == null) {
            instance = new AoServiceFactory();
        }
        return instance;
    }

}
