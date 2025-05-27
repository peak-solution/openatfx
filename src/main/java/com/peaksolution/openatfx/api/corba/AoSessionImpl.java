package com.peaksolution.openatfx.api.corba;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.asam.ods.RelationRange;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.api.AtfxWriter;
import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.NameValueUnit;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.util.ODSHelper;
import com.peaksolution.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.AoSession</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
class AoSessionImpl extends AoSessionPOA {
    private static final Logger LOG = LoggerFactory.getLogger(AoSessionImpl.class);

    private static int sessionId;

    private final POA modelPOA;
    private final BaseStructure baseStructure;
    private final OpenAtfxAPIImplementation api;
    private final CorbaAtfxCache corbaCache;
    private final Path path;
    private final int id;
    private final boolean isExtendedCompatibilityMode;

    /** lazy loaded objects */
    private POA instancePOA;
    private ApplicationStructure applicationStructure;
    private ApplElemAccess applElemAccess;

    /** the temporary backup original file for transaction handling */
    private File transactionFile;

    /**
     * Constructor.
     * 
     * @param modelPOA The POA.
     * @param fileHandler The file handler.
     * @param path The path to the ATFX file.
     * @param baseStructure The base structure.
     * @throws AoException 
     */
    public AoSessionImpl(POA modelPOA, IFileHandler fileHandler, OpenAtfxAPIImplementation api, Path path,
            BaseStructure baseStructure, boolean isExtendedCompatibilityMode) throws AoException {
        this.modelPOA = modelPOA;
        this.baseStructure = baseStructure;
        this.api = api;
        this.path = path;
        sessionId++;
        this.id = sessionId;
        this.isExtendedCompatibilityMode = isExtendedCompatibilityMode;
        
        this.corbaCache = new CorbaAtfxCache(api, modelPOA, baseStructure, fileHandler, this);
        this.corbaCache.setInstancePOA(getInstancePOA());
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
                this.instancePOA.set_servant_manager(new InstanceServantLocator(this.modelPOA, corbaCache, api));
                this.instancePOA.the_POAManager().activate();

                LOG.debug("Created instance POA");
            } catch (AdapterAlreadyExists | InvalidPolicy | AdapterInactive | WrongPolicy e) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            }
        }
        return this.instancePOA;
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
        return this.api.getContext().get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal();
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
        return api.getBaseModelVersion();
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
            if (applicationStructure == null) {
                ApplicationStructureImpl asImpl = new ApplicationStructureImpl(this.corbaCache, this.api, _this(), baseStructure);
                applicationStructure = ApplicationStructureHelper.narrow(this.modelPOA.servant_to_reference(asImpl));
                asImpl.loadInitialData(baseStructure, this);
            }
            return this.applicationStructure;
        } catch (WrongPolicy | ServantNotActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getApplElemAccess()
     */
    public ApplElemAccess getApplElemAccess() throws AoException {
        try {
            if (this.applElemAccess == null) {
                ApplElemAccessImpl aeaImpl = new ApplElemAccessImpl(getInstancePOA(), this.api, corbaCache);
                this.applElemAccess = ApplElemAccessHelper.narrow(this.modelPOA.servant_to_reference(aeaImpl));
            }
            return this.applElemAccess;
        } catch (WrongPolicy | ServantNotActive e) {
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
            List<String> list = new ArrayList<>();
            for (String str : this.api.getContext().keySet()) {
                if (PatternUtil.nameFilterMatchCI(str, varPattern)) {
                    list.add(str);
                }
            }
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.modelPOA, list.toArray(new String[0]));
            return NameIteratorHelper.narrow(this.modelPOA.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive | WrongPolicy e) {
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
            List<NameValue> list = new ArrayList<>();
            for (NameValueUnit nv : this.api.getContext().values()) {
                if (PatternUtil.nameFilterMatchCI(nv.getValName(), varPattern)) {
                    list.add(ODSHelper.convertNvuToNv(nv));
                }
            }
            NameValueIteratorImpl nvIteratorImpl = new NameValueIteratorImpl(this.modelPOA,
                                                                             list.toArray(new NameValue[0]));
            return NameValueIteratorHelper.narrow(this.modelPOA.servant_to_reference(nvIteratorImpl));
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#getContextByName(java.lang.String)
     */
    public NameValue getContextByName(String varName) throws AoException {
        NameValueUnit nvu = this.api.getContext().get(varName);
        if (nvu != null) {
            return ODSHelper.convertNvuToNv(nvu);
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Context '" + varName + "' not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#setContext(org.asam.ods.NameValue)
     */
    public void setContext(NameValue contextVariable) throws AoException {
        DataType odsDataType = contextVariable.value.u.discriminator();
        com.peaksolution.openatfx.api.DataType dt = com.peaksolution.openatfx.api.DataType.fromString(ODSHelper.dataType2String(odsDataType));
        
        Object value = null;
        switch (dt) {
            case DT_STRING:
                value = contextVariable.value.u.stringVal();
                break;
            case DT_LONG:
                value = contextVariable.value.u.longVal();
                break;
            case DT_LONGLONG:
                value = ODSHelper.asJLong(contextVariable.value.u.longlongVal());
                break;
            case DT_BOOLEAN:
                value = contextVariable.value.u.booleanVal();
                break;
            default:
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, OpenAtfxException.ERR_UNSUPPORTED_CONTEXT_DATATYPE + contextVariable.valName);
        }
        
        try {
            
            this.api.setContext(new NameValueUnit(contextVariable.valName, dt, value));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
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
        try {
            this.api.removeContext(varPattern);
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoSessionOperations#createBlob()
     */
    public Blob createBlob() throws AoException {
        try {
            BlobImpl blobImpl = new BlobImpl(modelPOA);
            return BlobHelper.narrow(this.modelPOA.servant_to_reference(blobImpl));
        } catch (WrongPolicy | ServantNotActive e) {
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
        if (!(this.corbaCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }
        
        // check if already a transaction is opened - multiple transactions are not supported!
        if (this.transactionFile != null) {
            throw new AoException(ErrorCode.AO_TRANSACTION_ALREADY_ACTIVE, SeverityFlag.ERROR, 0,
                                  "A transaction is already open and not yet commited or aborted");
        }

        // copy original file to tmp for backup
        try {
            File backupFile = File.createTempFile("openatfx_backup", ".atfx");
            backupFile.deleteOnExit();
            Files.copy(path, backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.transactionFile = backupFile;

            LOG.info("Started transaction [backupFile={}]", backupFile);
        } catch (IOException e) {
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
        if (!(this.corbaCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }

        // restore backup
        try {
            Path transactionFilePath = transactionFile.toPath();
            Files.delete(transactionFilePath);
            this.transactionFile = null;

            LOG.info("Aborted transaction");
        } catch (IOException e) {
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
        if (!(this.corbaCache.getFileHandler() instanceof LocalFileHandler)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Writing to ATFX file is only possible on local file system");
        }

        try {
            // overwrite backup file
            Path transactionFilePath = transactionFile.toPath();
            AtfxWriter.getInstance().writeXML(this.transactionFile, this.api);
            Files.copy(transactionFilePath, path, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(transactionFilePath);
            this.transactionFile = null;

            LOG.info("Committed transaction to '{}'", path);
        } catch (IOException e) {
            AoException aoe = new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
            aoe.initCause(e);
            throw aoe;
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
        List<ApplElem> applElemList = new ArrayList<>();
        List<ApplRel> applRelList = new ArrayList<>();

        for (Element ae : api.getElements()) {
            ApplElem applElem = new ApplElem();
            applElem.aid = ODSHelper.asODSLongLong(ae.getId());
            applElem.aeName = ae.getName();
            applElem.beName = ae.getBaseElement().getType();

            List<ApplAttr> applAttrList = new ArrayList<>();
            for (Attribute aa : ae.getAttributes()) {
                ApplAttr applAttr = new ApplAttr();
                applAttr.aaName = aa.getName();
                applAttr.baName = aa.getBaseName() == null ? "" : aa.getBaseName();
                applAttr.dType = ODSHelper.mapDataType(aa.getDataType());
                applAttr.length = aa.getLength();
                applAttr.isObligatory = aa.isObligatory();
                applAttr.isUnique = aa.isUnique();
                applAttr.unitId = ODSHelper.asODSLongLong(aa.getUnitId());
                applAttrList.add(applAttr);
            }
            applElem.attributes = applAttrList.toArray(new ApplAttr[0]);
            for (Relation ar : ae.getRelations()) {
                ApplRel applRel = new ApplRel();
                applRel.arName = ar.getRelationName();
                applRel.elem1 = ODSHelper.asODSLongLong(ae.getId());
                applRel.invName = "";
                Element relatedElem = ar.getElement2();
                if (relatedElem != null && !isExtendedCompatibilityMode) {
                    applRel.elem2 = ODSHelper.asODSLongLong(relatedElem.getId());
                    applRel.brName = ar.getBaseRelation() == null ? "" : ar.getBaseRelation().getName();
                    applRel.arRelationRange = new RelationRange(ar.getRelationRangeMin(), ar.getRelationRangeMax());
                    applRel.arRelationType = ar.getRelationType();
                    applRel.invName = ar.getInverseRelationName();
                    applRel.invBrName = ar.getBaseRelation() == null ? "" : ar.getBaseRelation().getInverseName(relatedElem.getType());
                    Relation inverseRel = ar.getInverseRelation();
                    if (inverseRel != null) {
                        applRel.invRelationRange = new RelationRange(inverseRel.getRelationRangeMin(), inverseRel.getRelationRangeMax());
                    } else {
                        applRel.invRelationRange = new RelationRange((short)-2, (short)-2);
                    }
                    applRelList.add(applRel);
                }
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
        List<EnumerationAttributeStructure> list = new ArrayList<>();
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
        List<EnumerationStructure> list = new ArrayList<>();
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

    /**
     * For unit tests
     * 
     * @return the {@link CorbaAtfxCache}
     */
    public CorbaAtfxCache getAtfxCache() {
        return this.corbaCache;
    }
}
