package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationInstanceElementSeq;
import org.asam.ods.AttrType;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementIteratorHelper;
import org.asam.ods.InstanceElementPOA;
import org.asam.ods.Measurement;
import org.asam.ods.MeasurementHelper;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrix;
import org.asam.ods.SubMatrixHelper;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.InstanceElement</code>.
 * 
 * @author Christian Rechner
 */
class InstanceElementImpl extends InstanceElementPOA {

    private static final Log LOG = LogFactory.getLog(InstanceElementImpl.class);

    protected final POA modelPOA;
    protected final POA instancePOA;
    protected final AtfxCache atfxCache;
    protected final long aid;
    protected final long iid;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public InstanceElementImpl(POA modelPOA, POA instancePOA, AtfxCache atfxCache, long aid, long iid) {
        this.modelPOA = modelPOA;
        this.instancePOA = instancePOA;
        this.atfxCache = atfxCache;
        this.aid = aid;
        this.iid = iid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getApplicationElement()
     */
    public ApplicationElement getApplicationElement() throws AoException {
        ApplicationElement ae = this.atfxCache.getApplicationElementById(aid);
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
        Long thisAid = ODSHelper.asJLong(getApplicationElement().getId());
        Long otherAid = ODSHelper.asJLong(compIeObj.getApplicationElement().getId());
        int res = thisAid.compareTo(otherAid);

        // compare instance ids
        if (res == 0) {
            Long thisIid = ODSHelper.asJLong(getId());
            Long otherIid = ODSHelper.asJLong(compIeObj.getId());
            res = thisIid.compareTo(otherIid);
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
        Integer attrNo = this.atfxCache.getAttrNoByBaName(this.aid, "name");
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Not application attribute of base attribute 'name' found for aid=" + aid);
        }

        // set value
        TS_Value value = this.atfxCache.getInstanceValue(aid, attrNo, iid);
        return (value == null) ? "" : value.u.stringVal();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setName(java.lang.String)
     */
    public void setName(String iaName) throws AoException {
        Integer attrNo = this.atfxCache.getAttrNoByBaName(this.aid, "name");
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Not application attribute of base attribute 'name' found for aid=" + aid);
        }

        // create value
        TS_Value value = new TS_Value();
        value.flag = (short) 15;
        value.u = new TS_Union();
        value.u.stringVal(iaName);

        // set value
        this.atfxCache.setInstanceValue(this.aid, this.iid, attrNo, value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#listAttributes(java.lang.String, org.asam.ods.AttrType)
     */
    public String[] listAttributes(String iaPattern, AttrType aType) throws AoException {
        List<String> list = new ArrayList<String>();
        // application attributes
        if (aType != AttrType.INSTATTR_ONLY) {
            for (String aaName : this.atfxCache.listApplicationAttributes(this.aid)) {
                if (PatternUtil.nameFilterMatch(aaName, iaPattern)) {
                    list.add(aaName);
                }
            }
        }
        // instance attributes
        if (aType != AttrType.APPLATTR_ONLY) {
            for (String iaName : this.atfxCache.listInstanceAttributes(aid, iid)) {
                if (PatternUtil.nameFilterMatch(iaName, iaPattern)) {
                    list.add(iaName);
                }
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValue(java.lang.String)
     */
    public NameValueUnit getValue(String aaName) throws AoException {
        // check if instance attribute
        TS_Value iaValue = this.atfxCache.getInstanceAttributeValue(aid, iid, aaName);
        if (iaValue != null) {
            return new NameValueUnit(aaName, iaValue, "");
        }

        // get attr number
        Integer attrNo = this.atfxCache.getAttrNoByName(aid, aaName);
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '" + aaName
                    + "' not found");
        }

        // no instance attribute, read application attribute
        TS_Value value = this.atfxCache.getInstanceValue(this.aid, attrNo, this.iid);
        String unitName = this.atfxCache.getUnitNameForAttr(this.aid, attrNo);

        return new NameValueUnit(aaName, value, unitName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueSeq(java.lang.String[])
     */
    public NameValueUnit[] getValueSeq(String[] attrNames) throws AoException {
        NameValueUnit[] values = new NameValueUnit[attrNames.length];
        for (int i = 0; i < attrNames.length; i++) {
            values[i] = getValue(attrNames[i]);
        }
        return values;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueByBaseName(java.lang.String)
     */
    public NameValueUnit getValueByBaseName(String baseAttrName) throws AoException {
        Integer attrNo = this.atfxCache.getAttrNoByBaName(this.aid, baseAttrName);
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "No ApplicationAttribute of BaseAttribute '" + baseAttrName
                                          + "' found for ApplicationElement '" + getApplicationElement().getName()
                                          + "'");
        }

        String aaName = this.atfxCache.getApplicationAttribute(aid, attrNo).getName();
        return getValue(aaName);
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
        Integer attrNo = this.atfxCache.getAttrNoByName(this.aid, nvu.valName);

        // instance attribute?
        if ((attrNo == null) && (this.atfxCache.getInstanceAttributeValue(aid, iid, nvu.valName) != null)) {
            this.atfxCache.setInstanceAttributeValue(aid, iid, nvu.valName, nvu.value);
            return;
        }

        // application attribute exists?
        else if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '" + nvu.valName
                    + "' not found");
        }

        // check if id has been updated, not allowed!
        Integer baseAttrNo = this.atfxCache.getAttrNoByBaName(this.aid, "id");
        if (baseAttrNo != null && baseAttrNo.equals(attrNo)) {
            throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                  "Updating the id of an instance element is not allowed!");
        }
        this.atfxCache.setInstanceValue(aid, this.iid, attrNo, nvu.value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setValueSeq(org.asam.ods.NameValueUnit[])
     */
    public void setValueSeq(NameValueUnit[] values) throws AoException {

        // trick for 'AoLocalColumn': sort the attribute 'sequence_representation' BEFORE the attribute 'values'. This
        // is needed for the write_mode 'file'.
        if (this.atfxCache.getAidsByBaseType("aolocalcolumn").contains(this.aid)) {
            Arrays.sort(values, new Comparator<NameValueUnit>() {

                public int compare(NameValueUnit o1, NameValueUnit o2) {
                    Integer i1 = atfxCache.getAttrNoByName(aid, o2.valName);
                    Integer i2 = atfxCache.getAttrNoByBaName(aid, "sequence_representation");
                    boolean isSeqRepVal = i1.equals(i2);
                    return isSeqRepVal ? 1 : 0;
                }
            });
        }

        for (NameValueUnit nvu : values) {
            setValue(nvu);
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
        // check for empty name
        if (instAttr.valName == null || instAttr.valName.length() < 1) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "Empty instance attribute name is not allowed");
        }
        // check for existing application attribute
        if (this.atfxCache.getAttrNoByName(this.aid, instAttr.valName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An ApplicationAttribute with name '" + instAttr.valName + "' already exists");
        }
        // check for existing instance attribute
        if (this.atfxCache.getInstanceAttributeValue(aid, iid, instAttr.valName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An InstanceAttribute with name '" + instAttr.valName + "' already exists");
        }
        // check data type
        DataType dt = instAttr.value.u.discriminator();
        if ((dt != DataType.DT_STRING) && (dt != DataType.DT_FLOAT) && (dt != DataType.DT_DOUBLE)
                && (dt != DataType.DT_BYTE) && (dt != DataType.DT_SHORT) && (dt != DataType.DT_LONG)
                && (dt != DataType.DT_LONGLONG) && (dt != DataType.DT_DATE)) {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "DataType is no allowed for InstanceAttributes: " + ODSHelper.dataType2String(dt));
        }
        this.atfxCache.setInstanceAttributeValue(aid, iid, instAttr.valName, instAttr.value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#removeInstanceAttribute(java.lang.String)
     */
    public void removeInstanceAttribute(String attrName) throws AoException {
        if (this.atfxCache.getInstanceAttributeValue(aid, iid, attrName) == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceAttribute '" + attrName
                    + "' not found");
        }
        this.atfxCache.removeInstanceAttribute(aid, iid, attrName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#renameInstanceAttribute(java.lang.String, java.lang.String)
     */
    public void renameInstanceAttribute(String oldName, String newName) throws AoException {
        // check if attribute exists
        if (this.atfxCache.getInstanceAttributeValue(aid, iid, oldName) == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceAttribute '" + oldName
                    + "' not found");
        }
        // check for empty name
        if (newName == null || newName.length() < 1) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "Empty instance attribute is name not allowed");
        }
        // check for existing application attribute
        if (this.atfxCache.getAttrNoByName(this.aid, newName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An ApplicationAttribute with name '" + newName + "' already exists");
        }
        // check for existing instance attribute
        if (this.atfxCache.getInstanceAttributeValue(aid, iid, newName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An InstanceAttribute with name '" + newName + "' already exists");
        }
        // rename
        TS_Value value = this.atfxCache.getInstanceAttributeValue(aid, iid, oldName);
        this.atfxCache.removeInstanceAttribute(aid, iid, oldName);
        this.atfxCache.setInstanceAttributeValue(aid, iid, newName, value);
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
            List<String> list = new ArrayList<String>();
            for (InstanceElement ie : collectRelatedInstances(applRel, iePattern)) {
                list.add(ie.getName());
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
     * @see org.asam.ods.InstanceElementOperations#getRelatedInstances(org.asam.ods.ApplicationRelation,
     *      java.lang.String)
     */
    public InstanceElementIterator getRelatedInstances(ApplicationRelation applRel, String iePattern)
            throws AoException {
        try {
            InstanceElement[] ieAr = collectRelatedInstances(applRel, iePattern);
            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.modelPOA, ieAr);
            return InstanceElementIteratorHelper.narrow(this.modelPOA.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
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
        Collection<Long> otherIids = this.atfxCache.getRelatedInstanceIds(this.aid, this.iid, applRel);

        // pattern 'all'
        if (iePattern.equals("*")) {
            InstanceElement[] ies = new InstanceElement[otherIids.size()];
            int i = 0;
            for (long otherIid : otherIids) {
                ies[i] = this.atfxCache.getInstanceById(this.instancePOA, otherAid, otherIid);
                i++;
            }
            return ies;
        }
        // filter by pattern
        else {
            List<InstanceElement> list = new ArrayList<InstanceElement>();
            for (long otherIid : otherIids) {
                InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, otherAid, otherIid);
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
            List<String> list = new ArrayList<String>();
            for (InstanceElement ie : collectRelatedInstancesByRelationship(ieRelationship, iePattern)) {
                list.add(ie.getName());
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
     * @see org.asam.ods.InstanceElementOperations#getRelatedInstancesByRelationship(org.asam.ods.Relationship,
     *      java.lang.String)
     */
    public InstanceElementIterator getRelatedInstancesByRelationship(Relationship ieRelationship, String iePattern)
            throws AoException {
        try {
            InstanceElement[] ieAr = collectRelatedInstancesByRelationship(ieRelationship, iePattern).toArray(new InstanceElement[0]);
            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.modelPOA, ieAr);
            return InstanceElementIteratorHelper.narrow(this.modelPOA.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
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
        List<ApplicationRelation> relList = new ArrayList<ApplicationRelation>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(this.aid)) {
            Relationship relShip = rel.getRelationship();
            if (ieRelationship.value() == Relationship._ALL_REL) {
                relList.add(rel);
            } else if (ieRelationship.value() == relShip.value()) {
                relList.add(rel);
            }
        }
        // collect related instances
        List<InstanceElement> ieList = new ArrayList<InstanceElement>();
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
        List<Long> otherIids = new ArrayList<Long>();
        otherIids.add(ODSHelper.asJLong(instElem.getId()));
        this.atfxCache.createInstanceRelations(this.aid, this.iid, applRel, otherIids);
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
            Collection<Long> otherIids = this.atfxCache.getRelatedInstanceIds(aid, iid, applRel);
            this.atfxCache.removeInstanceRelations(aid, iid, applRel, otherIids);
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
            List<Long> l = new ArrayList<Long>();
            l.add(ODSHelper.asJLong(instElem.getId()));
            this.atfxCache.removeInstanceRelations(this.aid, this.iid, applRel, l);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getAsamPath()
     */
    public String getAsamPath() throws AoException {
        StringBuffer sb = new StringBuffer();

        // check if environment application element exists
        InstanceElement envIe = this.atfxCache.getEnvironmentInstance(this.modelPOA, this.instancePOA);
        if (envIe != null) {
            sb.append(buildAsamPathPart(envIe));
        }

        // collect a path parts recursively
        List<String> paths = new LinkedList<String>();
        InstanceElement currentIe = this.atfxCache.getInstanceById(instancePOA, aid, iid);
        while (currentIe != null) {
            StringBuffer partSb = new StringBuffer();

            // skip environment
            if (currentIe != null
                    && currentIe.getApplicationElement().getBaseElement().getType().equals("AoEnvironment")) {
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
        StringBuffer sb = new StringBuffer();
        // application element name (mandatory)
        ApplicationElement ae = ie.getApplicationElement();
        sb.append("/[");
        sb.append(PatternUtil.escapeNameForASAMPath(ae.getName()));
        sb.append("]");
        // instance name (mandatory)
        sb.append(PatternUtil.escapeNameForASAMPath(ie.getName()));
        // version (optional)
        if (this.atfxCache.getAttrNoByBaName(ODSHelper.asJLong(ae.getId()), "version") != null) {
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
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#shallowCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement shallowCopy(String newName, String newVersion) throws AoException {
        InstanceElementCopyHelper copyHelper = new InstanceElementCopyHelper(atfxCache);
        InstanceElement ieToCopy = this.atfxCache.getInstanceById(instancePOA, aid, iid);
        return copyHelper.shallowCopy(ieToCopy, newName, newVersion);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#deepCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement deepCopy(String newName, String newVersion) throws AoException {
        InstanceElementCopyHelper copyHelper = new InstanceElementCopyHelper(atfxCache);
        InstanceElement ieToCopy = this.atfxCache.getInstanceById(instancePOA, aid, iid);
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

        byte[] oid = AtfxCache.toByta(new long[] { 1, aid, iid }); // 1=Measurement
        org.omg.CORBA.Object obj = instancePOA.create_reference_with_id(oid, MeasurementHelper.id());
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

        byte[] oid = AtfxCache.toByta(new long[] { 2, aid, iid }); // 2=SubMatrix
        org.omg.CORBA.Object obj = instancePOA.create_reference_with_id(oid, SubMatrixHelper.id());
        return SubMatrixHelper.unchecked_narrow(obj);
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
