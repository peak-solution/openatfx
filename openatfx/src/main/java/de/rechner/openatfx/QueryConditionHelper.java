package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationType;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelValueExt;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * <p>
 * Title: {@link QueryConditionHelper}
 * </p>
 * <b>Description:</b>
 * <p>
 * Helper class for handling a query's conditions. Handles multiple conditions with AND operators only. Supports EQ and
 * LIKE opCodes with their according CI and negated counterparts as well as INSET. Supports datatypes DT/DS_STRING,
 * DT/DS_LONGLONG and DT_ENUM. Supports conditions on other elements than in select, as long as the path to reach them
 * does not exceed 2 relation jumps. The path to related elements is identified by 
 * </p>
 *
 * @author Markus Renner
 *         <p>
 *         Company: Peak Solution GmbH
 *         </p>
 *         $Rev: $: Revision of last commit<br/>
 *         $Author: $: Author of last commit<br/>
 *         $Date: $: Date of last commit
 */
class QueryConditionHelper {
    private static final int MAX_RELATION_PATH_LENGTH = 2;
    private Collection<Long> iids;
    private final long aid;
    private final AtfxCache atfxCache;
    
    /**
     * Constructor.
     * 
     * @param aid the query's aid in selects
     * @param iids all available iids for the given aid
     * @param atfxCache the atfx cache
     */
    QueryConditionHelper (long aid, Collection<Long> iids, AtfxCache atfxCache) {
        this.aid = aid;
        this.iids = iids;
        this.atfxCache = atfxCache;
    }
    
