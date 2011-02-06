package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.ACL;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.ApplElemAccessPOA;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ElemId;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementHelper;
import org.asam.ods.QueryStructure;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.ResultSetExt;
import org.asam.ods.RightsSet;
import org.asam.ods.SelValue;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplElemAccess</code>.
 * 
 * @author Christian Rechner
 */
class ApplElemAccessImpl extends ApplElemAccessPOA {

    private static final Log LOG = LogFactory.getLog(ApplElemAccessImpl.class);

    private final POA poa;
    private final AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param atfxCache The ATFX cache.
     */
    public ApplElemAccessImpl(POA poa, AtfxCache atfxCache) {
        this.poa = poa;
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
        try {
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
                String idAttrName = this.atfxCache.getApplicationAttributeByBaName(aid, "id").getName();
                // add to ae group
                List<AIDNameValueSeqUnitId> list = aeGroupColumns.get(aid);
                if (list == null) {
                    list = new ArrayList<AIDNameValueSeqUnitId>();
                    aeGroupColumns.put(aid, list);
                }
                list.add(column);
                // check for id column
                if (column.attr.aaName.equals(idAttrName)) {
                    idColumns.put(aid, column);
                }
            }

            // create instances per application element
            List<ElemId> elemIdList = new ArrayList<ElemId>();
            for (long aid : aeGroupColumns.keySet()) {
                // iterate over rows
                for (int row = 0; row < numberOfRows; row++) {
                    // fetch or create id
                    long iid = 0;
                    AIDNameValueSeqUnitId idCol = idColumns.get(aid);
                    if (idCol != null) {
                        iid = ODSHelper.asJLong(ODSHelper.tsValueSeq2tsValue(idCol.values, row).u.longlongVal());
                    } else {
                        iid = this.atfxCache.nextIid(aid);
                    }
                    // create instance
                    InstanceElementImpl impl = new InstanceElementImpl(this.poa, this.atfxCache, aid, iid);
                    InstanceElement ie = InstanceElementHelper.narrow(poa.servant_to_reference(impl));
                    this.atfxCache.addInstance(aid, iid, ie);
                    // put values
                    for (AIDNameValueSeqUnitId avsui : aeGroupColumns.get(aid)) {
                        TS_Value value = ODSHelper.tsValueSeq2tsValue(avsui.values, row);
                        this.atfxCache.setInstanceValue(aid, iid, avsui.attr.aaName, value);
                    }
                    // TODO: create relations

                    elemIdList.add(new ElemId(ODSHelper.asODSLongLong(aid), ODSHelper.asODSLongLong(iid)));
                }
            }

            return elemIdList.toArray(new ElemId[0]);
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
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'deleteInstances' not implemented");
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
            LOG.warn("InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
            // throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
            // "InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
        }
        // lookup relation
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, relName);
        if (applRel == null || applRel.getElem2() == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation not found aid="
                    + aid + ",relName=" + relName);
        }
        // return related instance ids
        Set<Long> relInstIidSet = this.atfxCache.getRelatedInstanceIds(aid, iid, applRel);
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
            LOG.warn("InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
            // throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
            // "InstanceElement not found ElemId aid=" + aid + ",iid=" + iid);
        }

        // check relation
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, relName);
        if (applRel == null || applRel.getElem2() == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation not found aid="
                    + aid + ",relName=" + relName);
        }
        long otherAid = ODSHelper.asJLong(applRel.getElem2().getId());

        // alter relations (with check for existing instances)
        for (T_LONGLONG otherIidT : instIds) {
            long otherIid = ODSHelper.asJLong(otherIidT);
            if (type == SetType.INSERT || type == SetType.UPDATE || type == SetType.APPEND) {
                this.atfxCache.createInstanceRelation(aid, iid, applRel, otherIid);
            } else if (type == SetType.REMOVE) {
                this.atfxCache.removeInstanceRelation(otherAid, iid, applRel, otherIid);
            }
        }

        // System.out.println("SET RELATIONS FROM " + this.atfxCache.getApplicationElementNameById(aid) + "[" + aid
        // + "] to " + this.atfxCache.getApplicationElementNameById(otherAid) + "[" + otherAid + "] with relName "
        // + relName + ": " + Arrays.toString(ODSHelper.asJLong(instIds)));

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
        QueryBuilder queryBuilder = new QueryBuilder(this.atfxCache);
        for (AIDNameUnitId anui : aoq.anuSeq) {
            queryBuilder.addAid(ODSHelper.asJLong(anui.attr.aid));
        }
        for (SelValue selValue : aoq.condSeq) {
            queryBuilder.applySelValue(selValue);
        }

        // for (AIDNameUnitId anui : aoq.anuSeq) {
        // String aeName = this.atfxCache.getApplicationElementNameById(ODSHelper.asJLong(anui.attr.aid));
        // System.out.println("anuSeq: " + aeName + " - " + anui.attr.aaName);
        // }
        // for (SelValue selValue : aoq.condSeq) {
        // String aeName = this.atfxCache.getApplicationElementNameById(ODSHelper.asJLong(selValue.attr.attr.aid));
        // System.out.println("selValue: " + aeName + " - " + selValue.attr.attr.aaName + ": " + selValue.oper);
        // }
        // for (SelOperator selOperator : aoq.operSeq) {
        // System.out.println("selOperator: " + selOperator);
        // }
        // for (SelOrder selOrder : aoq.orderBy) {
        // System.out.println("selOrder: " + selOrder);
        // }
        // System.out.println("relName: " + aoq.relName);
        // System.out.println("relInst: "
        // + this.atfxCache.getApplicationElementNameById(ODSHelper.asJLong(aoq.relInst.aid)) + ","
        // + ODSHelper.asJLong(aoq.relInst.iid));
        //
        // System.out.println("-----------------------");

        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInstances' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstancesExt(org.asam.ods.QueryStructureExt, int)
     */
    public ResultSetExt[] getInstancesExt(QueryStructureExt aoq, int how_many) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInstancesExt' not implemented");
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
