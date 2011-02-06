package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationInstanceElementSeq;
import org.asam.ods.AttrType;
import org.asam.ods.BaseAttribute;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementIteratorHelper;
import org.asam.ods.InstanceElementPOA;
import org.asam.ods.Measurement;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrix;
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

    private final POA poa;
    private final AtfxCache atfxCache;
    private final long aid;
    private final Map<String, TS_Value> instanceAttributes;

    private long iid;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param atfxCache The ATFX cache.
     * @param aid The application element id.
     */
    public InstanceElementImpl(POA poa, AtfxCache atfxCache, long aid, long iid) {
        this.poa = poa;
        this.atfxCache = atfxCache;
        this.aid = aid;
        this.iid = iid;
        this.instanceAttributes = new LinkedHashMap<String, TS_Value>();
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
        return ODSHelper.getStringVal(getValueByBaseName("name"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setName(java.lang.String)
     */
    public void setName(String iaName) throws AoException {
        String attrName = this.atfxCache.getApplicationAttributeByBaName(this.aid, "name").getName();
        setValue(ODSHelper.createStringNVU(attrName, iaName));
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
            list.addAll(this.instanceAttributes.keySet());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValue(java.lang.String)
     */
    public NameValueUnit getValue(String attrName) throws AoException {
        // check if instance attribute
        TS_Value value = this.instanceAttributes.get(attrName);
        // no instance attribute, check application attribute
        if (value == null) {
            value = this.atfxCache.getInstanceValue(this.aid, this.iid, attrName);
        }
        // value not yet set
        if (value == null) {
            value = ODSHelper.createEmptyTS_Value(getApplicationElement().getAttributeByName(attrName).getDataType());
            this.atfxCache.setInstanceValue(this.aid, this.iid, attrName, value);
        }
        return new NameValueUnit(attrName, value, "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueSeq(java.lang.String[])
     */
    public NameValueUnit[] getValueSeq(String[] attrNames) throws AoException {
        List<NameValueUnit> list = new ArrayList<NameValueUnit>();
        for (String attrName : attrNames) {
            list.add(getValue(attrName));
        }
        return list.toArray(new NameValueUnit[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getValueByBaseName(java.lang.String)
     */
    public NameValueUnit getValueByBaseName(String baseAttrName) throws AoException {
        ApplicationAttribute aa = getApplicationElement().getAttributeByBaseName(baseAttrName);
        return getValue(aa.getName());
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
        // check if instance attribute
        if (this.instanceAttributes.containsKey(nvu.valName)) {
            this.instanceAttributes.put(nvu.valName, nvu.value);
        }
        // application attribute
        else {
            ApplicationAttribute applAttr = getApplicationElement().getAttributeByName(nvu.valName);
            // check if id has been updated
            BaseAttribute baseAttr = applAttr.getBaseAttribute();
            if (baseAttr != null && baseAttr.getName().equals("id")) {
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                      "Updating the id of an instance element is not allowed!");
//
//                this.atfxCache.updateInstanceId(aid, this.iid, newIid);
//                this.iid = newIid;
            }
            this.atfxCache.setInstanceValue(aid, this.iid, nvu.valName, nvu.value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#setValueSeq(org.asam.ods.NameValueUnit[])
     */
    public void setValueSeq(NameValueUnit[] values) throws AoException {
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
                                  "Empty instance attribute is name not allowed");
        }
        // check for existing application attribute
        if (this.atfxCache.getApplicationAttributeByName(this.aid, instAttr.valName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An ApplicationAttribute with name '" + instAttr.valName + "' already exists");
        }
        // check for existing instance attribute
        if (this.instanceAttributes.containsKey(instAttr.valName)) {
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
        this.instanceAttributes.put(instAttr.valName, instAttr.value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#removeInstanceAttribute(java.lang.String)
     */
    public void removeInstanceAttribute(String attrName) throws AoException {
        if (!this.instanceAttributes.containsKey(attrName)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceAttribute '" + attrName
                    + "' not found");
        }
        this.instanceAttributes.remove(attrName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#renameInstanceAttribute(java.lang.String, java.lang.String)
     */
    public void renameInstanceAttribute(String oldName, String newName) throws AoException {
        // check if attribute exists
        if (!this.instanceAttributes.containsKey(oldName)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceAttribute '" + oldName
                    + "' not found");
        }
        // check for empty name
        if (newName == null || newName.length() < 1) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "Empty instance attribute is name not allowed");
        }
        // check for existing application attribute
        if (this.atfxCache.getApplicationAttributeByName(this.aid, newName) != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An ApplicationAttribute with name '" + newName + "' already exists");
        }
        // check for existing instance attribute
        if (this.instanceAttributes.containsKey(newName)) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0,
                                  "An InstanceAttribute with name '" + newName + "' already exists");
        }
        // rename
        this.instanceAttributes.put(newName, this.instanceAttributes.get(oldName));
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
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.poa, list.toArray(new String[0]));
            return NameIteratorHelper.narrow(this.poa.servant_to_reference(nIteratorImpl));
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
            InstanceElement[] ieAr = collectRelatedInstances(applRel, iePattern).toArray(new InstanceElement[0]);
            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.poa, ieAr);
            return InstanceElementIteratorHelper.narrow(this.poa.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    private Collection<InstanceElement> collectRelatedInstances(ApplicationRelation applRel, String iePattern)
            throws AoException {
        long otherAid = ODSHelper.asJLong(applRel.getElem2().getId());
        List<InstanceElement> list = new ArrayList<InstanceElement>();
        for (long otherIid : this.atfxCache.getRelatedInstanceIds(this.aid, this.iid, applRel)) {
            InstanceElement ie = this.atfxCache.getInstanceById(otherAid, otherIid);
            if (PatternUtil.nameFilterMatch(ie.getName(), iePattern)) {
                list.add(ie);
            }
        }
        return list;
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
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.poa, list.toArray(new String[0]));
            return NameIteratorHelper.narrow(this.poa.servant_to_reference(nIteratorImpl));
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
            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.poa, ieAr);
            return InstanceElementIteratorHelper.narrow(this.poa.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

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
            ieList.addAll(collectRelatedInstances(applRel, iePattern));
        }
        return ieList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#createRelation(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.InstanceElement)
     */
    public void createRelation(ApplicationRelation relation, InstanceElement instElem) throws AoException {
        this.atfxCache.createInstanceRelation(this.aid, this.iid, relation, ODSHelper.asJLong(instElem.getId()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#removeRelation(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.InstanceElement)
     */
    public void removeRelation(ApplicationRelation applRel, InstanceElement instElem_nm) throws AoException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#createRelatedInstances(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.NameValueSeqUnit[], org.asam.ods.ApplicationRelationInstanceElementSeq[])
     */
    public InstanceElement[] createRelatedInstances(ApplicationRelation applRel, NameValueSeqUnit[] attributes,
            ApplicationRelationInstanceElementSeq[] relatedInstances) throws AoException {

        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#getAsamPath()
     */
    public String getAsamPath() throws AoException {
        StringBuffer sb = new StringBuffer();

        // check if environment application element exists
        InstanceElement envIe = this.atfxCache.getEnvironmentInstance();
        if (envIe != null) {
            sb.append(buildAsamPathPart(envIe));
        }

        // collect a path parts recursively
        List<String> paths = new LinkedList<String>();
        InstanceElement currentIe = _this();
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
        if (this.atfxCache.getApplicationAttributeByBaName(ODSHelper.asJLong(ae.getId()), "version") != null) {
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
     * @see org.asam.ods.InstanceElementOperations#upcastMeasurement()
     */
    public Measurement upcastMeasurement() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'upcastMeasurement' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#upcastSubMatrix()
     */
    public SubMatrix upcastSubMatrix() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'upcastSubMatrix' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#shallowCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement shallowCopy(String newName, String newVersion) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'shallowCopy' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementOperations#deepCopy(java.lang.String, java.lang.String)
     */
    public InstanceElement deepCopy(String newName, String newVersion) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'deepCopy' not implemented");
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