    /**
     * The given condition is applied to filter this helper's result.
     * Use this method to add all required conditions from the original query.
     * 
     * @param condition
     * @throws AoException
     */
    void applyCondition(SelValueExt condition) throws AoException {
        if (iids == null || atfxCache == null) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "QueryConditionHelper has not yet been initialized!");
        }
        
        iids = filter(condition);
    }
    
    /**
     * Filters all previously provided/filtered iids further with the given condition. Checks conditions on related
     * instances, handles several opCodes and supports datatypes DT_/DS_String, DT_/DS_LONGLONG and DT_ENUM.
     * 
     * @param condition the condition to apply
     * @return all of the iids, still valid for given condition
     * @throws AoException
     */
    private Set<Long> filter(SelValueExt condition) throws AoException {
        Set<Long> filteredIids = new HashSet<>();
        long conditionAid = ODSHelper.asJLong(condition.attr.attr.aid);
        
        for (Long iid : iids) {
            if (condition == null) {
                filteredIids.add(iid);
            } else if (condition.value.u.discriminator() == DataType.DT_STRING) {
                if (conditionAid == aid) {
                    Integer attrNo = atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                    TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                    if ((value != null) && (value.u != null) && (value.u.stringVal() != null)) {
                        boolean addToFilter = false;
                        if (isCIOpcode(condition.oper)) {
                            addToFilter = PatternUtil.nameFilterMatchCI(value.u.stringVal(), condition.value.u.stringVal());
                        } else {
                            addToFilter = PatternUtil.nameFilterMatch(value.u.stringVal(), condition.value.u.stringVal());
                        }
                        if (isNegatedOpcode(condition.oper)) {
                            addToFilter = !addToFilter;
                        }
                        if (addToFilter) {
                            filteredIids.add(iid);
                        }
                    }
                } else {
                    if (checkConditionOnRelatedInstances(iid, conditionAid, condition)) {
                        filteredIids.add(iid);
                    }
                }
            } else if ((condition.value.u.discriminator() == DataType.DS_STRING)
                    && (condition.oper == SelOpcode.INSET)) {
                Integer attrNo = atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                String[] cond = condition.value.u.stringSeq();
                Arrays.sort(cond); // sort so find method works
                if (Arrays.binarySearch(cond, value.u.stringVal()) > -1) {
                    filteredIids.add(iid);
                }

            } else if (condition.value.u.discriminator() == DataType.DT_LONGLONG) {
                if (conditionAid == aid) {
                    Integer attrNo = atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                    boolean addToFilter = false;
                    if (attrNo == null) {
                        ApplicationRelation ar = atfxCache.getRelationByName(aid, condition.attr.attr.aaName);
                        List<Long> longlongVals  = atfxCache.getRelatedInstanceIds(aid, iid, ar);
                        addToFilter = longlongVals.size() == 1 && longlongVals.get(0) == ODSHelper.asJLong(condition.value.u.longlongVal());
                    } else {
                        TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                        if ((value != null) && (value.u != null) && (value.u.longlongVal() != null)) {
                            addToFilter = ODSHelper.asJLong(value.u.longlongVal()) == ODSHelper.asJLong(condition.value.u.longlongVal());
                        }
                    }
                    if (isNegatedOpcode(condition.oper)) {
                        addToFilter = !addToFilter;
                    }
                    if (addToFilter) {
                        filteredIids.add(iid);
                    }
                } else {
                    if (checkConditionOnRelatedInstances(iid, conditionAid, condition)) {
                        filteredIids.add(iid);
                    }
                }
            } else if ((condition.value.u.discriminator() == DataType.DS_LONGLONG)
                    && (condition.oper == SelOpcode.INSET)) {
                Integer attrNo = atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                if (attrNo == null) {
                    ApplicationRelation ar = atfxCache.getRelationByName(aid, condition.attr.attr.aaName);
                    List<Long> longlongVals  = atfxCache.getRelatedInstanceIds(aid, iid, ar);
                    long[] cond = ODSHelper.asJLong(condition.value.u.longlongSeq());
                    Arrays.sort(cond); // sort so find method works

                    if (longlongVals.size() == 1 && Arrays.binarySearch(cond, longlongVals.get(0)) > -1) {
                        filteredIids.add(iid);
                    }
                } else {
                    TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                    long[] cond = ODSHelper.asJLong(condition.value.u.longlongSeq());
                    Arrays.sort(cond); // sort so find method works
                    if (Arrays.binarySearch(cond, ODSHelper.asJLong(value.u.longlongVal())) > -1) {
                        filteredIids.add(iid);
                    }
                }
            } else if (condition.value.u.discriminator() == DataType.DT_ENUM) {
                if (conditionAid == aid) {
                    Integer attrNo = atfxCache.getAttrNoByName(aid, condition.attr.attr.aaName);
                    TS_Value value = atfxCache.getInstanceValue(aid, attrNo, iid);
                    if ((value != null) && (value.u != null) && (value.u.enumVal() >= 0)
                            && value.u.enumVal() == condition.value.u.enumVal()) {
                        filteredIids.add(iid);
                    }
                } else {
                    if (checkConditionOnRelatedInstances(iid, conditionAid, condition)) {
                        filteredIids.add(iid);
                    }
                }
            }
        }
        
        return filteredIids;
    }
    
    private boolean isCIOpcode(SelOpcode op) {
        return op == SelOpcode.CI_EQ || op == SelOpcode.CI_GT || op == SelOpcode.CI_GTE || op == SelOpcode.CI_INSET
                || op == SelOpcode.CI_LIKE || op == SelOpcode.CI_LT || op == SelOpcode.CI_LTE || op == SelOpcode.CI_NEQ
                || op == SelOpcode.CI_NOTINSET || op == SelOpcode.CI_NOTLIKE;
    }
    
    private boolean isNegatedOpcode(SelOpcode op) {
        return op == SelOpcode.NEQ || op == SelOpcode.NOTINSET || op == SelOpcode.NOTLIKE || op == SelOpcode.CI_NEQ
                || op == SelOpcode.CI_NOTINSET || op == SelOpcode.CI_NOTLIKE;
    }
    
    /**
     * Returns the iids left after all applied conditions.
     * 
     * @return all filtered iids
     */
    Collection<Long> getFilteredIIDs() {
        return iids;
    }
    
    protected boolean checkConditionOnRelatedInstances(long iid, long conditionAid, SelValueExt condition) throws AoException {
        List<ApplicationRelation> relPath = findRelationPath(aid, conditionAid);
        return resolveRelationPathAndCheckCondition(relPath, iid, condition);
    }
    
    /**
     * Follows the given path of relations from the source iid and checks the given condition at its end.
     * 
     * @param relPath ordered relations for the path to reach a target element from a source element
     * @param iid the iid of the instance to start from
     * @param condition the condition to check on the instances at the end of the path
     * @return true, if a condition match was found, false otherwise
     * @throws AoException
     */
    private boolean resolveRelationPathAndCheckCondition(List<ApplicationRelation> relPath, long iid, SelValueExt condition) throws AoException {
        if (!relPath.isEmpty()) {
            ApplicationRelation ar = relPath.get(0);
            long elem1AID = ODSHelper.asJLong(ar.getElem1().getId());
            List<Long> relatedIids  = atfxCache.getRelatedInstanceIds(elem1AID, iid, ar);
            
            if (relatedIids.isEmpty()) {
                return false;
            } else if (relPath.size() == 1) {
                return checkConditionOnElement(relatedIids, condition);
            } else {
                for (long relatedIid : relatedIids) {
                    if (resolveRelationPathAndCheckCondition(relPath.subList(1, relPath.size()), relatedIid, condition)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks the given condition on all instances with one of the given iids. Uses a child instance of this helper to
     * perform the filter on the condition.
     * 
     * @param instanceIds the iids to check with condition
     * @param condition the condition to check on instances
     * @return true, if a condition match was found, false otherwise
     * @throws AoException
     */
    private boolean checkConditionOnElement(Collection<Long> instanceIds, SelValueExt condition) throws AoException {
        long elemAid = ODSHelper.asJLong(condition.attr.attr.aid);
        QueryConditionHelper childHelper = new QueryConditionHelper(elemAid, instanceIds, atfxCache);
        childHelper.applyCondition(condition);
        return !childHelper.getFilteredIIDs().isEmpty();
    }
    
    protected List<ApplicationRelation> findRelationPath(long fromAid, long toAid) throws AoException {
        List<ApplicationRelation> relationPath = recursivelyFindRelationPath(fromAid, toAid, 1);
        if (relationPath.isEmpty()) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "QueryStructureExt not supported: Could not identify relation between elements with aids "
                                          + fromAid + " and " + toAid);
        }
        return relationPath;
    }
    
    /**
     * Finds the shortest path to the target aid, prioritizing base and father-child relations. A maximum relation path
     * of MAX_RELATION_PATH_LENGTH is allowed, otherwise an empty path is returned.
     * 
     * @param fromAid
     * @param toAid
     * @param pathLength
     * @return
     * @throws AoException
     */
    private List<ApplicationRelation> recursivelyFindRelationPath(long fromAid, long toAid, long pathLength) throws AoException {
        List<ApplicationRelation> relationsPath = new ArrayList<>();
        
        if (pathLength > MAX_RELATION_PATH_LENGTH) {
            return Collections.emptyList();
        }
        
        // find relations, that directly connect the other element
        List<ApplicationRelation> foundRelations = new ArrayList<>();
        for (ApplicationRelation ar : atfxCache.getApplicationRelations(fromAid)) {
            if (ODSHelper.asJLong(ar.getElem2().getId()) == toAid) {
                foundRelations.add(ar);
            }
        }
        
        // recursively identify relation path to target element
        if (foundRelations.isEmpty()) {
            List<List<ApplicationRelation>> recursivePath = new ArrayList<>();
            for (ApplicationRelation ar : atfxCache.getApplicationRelations(fromAid)) {
                List<ApplicationRelation> currentPath = recursivelyFindRelationPath(ODSHelper.asJLong(ar.getElem2().getId()), toAid, pathLength + 1);
                if (currentPath == null || currentPath.isEmpty()) {
                    continue;
                }
                currentPath.add(0, ar);
                recursivePath.add(currentPath);
            }
            List<ApplicationRelation> shortestPath = new ArrayList<>();
            for (List<ApplicationRelation> list : recursivePath) {
                if (list == null) {
                    continue;
                }
                if (shortestPath.isEmpty()) {
                    shortestPath = list;
                    continue;
                }
                
                if (list.size() < shortestPath.size()) {
                    shortestPath = list;
                }
            }
            relationsPath.addAll(shortestPath);
        } else if (foundRelations.size() == 1) {
            relationsPath.add(foundRelations.get(0));
            return foundRelations;
        } else {
            ApplicationRelation ar = identifyRelevantRelation(foundRelations);
            if (ar != null) {
                relationsPath.add(ar);
            }
        }
        
        return relationsPath;
    }
    
    /**
     * If more than one relation were found that connect the according elements, the relevant one is identified by
     * prioritizing base relations and after that father-child relations.
     * 
     * @param relations
     * @return
     * @throws AoException
     */
    protected ApplicationRelation identifyRelevantRelation(List<ApplicationRelation> relations) throws AoException {
        if (relations.size() == 1) {
            return relations.get(0);
        } else {
            // if more than one relation between the elements was found, prefer a base relation
            ApplicationRelation ar = findBaseRelation(relations);
            if (ar != null) {
                return ar;
            // if only non-base relations could be found between elements prefer a FATHER_CHILD relation type
            } else {
                ar = findFatherChildRelation(relations);
                if (ar != null) {
                    return ar;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Tries to find a single base relation in the given relations.
     * 
     * @param relations
     * @return
     * @throws AoException
     */
    protected ApplicationRelation findBaseRelation(List<ApplicationRelation> relations) throws AoException {
        List<ApplicationRelation> foundBaseRelations = new ArrayList<>();
        for (ApplicationRelation ar : relations) {
            if (ar.getBaseRelation() != null) {
                foundBaseRelations.add(ar);
            }
        }
        
        if (foundBaseRelations.isEmpty()) {
            return null;
        } else if (foundBaseRelations.size() == 1) {
            return foundBaseRelations.get(0);
        } else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Automatic join for query could not be handled, since multiple base relations between the elements to join are not supported!");
        }
    }
    
    /**
     * Tries to find a single father-child relation in the given relations
     * 
     * @param relations
     * @return
     * @throws AoException
     */
    protected ApplicationRelation findFatherChildRelation(List<ApplicationRelation> relations) throws AoException {
        List<ApplicationRelation> foundFatherChildRelations = new ArrayList<>();
        for (ApplicationRelation ar : relations) {
            if (ar.getRelationType() == RelationType.FATHER_CHILD) {
                foundFatherChildRelations.add(ar);
            }
        }
        
        if (foundFatherChildRelations.isEmpty()) {
            return null;
        } else if (foundFatherChildRelations.size() == 1) {
            return foundFatherChildRelations.get(0);
        } else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Automatic join for query could not be handled, since multiple FATHER_CHILD relations between the elements to join are not supported!");
        }
    }
}
