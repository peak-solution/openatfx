package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.asam.ods.ACL;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.ApplElemAccessPOA;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.QueryStructure;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.ResultSetExt;
import org.asam.ods.RightsSet;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelType;
import org.asam.ods.SelValueExt;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.ApplElemAccess</code>.
 * 
 * @author Christian Rechner
 */
class ApplElemAccessImpl extends ApplElemAccessPOA {

    private final AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param atfxCache The ATFX cache.
     */
    public ApplElemAccessImpl(POA poa, AtfxCache atfxCache) {
        this.atfxCache = atfxCache;
    }

    /***********************************************************************************
     * insert/update/delete
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#insertInstances(org.asam.ods.AIDNameValueSeqUnitId[])
     */
    public ElemId[] insertInstances(AIDNameValueSeqUnitId[] val) throws AoException {
        // check for empty data
        if (val == null || val.length < 1) {
            return new ElemId[0];
        }
        int numberOfRows = val[0].values.flag.length;

        // group by application element id and check if id attr is given
        Map<Long, AIDNameValueSeqUnitId> idColumns = new HashMap<Long, AIDNameValueSeqUnitId>();
        Map<Long, List<AIDNameValueSeqUnitId>> aeGroupColumns = new HashMap<Long, List<AIDNameValueSeqUnitId>>();
        for (AIDNameValueSeqUnitId column : val) {
            long aid = ODSHelper.asJLong(column.attr.aid);
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, column.attr.aaName);
            Integer idAttrNo = this.atfxCache.getAttrNoByBaName(aid, "id");
            if (idAttrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "Not application attribute of base attribute 'id' found for aid=" + aid);
            }
            // add to ae group
            List<AIDNameValueSeqUnitId> list = aeGroupColumns.get(aid);
            if (list == null) {
                list = new ArrayList<AIDNameValueSeqUnitId>();
                aeGroupColumns.put(aid, list);
            }
            list.add(column);
            // check for id column
            if (attrNo.equals(idAttrNo)) {
                idColumns.put(aid, column);
            }
        }

        // create instances per application element
        List<ElemId> elemIdList = new ArrayList<ElemId>();
        for (long aid : aeGroupColumns.keySet()) {
            AIDNameValueSeqUnitId idCol = idColumns.get(aid);
            // iterate over rows
            for (int row = 0; row < numberOfRows; row++) {
                // fetch or create id
                long iid = 0;
                if (idCol != null) {
                    iid = ODSHelper.asJLong(ODSHelper.tsValueSeq2tsValue(idCol.values, row).u.longlongVal());
                } else {
                    iid = this.atfxCache.nextIid(aid);
                }
                // create instance
                this.atfxCache.addInstance(aid, iid);
                // put values
                for (AIDNameValueSeqUnitId avsui : aeGroupColumns.get(aid)) {
                    TS_Value value = ODSHelper.tsValueSeq2tsValue(avsui.values, row);
                    Integer attrNo = this.atfxCache.getAttrNoByName(aid, avsui.attr.aaName);
                    this.atfxCache.setInstanceValue(aid, iid, attrNo, value);
                }
                // TODO: create relations

                elemIdList.add(new ElemId(ODSHelper.asODSLongLong(aid), ODSHelper.asODSLongLong(iid)));
            }
        }

        return elemIdList.toArray(new ElemId[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#updateInstances(org.asam.ods.AIDNameValueSeqUnitId[])
     */
    public void updateInstances(AIDNameValueSeqUnitId[] val) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'updateInstances' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#deleteInstances(org.asam.ods.T_LONGLONG, org.asam.ods.T_LONGLONG[])
     */
    public void deleteInstances(T_LONGLONG aid, T_LONGLONG[] instIds) throws AoException {
        // check if Application Element exists
        long jAid = ODSHelper.asJLong(aid);
        if (this.atfxCache.getApplicationElementById(jAid) == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "ApplicationElement with id="
                    + jAid + " not found");
        }
        // delete each instance
        for (T_LONGLONG instId : instIds) {
            long jIid = ODSHelper.asJLong(instId);
            if (!this.atfxCache.instanceExists(jAid, jIid)) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "InstanceElement not found ElemId aid=" + jAid + ",iid=" + jIid);
            }
            this.atfxCache.removeInstance(jAid, ODSHelper.asJLong(instId));
        }
    }

    /***********************************************************************************
     * get/set relations
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getRelInst(org.asam.ods.ElemId, java.lang.String)
     */
    public T_LONGLONG[] getRelInst(ElemId elem, String relName) throws AoException {
        // check ElemId
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        if (!this.atfxCache.instanceExists(aid, iid)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
        }
        // lookup relation
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, relName);
        if (applRel == null || applRel.getElem2() == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation not found aid="
                    + aid + ",relName=" + relName);
        }
        // return related instance ids
        Collection<Long> relInstIidSet = this.atfxCache.getRelatedInstanceIds(aid, iid, applRel);
        T_LONGLONG[] relInstIids = new T_LONGLONG[relInstIidSet.size()];
        int idx = 0;
        for (Long relInstIid : relInstIidSet) {
            relInstIids[idx] = ODSHelper.asODSLongLong(relInstIid);
            idx++;
        }
        return relInstIids;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setRelInst(org.asam.ods.ElemId, java.lang.String,
     *      org.asam.ods.T_LONGLONG[], org.asam.ods.SetType)
     */
    public void setRelInst(ElemId elem, String relName, T_LONGLONG[] instIds, SetType type) throws AoException {
        // check ElemId
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        if (!this.atfxCache.instanceExists(aid, iid)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
        }

        // check relation
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, relName);
//        if (applRel == null || applRel.getElem2() == null) {
//            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation not found aid="
//                    + aid + ",relName=" + relName);
//        }

        // alter relations
        Collection<Long> otherIidsToSet = new ArrayList<Long>();
        Collection<Long> otherIidsToRemove = new ArrayList<Long>();
        for (T_LONGLONG otherIidT : instIds) {
            long otherIid = ODSHelper.asJLong(otherIidT);
            if (type == SetType.INSERT || type == SetType.UPDATE || type == SetType.APPEND) {
                otherIidsToSet.add(otherIid);
            } else if (type == SetType.REMOVE) {
                otherIidsToSet.add(otherIid);
            }
        }

        this.atfxCache.createInstanceRelations(aid, iid, applRel, otherIidsToSet);
        this.atfxCache.removeInstanceRelations(aid, iid, applRel, otherIidsToRemove);
    }

    /***********************************************************************************
     * queries
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstances(org.asam.ods.QueryStructure, int)
     */
    public ElemResultSet[] getInstances(QueryStructure aoq, int how_many) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstancesExt(org.asam.ods.QueryStructureExt, int)
     */
    public ResultSetExt[] getInstancesExt(QueryStructureExt aoq, int how_many) throws AoException {
        SelValueExt condition = null;
        Long aid = null;
        // this method does only process a certain kind of query:
        // - selects from only one application element
        // - no joins
        // - no group by's;
        // - no aggregate functions
        // - no order by
        // - only one or no string condition
        // - allowed condition selOpCodes: SelOpcode.CI_EQ and SelOpcode.CI_LIKE

        if (aoq == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt must not be null");
        }
        // query must contain at least one select statement. otherwise just return an empty result set
        if (aoq.anuSeq == null || aoq.anuSeq.length < 1) {
            return new ResultSetExt[] { new ResultSetExt(new ElemResultSetExt[0], null) };
        }
        // do not allow 'group by'
        if (aoq.groupBy != null && aoq.groupBy.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains 'group by' statements");
        }
        // do not allow joins
        if (aoq.joinSeq != null && aoq.joinSeq.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains joins");
        }
        // do not allow 'order by'
        if (aoq.orderBy != null && aoq.orderBy.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains 'order by' statements");
        }

        // do not allow null AIDNames, aggregate functions or null attribute names in any of the selects. Also do not
        // allow more than one application element.
        for (SelAIDNameUnitId anu : aoq.anuSeq) {
            if (anu.attr == null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Invalid SelAIDNameUnitId found: AIDName was null");
            }
            if (anu.aggregate == null && anu.aggregate != AggrFunc.NONE) {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                      "Invalid SelAIDNameUnitId found: Aggregate functions not supported");
            }
            if (anu.attr.aaName == null || anu.attr.aaName.trim().length() < 1) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Invalid SelAIDNameUnitId found: Application attribute name was null or empty");
            }
            if (aid == null) {
                aid = ODSHelper.asJLong(anu.attr.aid);
            } else {
                // compare the application element ids. they must be equal, or else the query contains select statements
                // for more than one application element
                if (ODSHelper.asJLong(anu.attr.aid) != aid) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "QueryStructureExt not supported: Contains select statements for more than one application element");
                }
            }
        }

        // only allow one condition
        if (aoq.condSeq != null && aoq.condSeq.length > 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains more than one condition");
        }

        // get the condition if there is one
        if (aoq.condSeq != null && aoq.condSeq.length > 0) {
            SelItem cond = aoq.condSeq[0];
            if (cond.discriminator() != SelType.SEL_VALUE_TYPE) {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                      "QueryStructureExt not supported: Condition is not of the SelType '"
                                              + SelType.SEL_VALUE_TYPE + "'");
            }

            condition = cond.value();
        }

        // only allow string conditions
        if (condition != null && condition.value.u.discriminator() != DataType.DT_STRING) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Condition DataType '"
                    + condition.value.u.discriminator() + "' not supported. Only '" + DataType.DT_STRING
                    + "' is implemented");
        }

        // only allow SelOpcode.CI_EQ and SelOpcode.CI_LIKE
        if (condition != null && condition.oper != SelOpcode.CI_EQ && condition.oper != SelOpcode.CI_LIKE) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Condition '" + condition.oper
                                          + "' not supported. Only '" + SelOpcode.CI_EQ + "' and '" + SelOpcode.CI_LIKE
                                          + "' are implemented");
        }

        // get the application element
        ApplicationElement selectedAe = atfxCache.getApplicationElementById(aid);
        if (selectedAe == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt invalid: Given AID '" + aid
                                          + "' does not reference a existing application element");
        }

        // make sure the condition and selected attributes exist
        List<String> applicationAttributeNameList = new ArrayList<String>();
        for (ApplicationAttribute attr : selectedAe.getAttributes("*")) {
            applicationAttributeNameList.add(attr.getName());
        }
        for (SelAIDNameUnitId selectedAttribute : aoq.anuSeq) {
            if (!applicationAttributeNameList.contains(selectedAttribute.attr.aaName)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "QueryStructureExt invalid: Selected attribute '" + selectedAttribute
                                              + "' does not exist in selected application element");
            }
        }
        if (condition != null) {
            String conditionAttributeName = condition.attr.attr.aaName;
            if (!applicationAttributeNameList.contains(conditionAttributeName)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "QueryStructureExt invalid: Condition attribute '" + conditionAttributeName
                                              + "' does not exist in selected application element");
            }
        }

        // get all queries instances -> get all instances, then filter manually, because the 'getInstances()' method
        // uses case sensitivity
        Set<Long> ieIds = atfxCache.getInstanceIds(aid);
        List<Long> resultList = new ArrayList<Long>();
        for (Long iid : ieIds) {
            if (condition == null) {
                resultList.add(iid);
            } else {
                String conditionString = condition.value.u.stringVal();
                Integer attrNo = this.atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                TS_Value instanceAttributeValue = atfxCache.getInstanceValue(aid, iid, attrNo);
                if (instanceAttributeValue != null && instanceAttributeValue.u != null
                        && instanceAttributeValue.u.stringVal() != null) {
                    String instanceAttributeStringValue = instanceAttributeValue.u.stringVal();
                    if (PatternUtil.nameFilterMatchCI(instanceAttributeStringValue, conditionString)) {
                        resultList.add(iid);
                    }
                }
            }
        }

        // build the result set
        List<NameValueSeqUnitId> resultValues = new ArrayList<NameValueSeqUnitId>();
        long[] iids = new long[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            iids[i] = resultList.get(i);
        }
        for (SelAIDNameUnitId selectedAttribute : aoq.anuSeq) {
            NameValueSeqUnitId nvsui = new NameValueSeqUnitId();
            nvsui.valName = selectedAttribute.attr.aaName;
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, nvsui.valName);
            nvsui.value = atfxCache.listInstanceValues(aid, iids, attrNo);
            nvsui.unitId = selectedAttribute.unitId;
            resultValues.add(nvsui);
        }

        ElemResultSetExt elem = new ElemResultSetExt(ODSHelper.asODSLongLong(aid),
                                                     resultValues.toArray(new NameValueSeqUnitId[0]));
        ResultSetExt res = new ResultSetExt();
        res.firstElems = new ElemResultSetExt[] { elem };
        return new ResultSetExt[] { res };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getValueMatrixInMode(org.asam.ods.ElemId,
     *      org.asam.ods.ValueMatrixMode)
     */
    public ValueMatrix getValueMatrixInMode(ElemId elem, ValueMatrixMode vmMode) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getValueMatrixInMode' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getValueMatrix(org.asam.ods.ElemId)
     */
    public ValueMatrix getValueMatrix(ElemId elem) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getValueMatrixInMode' not implemented");
    }

    /***********************************************************************************
     * security
     ***********************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setAttributeRights(org.asam.ods.T_LONGLONG, java.lang.String,
     *      org.asam.ods.T_LONGLONG, int, org.asam.ods.RightsSet)
     */
    public void setAttributeRights(T_LONGLONG aid, String attrName, T_LONGLONG usergroupId, int rights, RightsSet set)
            throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setAttributeRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setElementRights(org.asam.ods.T_LONGLONG, org.asam.ods.T_LONGLONG,
     *      int, org.asam.ods.RightsSet)
     */
    public void setElementRights(T_LONGLONG aid, T_LONGLONG usergroupId, int rights, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setElementRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setInstanceRights(org.asam.ods.T_LONGLONG, org.asam.ods.T_LONGLONG[],
     *      org.asam.ods.T_LONGLONG, int, org.asam.ods.RightsSet)
     */
    public void setInstanceRights(T_LONGLONG aid, T_LONGLONG[] instIds, T_LONGLONG usergroupId, int rights,
            RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInstanceRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getAttributeRights(org.asam.ods.T_LONGLONG, java.lang.String)
     */
    public ACL[] getAttributeRights(T_LONGLONG aid, String attrName) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getAttributeRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getElementRights(org.asam.ods.T_LONGLONG)
     */
    public ACL[] getElementRights(T_LONGLONG aid) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getElementRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstanceRights(org.asam.ods.T_LONGLONG, org.asam.ods.T_LONGLONG)
     */
    public ACL[] getInstanceRights(T_LONGLONG aid, T_LONGLONG iid) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInstanceRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setElementInitialRights(org.asam.ods.T_LONGLONG,
     *      org.asam.ods.T_LONGLONG, int, org.asam.ods.T_LONGLONG, org.asam.ods.RightsSet)
     */
    public void setElementInitialRights(T_LONGLONG aid, T_LONGLONG usergroupId, int rights, T_LONGLONG refAid,
            RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setElementInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setInstanceInitialRights(org.asam.ods.T_LONGLONG,
     *      org.asam.ods.T_LONGLONG[], org.asam.ods.T_LONGLONG, int, org.asam.ods.T_LONGLONG, org.asam.ods.RightsSet)
     */
    public void setInstanceInitialRights(T_LONGLONG aid, T_LONGLONG[] instIds, T_LONGLONG usergroupId, int rights,
            T_LONGLONG refAid, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInstanceInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setInitialRightReference(org.asam.ods.T_LONGLONG, java.lang.String,
     *      org.asam.ods.RightsSet)
     */
    public void setInitialRightReference(T_LONGLONG aid, String refName, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInitialRightReference' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInitialRightReference(org.asam.ods.T_LONGLONG)
     */
    public String[] getInitialRightReference(T_LONGLONG aid) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInitialRightReference' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getElementInitialRights(org.asam.ods.T_LONGLONG)
     */
    public InitialRight[] getElementInitialRights(T_LONGLONG aid) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getElementInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstanceInitialRights(org.asam.ods.T_LONGLONG,
     *      org.asam.ods.T_LONGLONG)
     */
    public InitialRight[] getInstanceInitialRights(T_LONGLONG aid, T_LONGLONG iid) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInstanceInitialRights' not implemented");
    }

}
