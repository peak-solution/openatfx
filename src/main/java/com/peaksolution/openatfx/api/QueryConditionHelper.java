package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.JoinDef;
import org.asam.ods.RelationType;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelValueExt;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.util.ODSHelper;
import com.peaksolution.openatfx.util.PatternUtil;


/**
 * <p>
 * Title: {@link QueryConditionHelper}
 * </p>
 * <b>Description:</b>
 * <p>
 * Helper class for handling a query's conditions. Handles multiple conditions with AND operators only. Supports EQ and
 * LIKE opCodes with their according CI and negated counterparts as well as INSET. Supports datatypes DT/DS_STRING,
 * DT/DS_LONGLONG and DT/DS_ENUM. Supports conditions on other elements than in select, as long as the path to reach
 * them does not exceed 2 relation jumps. The path to related elements is identified by
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
public class QueryConditionHelper {

    private static final int DEFAULT_MAX_RELATION_PATH_LENGTH = 3;
    private Collection<Long> iids;
    private final JoinDef[] joins;
    private final long aid;
    private final OpenAtfxAPIImplementation api;
    private final int maxConditionRelationFollow;

    /**
     * Constructor.
     * 
     * @param aid the query's aid in selects
     * @param iids all available iids for the given aid
     * @param api the atfx cache
     * @throws OpenAtfxException
     */
    public QueryConditionHelper(long aid, Collection<Long> iids, JoinDef[] joins, OpenAtfxAPIImplementation api)
            throws OpenAtfxException {
        this.aid = aid;
        this.iids = iids;
        this.joins = joins;
        this.api = api;

        NameValueUnit configuredMaxRelValue = api.getContext().get("MAX_CONDITION_RELATION_FOLLOW");
        if (configuredMaxRelValue != null) {
            String configuredMaxRelValueString = configuredMaxRelValue.getValue().valueToString();
            if (configuredMaxRelValueString != null && !configuredMaxRelValueString.isEmpty()) {
                maxConditionRelationFollow = Integer.parseInt(configuredMaxRelValueString);
            } else {
                maxConditionRelationFollow = DEFAULT_MAX_RELATION_PATH_LENGTH;
            }
        } else {
            maxConditionRelationFollow = DEFAULT_MAX_RELATION_PATH_LENGTH;
        }
    }

    /**
     * The given condition is applied to filter this helper's result. Use this method to add all required conditions
     * from the original query.
     * 
     * @param condition
     * @throws OpenAtfxException
     */
    public void applyCondition(SelValueExt condition) throws OpenAtfxException {
        if (iids == null || api == null) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "QueryConditionHelper has not yet been initialized!");
        }

        iids = filter(condition);
    }

    /**
     * Filters all previously provided/filtered iids further with the given condition. Checks conditions on related
     * instances, handles several opCodes and supports datatypes DT_/DS_String, DT_/DS_LONGLONG and DT_ENUM.
     * 
     * @param condition the condition to apply
     * @return all of the iids, still valid for given condition
     * @throws OpenAtfxException
     */
    private Set<Long> filter(SelValueExt condition) throws OpenAtfxException {
        Set<Long> filteredIids = new HashSet<>();
        long conditionAid = ODSHelper.asJLong(condition.attr.attr.aid);

        // for performance reasons the values need to be fetched separately once before iterating over iids
        String stringVal = null;
        String[] stringSeqVal = null;
        T_LONGLONG longVal = null;
        long[] longSeqVal = null;
        Integer intVal = null;
        int[] intSeqVal = null;
        if (condition.value.u.discriminator() == DataType.DT_STRING) {
            stringVal = condition.value.u.stringVal();
        } else if (condition.value.u.discriminator() == DataType.DS_STRING) {
            stringSeqVal = condition.value.u.stringSeq();
            Arrays.sort(stringSeqVal); // sort so find method works
        } else if (condition.value.u.discriminator() == DataType.DT_LONGLONG) {
            longVal = condition.value.u.longlongVal();
        } else if (condition.value.u.discriminator() == DataType.DS_LONGLONG) {
            T_LONGLONG[] longLongSeqVal = condition.value.u.longlongSeq();
            longSeqVal = ODSHelper.asJLong(longLongSeqVal);
            Arrays.sort(longSeqVal); // sort so find method works
        } else if (condition.value.u.discriminator() == DataType.DT_ENUM) {
            intVal = condition.value.u.enumVal();
        } else if (condition.value.u.discriminator() == DataType.DS_ENUM) {
            intSeqVal = condition.value.u.enumSeq();
            Arrays.sort(intSeqVal); // sort so find method works
        }

        AtfxElement element = api.getAtfxElement(aid);
        for (Long iid : iids) {
            if (condition == null) {
                filteredIids.add(iid);
            } else if (conditionAid != aid) {
                if (checkConditionOnRelatedInstances(iid, conditionAid, condition)) {
                    filteredIids.add(iid);
                }
            } else if (condition.value.u.discriminator() == DataType.DT_STRING) {
                Instance instance = api.getInstanceById(aid, iid);
                NameValueUnit value = instance.getValue(condition.attr.attr.aaName);
                if (value != null) {
                    boolean addToFilter = false;
                    if ((condition.oper == SelOpcode.IS_NULL && !value.hasValidValue())
                            || (condition.oper == SelOpcode.IS_NOT_NULL && value.hasValidValue())) {
                        addToFilter = true;
                    } else if (isCIOpcode(condition.oper)) {
                        addToFilter = PatternUtil.nameFilterMatchCI(value.getValue().stringVal(), stringVal);
                    } else {
                        addToFilter = PatternUtil.nameFilterMatch(value.getValue().stringVal(), stringVal);
                    }
                    if (isNegatedOpcode(condition.oper)) {
                        addToFilter = !addToFilter;
                    }
                    if (addToFilter) {
                        filteredIids.add(iid);
                    }
                }
            } else if (condition.value.u.discriminator() == DataType.DS_STRING) {
                Instance instance = api.getInstanceById(aid, iid);
                NameValueUnit value = instance.getValue(condition.attr.attr.aaName);
                if ((condition.oper == SelOpcode.INSET
                        && Arrays.binarySearch(stringSeqVal, value.getValue().stringVal()) > -1)
                        || condition.oper == SelOpcode.NOTINSET
                                && Arrays.binarySearch(stringSeqVal, value.getValue().stringVal()) < 0) {
                    filteredIids.add(iid);
                }

            } else if (condition.value.u.discriminator() == DataType.DT_LONGLONG) {
                Integer attrNo = element.getAttrNoByName(condition.attr.attr.aaName);
                boolean addToFilter = false;
                if (attrNo == null) {
                    // is a relation condition
                    AtfxRelation ar = element.getRelationByName(condition.attr.attr.aaName);
                    List<Long> longlongVals = api.getRelatedInstanceIds(aid, iid, ar);
                    addToFilter = longlongVals.size() == 1 && longlongVals.get(0) == ODSHelper.asJLong(longVal);
                } else {
                    // is an attribute condition
                    Instance instance = api.getInstanceById(aid, iid);
                    NameValueUnit value = instance.getValue(attrNo);
                    if (value != null) {
                        addToFilter = value.getValue().longlongVal() == ODSHelper.asJLong(longVal);
                    }
                }
                if (isNegatedOpcode(condition.oper)) {
                    addToFilter = !addToFilter;
                }
                if (addToFilter) {
                    filteredIids.add(iid);
                }
            } else if ((condition.value.u.discriminator() == DataType.DS_LONGLONG)
                    && (condition.oper == SelOpcode.INSET)) {
                Integer attrNo = element.getAttrNoByName(condition.attr.attr.aaName);
                if (attrNo == null) {
                    // is a relation condition
                    AtfxRelation ar = element.getRelationByName(condition.attr.attr.aaName);
                    List<Long> longlongVals = api.getRelatedInstanceIds(aid, iid, ar);
                    if (longlongVals.size() == 1 && ((condition.oper == SelOpcode.INSET
                            && Arrays.binarySearch(longSeqVal, longlongVals.get(0)) > -1)
                            || condition.oper == SelOpcode.NOTINSET
                                    && Arrays.binarySearch(longSeqVal, longlongVals.get(0)) < 0)) {
                        filteredIids.add(iid);
                    }
                } else {
                    // is an attribute condition
                    Instance instance = api.getInstanceById(aid, iid);
                    NameValueUnit value = instance.getValue(attrNo);
                    long searchValue = value.getValue().longlongVal();
                    if ((condition.oper == SelOpcode.INSET && Arrays.binarySearch(longSeqVal, searchValue) > -1)
                            || condition.oper == SelOpcode.NOTINSET
                                    && Arrays.binarySearch(longSeqVal, searchValue) < 0) {
                        filteredIids.add(iid);
                    }
                }
            } else if (condition.value.u.discriminator() == DataType.DT_ENUM) {
                Instance instance = api.getInstanceById(aid, iid);
                NameValueUnit value = instance.getValue(condition.attr.attr.aaName);
                if (value != null) {
                    if ((condition.oper == SelOpcode.IS_NULL && !value.hasValidValue())
                            || (condition.oper == SelOpcode.IS_NOT_NULL && value.hasValidValue())
                            || (value.getValue().enumVal() >= 0 && value.hasValidValue() && intVal != null
                                    && value.getValue().enumVal() == intVal)) {
                        filteredIids.add(iid);
                    }
                }
            } else if (condition.value.u.discriminator() == DataType.DS_ENUM) {
                Instance instance = api.getInstanceById(aid, iid);
                NameValueUnit value = instance.getValue(condition.attr.attr.aaName);
                if ((condition.oper == SelOpcode.INSET
                        && Arrays.binarySearch(intSeqVal, value.getValue().enumVal()) > -1)
                        || condition.oper == SelOpcode.NOTINSET
                                && Arrays.binarySearch(intSeqVal, value.getValue().enumVal()) < 0) {
                    filteredIids.add(iid);
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
    public Collection<Long> getFilteredIIDs() {
        return iids;
    }

    protected boolean checkConditionOnRelatedInstances(long iid, long conditionAid, SelValueExt condition)
            throws OpenAtfxException {
        List<AtfxRelation> relPath = findRelationPath(aid, conditionAid);
        return resolveRelationPathAndCheckCondition(relPath, iid, condition);
    }

    /**
     * Follows the given path of relations from the source iid and checks the given condition at its end.
     * 
     * @param relPath ordered relations for the path to reach a target element from a source element
     * @param iid the iid of the instance to start from
     * @param condition the condition to check on the instances at the end of the path
     * @return true, if a condition match was found, false otherwise
     * @throws OpenAtfxException
     */
    private boolean resolveRelationPathAndCheckCondition(List<AtfxRelation> relPath, long iid, SelValueExt condition)
            throws OpenAtfxException {
        if (!relPath.isEmpty()) {
            AtfxRelation ar = relPath.get(0);
            List<Long> relatedIids = api.getRelatedInstanceIds(ar.getElement1().getId(), iid, ar);

            if (relatedIids.isEmpty()) {
                return false;
            } else if (relPath.size() == 1) {
                return checkConditionOnElement(relatedIids, condition);
            } else {
                for (long relatedIid : relatedIids) {
                    if (resolveRelationPathAndCheckCondition(relPath.subList(1, relPath.size()), relatedIid,
                                                             condition)) {
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
     * @throws OpenAtfxException
     */
    private boolean checkConditionOnElement(Collection<Long> instanceIds, SelValueExt condition)
            throws OpenAtfxException {
        long elemAid = ODSHelper.asJLong(condition.attr.attr.aid);
        QueryConditionHelper childHelper = new QueryConditionHelper(elemAid, instanceIds, joins, api);
        childHelper.applyCondition(condition);
        return !childHelper.getFilteredIIDs().isEmpty();
    }

    protected List<AtfxRelation> findRelationPath(long fromAid, long toAid) throws OpenAtfxException {
        List<AtfxRelation> relationPath = recursivelyFindRelationPath(fromAid, toAid, 1);
        if (relationPath.isEmpty()) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
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
     * @throws OpenAtfxException
     */
    private List<AtfxRelation> recursivelyFindRelationPath(long fromAid, long toAid, long pathLength)
            throws OpenAtfxException {
        List<AtfxRelation> relationsPath = new ArrayList<>();

        if (pathLength > maxConditionRelationFollow) {
            return Collections.emptyList();
        }

        // find relations, that directly connect the other element
        AtfxElement element1 = api.getAtfxElement(fromAid);
        List<AtfxRelation> foundRelations = new ArrayList<>();

        for (AtfxRelation ar : element1.getAtfxRelations()) {
            long targetAidFromRelation = ar.getElement2().getId();
            if (!(targetAidFromRelation < 1 && api.isExtendedCompatibilityMode()) && targetAidFromRelation == toAid) {
                foundRelations.add(ar);
            }
        }

        // recursively identify relation path to target element
        if (foundRelations.isEmpty()) {
            List<List<AtfxRelation>> recursivePath = new ArrayList<>();
            for (AtfxRelation ar : element1.getAtfxRelations()) {
                long targetAidFromRelation = ar.getElement2().getId();
                if (targetAidFromRelation > 0) {
                    List<AtfxRelation> currentPath = recursivelyFindRelationPath(targetAidFromRelation, toAid,
                                                                                 pathLength + 1);
                    if (currentPath == null || currentPath.isEmpty()) {
                        continue;
                    }
                    currentPath.add(0, ar);
                    recursivePath.add(currentPath);
                }
            }
            List<AtfxRelation> shortestPath = new ArrayList<>();
            for (List<AtfxRelation> list : recursivePath) {
                if (list == null) {
                    continue;
                }
                if (shortestPath.isEmpty()) {
                    shortestPath = list;
                    continue;
                }

                shortestPath = comparePaths(list, shortestPath);
            }
            relationsPath.addAll(shortestPath);
        } else if (foundRelations.size() == 1) {
            relationsPath.add(foundRelations.get(0));
            return foundRelations;
        } else {
            AtfxRelation ar = identifyRelevantRelation(foundRelations);
            if (ar != null) {
                relationsPath.add(ar);
            }
        }

        return relationsPath;
    }

    /**
     * Gives precedence to paths that have more father/child relations, more base relations or less of other relations.
     * If all the same, then the shorter path is returned. If the length is equal, the previous best relation is
     * returned.
     * 
     * @param newPath
     * @param previousBestPath
     * @return the "better" path
     * @throws OpenAtfxException
     */
    private List<AtfxRelation> comparePaths(List<AtfxRelation> newPath, List<AtfxRelation> previousBestPath)
            throws OpenAtfxException {
        int nrOfPreviousBaseRelations = 0;
        int nrOfPreviousFatherChildRelations = 0;
        int nrOfPreviousLowImportanceRelations = 0;
        for (int i = 0; i < previousBestPath.size(); i++) {
            AtfxRelation ar = previousBestPath.get(i);
            String baseRelationName = ar.getBaseName();
            if (baseRelationName != null && !baseRelationName.isBlank()) {
                nrOfPreviousBaseRelations++;
            }
            if (Relationship.FATHER == ar.getRelationship() || Relationship.CHILD == ar.getRelationship()) {
                nrOfPreviousFatherChildRelations++;
            } else if (baseRelationName == null || baseRelationName.isBlank()) {
                nrOfPreviousLowImportanceRelations++;
            }
        }

        int nrOfNewBaseRelations = 0;
        int nrOfNewFatherChildRelations = 0;
        int nrOfNewLowImportanceRelations = 0;
        for (int i = 0; i < newPath.size(); i++) {
            AtfxRelation ar = newPath.get(i);
            String baseRelationName = ar.getBaseName();
            if (baseRelationName != null && !baseRelationName.isBlank()) {
                nrOfNewBaseRelations++;
            }
            if (Relationship.FATHER == ar.getRelationship() || Relationship.CHILD == ar.getRelationship()) {
                nrOfNewFatherChildRelations++;
            } else if (baseRelationName == null || baseRelationName.isBlank()) {
                nrOfNewLowImportanceRelations++;
            }
        }

        List<AtfxRelation> newBestPath = previousBestPath;
        if (nrOfNewLowImportanceRelations < nrOfPreviousLowImportanceRelations
                || nrOfNewFatherChildRelations > nrOfPreviousFatherChildRelations
                || nrOfNewBaseRelations > nrOfPreviousBaseRelations || (newPath.size() < previousBestPath.size()
                        && nrOfNewLowImportanceRelations == nrOfPreviousLowImportanceRelations)) {
            newBestPath = newPath;
        }
        return newBestPath;
    }

    /**
     * If more than one relation were found that connect the according elements, the relevant one is identified by
     * prioritizing base relations and after that father-child relations.
     * 
     * @param relations
     * @return
     * @throws OpenAtfxException
     */
    protected AtfxRelation identifyRelevantRelation(List<AtfxRelation> relations) throws OpenAtfxException {
        if (relations.size() == 1) {
            return relations.get(0);
        } else {
            // if more than one relation between the elements was found, apply some rules to identify the right one
            // if an explicit identifying join was provided, use that one to identify the relation
            AtfxRelation ar = checkJoinsToIdentifyRelation(relations, joins);

            // if more than one relation between the elements was found, prefer a base relation
            if (ar == null) {
                ar = findBaseRelation(relations);
            }
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
     * @param relations
     * @param joins
     * @return
     * @throws OpenAtfxException
     */
    private AtfxRelation checkJoinsToIdentifyRelation(List<AtfxRelation> relations, JoinDef[] joins)
            throws OpenAtfxException {
        if (joins == null || joins.length < 1) {
            return null;
        }
        for (AtfxRelation relation : relations) {
            for (JoinDef join : joins) {
                long joinFrom = ODSHelper.asJLong(join.fromAID);
                long joinTo = ODSHelper.asJLong(join.toAID);
                long relationFrom = relation.getElement1().getId();
                long relationTo = relation.getElement2().getId();

                // check if join references the "normal" relation direction of the current relation
                if (joinFrom == relationFrom && joinTo == relationTo
                        && join.refName.equals(relation.getRelationName())) {
                    return relation;
                }
                // check if join references the inverse relation direction of current relation
                else if (joinFrom == relationTo && joinTo == relationFrom
                        && join.refName.equals(relation.getInverseRelationName())) {
                    return relation;
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
     * @throws OpenAtfxException
     */
    protected AtfxRelation findBaseRelation(List<AtfxRelation> relations) throws OpenAtfxException {
        List<AtfxRelation> foundBaseRelations = new ArrayList<>();
        for (AtfxRelation ar : relations) {
            String baseRelationName = ar.getBaseName();
            if (baseRelationName != null && !baseRelationName.isBlank()) {
                foundBaseRelations.add(ar);
            }
        }

        if (foundBaseRelations.isEmpty()) {
            return null;
        } else if (foundBaseRelations.size() == 1) {
            return foundBaseRelations.get(0);
        } else {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Automatic join for query could not be handled, since multiple base relations between the elements to join are not supported!");
        }
    }

    /**
     * Tries to find a single father-child relation in the given relations
     * 
     * @param relations
     * @return
     * @throws OpenAtfxException
     */
    protected AtfxRelation findFatherChildRelation(List<AtfxRelation> relations) throws OpenAtfxException {
        List<AtfxRelation> foundFatherChildRelations = new ArrayList<>();
        for (AtfxRelation ar : relations) {
            if (ar.getRelationType() == RelationType.FATHER_CHILD) {
                foundFatherChildRelations.add(ar);
            }
        }

        if (foundFatherChildRelations.isEmpty()) {
            return null;
        } else if (foundFatherChildRelations.size() == 1) {
            return foundFatherChildRelations.get(0);
        } else {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Automatic join for query could not be handled, since multiple FATHER_CHILD relations between the elements to join are not supported!");
        }
    }
}
