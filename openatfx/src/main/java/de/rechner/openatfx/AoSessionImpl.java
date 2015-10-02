package de.rechner.openatfx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.AoSessionPOA;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplElemAccessHelper;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ApplicationStructureHelper;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.BaseStructure;
import org.asam.ods.Blob;
import org.asam.ods.BlobHelper;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.NameValueIteratorHelper;
import org.asam.ods.QueryEvaluator;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.io.AtfxWriter;
import de.rechner.openatfx.util.FileUtil;
import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.AoSession</code>.
 * 
 * @author Christian Rechner
 */
public class AoSessionImpl extends AoSessionPOA {

    private static final Log LOG = LogFactory.getLog(AoSessionImpl.class);

    /** The static context variables, these may not be changed */
    private static final Map<String, NameValue> STATIC_CONTEXT = new LinkedHashMap<String, NameValue>();
    static {
        STATIC_CONTEXT.put("WILDCARD_ALL", ODSHelper.createStringNV("WILDCARD_ALL", "*"));
        STATIC_CONTEXT.put("WILDCARD_ESC", ODSHelper.createStringNV("WILDCARD_ESC", "\\"));
        STATIC_CONTEXT.put("WILDCARD_ONE", ODSHelper.createStringNV("WILDCARD_ONE", "."));
        STATIC_CONTEXT.put("USER", ODSHelper.createStringNV("USER", System.getProperty("user.name")));
        STATIC_CONTEXT.put("PASSWORD", ODSHelper.createStringNV("PASSWORD", "***********"));
        STATIC_CONTEXT.put("ODSVERSION", ODSHelper.createStringNV("ODSVERSION", "5.3.0"));
        STATIC_CONTEXT.put("CREATE_COSESSION_ALLOWED", ODSHelper.createStringNV("CREATE_COSESSION_ALLOWED", "FALSE"));
        STATIC_CONTEXT.put("FILE_NOTATION", ODSHelper.createStringNV("FILE_NOTATION", "UNC_UNIX"));
        STATIC_CONTEXT.put("FILE_MODE", ODSHelper.createStringNV("FILE_MODE", "SINGLE_VOLUME"));
        STATIC_CONTEXT.put("FILE_ROOT", ODSHelper.createStringNV("FILE_ROOT", ""));
        STATIC_CONTEXT.put("FILE_ROOT_EXTREF", ODSHelper.createStringNV("FILE_ROOT_EXTREF", ""));
        STATIC_CONTEXT.put("VALUEMATRIX_MODE", ODSHelper.createStringNV("VALUEMATRIX_MODE", "CALCULATED"));
        STATIC_CONTEXT.put("FILENAME", ODSHelper.createStringNV("FILENAME", ""));
        STATIC_CONTEXT.put("TYPE", ODSHelper.createStringNV("TYPE", "XATF-ASCII"));
    }

    private static int SESSION_NO;

    private final POA modelPOA;
    private final BaseStructure baseStructure;
    private final AtfxCache atfxCache;
    private final String path;

    /** lazy loaded objects */
    private POA instancePOA;
    private ApplicationStructure applicationStructure;
    private ApplElemAccess applElemAccess;

    /** the temporary backup original file for transaction handling */
    private File transactionFile;

    private final int id;

