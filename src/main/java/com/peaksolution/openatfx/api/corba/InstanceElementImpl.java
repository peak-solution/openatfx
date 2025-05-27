package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationInstanceElementSeq;
import org.asam.ods.AttrType;
import org.asam.ods.Blob;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementPOA;
import org.asam.ods.Measurement;
import org.asam.ods.MeasurementHelper;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.ODSFile;
import org.asam.ods.ODSFileHelper;
import org.asam.ods.RelationRange;
import org.asam.ods.Relationship;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrix;
import org.asam.ods.SubMatrixHelper;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.api.AtfxInstance;
import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.DataType;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.util.ODSHelper;
import com.peaksolution.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.InstanceElement</code>.
 * 
 * @author Christian Rechner
 */
class InstanceElementImpl extends InstanceElementPOA {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceElementImpl.class);

    protected final AtfxInstance delegate;
    protected final CorbaAtfxCache corbaCache;
    
    protected final POA modelPOA;
    protected final POA instancePOA;
    
    protected final long aid;
    protected final long iid;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param atfxInstance The delegate instance.
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public InstanceElementImpl(POA modelPOA, POA instancePOA, CorbaAtfxCache atfxCache, AtfxInstance atfxInstance) {
        this.modelPOA = modelPOA;
        this.instancePOA = instancePOA;
        this.delegate = atfxInstance;
        this.corbaCache = atfxCache;
        this.aid = atfxInstance.getAid();
        this.iid = atfxInstance.getIid();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getApplicationElement()
     */
    public ApplicationElement getApplicationElement() throws AoException {
        ApplicationElement ae = this.corbaCache.getApplicationElementById(aid);
        if (ae == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Unable to get application element");
        }
        return ae;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#compare(org.asam.ods.InstanceElement)
     */
    public T_LONGLONG compare(InstanceElement compIeObj) throws AoException {
        // compare application element ids
        Long otherAid = ODSHelper.asJLong(compIeObj.getApplicationElement().getId());
        int res = Long.compare(aid, otherAid);

        // compare instance ids
        if (res == 0) {
            Long otherIid = ODSHelper.asJLong(compIeObj.getId());
            res = Long.compare(iid, otherIid);
        }

        return ODSHelper.asODSLongLong(res);
    }

    /***********************************************************************************
     * instance values
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getId()
     */
    public T_LONGLONG getId() throws AoException {
        return ODSHelper.asODSLongLong(this.iid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getName()
     */
    public String getName() throws AoException {
        try {
            return delegate.getName();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setName(java.lang.String)
     */
    public void setName(String iaName) throws AoException {
        // set value
        try {
            Attribute nameAttr = delegate.getElement().getAttributeByBaseName("name");
            delegate.setAttributeValue(new com.peaksolution.openatfx.api.NameValueUnit(nameAttr.getName(), DataType.DT_STRING, iaName));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#listAttributes(java.lang.String, org.asam.ods.AttrType)
     */
    public String[] listAttributes(String iaPattern, AttrType aType) throws AoException {
        List<String> list = new ArrayList<>();
        try {
            // application attributes
            if (aType != AttrType.INSTATTR_ONLY) {
                for (Attribute currentAttribute : delegate.getElement().getAttributes()) {
                    String attrName = currentAttribute.getName();
                    if (PatternUtil.nameFilterMatch(attrName, iaPattern)) {
                        list.add(attrName);
                    }
                }
            }
            // instance attributes
            if (aType != AttrType.APPLATTR_ONLY) {
                for (com.peaksolution.openatfx.api.NameValueUnit nvu : delegate.getInstanceAttributes()) {
                    String attrName = nvu.getValName();
                    if (PatternUtil.nameFilterMatch(attrName, iaPattern)) {
                        list.add(attrName);
                    }
                }
            }
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValue(java.lang.String)
     */
    public NameValueUnit getValue(String aaName) throws AoException {
        try {
            Attribute attribute = delegate.getElement().getAttributeByName(aaName);
            NameValueUnit nvu = null;
            if (attribute != null && DataType.DT_BLOB == attribute.getDataType()) {
                nvu = ODSHelper.createEmptyNVU(aaName, org.asam.ods.DataType.DT_BLOB);
                int attrNo = delegate.getElement().getAttrNoByName(aaName);
                Blob blob = null;
                // create not yet existing ODS blob and cache it
                com.peaksolution.openatfx.api.NameValueUnit existingBlobNvu = delegate.getValue(attrNo);
                if (existingBlobNvu != null && existingBlobNvu.isValid()) {
                    com.peaksolution.openatfx.api.Blob orgBlob = existingBlobNvu.getValue().blobVal();
                    blob = corbaCache.getSessionImpl().createBlob();
                    blob.setHeader(orgBlob.getHeader());
                    blob.set(orgBlob.get(0, orgBlob.getLength()));
                    nvu.value.flag = (short)15;
                }
                nvu.value.u.blobVal(blob);
            } else {
                // this may be a non-blob application or instance attribute
                com.peaksolution.openatfx.api.NameValueUnit delegateNvu = delegate.getValue(aaName);
                if (delegateNvu == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Attribute '" + aaName
                                          + "' not found");
                }
                nvu = ODSHelper.mapNvu(delegateNvu);
            }
            return nvu;
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueSeq(java.lang.String[])
     */
    public NameValueUnit[] getValueSeq(String[] attrNames) throws AoException {
        String[] names = attrNames;
        if (attrNames.length == 1 && attrNames[0].equals("*")) {
            names = listAttributes("*", AttrType.ALL);
        }
        Collection<NameValueUnit> values = new ArrayList<>();
        for (String attrName : names) {
            values.add(getValue(attrName));
        }
        return values.toArray(new NameValueUnit[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueByBaseName(java.lang.String)
     */
    public NameValueUnit getValueByBaseName(String baseAttrName) throws AoException {
        try {
            return ODSHelper.mapNvu(delegate.getValueByBaseName(baseAttrName));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueInUnit(org.asam.ods.NameUnit)
     */
    public NameValueUnit getValueInUnit(NameUnit attr) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getValueInUnit' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setValue(org.asam.ods.NameValueUnit)
     */
    public void setValue(NameValueUnit nvu) throws AoException {
        try {
            if (delegate.doesAttributeExist(nvu.valName, null, true)) {
                delegate.setValue(ODSHelper.mapNvu(nvu));
            }
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setValueSeq(org.asam.ods.NameValueUnit[])
     */
    public void setValueSeq(NameValueUnit[] values) throws AoException {
        try {
            delegate.setAttributeValues(Arrays.asList(ODSHelper.mapNvus(values)));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /***********************************************************************************
     * instance attributes
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#addInstanceAttribute(org.asam.ods.NameValueUnit)
     */
    public void addInstanceAttribute(NameValueUnit instAttr) throws AoException {
        try {
            // check for existing instance attribute
            if (delegate.listInstanceAttributes().contains(instAttr.valName)) {
                throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                      "An InstanceAttribute with name '" + instAttr.valName + "' already exists");
            }
            delegate.setInstanceValue(ODSHelper.mapNvu(instAttr));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#removeInstanceAttribute(java.lang.String)
     */
    public void removeInstanceAttribute(String attrName) throws AoException {
        try {
            com.peaksolution.openatfx.api.NameValueUnit nvu = delegate.getInstanceAttribute(attrName);
            if (nvu == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceAttribute '" + attrName
                                      + "' not found at " + delegate);
            }
            
            delegate.setInstanceValue(new com.peaksolution.openatfx.api.NameValueUnit(attrName, nvu.getValue().discriminator(), null));
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#renameInstanceAttribute(java.lang.String, java.lang.String)
     */
    public void renameInstanceAttribute(String oldName, String newName) throws AoException {
        try {
            delegate.renameInstanceAttribute(oldName, newName);
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /***********************************************************************************
     * relations
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#listRelatedInstances(org.asam.ods.ApplicationRelation,
     *      java.lang.String)
     */
    public NameIterator listRelatedInstances(ApplicationRelation applRel, String iePattern) throws AoException {
        try {
            List<String> list = new ArrayList<>();
            for (InstanceElement ie : collectRelatedInstances(applRel, iePattern)) {
                list.add(ie.getName());
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
     * @see org.asam.ods.InstanceElementOperations#getRelatedInstances(org.asam.ods.ApplicationRelation,
     *      java.lang.String)
     */
    public InstanceElementIterator getRelatedInstances(ApplicationRelation applRel, String iePattern)
            throws AoException {
        InstanceElement[] ieAr = collectRelatedInstances(applRel, iePattern);
        return corbaCache.newInstanceElementIterator(instancePOA, ieAr);
    }

    /**
     * Collect related instances by given application relation.
     * 
     * @param applRel The application relation.
     * @param iePattern The name pattern.
     * @return Array of instance elements.
     * @throws AoException Error fetching related instances.
     */
    private InstanceElement[] collectRelatedInstances(ApplicationRelation applRel, String iePattern) throws AoException {
        long otherAid = ODSHelper.asJLong(applRel.getElem2().getId());
        Collection<Long> otherIids = this.corbaCache.getRelatedInstanceIds(this.aid, this.iid, applRel);

        // pattern 'all'
        if (iePattern.equals("*")) {
            InstanceElement[] ies = new InstanceElement[otherIids.size()];
            int i = 0;
            for (long otherIid : otherIids) {
                ies[i] = this.corbaCache.getInstanceById(this.instancePOA, otherAid, otherIid);
                i++;
            }
            return ies;
        }
        // filter by pattern
        else {
            List<InstanceElement> list = new ArrayList<>();
            for (long otherIid : otherIids) {
                InstanceElement ie = this.corbaCache.getInstanceById(this.instancePOA, otherAid, otherIid);
                if (ie != null && PatternUtil.nameFilterMatch(ie.getName(), iePattern)) {
                    list.add(ie);
                }
            }
            return list.toArray(new InstanceElement[0]);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#listRelatedInstancesByRelationship(org.asam.ods.Relationship,
     *      java.lang.String)
     */
    public NameIterator listRelatedInstancesByRelationship(Relationship ieRelationship, String iePattern)
            throws AoException {
        try {
            List<String> list = new ArrayList<>();
            for (InstanceElement ie : collectRelatedInstancesByRelationship(ieRelationship, iePattern)) {
                list.add(ie.getName());
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
     * @see org.asam.ods.InstanceElementOperations#getRelatedInstancesByRelationship(org.asam.ods.Relationship,
     *      java.lang.String)
     */
    public InstanceElementIterator getRelatedInstancesByRelationship(Relationship ieRelationship, String iePattern)
            throws AoException {
        InstanceElement[] ieAr = collectRelatedInstancesByRelationship(ieRelationship, iePattern).toArray(new InstanceElement[0]);
        return corbaCache.newInstanceElementIterator(instancePOA, ieAr);
    }

    /**
     * Collect related instances by given relationship.
     * 
     * @param ieRelationship The relationship.
     * @param iePattern The name pattern.
     * @return Collection of instance elements.
     * @throws AoException Error fetching related instances.
     */
    private Collection<InstanceElement> collectRelatedInstancesByRelationship(Relationship ieRelationship,
            String iePattern) throws AoException {
        // collect relations
        List<ApplicationRelation> relList = new ArrayList<>();
        for (ApplicationRelation rel : this.corbaCache.getApplicationRelations(this.aid)) {
            RelationRange invRelRange = rel.getInverseRelationRange();
            if (invRelRange.min == -2 || invRelRange.max == -2)
            {
                continue;
            }
            Relationship relShip = rel.getRelationship();
            if (ieRelationship.value() == Relationship._ALL_REL || ieRelationship.value() == relShip.value()) {
                relList.add(rel);
            }
        }
        // collect related instances
        List<InstanceElement> ieList = new ArrayList<>();
        for (ApplicationRelation applRel : relList) {
            ieList.addAll(Arrays.asList(collectRelatedInstances(applRel, iePattern)));
        }
        return ieList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#createRelation(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.InstanceElement)
     */
    public void createRelation(ApplicationRelation applRel, InstanceElement instElem) throws AoException {
        // check if relation belongs to instance application element
        if (aid != ODSHelper.asJLong(applRel.getElem1().getId())) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "ApplicationRelation '"
                    + applRel.getRelationName() + "' is not defined at application element '"
                    + getApplicationElement().getName() + "'");
        }
        // check if inverse relation belongs to other instance application element
        if (ODSHelper.asJLong(instElem.getApplicationElement().getId()) != ODSHelper.asJLong(applRel.getElem2().getId())) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "ApplicationRelation '"
                    + applRel.getInverseRelationName() + "' is not defined at application element '"
                    + instElem.getApplicationElement().getName() + "'");
        }
        // create relation
        List<Long> otherIids = new ArrayList<>();
        otherIids.add(ODSHelper.asJLong(instElem.getId()));
        this.corbaCache.createInstanceRelations(this.aid, this.iid, applRel, otherIids);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#removeRelation(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.InstanceElement)
     */
    public void removeRelation(ApplicationRelation applRel, InstanceElement instElem) throws AoException {
        // check if relation belongs to instance application element
        if (this.aid != ODSHelper.asJLong(applRel.getElem1().getId())) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "ApplicationRelation '"
                    + applRel.getRelationName() + "' is not defined at application element '"
                    + getApplicationElement().getName() + "'");
        }
        // remove all other relation
        if (instElem == null) {
            Collection<Long> otherIids = this.corbaCache.getRelatedInstanceIds(aid, iid, applRel);
            this.corbaCache.removeInstanceRelations(aid, iid, applRel, otherIids);
        }
        // remove a certain relation
        else {
            // check if inverse relation belongs to other instance application
            // element
            if (ODSHelper.asJLong(instElem.getApplicationElement().getId()) != ODSHelper.asJLong(applRel.getElem2()
                                                                                                        .getId())) {
                throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "ApplicationRelation '"
                        + applRel.getInverseRelationName() + "' is not defined at application element '"
                        + instElem.getApplicationElement().getName() + "'");
            }
            List<Long> l = new ArrayList<>();
            l.add(ODSHelper.asJLong(instElem.getId()));
            this.corbaCache.removeInstanceRelations(this.aid, this.iid, applRel, l);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getAsamPath()
     */
    public String getAsamPath() throws AoException {
        StringBuilder sb = new StringBuilder();

        // check if environment application element exists
        InstanceElement envIe = this.corbaCache.getEnvironmentInstance(this.instancePOA);
        if (envIe != null) {
            sb.append(buildAsamPathPart(envIe));
        }

        // collect a path parts recursively
        List<String> paths = new LinkedList<>();
        InstanceElement currentIe = this.corbaCache.getInstanceById(instancePOA, aid, iid);
        while (currentIe != null) {
            StringBuilder partSb = new StringBuilder();

            // skip environment
            if (currentIe.getApplicationElement().getBaseElement().getType().equals("AoEnvironment")) {
                break;
            }

            // add to paths
            partSb.append(buildAsamPathPart(currentIe));

            // navigate to father
            InstanceElementIterator fatherIeIter = currentIe.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
            currentIe = (fatherIeIter.getCount() > 0) ? fatherIeIter.nextOne() : null;
            fatherIeIter.destroy();

            paths.add(partSb.toString());
        }

        // build ASAM path
        Collections.reverse(paths);
        for (String path : paths) {
            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * Builds the ASAM path part of an instance element.
     * <p>
     * This part contains the application element name, the instance name and optionally the version attribute value.
     * 
     * @param ie The instance.
     * @return The ASAM path part string.
     * @throws AoException Error reading values.
     */
    private String buildAsamPathPart(InstanceElement ie) throws AoException {
        StringBuilder sb = new StringBuilder();
        // application element name (mandatory)
        ApplicationElement ae = ie.getApplicationElement();
        sb.append("/[");
        sb.append(PatternUtil.escapeNameForASAMPath(ae.getName()));
        sb.append("]");
        // instance name (mandatory)
        sb.append(PatternUtil.escapeNameForASAMPath(ie.getName()));
        // version (optional)
        if (corbaCache.getApplicationAttributeByBaseName(ODSHelper.asJLong(ae.getId()), "version") != null) {
            NameValueUnit versionValue = ie.getValueByBaseName("version");
            if (versionValue.value.flag == 15 && versionValue.value.u.stringVal().length() > 0) {
                sb.append(";");
                sb.append(PatternUtil.escapeNameForASAMPath(versionValue.value.u.stringVal()));
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#destroy()
     */
    public void destroy() throws AoException {
        try {
            byte[] id = this.modelPOA.servant_to_id(this);
            this.modelPOA.deactivate_object(id);
        } catch (WrongPolicy | ObjectNotActive | ServantNotActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#shallowCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement shallowCopy(String newName, String newVersion) throws AoException {
        InstanceElementCopyHelper copyHelper = new InstanceElementCopyHelper(corbaCache);
        InstanceElement ieToCopy = this.corbaCache.getInstanceById(instancePOA, aid, iid);
        return copyHelper.shallowCopy(ieToCopy, newName, newVersion);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#deepCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement deepCopy(String newName, String newVersion) throws AoException {
        InstanceElementCopyHelper copyHelper = new InstanceElementCopyHelper(corbaCache);
        InstanceElement ieToCopy = this.corbaCache.getInstanceById(instancePOA, aid, iid);
        return copyHelper.deepCopy(ieToCopy, newName, newVersion);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#upcastMeasurement()
     */
    public Measurement upcastMeasurement() throws AoException {
        // check if application element is of type 'AoMeasurement'
        String beName = getApplicationElement().getBaseElement().getType();
        if (!beName.equalsIgnoreCase("AoMeasurement")) {
            throw new AoException(ErrorCode.AO_INVALID_BASETYPE, SeverityFlag.ERROR, 0,
                                  "InstanceElement is not of base type 'AoMeasurement'");
        }

        byte[] oid = CorbaAtfxCache.toBytArray(new long[] { 1, aid, iid }); // 1=Measurement
        org.omg.CORBA.Object obj;
        try {
            obj = instancePOA.create_reference_with_id(oid, MeasurementHelper.id());
        } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        return MeasurementHelper.unchecked_narrow(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#upcastSubMatrix()
     */
    public SubMatrix upcastSubMatrix() throws AoException {
        // check if application element is of type 'AoSubMatrix'
        String beName = getApplicationElement().getBaseElement().getType();
        if (!beName.equalsIgnoreCase("AoSubMatrix")) {
            throw new AoException(ErrorCode.AO_INVALID_BASETYPE, SeverityFlag.ERROR, 0,
                                  "InstanceElement is not of base type 'AoSubMatrix'!");
        }

        byte[] oid = CorbaAtfxCache.toBytArray(new long[] { 2, aid, iid }); // 2=SubMatrix
        org.omg.CORBA.Object obj;
        try {
            obj = instancePOA.create_reference_with_id(oid, SubMatrixHelper.id());
        } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        return SubMatrixHelper.unchecked_narrow(obj);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#upcastODSFile()
     */
    public ODSFile upcastODSFile() throws AoException {
        // check if application element is of type 'AoFile'
        String beName = getApplicationElement().getBaseElement().getType();
        if (!beName.equalsIgnoreCase("AoFile")) {
            throw new AoException(ErrorCode.AO_INVALID_BASETYPE, SeverityFlag.ERROR, 0,
                                  "InstanceElement is not of base type 'AoFile'");
        }

        byte[] oid = CorbaAtfxCache.toBytArray(new long[] { 4, aid, iid }); // 4=File
        org.omg.CORBA.Object obj;
        try {
            obj = instancePOA.create_reference_with_id(oid, ODSFileHelper.id());
        } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        return ODSFileHelper.unchecked_narrow(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#createRelatedInstances(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.NameValueSeqUnit[], org.asam.ods.ApplicationRelationInstanceElementSeq[])
     */
    public InstanceElement[] createRelatedInstances(ApplicationRelation applRel, NameValueSeqUnit[] attributes,
            ApplicationRelationInstanceElementSeq[] relatedInstances) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'createRelatedInstances' not implemented");
    }

    /***********************************************************************************
     * security
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getRights()
     */
    public ACL[] getRights() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'getRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setRights(org.asam.ods.InstanceElement, int, org.asam.ods.RightsSet)
     */
    public void setRights(InstanceElement usergroup, int rights, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'setRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getInitialRights()
     */
    public InitialRight[] getInitialRights() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setInitialRights(org.asam.ods.InstanceElement, int,
     *      org.asam.ods.T_LONGLONG, org.asam.ods.RightsSet)
     */
    public void setInitialRights(InstanceElement usergroup, int rights, T_LONGLONG refAid, RightsSet set)
            throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInitialRights' not implemented");
    }
}
