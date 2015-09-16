package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.asam.ods.ACL;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.ApplElemAccessPOA;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.AttrResultSet;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.ODSFile;
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
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
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

    private final POA instancePOA;
    private final AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     */
    public ApplElemAccessImpl(POA instancePOA, AtfxCache atfxCache) {
        this.instancePOA = instancePOA;
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
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "Not application attribute found with name '" + column.attr.aaName
                                              + "' found for aid=" + aid);
            }
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
        List<ElemId> elemIdList = new ArrayList<ElemId>(numberOfRows);
        for (final long aid : aeGroupColumns.keySet()) { // iterate over application elements

            // find 'id' column
            AIDNameValueSeqUnitId idCol = idColumns.get(aid);

            // trick for 'AoLocalColumn': sort the attribute 'sequence_representation' BEFORE the attribute 'values'.
            // This is needed to write the values correctly
            Set<Long> lcAids = this.atfxCache.getAidsByBaseType("aolocalcolumn");
            if (lcAids != null && lcAids.contains(aid)) {
                Collections.sort(aeGroupColumns.get(aid), new Comparator<AIDNameValueSeqUnitId>() {

                    public int compare(AIDNameValueSeqUnitId o1, AIDNameValueSeqUnitId o2) {
                        Integer i1 = atfxCache.getAttrNoByName(aid, o2.attr.aaName);
                        Integer i2 = atfxCache.getAttrNoByBaName(aid, "sequence_representation");
                        boolean isSeqRepVal = i1.equals(i2);
                        return isSeqRepVal ? 1 : 0;
                    }
                });
            }

            // iterate over rows (instances)
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
                // write id column to cache (if autogenerated)
                if (idCol == null) {
                    Integer idAttrNo = this.atfxCache.getAttrNoByBaName(aid, "id");
                    TS_Value value = new TS_Value();
                    value.flag = (short) 15;
                    value.u = new TS_Union();
                    value.u.longlongVal(ODSHelper.asODSLongLong(iid));
                    this.atfxCache.setInstanceValue(aid, iid, idAttrNo, value);
                }
                // write instance values
                for (AIDNameValueSeqUnitId anvsui : aeGroupColumns.get(aid)) {
                    int attrNo = this.atfxCache.getAttrNoByName(aid, anvsui.attr.aaName);
                    TS_Value value = ODSHelper.tsValueSeq2tsValue(anvsui.values, row);
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
        // check 'ElemId'
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        if (!this.atfxCache.instanceExists(aid, iid)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
        }

        // check 'relName'
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, relName);
        if (applRel == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation not found aid="
                    + aid + ",relName=" + relName);
        }
        long otherAid = ODSHelper.asJLong(applRel.getElem2().getId());

        // check 'instIds' and create relation
        if (type == SetType.INSERT || type == SetType.UPDATE || type == SetType.APPEND) {
            Collection<Long> otherIidsToSet = new ArrayList<Long>(instIds.length);
            for (T_LONGLONG otherIidT : instIds) {
                long otherIid = ODSHelper.asJLong(otherIidT);
                if (!this.atfxCache.instanceExists(otherAid, otherIid)) { // throw not found error
                    String sourceAeName = atfxCache.getApplicationElementNameById(ODSHelper.asJLong(elem.aid));
                    String targetAeName = atfxCache.getApplicationElementNameById(otherAid);
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "Target InstanceElement not found: Source[aid=" + ODSHelper.asJLong(elem.aid)
                                                  + ",aeName=" + sourceAeName + ",iid=" + ODSHelper.asJLong(elem.iid)
                                                  + "] -> Target[aid=" + otherAid + ",aeName=" + targetAeName + ",iid="
                                                  + otherIid + "]");
                }
                otherIidsToSet.add(otherIid);
            }
            this.atfxCache.createInstanceRelations(aid, iid, applRel, otherIidsToSet);
        }
        // remove relations
        else if (type == SetType.REMOVE) {
            Collection<Long> otherIidsToRemove = new ArrayList<Long>(instIds.length);
            for (T_LONGLONG otherIidT : instIds) {
                otherIidsToRemove.add(ODSHelper.asJLong(otherIidT));
            }
            this.atfxCache.createInstanceRelations(aid, iid, applRel, otherIidsToRemove);
        }
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
        // check for non supported features
        if (aoq == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "'aoq' must not be null");
        }
        if (aoq.operSeq == null || aoq.operSeq.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "handling of 'operSeq' is not yet implemented in method 'getInstances()'");
        }
        if (aoq.orderBy == null || aoq.orderBy.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "handling of 'orderBy' is not yet implemented in method 'getInstances()'");
        }
        Set<Long> set = new HashSet<Long>();
        for (AIDNameUnitId anui : aoq.anuSeq) {
            set.add(ODSHelper.asJLong(anui.attr.aid));
        }
        if (set.size() > 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Only attributes of exactly one application element may be queried (other not yet implemented)!");
        }
        // no data queried
        if (aoq.anuSeq == null || aoq.anuSeq.length < 1) {
            return new ElemResultSet[0];
        }

        // prepare result
        ElemResultSet ers = new ElemResultSet();
        ers.aid = aoq.anuSeq[0].attr.aid; // fetch aid from first column
        ers.attrValues = new AttrResultSet[aoq.anuSeq.length];

        // get instance ids
        Collection<Long> iids = new LinkedHashSet<Long>(0);
        if (aoq.relName == null || aoq.relName.length() < 1) { // all
            iids = this.atfxCache.getInstanceIds(ODSHelper.asJLong(aoq.anuSeq[0].attr.aid));
        } else { // filtered
            long aid = ODSHelper.asJLong(aoq.relInst.aid);
            long iid = ODSHelper.asJLong(aoq.relInst.iid);
            ApplicationRelation rel = atfxCache.getApplicationRelationByName(aid, aoq.relName);
            if (rel == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Application relation '"
                        + aoq.relName + "' not found!");
            }
            iids = this.atfxCache.getRelatedInstanceIds(aid, iid, rel);
        }

        for (int i = 0; i < aoq.anuSeq.length; i++) {
            long aid = ODSHelper.asJLong(aoq.anuSeq[i].attr.aid);
            String attrName = aoq.anuSeq[i].attr.aaName;
            Integer attrNo = atfxCache.getAttrNoByName(aid, attrName);
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Attribute '" + attrName
                        + "' not found!");
            }
            ers.attrValues[i] = new AttrResultSet();
            ers.attrValues[i].attrValues = new NameValueSeqUnitId();
            ers.attrValues[i].attrValues.unitId = aoq.anuSeq[i].unitId;
            ers.attrValues[i].attrValues.valName = aoq.anuSeq[i].attr.aaName;
            ers.attrValues[i].attrValues.value = atfxCache.getInstanceValues(aid, attrNo, iids);
        }

        return new ElemResultSet[] { ers };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstancesExt(org.asam.ods.QueryStructureExt, int)
     */
    public ResultSetExt[] getInstancesExt(QueryStructureExt aoq, int how_many) throws AoException {
        SelValueExt condition = null;
        // this method does only process a certain kind of query:
        // - selects from only one application element
        // - no joins
        // - no group by's;
        // - no aggregate functions
        // - no order by
        // - only one or no DT_STRING or DS_LONGLONG condition
        // - allowed condition selOpCodes: SelOpCode.EQ, SelOpcode.CI_EQ, SelOpCode.LIKE, SelOpcode.CI_LIKE

        if (aoq == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt must not be null");
        }
        // query must contain at least one select statement. otherwise just return an empty result set
        if (aoq.anuSeq == null || aoq.anuSeq.length < 1) {
            return new ResultSetExt[] { new ResultSetExt(new ElemResultSetExt[0], null) };
        }
        // do not allow joins
        if (aoq.joinSeq != null && aoq.joinSeq.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains joins");
        }
        // do not allow 'group by'
        if (aoq.groupBy != null && aoq.groupBy.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains 'group by' statements");
        }
        // ignore 'order_by'
        // if (aoq.orderBy != null && aoq.orderBy.length > 0) {
        // throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
        // "QueryStructureExt not supported: Contains 'order by' statements");
        // }

        // do not allow null AIDNames, aggregate functions or null attribute names in any of the selects. Also do not
        // allow more than one application element.
        Long aid = null;
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
        if (atfxCache.getApplicationElementNameById(aid) == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt invalid: Given AID '" + aid
                                          + "' does not reference a existing application element");
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

        // only allow conditions of type DT_STRING or DS_LONGLONG
        if ((condition != null) && (condition.value.u.discriminator() != DataType.DT_STRING)
                && (condition.value.u.discriminator() != DataType.DS_LONGLONG)) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Condition DataType '"
                    + ODSHelper.dataType2String(condition.value.u.discriminator()) + "' not supported.");
        }

        // only allow CI_EQ, CI_LIKE or INSET
        if (condition != null && (condition.oper != SelOpcode.CI_EQ) && (condition.oper != SelOpcode.CI_LIKE)
                && (condition.oper != SelOpcode.INSET)) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Condition '" + condition.oper
                                          + "' not yet supported.");
        }

        // make sure the selected attributes exists
        for (SelAIDNameUnitId selectedAttribute : aoq.anuSeq) {
            if (atfxCache.getAttrNoByName(aid, selectedAttribute.attr.aaName) == null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "QueryStructureExt invalid: Selected attribute '" + selectedAttribute
                                              + "' does not exist in selected application element");
            }
        }
        // make sure the condition attributes exist
        if (condition != null) {
            String conditionAttributeName = condition.attr.attr.aaName;
            if (atfxCache.getAttrNoByName(aid, conditionAttributeName) == null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "QueryStructureExt invalid: Condition attribute '" + conditionAttributeName
                                              + "' does not exist in selected application element");
            }
        }

        // get all queries instances -> get all instances, then filter manually, because the 'getInstances()' method
        // uses case sensitivity
        Set<Long> ieIds = atfxCache.getInstanceIds(aid);
        List<Long> filteredIids = new ArrayList<Long>();
        for (Long iid : ieIds) {
            if (condition == null) {
                filteredIids.add(iid);
            } else if (condition.value.u.discriminator() == DataType.DT_STRING) {
                Integer attrNo = this.atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                if ((value != null) && (value.u != null) && (value.u.stringVal() != null)) {
                    if (PatternUtil.nameFilterMatchCI(value.u.stringVal(), condition.value.u.stringVal())) {
                        filteredIids.add(iid);
                    }
                }
            } else if ((condition.value.u.discriminator() == DataType.DS_LONGLONG)
                    && (condition.oper == SelOpcode.INSET)) {
                Integer attrNo = this.atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                long[] cond = ODSHelper.asJLong(condition.value.u.longlongSeq());
                Arrays.sort(cond); // sort so find method works
                if (Arrays.binarySearch(cond, ODSHelper.asJLong(value.u.longlongVal())) > -1) {
                    filteredIids.add(iid);
                }

            }
        }

        // build the result set
        ElemResultSetExt erse = new ElemResultSetExt();
        erse.aid = ODSHelper.asODSLongLong(aid);
        erse.values = new NameValueSeqUnitId[aoq.anuSeq.length];
        for (int col = 0; col < erse.values.length; col++) {
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, aoq.anuSeq[col].attr.aaName);
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = aoq.anuSeq[col].attr.aaName;
            erse.values[col].value = new TS_ValueSeq();
            erse.values[col].value = atfxCache.getInstanceValues(aid, attrNo, filteredIids);
        }

        return new ResultSetExt[] { new ResultSetExt(new ElemResultSetExt[] { erse }, null) };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getValueMatrix(org.asam.ods.ElemId)
     */
    public ValueMatrix getValueMatrix(ElemId elem) throws AoException {
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, aid, iid);
        String beName = ie.getApplicationElement().getBaseElement().getType();
        if (beName.equalsIgnoreCase("AoMeasurement")) {
            return ie.upcastMeasurement().getValueMatrix();
        } else if (beName.equalsIgnoreCase("AoSubMatrix")) {
            return ie.upcastSubMatrix().getValueMatrix();
        }
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Unable to build ValueMatrix on object: " + ie.getAsamPath());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getValueMatrixInMode(org.asam.ods.ElemId,
     *      org.asam.ods.ValueMatrixMode)
     */
    public ValueMatrix getValueMatrixInMode(ElemId elem, ValueMatrixMode vmMode) throws AoException {
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, aid, iid);
        String beName = ie.getApplicationElement().getBaseElement().getType();
        if (beName.equalsIgnoreCase("AoMeasurement")) {
            return ie.upcastMeasurement().getValueMatrixInMode(vmMode);
        } else if (beName.equalsIgnoreCase("AoSubMatrix")) {
            return ie.upcastSubMatrix().getValueMatrixInMode(vmMode);
        }
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Unable to build ValueMatrix on object: " + ie.getAsamPath());
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

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getODSFile(org.asam.ods.ElemId)
     */
    public ODSFile getODSFile(ElemId elem) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getODSFile' not implemented");
    }

}
