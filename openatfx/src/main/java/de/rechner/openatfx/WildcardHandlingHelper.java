package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.AttrResultSet;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Helper class to handle the return of attribute and relation values for getInstances/Ext() queries in case a wildcard
 * was given for the attribute selection.
 * 
 * @author Markus Renner
 */
public class WildcardHandlingHelper {

    private final AtfxCache cache;
    private final long aid;
    private Integer[] attributeNumbers = null;
    private String[] attributeNames = null;
    private ApplicationRelation[] relations = null;

    public WildcardHandlingHelper(AtfxCache cache, long aid) {
        this.cache = cache;
        this.aid = aid;
    }

    /**
     * Prepares all attributes and relations to return when a wildcard was given.  
     * 
     * @throws AoException
     */
    private void init() throws AoException {
        boolean isLocalColumn = this.cache.getAidsByBaseType("aolocalcolumn").contains(aid);
        Collection<ApplicationAttribute> allApplicationAttributes = this.cache.getApplicationAttributes(aid);
        Collection<ApplicationRelation> allApplicationRelations = this.cache.getApplicationRelations(aid);
        List<String> attrNames = new ArrayList<String>();
        List<ApplicationRelation> relevantRelations = new ArrayList<ApplicationRelation>();
        List<Integer> attrNumbers = new ArrayList<Integer>();
        // add all application attributes of the current element
        for (ApplicationAttribute currentAttr : allApplicationAttributes) {
            // skip values attribute of AoLocalColumn according to ODS 5.3.1 specification chapter 5.2.3.9
            if (isLocalColumn && currentAttr.getBaseAttribute() != null
                    && currentAttr.getBaseAttribute().getName().equals("values")) {
                continue;
            }
            String currentAttrName = currentAttr.getName();
            attrNames.add(currentAttrName);
            attrNumbers.add(this.cache.getAttrNoByName(aid, currentAttrName));
        }
        // add all not "to-n" relations
        for (ApplicationRelation currentRel : allApplicationRelations) {
        	if (currentRel.getRelationRange().max >= 0) {
        	    relevantRelations.add(currentRel);
        	}
        }
        attributeNames = attrNames.toArray(new String[0]);
        attributeNumbers = attrNumbers.toArray(new Integer[0]);
        relations = relevantRelations.toArray(new ApplicationRelation[0]);
    }

    /**
     * Adds all application attribute values of the instances with given ids to the given query result. Also adds
     * values for all not "to n" relations.
     * 
     * @param ers the query result to fill
     * @param iids the instance ids for which to add all attribute values and relevant relation values
     * @throws AoException on any errors finding and setting the values to the result
     */
    public void fillElemResultSet(ElemResultSet ers, Collection<Long> iids) throws AoException {
        init();
        int nrOfAttributes = getNrOfAttributes();
        ers.attrValues = new AttrResultSet[nrOfAttributes + relations.length];
        for (int col = 9; col < attributeNames.length; col++) {
            ers.attrValues[col] = new AttrResultSet();
            ers.attrValues[col].attrValues = new NameValueSeqUnitId();
            ers.attrValues[col].attrValues.unitId = cache.getUnitIIDForAttr(aid, attributeNumbers[col]);
            ers.attrValues[col].attrValues.valName = attributeNames[col];
            ers.attrValues[col].attrValues.value = cache.getInstanceValues(aid, attributeNumbers[col], iids);
        }
        // add relevant relation attributes to result for relations
        int col = nrOfAttributes;
        for (ApplicationRelation rel : relations) {
            ers.attrValues[col] = new AttrResultSet();
            ers.attrValues[col].attrValues = new NameValueSeqUnitId();
            ers.attrValues[col].attrValues.valName = relations[col- nrOfAttributes].getRelationName();
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
     * @throws AoException on any errors finding and setting the values to the result
     */
    public void fillElemResultsSetExt(ElemResultSetExt erse, Collection<Long> iids) throws AoException {
        init();
        int nrOfAttributes = getNrOfAttributes();
        erse.values = new NameValueSeqUnitId[nrOfAttributes + relations.length];
        // add application attributes to result
        for (int col = 0; col < nrOfAttributes; col++) {
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = attributeNames[col];
            erse.values[col].value = cache.getInstanceValues(aid, attributeNumbers[col], iids);
            erse.values[col].unitId = new T_LONGLONG(0, 0);
        }
        // add relevant relation attributes to result for relations
        int col = nrOfAttributes;
        for (ApplicationRelation rel : relations) {
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = relations[col- nrOfAttributes].getRelationName();
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
     * @throws AoException
     */
    private TS_ValueSeq getRelationValues(Collection<Long> iids, ApplicationRelation rel) throws AoException {
        // get the related iids for all given iids for the given relation
        List<Long> relationValues = new ArrayList<Long>();
        for (Long iid : iids) {
            List<Long> relatedIds = cache.getRelatedInstanceIds(aid, iid, rel);
            if (relatedIds.size() > 1) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                        "Wildcard handling at instances query handled relation that had more than one target instance!");
            } else if (relatedIds.isEmpty()) {
                relationValues.add(null);
            } else {
                relationValues.add(relatedIds.get(0));
            }
        }
        
        List<TS_Value> list = new ArrayList<TS_Value>();
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

    /**
     * Checks the number of attributes and attribute numbers before returning the count of them.
     * 
     * @return the number of attributes prepared in this helper instance
     * @throws AoException
     */
    private int getNrOfAttributes() throws AoException {
        if (attributeNames.length == attributeNumbers.length) {
            return attributeNames.length;
        } else {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Wildcard handling at instances query produced inconsistent number of attributes!");
        }
    }
}
