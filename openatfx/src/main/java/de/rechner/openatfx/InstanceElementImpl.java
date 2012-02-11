package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
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
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
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

    private final POA modelPOA;
    private final POA instancePOA;
    private final AtfxCache atfxCache;
    private final long aid;
    private final long iid;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param aid The application element id.
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
        return ODSHelper.getStringVal(getValueByBaseName("name"));
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
        value.flag = 15;
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
        if (isExternalComponentValue(aaName)) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Reading the 'values' of external components is not yet implemented");
        }

        // check if instance attribute
        TS_Value value = this.atfxCache.getInstanceAttributeValue(aid, iid, aaName);

        // no instance attribute, check application attribute
        if (value == null) {
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, aaName);
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '" + aaName
                        + "' not found");
            }
            value = this.atfxCache.getInstanceValue(this.aid, this.iid, attrNo);

            // value not found, return empty
            if (value == null) {
                ApplicationAttribute aa = this.atfxCache.getApplicationAttribute(aid, attrNo);
                if (aa == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '"
                            + aaName + "' not found");
                }
                DataType dt = aa.getDataType();
                // special case: attribute "values" of "AoLocalColumn" is not set!
                if (dt == DataType.DT_UNKNOWN && isLocalColumnValuesAttribute(aaName)) {
                    dt = getDataTypeForLocalColumnValues();
                }
                value = ODSHelper.createEmptyTS_Value(dt);
                this.atfxCache.setInstanceValue(aid, iid, attrNo, value);
            }

        }

        return new NameValueUnit(aaName, value, "");
    }

    /**
     * In case this InstanceElement is from the ApplicationElement derived from "AoLocalColumn", the datatype of the
     * related "AoMeasurementQuantity" instance is returned.
     * 
     * @throws AoException
     */
    private DataType getDataTypeForLocalColumnValues() throws AoException {
        ApplicationRelation[] rels = getApplicationElement().getRelationsByBaseName("measurement_quantity");
        if (rels.length > 0) {
            ApplicationRelation rel = rels[0];

            Collection<Long> meaQuaIids = this.atfxCache.getRelatedInstanceIds(aid, iid, rels[0]);
            if (!meaQuaIids.isEmpty()) {
                long meaQuaAid = ODSHelper.asJLong(rel.getElem2().getId());
                long meaQuaIid = meaQuaIids.iterator().next();
                Integer attrNoDt = this.atfxCache.getAttrNoByBaName(meaQuaAid, "datatype");
                if (attrNoDt != null) {
                    TS_Value dtValue = this.atfxCache.getInstanceValue(meaQuaAid, meaQuaIid, attrNoDt);
                    if (dtValue != null && dtValue.flag == 15 && dtValue.u.discriminator() == DataType.DT_ENUM) {
                        int val = dtValue.u.enumVal();
                        if (val == 1) { // DT_STRING
                            return DataType.DS_STRING;
                        } else if (val == 2) { // DT_SHORT
                            return DataType.DS_SHORT;
                        } else if (val == 3) { // DT_FLOAT
                            return DataType.DS_FLOAT;
                        } else if (val == 4) { // DT_BOOLEAN
                            return DataType.DS_BOOLEAN;
                        } else if (val == 5) { // DT_BYTE
                            return DataType.DS_BYTE;
                        } else if (val == 6) { // DT_LONG
                            return DataType.DS_LONG;
                        } else if (val == 7) { // DT_DOUBLE
                            return DataType.DS_DOUBLE;
                        } else if (val == 8) { // DT_LONGLONG
                            return DataType.DS_LONGLONG;
                        } else if (val == 10) { // DT_DATE
                            return DataType.DS_DATE;
                        } else if (val == 11) { // DT_BYTESTR
                            return DataType.DS_BYTESTR;
                        } else if (val == 14) { // DT_COMPLEX
                            return DataType.DS_COMPLEX;
                        } else if (val == 15) { // DT_DCOMPLEX
                            return DataType.DS_DCOMPLEX;
                        } else if (val == 28) { // DT_EXTERNALREFERENCE
                            return DataType.DS_EXTERNALREFERENCE;
                        } else if (val == 30) { // DT_ENUM
                            return DataType.DS_ENUM;
                        }
                    }
                }
            }
        }
        throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                              "Implementation problem at method 'getDataTypeForLocalColumnValues()' for instance '"
                                      + getAsamPath() + "'");
    }

    /**
     * Checks whether given attribute name is from base attribute 'values' of and this instance is from base element
     * 'AoLocalColumn'.
     * 
     * @param aaName The application attribute name.
     * @return True, if attribute is 'values.
     */
    private boolean isLocalColumnValuesAttribute(String aaName) {
        Set<Long> localColumnAids = this.atfxCache.getAidsByBaseType("aolocalcolumn");
        if (localColumnAids != null && localColumnAids.contains(aid)) {
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, aaName);
            Integer valuesAttrNo = this.atfxCache.getAttrNoByBaName(aid, "values");
            return (attrNo != null) && (valuesAttrNo != null) && (attrNo.equals(valuesAttrNo));
        }
        return false;
    }

    /**
     * Checks whether the value queried is an external component value of a local column.
     * 
     * @param aaName The application attribute name.
     * @throws AoException Error checking application attribute.
     */
    private boolean isExternalComponentValue(String aaName) throws AoException {
        Set<Long> localColumnAids = this.atfxCache.getAidsByBaseType("aolocalcolumn");
        if (localColumnAids != null && localColumnAids.contains(this.aid)) {

            Integer attrNo = this.atfxCache.getAttrNoByName(aid, aaName);
            Integer valuesAttrNo = this.atfxCache.getAttrNoByBaName(aid, "values");

            if (attrNo != null && valuesAttrNo != null && attrNo.equals(valuesAttrNo)) {
                NameValueUnit seqRep = this.getValueByBaseName("sequence_representation");
                int seqRepEnum = ODSHelper.getEnumVal(seqRep);
                // check if the sequence representation is 7(external_component), 8(raw_linear_external),
                // 9(raw_polynomial_external) or 11(raw_linear_calibrated_external)
                if (seqRepEnum == 7 || seqRepEnum == 8 || seqRepEnum == 9 || seqRepEnum == 11) {
                    return true;
                }
            }

        }
        return false;
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
        // check if instance attribute
        if (this.atfxCache.getInstanceAttributeValue(aid, iid, nvu.valName) != null) {
            this.atfxCache.setInstanceAttributeValue(aid, iid, nvu.valName, nvu.value);
        }

        // application attribute
        else {
            Integer attrNo = this.atfxCache.getAttrNoByName(this.aid, nvu.valName);
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '"
                        + nvu.valName + "' not found");
            }

            // check if id has been updated
            Integer baseAttrNo = this.atfxCache.getAttrNoByBaName(this.aid, "id");
            if (baseAttrNo != null && baseAttrNo.equals(attrNo)) {
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                      "Updating the id of an instance element is not allowed!");
            }
            this.atfxCache.setInstanceValue(aid, this.iid, attrNo, nvu.value);
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
            this.modelPOA.activate_object(nIteratorImpl);
            return NameIteratorHelper.narrow(this.modelPOA.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
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
            this.modelPOA.activate_object(ieIteratorImpl);
            return InstanceElementIteratorHelper.narrow(this.modelPOA.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
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
            this.modelPOA.activate_object(nIteratorImpl);
            return NameIteratorHelper.narrow(this.modelPOA.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
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
            this.modelPOA.activate_object(ieIteratorImpl);
            return InstanceElementIteratorHelper.narrow(this.modelPOA.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
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
        // check if inverse relation belongs to other instance application
        // element
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
