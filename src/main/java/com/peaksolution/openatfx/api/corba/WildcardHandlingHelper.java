package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.asam.ods.AttrResultSet;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.api.AtfxAttribute;
import com.peaksolution.openatfx.api.AtfxElement;
import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Helper class to handle the return of attribute and relation values for getInstances/Ext() queries in case a wildcard
 * was given for the attribute selection.
 * 
 * @author Markus Renner
 */
public class WildcardHandlingHelper {

    private final OpenAtfxAPIImplementation api;
    private final long aid;
    private final AtfxElement element;
    private final Collection<AtfxAttribute> attributes;
    private final Collection<AtfxRelation> relations;

    public WildcardHandlingHelper(OpenAtfxAPIImplementation api, long aid) {
        this.api = api;
        this.aid = aid;
        this.element = api.getAtfxElement(aid);
        this.attributes = new ArrayList<>();
        this.relations = new ArrayList<>();
    }

    /**
     * Prepares all attributes and relations to return when a wildcard was given.
     */
    private void init() {
        Collection<AtfxAttribute> allApplicationAttributes = element.getAtfxAttributes();
        Collection<AtfxRelation> allApplicationRelations = element.getAtfxRelations();
        // add all application attributes of the current element
        for (AtfxAttribute currentAttr : allApplicationAttributes) {
            // skip values attribute of AoLocalColumn according to ODS 5.3.1 specification chapter 5.2.3.9
            if (currentAttr.isLocalColumnValuesAttr()) {
                continue;
            }
            attributes.add(currentAttr);
        }
        // add all not "to-n" relations
        for (AtfxRelation currentRel : allApplicationRelations) {
        	if (currentRel.getRelationRangeMax() >= 0) {
        	    relations.add(currentRel);
        	}
        }
    }

    /**
     * Adds all application attribute values of the instances with given ids to the given query result. Also adds
     * values for all not "to n" relations.
     * 
     * @param ers the query result to fill
     * @param iids the instance ids for which to add all attribute values and relevant relation values
     * @throws OpenAtfxException on any errors finding and setting the values to the result
     */
    public void fillElemResultSet(ElemResultSet ers, Collection<Long> iids) throws OpenAtfxException {
        init();
        ers.attrValues = new AttrResultSet[attributes.size() + relations.size()];
        
        // add application attributes to result
        int col = 0;
        for (AtfxAttribute attr : attributes) {
            ers.attrValues[col] = new AttrResultSet();
            ers.attrValues[col].attrValues = new NameValueSeqUnitId();
            ers.attrValues[col].attrValues.valName = attr.getName();
            ers.attrValues[col].attrValues.value = ODSHelper.getInstanceValues(api, aid, attr.getName(), iids);
            ers.attrValues[col].attrValues.unitId = ODSHelper.asODSLongLong(attr.getUnitId());
            col++;
        }
        
        // add relevant relation attributes to result for relations
        for (AtfxRelation rel : relations) {
            ers.attrValues[col] = new AttrResultSet();
            ers.attrValues[col].attrValues = new NameValueSeqUnitId();
            ers.attrValues[col].attrValues.valName = rel.getRelationName();
            ers.attrValues[col].attrValues.value = getRelationValues(iids, rel);
            ers.attrValues[col].attrValues.unitId = new T_LONGLONG(0, 0);
            col++;
        }
    }

    /**
     * Adds all application attribute values of the instances with given ids to the given query result. Also adds
     * values for all not "to n" relations.
     * 
     * @param erse the query result to fill
     * @param iids the instance ids for which to add all attribute values and relevant relation values
     * @throws OpenAtfxException on any errors finding and setting the values to the result
     */
    public void fillElemResultsSetExt(ElemResultSetExt erse, Collection<Long> iids) throws OpenAtfxException {
        init();
        erse.values = new NameValueSeqUnitId[attributes.size() + relations.size()];
        
        // add application attributes to result
        int col = 0;
        for (AtfxAttribute attr : attributes) {
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = attr.getName();
            erse.values[col].value = ODSHelper.getInstanceValues(api, aid, attr.getName(), iids);
            erse.values[col].unitId = ODSHelper.asODSLongLong(attr.getUnitId());
            col++;
        }
        
        // add relevant relation attributes to result for relations
        for (AtfxRelation rel : relations) {
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = rel.getRelationName();
            erse.values[col].value = getRelationValues(iids, rel);
            erse.values[col].unitId = new T_LONGLONG(0, 0);
            col++;
        }
    }
    
    /**
     * Returns the related iids for all given iids and the given relation as a TS_ValueSeq.
     * 
     * @param iids the instance ids of all instances
     * @param rel the relation with which to get the related instance ids
     * @return the related iids value sequence
     * @throws OpenAtfxException
     */
    private TS_ValueSeq getRelationValues(Collection<Long> iids, AtfxRelation rel) throws OpenAtfxException {
        // get the related iids for all given iids for the given relation
        List<Long> relationValues = new ArrayList<>();
        for (Long iid : iids) {
            List<Long> relatedIds = api.getRelatedInstanceIds(aid, iid, rel);
            if (relatedIds.size() > 1) {
                throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                            "Wildcard handling at instances query handled relation that had more than one target instance!");
            } else if (relatedIds.isEmpty()) {
                relationValues.add(null);
            } else {
                relationValues.add(relatedIds.get(0));
            }
        }
        
        List<TS_Value> list = new ArrayList<>();
        DataType dt = DataType.DT_LONGLONG;
        for (Long relatediid : relationValues) {
            TS_Value value = ODSHelper.createEmptyTS_Value(dt);
            if (relatediid != null) {
                value.flag = 15;
                value.u.longlongVal(ODSHelper.asODSLongLong(relatediid));
            }
            list.add(value);
        }

        return ODSHelper.tsValue2tsValueSeq(list.toArray(new TS_Value[0]), dt);
    }
}