    /**
     * Constructor.
     * 
     * @param modelPOA The POA.
     * @param fileHandler The file handler.
     * @param path The path to the ATFX file.
     * @param baseStructure The base structure.
     * @throws IOException
     */
    public AoSessionImpl(POA modelPOA, IFileHandler fileHandler, String path, BaseStructure baseStructure)
            throws IOException {
        this.modelPOA = modelPOA;
        this.baseStructure = baseStructure;
        this.atfxCache = new AtfxCache(fileHandler);
        this.path = path;
        SESSION_NO++;
        this.id = SESSION_NO;

        // fill initial context
        String fileStr = fileHandler.getFileName(path);
        String directoryStr = fileHandler.getFileRoot(path);
        this.atfxCache.getContext().putAll(STATIC_CONTEXT);
        this.atfxCache.getContext().put("write_mode", ODSHelper.createStringNV("write_mode", "database"));
        this.atfxCache.getContext().put("FILE_ROOT", ODSHelper.createStringNV("FILE_ROOT", directoryStr));
        this.atfxCache.getContext().put("FILE_ROOT_EXTREF", ODSHelper.createStringNV("FILE_ROOT_EXTREF", directoryStr));
        this.atfxCache.getContext().put("FILENAME", ODSHelper.createStringNV("FILENAME", fileStr));
        this.atfxCache.getContext().put("EXT_COMP_SEGSIZE",
                                        ODSHelper.createLongLongNV("EXT_COMP_SEGSIZE", 1024 * 1024 * 500)); // 500 MB
        this.atfxCache.getContext().put("INDENT_XML", ODSHelper.createStringNV("INDENT_XML", "TRUE"));
        this.atfxCache.getContext().put("WRITE_EXTERNALCOMPONENTS",
                                        ODSHelper.createStringNV("WRITE_EXTERNALCOMPONENTS", "FALSE"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getId()
     */
    public int getId() throws AoException {
        return this.id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getName()
     */
    public String getName() throws AoException {
        return this.atfxCache.getContext().get("FILENAME").value.u.stringVal();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getDescription()
     */
    public String getDescription() throws AoException {
        return "ATFX File";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getType()
     */
    public String getType() throws AoException {
        return getBaseStructure().getVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getBaseStructure()
     */
    public BaseStructure getBaseStructure() throws AoException {
        return this.baseStructure;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getApplicationStructure()
     */
    public ApplicationStructure getApplicationStructure() throws AoException {
        try {
            if (this.applicationStructure == null) {
                ApplicationStructureImpl asImpl = new ApplicationStructureImpl(this.modelPOA, getInstancePOA(),
                                                                               this.atfxCache, _this());
                this.applicationStructure = ApplicationStructureHelper.narrow(this.modelPOA.servant_to_reference(asImpl));
            }
            return this.applicationStructure;
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    private POA getInstancePOA() throws AoException {
        if (this.instancePOA == null) {
            try {
                String poaName = "AoSession.InstancePOA." + UUID.randomUUID().toString();
                this.instancePOA = this.modelPOA.create_POA(poaName,
                                                            null,
                                                            new Policy[] {
                                                                    modelPOA.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID),
                                                                    modelPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
                                                                    modelPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                                                                    modelPOA.create_implicit_activation_policy(ImplicitActivationPolicyValue.NO_IMPLICIT_ACTIVATION),
                                                                    modelPOA.create_servant_retention_policy(ServantRetentionPolicyValue.NON_RETAIN),
                                                                    modelPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_SERVANT_MANAGER),
                                                                    modelPOA.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL) });
                this.instancePOA.set_servant_manager(new InstanceServantLocator(this.modelPOA, atfxCache));
                this.instancePOA.the_POAManager().activate();

                LOG.debug("Created instance POA");
            } catch (AdapterAlreadyExists e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            } catch (InvalidPolicy e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            } catch (AdapterInactive e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            } catch (WrongPolicy e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            }
        }
        return this.instancePOA;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getApplElemAccess()
     */
    public ApplElemAccess getApplElemAccess() throws AoException {
        try {
            if (this.applElemAccess == null) {
                ApplElemAccessImpl aeaImpl = new ApplElemAccessImpl(this.instancePOA, this.atfxCache);
                this.applElemAccess = ApplElemAccessHelper.narrow(this.modelPOA.servant_to_reference(aeaImpl));
            }
            return this.applElemAccess;
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#createQueryEvaluator()
     */
    public QueryEvaluator createQueryEvaluator() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'createQueryEvaluator' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#listContext(java.lang.String)
     */
    public NameIterator listContext(String varPattern) throws AoException {
        try {
            List<String> list = new ArrayList<String>();
            for (String str : this.atfxCache.getContext().keySet()) {
                if (PatternUtil.nameFilterMatch(str, varPattern)) {
                    list.add(str);
                }
            }
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.modelPOA, list.toArray(new String[0]));
            return NameIteratorHelper.narrow(this.modelPOA.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getContext(java.lang.String)
     */
    public NameValueIterator getContext(String varPattern) throws AoException {
        try {
            List<NameValue> list = new ArrayList<NameValue>();
            for (NameValue nv : this.atfxCache.getContext().values()) {
                if (PatternUtil.nameFilterMatch(nv.valName, varPattern)) {
                    list.add(ODSHelper.cloneNV(nv));
                }
            }
            NameValueIteratorImpl nvIteratorImpl = new NameValueIteratorImpl(this.modelPOA,
                                                                             list.toArray(new NameValue[0]));
            return NameValueIteratorHelper.narrow(this.modelPOA.servant_to_reference(nvIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getContextByName(java.lang.String)
     */
    public NameValue getContextByName(String varName) throws AoException {
        NameValue nv = this.atfxCache.getContext().get(varName);
        if (nv != null) {
            return nv;
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Context '" + varName + "' not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setContext(org.asam.ods.NameValue)
     */
    public void setContext(NameValue contextVariable) throws AoException {
        // check if readonly context
        if (STATIC_CONTEXT.containsKey(contextVariable.valName)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, "Context '"
                    + contextVariable.valName + "' is readonly");
        }
        this.atfxCache.getContext().put(contextVariable.valName, ODSHelper.cloneNV(contextVariable));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setContextString(java.lang.String, java.lang.String)
     */
    public void setContextString(String varName, String value) throws AoException {
        setContext(ODSHelper.createStringNV(varName, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#removeContext(java.lang.String)
     */
    public void removeContext(String varPattern) throws AoException {
        // check if readonly context should be removed
        for (NameValue nv : this.atfxCache.getContext().values()) {
            if (PatternUtil.nameFilterMatch(nv.valName, varPattern) && STATIC_CONTEXT.containsKey(nv.valName)) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                      "Unable to remove readonly context '" + nv.valName + "'");
            }
        }
        // remove matching context
        List<String> toRemove = new ArrayList<String>();
        for (NameValue nv : this.atfxCache.getContext().values()) {
            if (PatternUtil.nameFilterMatch(nv.valName, varPattern)) {
                toRemove.add(nv.valName);
            }
        }
        for (String valName : toRemove) {
            this.atfxCache.getContext().remove(valName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#createBlob()
     */
    public Blob createBlob() throws AoException {
        try {
            BlobImpl blobImpl = new BlobImpl(this.modelPOA);
            return BlobHelper.narrow(this.modelPOA.servant_to_reference(blobImpl));
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setCurrentInitialRights(org.asam.ods.InitialRight[], boolean)
     */
    public void setCurrentInitialRights(InitialRight[] irlEntries, boolean set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setCurrentInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getLockMode()
     */
    public short getLockMode() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getLockMode' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setLockMode(short)
     */
    public void setLockMode(short lockMode) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setLockMode' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getUser()
     */
    public InstanceElement getUser() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'getUser' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    public void setPassword(String username, String oldPassword, String newPassword) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setPassword' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#startTransaction()
     */
    public void startTransaction() throws AoException {
        // writing to ATFX file is only possible on local file system!
        if (!(this.atfxCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }
        File localAtfxFile = new File(this.path);

        // check if already a transaction is opened - multiple transactions are not supported!
        if (this.transactionFile != null) {
            throw new AoException(ErrorCode.AO_TRANSACTION_ALREADY_ACTIVE, SeverityFlag.ERROR, 0,
                                  "A transaction is already open and not yet commited or aborted");
        }

        // copy original file to tmp for backup
        try {
            File backupFile = File.createTempFile("openatfx_backup", ".atfx");
            backupFile.deleteOnExit();
            FileUtil.copyFile(localAtfxFile, backupFile);
            this.transactionFile = backupFile;

            LOG.info("Started transaction [backupFile=" + backupFile + "]");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#abortTransaction()
     */
    public void abortTransaction() throws AoException {
        // check if already a transaction is opened - multiple transactions are not supported!
        if (this.transactionFile == null) {
            throw new AoException(ErrorCode.AO_TRANSACTION_NOT_ACTIVE, SeverityFlag.ERROR, 0, "No transaction active");
        }

        // writing to ATFX file is only possible on local file system!
        if (!(this.atfxCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }
        File localAtfxFile = new File(this.path);

        // restore backup
        try {
            FileUtil.copyFile(this.transactionFile, localAtfxFile);
            this.transactionFile.delete();
            this.transactionFile = null;

            LOG.info("Aborted transaction");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#commitTransaction()
     */
    public void commitTransaction() throws AoException {
        // check if already a transaction is opened - multiple transactions are not supported!
        if (this.transactionFile == null) {
            throw new AoException(ErrorCode.AO_TRANSACTION_NOT_ACTIVE, SeverityFlag.ERROR, 0, "No transaction active");
        }

        // writing to ATFX file is only possible on local file system!
        if (!(this.atfxCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }
        File localAtfxFile = new File(this.path);

        try {
            // overwrite backup file
            AtfxWriter.getInstance().writeXML(this.transactionFile, _this());

            FileUtil.copyFile(this.transactionFile, localAtfxFile);
            this.transactionFile.delete();
            this.transactionFile = null;

            LOG.info("Commited transaction to '" + localAtfxFile.getAbsolutePath() + "'");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#flush()
     */
    public void flush() throws AoException {
        // TODO To be implemented
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#close()
     */
    public void close() throws AoException {
        if (this.instancePOA != null) {
            this.instancePOA.destroy(false, false);
        }
        this.modelPOA.destroy(false, false);
        LOG.info("Closed ATFX AoSession");
        System.gc();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#createCoSession()
     */
    public AoSession createCoSession() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'createCoSession' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getApplicationStructureValue()
     */
    public ApplicationStructureValue getApplicationStructureValue() throws AoException {
        List<ApplElem> applElemList = new ArrayList<ApplElem>();
        List<ApplRel> applRelList = new ArrayList<ApplRel>();

        for (ApplicationElement ae : this.atfxCache.getApplicationElements()) {
            ApplElem applElem = new ApplElem();
            applElem.aid = ae.getId();
            applElem.aeName = ae.getName();
            applElem.beName = ae.getBaseElement().getType();

            List<ApplAttr> applAttrList = new ArrayList<ApplAttr>();
            for (ApplicationAttribute aa : this.atfxCache.getApplicationAttributes(ODSHelper.asJLong(ae.getId()))) {
                ApplAttr applAttr = new ApplAttr();
                applAttr.aaName = aa.getName();
                applAttr.baName = aa.getBaseAttribute() == null ? "" : aa.getBaseAttribute().getName();
                applAttr.dType = aa.getDataType();
                applAttr.length = aa.getLength();
                applAttr.isObligatory = aa.isObligatory();
                applAttr.isUnique = aa.isUnique();
                applAttr.unitId = aa.getUnit();
                applAttrList.add(applAttr);
            }
            applElem.attributes = applAttrList.toArray(new ApplAttr[0]);
            for (ApplicationRelation ar : ae.getAllRelations()) {
                ApplRel applRel = new ApplRel();
                applRel.arName = ar.getRelationName();
                applRel.elem1 = ar.getElem1().getId();
                ApplicationElement appelem = ar.getElem2();
                applRel.elem2 = appelem.getId();
                applRel.brName = ar.getBaseRelation() == null ? "" : ar.getBaseRelation().getRelationName();
                applRel.arRelationRange = ar.getRelationRange();
                applRel.arRelationType = ar.getRelationType();
                applRel.invName = ar.getInverseRelationName();
                applRel.invBrName = ar.getBaseRelation() == null ? "" : ar.getBaseRelation().getInverseRelationName();
                applRel.invRelationRange = ar.getInverseRelationRange();
                applRelList.add(applRel);
            }

            applElemList.add(applElem);
        }
        ApplicationStructureValue asv = new ApplicationStructureValue();
        asv.applElems = applElemList.toArray(new ApplElem[0]);
        asv.applRels = applRelList.toArray(new ApplRel[0]);
        return asv;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getEnumerationAttributes()
     */
    public EnumerationAttributeStructure[] getEnumerationAttributes() throws AoException {
        List<EnumerationAttributeStructure> list = new ArrayList<EnumerationAttributeStructure>();
        for (ApplicationElement ae : getApplicationStructure().getElements("*")) {
            for (ApplicationAttribute aa : ae.getAttributes("*")) {
                if (aa.getDataType() == DataType.DT_ENUM || aa.getDataType() == DataType.DS_ENUM) {
                    EnumerationAttributeStructure eas = new EnumerationAttributeStructure();
                    eas.aid = ae.getId();
                    eas.aaName = aa.getName();
                    eas.enumName = aa.getEnumerationDefinition().getName();
                    list.add(eas);
                }
            }
        }
        return list.toArray(new EnumerationAttributeStructure[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getEnumerationStructure()
     */
    public EnumerationStructure[] getEnumerationStructure() throws AoException {
        ApplicationStructure as = getApplicationStructure();
        List<EnumerationStructure> list = new ArrayList<EnumerationStructure>();
        for (String enumDefName : as.listEnumerations()) {
            EnumerationDefinition enumDef = as.getEnumerationDefinition(enumDefName);
            String[] itemNames = enumDef.listItemNames();
            EnumerationStructure es = new EnumerationStructure();
            es.enumName = enumDef.getName();
            es.items = new EnumerationItemStructure[itemNames.length];
            for (int i = 0; i < itemNames.length; i++) {
                es.items[i] = new EnumerationItemStructure();
                es.items[i].index = enumDef.getItem(itemNames[i]);
                es.items[i].itemName = itemNames[i];
            }
            list.add(es);
        }
        return list.toArray(new EnumerationStructure[0]);
    }

}
