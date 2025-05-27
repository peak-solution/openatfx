package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.asam.ods.ACL;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.ApplElemAccessPOA;
import org.asam.ods.ApplicationAttribute;
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
import org.asam.ods.SelOperator;
import org.asam.ods.SelType;
import org.asam.ods.SelValueExt;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;

import com.peaksolution.openatfx.api.AtfxElement;
import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.Instance;
import com.peaksolution.openatfx.api.NameValueUnit;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.QueryConditionHelper;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.api.SingleValue;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplElemAccess</code>.
 * 
 * @author Christian Rechner
 */
class ApplElemAccessImpl extends ApplElemAccessPOA {

    // private static final Log LOG = LogFactory.getLog(ApplElemAccessImpl.class);

    private final POA instancePOA;
    private final OpenAtfxAPIImplementation api;
    private final CorbaAtfxCache corbaCache;

    /**
     * Constructor.
     * 
     * @param instancePOA The instance POA.
     * @param api The OpenAtfxAPI.
     * @param atfxCache The ATFX cache.
     */
    public ApplElemAccessImpl(POA instancePOA, OpenAtfxAPIImplementation api, CorbaAtfxCache atfxCache) {
        this.instancePOA = instancePOA;
        this.api = api;
        this.corbaCache = atfxCache;
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
        Map<Long, AIDNameValueSeqUnitId> idColumns = new HashMap<>();
        Map<Long, List<AIDNameValueSeqUnitId>> aeGroupColumns = new HashMap<>();
        for (AIDNameValueSeqUnitId column : val) {
            long aid = ODSHelper.asJLong(column.attr.aid);
            Element element = api.getElementById(aid);
            if (element == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Element with aid=" + aid + " not found to insert instances for");
            }
            
            Attribute attr = element.getAttributeByName(column.attr.aaName);
            // add to ae group
            List<AIDNameValueSeqUnitId> list = aeGroupColumns.get(aid);
            if (list == null) {
                list = new ArrayList<>();
                aeGroupColumns.put(aid, list);
            }
            list.add(column);
            // check for id column
            if (attr != null && OpenAtfxConstants.BA_ID.equalsIgnoreCase(attr.getBaseName())) {
                idColumns.put(aid, column);
            }
        }

        // create instances per application element
        List<ElemId> elemIdList = new ArrayList<>(numberOfRows);
        try {
            for (Entry<Long, List<AIDNameValueSeqUnitId>> elementEntry : aeGroupColumns.entrySet()) { // iterate over application elements
                long aid = elementEntry.getKey();
                Element element = api.getElementById(aid);
                // find 'id' column
                AIDNameValueSeqUnitId idCol = idColumns.get(aid);
    
                // trick for 'AoLocalColumn': sort the attribute 'sequence_representation' BEFORE the attribute 'values'.
                // This is needed to write the values correctly
                Collection<Long> lcAids = this.corbaCache.getAidsByBaseType("aolocalcolumn");
                if (lcAids != null && lcAids.contains(aid)) {
                    Collections.sort(aeGroupColumns.get(aid), (AIDNameValueSeqUnitId o1, AIDNameValueSeqUnitId o2) -> {
                        Attribute attr = element.getAttributeByName(o2.attr.aaName);
                        if (attr == null) {
                            return -1;
                        }
                        boolean isSeqRepVal = false;
                        if (attr.getBaseName() != null) {
                            isSeqRepVal = attr.getBaseName().equalsIgnoreCase("sequence_representation");
                        }
                        return isSeqRepVal ? 1 : 0;
                    });
                }
    
                // iterate over rows (instances)
                for (int row = 0; row < numberOfRows; row++) {
                    Collection<NameValueUnit> values = new ArrayList<>();
                    
                    // fetch or create id
                    long iid = 0;
                    if (idCol != null) {
                        if (idCol.values.u.discriminator().equals(DataType.DT_LONG)) {
                            iid = ODSHelper.tsValueSeq2tsValue(idCol.values, row).u.longVal();
                        } else {
                            iid = ODSHelper.asJLong(ODSHelper.tsValueSeq2tsValue(idCol.values, row).u.longlongVal());
                        }
                    }
                    
                    if (iid > 0) {
                        Attribute idAttr = element.getAttributeByBaseName("id");
                        values.add(new NameValueUnit(idAttr.getName(), com.peaksolution.openatfx.api.DataType.DT_LONGLONG, iid));
                    }
    
                    // collect instance values or relations
                    Map<String, Collection<Long>> relatedIidsByRelName = new HashMap<>();
                    Collection<NameValueUnit> instanceAttributes = new ArrayList<>();
                    for (AIDNameValueSeqUnitId anvsui : aeGroupColumns.get(aid)) {
                        if (anvsui.values.flag[row] != (short) 15) {
                            continue;
                        }
                        
                        long unitId = 0;
                        String unitName = "";
                        T_LONGLONG unitIdLong = anvsui.unitId;
                        if (unitIdLong != null) {
                            unitId = ODSHelper.asJLong(unitIdLong);
                            if (unitId > 0) {
                                unitName = api.getUnitName(unitId);
                            }
                        }
                        
                        Attribute currentAttr = element.getAttributeByName(anvsui.attr.aaName);
                        TS_Value value = ODSHelper.tsValueSeq2tsValue(anvsui.values, row);
                        // attribute is an application attribute
                        if (currentAttr != null) {
                            if (currentAttr.getUnitId() != unitId) {
                                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                                      "updateInstances() called for " + currentAttr
                                                              + " with a unit id " + unitId + "'(" + unitName
                                                              + "), which is different from the attribute's unit id "
                                                              + currentAttr.getUnitId()
                                                              + ", but unit conversion is not supported!");
                            }
                            
                            values.add(new NameValueUnit(currentAttr.getName(), ODSHelper.mapUnion(value), unitName));
                        }
                        // attribute is an application relation or an instance attribute
                        else {
                            Relation rel = element.getRelationByName(anvsui.attr.aaName);
                            if (rel != null) {
                                List<Long> otherIids = new ArrayList<>();
                                if (value.u.discriminator().equals(DataType.DT_LONGLONG)) {
                                    otherIids.add(ODSHelper.asJLong(value.u.longlongVal()));
                                } else if (value.u.discriminator().equals(DataType.DS_LONGLONG)) {
                                    for (T_LONGLONG otherIid : value.u.longlongSeq()) {
                                        otherIids.add(ODSHelper.asJLong(otherIid));
                                    }
                                }
                                relatedIidsByRelName.put(rel.getRelationName(), otherIids);
                            }
                            // not defined in application model, assume instance attribute
                            else {
                                instanceAttributes.add(new NameValueUnit(anvsui.attr.aaName, ODSHelper.mapUnion(value),
                                                                         unitName));
                            }
                        }
                    }
                    
                    Instance newInstance = api.createInstance(aid, values);
                    iid = newInstance.getIid();
                    for (Entry<String, Collection<Long>> entry : relatedIidsByRelName.entrySet()) {
                        api.setRelatedInstances(aid, iid, entry.getKey(), entry.getValue(), SetType.INSERT);
                    }
                    for (NameValueUnit instAttr : instanceAttributes) {
                        newInstance.setInstanceValue(instAttr);
                    }
    
                    elemIdList.add(new ElemId(ODSHelper.asODSLongLong(aid), ODSHelper.asODSLongLong(iid)));
                }
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }

        return elemIdList.toArray(new ElemId[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#updateInstances(org.asam.ods.AIDNameValueSeqUnitId[])
     */
    public void updateInstances(AIDNameValueSeqUnitId[] val) throws AoException {
        // check for empty data
        if (val == null || val.length < 1) {
            return;
        }
        int numberOfRows = val[0].values.flag.length;

        // group by application element id and check if id attr is given
        Map<Long, AIDNameValueSeqUnitId> idColumns = new HashMap<>();
        Map<Long, List<AIDNameValueSeqUnitId>> aeGroupColumns = new HashMap<>();
        for (AIDNameValueSeqUnitId column : val) {
            long aid = ODSHelper.asJLong(column.attr.aid);
            Element element = api.getElementById(aid);
            if (element == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Element with aid=" + aid + " not found to insert instances for");
            }
            
            Attribute attr = element.getAttributeByName(column.attr.aaName);
            // add to ae group
            List<AIDNameValueSeqUnitId> list = aeGroupColumns.get(aid);
            if (list == null) {
                list = new ArrayList<>();
                aeGroupColumns.put(aid, list);
            }
            list.add(column);
            // check for id column
            if (attr != null && OpenAtfxConstants.BA_ID.equalsIgnoreCase(attr.getBaseName())) {
                idColumns.put(aid, column);
            }
        }
        
        try {
            for (Entry<Long, List<AIDNameValueSeqUnitId>> elementEntry : aeGroupColumns.entrySet()) { // iterate over application elements
                long aid = elementEntry.getKey();
                Element element = api.getElementById(aid);
                
                // find 'id' column
                AIDNameValueSeqUnitId idCol = idColumns.get(aid);
    
                // iterate over rows (instances)
                List<AIDNameValueSeqUnitId> attributes = aeGroupColumns.get(aid);
                for (int row = 0; row < numberOfRows; row++) {
                    // fetch id
                    long iid = 0;
                    if (idCol != null) {
                        iid = ODSHelper.asJLong(ODSHelper.tsValueSeq2tsValue(idCol.values, row).u.longlongVal());
                    }
                    
                    Instance instance = api.getInstanceById(aid, iid);
                    
                    // write instance values or relation
                    for (AIDNameValueSeqUnitId anvsui : attributes) {
                        if (idCol.equals(anvsui)) {
                            continue;
                        }
                        boolean setInvalid = false;
                        if (anvsui.values.flag[row] == 0) {
                            setInvalid = true;
                        } else if (anvsui.values.flag[row] != (short) 15) {
                            continue;
                        }
                        
                        T_LONGLONG unitIdLong = anvsui.unitId;
                        long unitId = 0;
                        String unitName = "";
                        if (unitIdLong != null) {
                            unitId = ODSHelper.asJLong(unitIdLong);
                            unitName = api.getUnitName(unitId);
                        }
                        
                        Attribute currentAttr = element.getAttributeByName(anvsui.attr.aaName);
                        TS_Value value;
                        if (setInvalid) {
                            value = ODSHelper.createEmptyTS_Value(anvsui.values.u.discriminator());
                        } else {
                            value = ODSHelper.tsValueSeq2tsValue(anvsui.values, row);
                        }
                        // attribute is an application attribute
                        if (currentAttr != null) {
                            if (currentAttr.getUnitId() != unitId) {
                                String attrUnitName = api.getUnitName(currentAttr.getUnitId());
                                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                                      "updateInstances() called for " + currentAttr
                                                              + " with a unit '" + unitName
                                                              + "', different from the attribute's unit '"
                                                              + attrUnitName
                                                              + "', but unit conversion is not supported!");
                            }
                            
                            instance.setAttributeValue(new NameValueUnit(currentAttr.getName(), ODSHelper.mapUnion(value), unitName));
                        }
                        // attribute is an application relation or an instance attribute
                        else {
                            Relation rel = element.getRelationByName(anvsui.attr.aaName);
                            if (rel != null) {
                                List<Long> otherIids = new ArrayList<>();
                                if (value.u.discriminator().equals(DataType.DT_LONGLONG)) {
                                    otherIids.add(ODSHelper.asJLong(value.u.longlongVal()));
                                } else if (value.u.discriminator().equals(DataType.DS_LONGLONG)) {
                                    for (T_LONGLONG otherIid : value.u.longlongSeq()) {
                                        otherIids.add(ODSHelper.asJLong(otherIid));
                                    }
                                }
                                instance.addRelationValue(rel, otherIids);
                            }
                            // not defined in application model, assume instance attribute
                            else {
                                instance.setInstanceValue(new NameValueUnit(anvsui.attr.aaName, ODSHelper.mapUnion(value), unitName));
                            }
                        }
                    }
                }
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#deleteInstances(org.asam.ods.T_LONGLONG, org.asam.ods.T_LONGLONG[])
     */
    public void deleteInstances(T_LONGLONG aid, T_LONGLONG[] instIds) throws AoException {
        long jAid = ODSHelper.asJLong(aid);
        for (T_LONGLONG instId : instIds) {
            this.corbaCache.removeInstanceElement(jAid, ODSHelper.asJLong(instId));
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
        try {
            Collection<Long> relatedIids = api.getRelatedInstanceIds(ODSHelper.asJLong(elem.aid), ODSHelper.asJLong(elem.iid), relName);
            Collection<T_LONGLONG> tlongIids = new ArrayList<>();
            for (Long relatedIid : relatedIids) {
                tlongIids.add(ODSHelper.asODSLongLong(relatedIid));
            }
            return tlongIids.toArray(new T_LONGLONG[0]);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#setRelInst(org.asam.ods.ElemId, java.lang.String,
     *      org.asam.ods.T_LONGLONG[], org.asam.ods.SetType)
     */
    public void setRelInst(ElemId elem, String relName, T_LONGLONG[] instIds, SetType type) throws AoException {
        List<Long> iids = Arrays.stream(instIds).map(ODSHelper::asJLong).toList();
        try {
            api.setRelatedInstances(ODSHelper.asJLong(elem.aid), ODSHelper.asJLong(elem.iid), relName, iids, type);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
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
    public ElemResultSet[] getInstances(QueryStructure aoq, int howMany) throws AoException {
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
        Set<Long> set = new HashSet<>();
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
        long aidOfFirstColumn = ODSHelper.asJLong(ers.aid);
        WildcardHandlingHelper wildcardHelper = null;
        if ("*".equals(aoq.anuSeq[0].attr.aaName)) {
            wildcardHelper = new WildcardHandlingHelper(api, aidOfFirstColumn);
        } else {
            ers.attrValues = new AttrResultSet[aoq.anuSeq.length];
        }
        
        try {
            // get instance ids
            Collection<Long> iids = null;
            long aid = ODSHelper.asJLong(aoq.anuSeq[0].attr.aid);
            if (aoq.relName == null || aoq.relName.length() < 1) { // all of application element
                Collection<Instance> instances = api.getInstances(aid);
                iids = instances.stream().map(Instance::getIid).toList();
            } else { // filtered
                long relAid = ODSHelper.asJLong(aoq.relInst.aid);
                long iid = ODSHelper.asJLong(aoq.relInst.iid);
                Relation rel = api.getRelationByName(relAid, aoq.relName);
                // dirty hack for dirty clients: lookup inverse relation
                if (rel == null) {
                    rel = api.getRelationByName(aid, aoq.relName).getInverseRelation();
                }
                if (rel == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "Application relation [aid=" + relAid + ",aeName="
                                                  + api.getElementById(relAid).getName() + ",relName=" + aoq.relName
                                                  + "] not found!");
                }
                iids = api.getRelatedInstanceIds(relAid, iid, rel);
            }
        
            if (wildcardHelper != null) {
                wildcardHelper.fillElemResultSet(ers, iids);
            } else {
                for (int i = 0; i < aoq.anuSeq.length; i++) {
                    aid = ODSHelper.asJLong(aoq.anuSeq[i].attr.aid);
                    Element currentElement = api.getElementById(aid);
                    String attrName = aoq.anuSeq[i].attr.aaName;
                    Integer attrNo = currentElement.getAttrNoByName(attrName);
                    if (attrNo == null) {
                        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                              "Attribute '" + attrName + "' not found!");
                    }
                    ers.attrValues[i] = new AttrResultSet();
                    ers.attrValues[i].attrValues = new NameValueSeqUnitId();
                    ers.attrValues[i].attrValues.unitId = aoq.anuSeq[i].unitId;
                    ers.attrValues[i].attrValues.valName = attrName;
                    ers.attrValues[i].attrValues.value = ODSHelper.getInstanceValues(api, aid, attrName, iids);
                }
            }
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
        
        return new ElemResultSet[] { ers };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getInstancesExt(org.asam.ods.QueryStructureExt, int)
     */
    public ResultSetExt[] getInstancesExt(QueryStructureExt aoq, int how_many) throws AoException {
        if (aoq == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt must not be null");
        }
        // LOG.debug("getInstancesExt: anuSeq" + ODSHelper.anuSeq2string(aoq.anuSeq) + ",condSeq=" + aoq.condSeq);
        List<SelValueExt> conditions = new ArrayList<>();
        // this method does only process a certain kind of query:
        // - only a single join is supported
        // - no group by's
        // - no aggregate functions (except MAX)
        // - order by is ignored
        // - only one or no DT_STRING or DS_LONGLONG condition
        // - only specific condition selOpCodes supported
        // - conditions only with AND operators

        // query must contain at least one select statement. otherwise just return an empty result set
        if (aoq.anuSeq == null || aoq.anuSeq.length < 1) {
            return new ResultSetExt[] { new ResultSetExt(new ElemResultSetExt[0], null) };
        }
        // do not allow more than one join
        if (aoq.joinSeq != null && aoq.joinSeq.length > 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains join(s) with more than one relation");
        }
        // do not allow 'group by'
        if (aoq.groupBy != null && aoq.groupBy.length > 0) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Contains 'group by' statements");
        }
        
        // only allow the AND operator between conditions
        if (aoq.condSeq != null && aoq.condSeq.length > 1) {
            for (SelItem condition : aoq.condSeq) {
                if (SelType.SEL_OPERATOR_TYPE.equals(condition.discriminator()) && !SelOperator.AND.equals(condition.operator())) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                            "QueryStructureExt not supported: Contains a condition operator other than AND");
                }
            }
        }

        // get the conditions if there are any, ignore operators
        if (aoq.condSeq != null && aoq.condSeq.length > 0) {
            for (SelItem condition : aoq.condSeq) {
                if (condition.discriminator() == SelType.SEL_VALUE_TYPE) {
                    conditions.add(condition.value());
                }
            }
        }
        
        if (conditions != null && !conditions.isEmpty()) {
            for (SelValueExt condition : conditions) {
                if (condition == null) {
                    continue;
                }
                // only support conditions of type D?_STRING, D?_LONGLONG or D?_ENUM
                if (condition.value.u.discriminator() != DataType.DT_STRING
                        && condition.value.u.discriminator() != DataType.DS_STRING
                        && condition.value.u.discriminator() != DataType.DT_LONGLONG
                        && condition.value.u.discriminator() != DataType.DS_LONGLONG
                        && condition.value.u.discriminator() != DataType.DT_ENUM
                        && condition.value.u.discriminator() != DataType.DS_ENUM) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Condition DataType '"
                            + ODSHelper.dataType2String(condition.value.u.discriminator()) + "' not supported.");
                 // only allow (CI_)(N)EQ, (CI_)(NOT)LIKE, (NOT)INSET, IS_NULL and IS_NOT_NULL
                } else if (condition.oper != SelOpcode.EQ && condition.oper != SelOpcode.CI_EQ
                        && condition.oper != SelOpcode.NEQ && condition.oper != SelOpcode.CI_NEQ
                        && condition.oper != SelOpcode.LIKE && condition.oper != SelOpcode.CI_LIKE
                        && condition.oper != SelOpcode.NOTLIKE && condition.oper != SelOpcode.CI_NOTLIKE
                        && condition.oper != SelOpcode.INSET && condition.oper != SelOpcode.NOTINSET
                        && condition.oper != SelOpcode.IS_NULL && condition.oper != SelOpcode.IS_NOT_NULL) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "QueryStructureExt not supported: Condition operator '" + condition.oper.value()
                                                  + "' not yet supported.");
                // make sure the condition attribute or relation exists
                } else {
                    String conditionAttributeName = condition.attr.attr.aaName;
                    long conditionAid = ODSHelper.asJLong(condition.attr.attr.aid);
                    if (corbaCache.getApplicationAttribute(conditionAid, conditionAttributeName) == null
                            && corbaCache.getApplicationRelationByName(conditionAid, conditionAttributeName) == null) {
                        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                              "QueryStructureExt invalid: Condition attribute '" + conditionAttributeName
                                                      + "' does not exist in the condition's application element");
                    }
                }
            }
        }
        
        // do not allow null AIDNames, aggregate functions or null attribute names in any of the selects
        Map<Long, String> querySelectAidsToElementName = new HashMap<>();
        List<SelAIDNameUnitId> aggregateSelects = new ArrayList<>();
        Map<Long, Collection<SelAIDNameUnitId>> selectsPerAid = new HashMap<>();
        for (SelAIDNameUnitId anu : aoq.anuSeq) {
            if (anu.attr == null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Invalid SelAIDNameUnitId found: AIDName was null");
            }
            if (anu.aggregate != null && anu.aggregate != AggrFunc.NONE) {
                if (anu.aggregate == AggrFunc.MAX) {
                    aggregateSelects.add(anu);
                } else {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                            "Invalid SelAIDNameUnitId found: Only MAX Aggregate function is supported");
                }
            }
            if (anu.attr.aaName == null || anu.attr.aaName.trim().length() < 1) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Invalid SelAIDNameUnitId found: Application attribute name was null or empty");
            }
            
            long aid = ODSHelper.asJLong(anu.attr.aid);
            if (!querySelectAidsToElementName.containsKey(aid)) {
                String queriedAeName = corbaCache.getApplicationElementNameById(aid);
                if (queriedAeName == null) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "QueryStructureExt invalid: Given AID '" + aid
                                                  + "' does not reference a existing application element");
                }
                querySelectAidsToElementName.put(aid, queriedAeName);
            }
            
            selectsPerAid.computeIfAbsent(ODSHelper.asJLong(anu.attr.aid), v -> new ArrayList<>()).add(anu);
        }
        
        ElemResultSetExt[] erses = new ElemResultSetExt[querySelectAidsToElementName.size()];
        int elementCounter = 0;
        try {
            for (Entry<Long, String> selectAidEntry : querySelectAidsToElementName.entrySet()) {
                long queryAid = selectAidEntry.getKey();
                AtfxElement queryElement = api.getAtfxElement(queryAid);
                
                // prepare aggregate helper
                QueryAggregationHelper aggregateHelper = null;
                if (!aggregateSelects.isEmpty()) {
                    aggregateHelper = new QueryAggregationHelper(api, aoq, queryAid, aggregateSelects);
                }
                
             	// make sure the selected attributes or relation exists
                WildcardHandlingHelper wildcardHelper = null;
                for (SelAIDNameUnitId selectedAttribute : selectsPerAid.get(queryAid)) {
                    if ("*".equals(selectedAttribute.attr.aaName)) {
                        wildcardHelper = new WildcardHandlingHelper(api, queryAid);
                        break;
                    }
                    if (corbaCache.getApplicationAttribute(queryAid, selectedAttribute.attr.aaName) == null
                            && corbaCache.getApplicationRelationByName(queryAid, selectedAttribute.attr.aaName) == null) {
                        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                              "QueryStructureExt invalid: Selected attribute '" + selectedAttribute.attr.aaName
                                                      + "' (aid=" + selectedAttribute.attr.aid.low
                                                      + ") does not exist in selected application element '"
                                                      + selectAidEntry.getValue() + "' (aid=" + queryAid + ")");
                    }
                }
    
                // get all queried instances -> get all instances, then filter manually, because the 'getInstances()' method
                // uses case sensitivity
                Collection<Instance> instances = api.getInstances(queryAid);
                Collection<Long> instanceIds = instances.stream().map(Instance::getIid).toList();
                QueryConditionHelper conditionHelper = new QueryConditionHelper(queryAid, instanceIds, aoq.joinSeq, api);
                for (SelValueExt condition : conditions) {
                    conditionHelper.applyCondition(condition);
                }
                List<Long> filteredIids = new ArrayList<>(conditionHelper.getFilteredIIDs());
                
                // build the result set
                ElemResultSetExt erse = new ElemResultSetExt();
                erse.aid = ODSHelper.asODSLongLong(queryAid);
                if (aggregateHelper != null) {
                    aggregateHelper.fillElemResultSetExt(erse, filteredIids);
                } else if (wildcardHelper != null) {
                    wildcardHelper.fillElemResultsSetExt(erse, filteredIids);
                } else {
                    Collection<SelAIDNameUnitId> selects = selectsPerAid.get(queryAid);
                	erse.values = new NameValueSeqUnitId[selects.size()];
                	int col = 0;
                	for (SelAIDNameUnitId sel : selects) {
                	    ApplicationAttribute attribute = this.corbaCache.getApplicationAttribute(queryAid, sel.attr.aaName);
                	    AtfxRelation relation = queryElement.getRelationByName(sel.attr.aaName);
                	    erse.values[col] = new NameValueSeqUnitId();
                	    erse.values[col].valName = sel.attr.aaName;
                	    erse.values[col].value = new TS_ValueSeq();
                	    erse.values[col].unitId = new T_LONGLONG(0, 0);
                	    
                	    if (attribute == null) {
                	        boolean isSequenceValue = relation.getRelationRangeMax() == -1;
                	        com.peaksolution.openatfx.api.DataType dt = com.peaksolution.openatfx.api.DataType.DT_LONGLONG;
                	        if (isSequenceValue) {
                	            dt = com.peaksolution.openatfx.api.DataType.DS_LONGLONG;
                	        }
                	        List<TS_Value> values = new ArrayList<>();
                	        for (Long iid : filteredIids) {
                	            List<Long> relIids = api.getRelatedInstanceIds(queryAid, iid, relation);
                	            if (relIids.isEmpty()) {
                	                values.add(ODSHelper.createEmptyTS_Value(ODSHelper.mapDataType(dt)));
                	            } else if (isSequenceValue) {
                	                long[] array = relIids.stream().mapToLong(l -> l).toArray();
                	                SingleValue value = new SingleValue(dt, array);
                	                values.add(ODSHelper.mapTSValue(value));
                	            } else {
                	                SingleValue value = new SingleValue(dt, relIids.get(0));
                	                values.add(ODSHelper.mapTSValue(value));
                	            }
                	        }
                	        TS_ValueSeq valSeq = ODSHelper.tsValue2tsValueSeq(values.toArray(new TS_Value[0]), ODSHelper.mapDataType(dt));
               		        erse.values[col].value = valSeq;
               		        
                	    } else {
                	        erse.values[col].value = corbaCache.getInstanceValues(queryAid, sel.attr.aaName, filteredIids);
                	    }
                	    col++;
                	}
                }
                erses[elementCounter++] = erse;
            }
            
            if (aoq.joinSeq != null && aoq.joinSeq.length > 0 && erses.length > 1) {
                // Join helper will only be used for queries with at least one join and selections for two elements, for
                // queries with only one element select, the join was already used in condition helper to identify the
                // correct relation.
                JoinHelper joinHelper = new JoinHelper(api, aoq.joinSeq[0]);
                erses = joinHelper.join(erses);
            }
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
        
        return new ResultSetExt[] { new ResultSetExt(erses, null) };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getValueMatrix(org.asam.ods.ElemId)
     */
    public ValueMatrix getValueMatrix(ElemId elem) throws AoException {
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        InstanceElement ie = this.corbaCache.getInstanceById(this.instancePOA, aid, iid);
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
        InstanceElement ie = this.corbaCache.getInstanceById(this.instancePOA, aid, iid);
        String beName = ie.getApplicationElement().getBaseElement().getType();
        if (beName.equalsIgnoreCase("AoMeasurement")) {
            return ie.upcastMeasurement().getValueMatrixInMode(vmMode);
        } else if (beName.equalsIgnoreCase("AoSubMatrix")) {
            return ie.upcastSubMatrix().getValueMatrixInMode(vmMode);
        }
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Unable to build ValueMatrix on object: " + ie.getAsamPath());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplElemAccessOperations#getODSFile(org.asam.ods.ElemId)
     */
    public ODSFile getODSFile(ElemId elem) throws AoException {
        long aid = ODSHelper.asJLong(elem.aid);
        long iid = ODSHelper.asJLong(elem.iid);
        InstanceElement ie = this.corbaCache.getInstanceById(this.instancePOA, aid, iid);
        String beName = ie.getApplicationElement().getBaseElement().getType();
        if (beName.equalsIgnoreCase("AoFile")) {
            return ie.upcastODSFile();
        }
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Unable to get ODSFile from object: " + ie.getAsamPath());
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
