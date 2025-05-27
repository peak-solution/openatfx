package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.NameValueUnitId;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.api.AtfxElement;
import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.util.ODSHelper;

/**
 * Queries handled by this helper class are usually referencing an m2n relation for a join. The query result will be
 * created along the respective relational database logic for joins. As expected to be returned by a DB-backed ODS
 * service, the result nvsuis will contain rows for each single pair of instances from both elements of the join. The
 * other columns selected for each element will be retained and filled along the two participating instances in each
 * row.
 * 
 * @author Markus Renner
 */
public class JoinHelper {

    private final OpenAtfxAPIImplementation api;
    private final JoinDef joinDef;

    public JoinHelper(OpenAtfxAPIImplementation api, JoinDef joinDef) {
        this.joinDef = joinDef;
        this.api = api;
    }

    public ElemResultSetExt[] join(ElemResultSetExt[] erses) throws OpenAtfxException {
        if (erses.length > 2) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "QueryStructureExt invalid: Join is only supported for two selected aids, but query contained "
                                                + erses.length);
        }
        
        long rootAid = ODSHelper.asJLong(joinDef.fromAID);
        AtfxElement element = api.getAtfxElement(rootAid);
        AtfxRelation joinRelation = element.getRelationByName(joinDef.refName);
        if (joinRelation == null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "QueryStructureExt invalid: Join relation '"
                    + joinDef.refName + "' could not be identified at element " + element.getName());
        }
        
        // check if join is an m2n relation, otherwise return erses as received
        if (joinRelation.getRelationRangeMax() != -1 || joinRelation.getInverseRelation().getRelationRangeMax() != -1) {
            return erses;
        }

        // collect iids and their values to create new joined nvsuis from afterwards
        Map<Long, long[]> iidsByAid = new HashMap<>();
        Map<Long, Map<Long, Collection<NameValueUnitId>>> iidsToValuesByAid = new HashMap<>();
        boolean useLongIids = false;
        for (int i = 0; i < erses.length; i++) {
            ElemResultSetExt erse = erses[i];
            long aid = ODSHelper.asJLong(erse.aid);
            
            // identify id attribute of current element
            NameValueSeqUnitId idNvsui = findIdNvsui(erse);
            String idAttrName = idNvsui.valName;
            DataType idDt = idNvsui.value.u.discriminator();
            useLongIids = DataType._DS_LONG == idDt.value();
            
            // extract values from sequence for each iid
            Map<Long, Collection<NameValueUnitId>> currentValuesMap = null;
            if (useLongIids) {
                // fill iidLists
                iidsByAid.put(aid, Arrays.stream(idNvsui.value.u.longVal()).asLongStream().toArray());
                
                // process with DT_LONG iids
                currentValuesMap = createValuesMapForLongIds(erse, idNvsui, idAttrName);
            } else {
                // fill iidLists
                iidsByAid.put(aid, Arrays.stream(idNvsui.value.u.longlongVal()).map(ODSHelper::asJLong).mapToLong(l -> l).toArray());
                
                // process with DT_LONGLONG iids
                currentValuesMap = createValuesMapForLongLongIds(erse, idNvsui, idAttrName);
            }
            iidsToValuesByAid.computeIfAbsent(aid, v -> new HashMap<>()).putAll(currentValuesMap);
        }
        
        return createJoinedResult(rootAid, joinRelation, iidsByAid, iidsToValuesByAid, useLongIids);
    }
    
    /**
     * @param rootAid
     * @param joinRelation
     * @param iidsByAid
     * @param iidsToValuesByAid
     * @param useLongIids
     * @return
     * @throws OpenAtfxException
     */
    private ElemResultSetExt[] createJoinedResult(long rootAid, AtfxRelation joinRelation,
            Map<Long, long[]> iidsByAid, Map<Long, Map<Long, Collection<NameValueUnitId>>> iidsToValuesByAid,
            boolean useLongIids) throws OpenAtfxException {
        long relAid = joinRelation.getAtfxElement2().getId();
        long[] iids = iidsByAid.get(rootAid);
        if (iids == null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "QueryStructureExt invalid: Expected selects for aid " + rootAid
                                                + " in query containing join '" + joinRelation.getRelationName()
                                                + "', but could not find any!");
        }
        List<Long> rootIids = Arrays.stream(iids).boxed().collect(Collectors.toList());
        long[][] relatedIids = new long[iids.length][];
        for (int i = 0; i < relatedIids.length; i++) {
            long rootIid = rootIids.get(i);
            List<Long> relIds = api.getRelatedInstanceIds(rootAid, rootIid, joinRelation);
            relatedIids[i] = relIds.stream().mapToLong(Long::longValue).toArray();
        }
        
        Map<Long, Map<String, NameValueSeqUnitId>> nvsuisByNameByAid = new HashMap<>();
        Map<NameValueSeqUnitId, NvsuiHelper> helperByNvsui = new HashMap<>();
        List<ElemResultSetExt> results = new ArrayList<>(iidsByAid.size());
        for (int i = 0; i < iids.length; i++) {
            long rootIid = iids[i];
            Collection<NameValueUnitId> rootNvuis = iidsToValuesByAid.get(rootAid).get(rootIid);
            long[] related = relatedIids[i];
            for (long relIid : related) {
                Collection<NameValueUnitId> relNvuis = iidsToValuesByAid.get(relAid).get(relIid);
                
                handleValuesForInstance(rootAid, rootNvuis, nvsuisByNameByAid, helperByNvsui);
                handleValuesForInstance(relAid, relNvuis, nvsuisByNameByAid, helperByNvsui);
            }
        }
        
        for (Entry<Long, Map<String, NameValueSeqUnitId>> aidEntry : nvsuisByNameByAid.entrySet()) {
            long aid = aidEntry.getKey();
            Collection<NameValueSeqUnitId> nvsuisForElement = aidEntry.getValue().values();
            ElemResultSetExt erse = new ElemResultSetExt(ODSHelper.asODSLongLong(aid), createJoinedNvsuis(nvsuisForElement, helperByNvsui));
            results.add(erse);
        }
        
        return results.toArray(new ElemResultSetExt[0]);
    }
    
    private NameValueSeqUnitId[] createJoinedNvsuis(Collection<NameValueSeqUnitId> nvsuis, Map<NameValueSeqUnitId, NvsuiHelper> helperByNvsui) {
        Collection<NameValueSeqUnitId> joined = new ArrayList<>(nvsuis.size());
        for (NameValueSeqUnitId nvsui : nvsuis) {
            NvsuiHelper helper = helperByNvsui.get(nvsui);
            helper.fillNvsui(nvsui);
            joined.add(nvsui);
        }
        return joined.toArray(new NameValueSeqUnitId[0]);
    }
    
    /**
     * @param aid
     * @param nvuis
     * @param nvsuisByNameByAid
     * @param helperByNvsui
     */
    private void handleValuesForInstance(long aid, Collection<NameValueUnitId> nvuis,
            Map<Long, Map<String, NameValueSeqUnitId>> nvsuisByNameByAid, Map<NameValueSeqUnitId, NvsuiHelper> helperByNvsui) {
        for (NameValueUnitId currentNvui : nvuis) {
            DataType dt = currentNvui.value.u.discriminator();
            NameValueSeqUnitId nvsui = nvsuisByNameByAid.computeIfAbsent(aid, v -> new HashMap<>())
                                                        .computeIfAbsent(currentNvui.valName,
                                                                         v -> new NameValueSeqUnitId(currentNvui.valName,
                                                                                                     new TS_ValueSeq(new TS_UnionSeq(),
                                                                                                                     new short[0]),
                                                                                                     currentNvui.unitId));
            NvsuiHelper valuesHelper = helperByNvsui.computeIfAbsent(nvsui, v -> new NvsuiHelper(dt));
            valuesHelper.addValue(valuesHelper.getValueFromNvui(currentNvui), currentNvui.value.flag);
        }
    }

    /**
     * @param element
     * @param relationName
     * @return
     * @throws OpenAtfxException
     */
    private AtfxRelation getRelationByName(AtfxElement element, String relationName) throws OpenAtfxException {
        if (relationName != null) {
            for (AtfxRelation relation : element.getAtfxRelations()) {
                if (relationName.equals(relation.getRelationName())) {
                    return relation;
                }
            }
        }
        return null;
    }
    
    /**
     * @param useLongIids
     * @param relatedIidsSeq
     * @return
     */
    private long[][] extractRelatedIids(boolean useLongIids, TS_ValueSeq relatedIidsSeq) {
        long[][] relatedIids = null;
        if (useLongIids) {
            int[][] intIids = relatedIidsSeq.u.longSeq();
            relatedIids = new long[intIids.length][];
            for (int i = 0; i < intIids.length; i++) {
                int[] ids = intIids[i];
                relatedIids[i] = Arrays.stream(ids).mapToLong(l -> l).toArray();
            }
        } else {
            T_LONGLONG[][] longIids = relatedIidsSeq.u.longlongSeq();
            relatedIids = new long[longIids.length][];
            for (int i = 0; i < longIids.length; i++) {
                T_LONGLONG[] ids = longIids[i];
                relatedIids[i] = Arrays.stream(ids).map(ODSHelper::asJLong).mapToLong(l -> l).toArray();
            }
        }
        return relatedIids;
    }
    
    /**
     * @param erse
     * @return
     * @throws OpenAtfxException
     */
    private NameValueSeqUnitId findIdNvsui(ElemResultSetExt erse) throws OpenAtfxException {
        Element element = api.getElementById(ODSHelper.asJLong(erse.aid));
        Attribute idAttr = element.getAttributeByBaseName("id");
        String idAttrName = idAttr.getName();
        for (NameValueSeqUnitId nvsui : erse.values) {
            if (idAttrName.equals(nvsui.valName)) {
                return nvsui;
            }
        }

        throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                    "QueryStructureExt invalid: Join is only supported when at least the "
                                            + "id attribute is contained in selects for each element! No id attribute select found for aid "
                                            + ODSHelper.asJLong(erse.aid));
    }
    
    /**
     * @param erse
     * @param idNvsui
     * @param idAttrName
     * @return
     * @throws OpenAtfxException 
     */
    private Map<Long, Collection<NameValueUnitId>> createValuesMapForLongIds(ElemResultSetExt erse, NameValueSeqUnitId idNvsui,
            String idAttrName) throws OpenAtfxException {
        Map<Long, Collection<NameValueUnitId>> currentValuesMap = new HashMap<>();
        int[] iids = idNvsui.value.u.longVal();
        for (int j = 0; j < iids.length; j++) {
            int currentIid = iids[j];
            for (NameValueSeqUnitId nvsui : erse.values) {
                DataType currentDt = nvsui.value.u.discriminator();
                DataType singleDt = ODSHelper.getSingleDataType(currentDt);
                if (nvsui.valName.equals(idAttrName)) {
                    TS_Value idVal = ODSHelper.createEmptyTS_Value(singleDt);
                    idVal.flag = 15;
                    idVal.u.longVal(currentIid);
                    currentValuesMap.computeIfAbsent(Long.valueOf(currentIid), v -> new ArrayList<>())
                                    .add(new NameValueUnitId(idAttrName, idVal, nvsui.unitId));
                } else {
                    short flag = nvsui.value.flag[j];
                    NameValueUnitId nvui = null;
                    if (flag == 0) {
                        nvui = new NameValueUnitId(nvsui.valName, ODSHelper.createEmptyTS_Value(singleDt),
                                                   nvsui.unitId);
                    } else {
                        TS_Union u = ODSHelper.tsUnionSeq2tsUnion(nvsui.value.u, j);
                        nvui = new NameValueUnitId(nvsui.valName, new TS_Value(u, flag), nvsui.unitId);
                    }
                    currentValuesMap.computeIfAbsent(Long.valueOf(currentIid), v -> new ArrayList<>())
                                    .add(nvui);
                }
            }
        }
        return currentValuesMap;
    }
    
    /**
     * @param erse
     * @param idNvsui
     * @param idAttrName
     * @return
     * @throws OpenAtfxException 
     */
    private Map<Long, Collection<NameValueUnitId>> createValuesMapForLongLongIds(ElemResultSetExt erse, NameValueSeqUnitId idNvsui,
            String idAttrName) throws OpenAtfxException {
        Map<Long, Collection<NameValueUnitId>> currentValuesMap = new HashMap<>();
        T_LONGLONG[] longIids = idNvsui.value.u.longlongVal();
        for (int j = 0; j < longIids.length; j++) {
            T_LONGLONG currentIid = longIids[j];
            for (NameValueSeqUnitId nvsui : erse.values) {
                DataType currentDt = nvsui.value.u.discriminator();
                if (nvsui.valName.equals(idAttrName)) {
                    DataType singleDt = ODSHelper.getSingleDataType(currentDt);
                    TS_Value idVal = ODSHelper.createEmptyTS_Value(singleDt);
                    idVal.flag = 15;
                    idVal.u.longlongVal(currentIid);
                    currentValuesMap.computeIfAbsent(ODSHelper.asJLong(currentIid), v -> new ArrayList<>())
                                    .add(new NameValueUnitId(idAttrName, idVal, nvsui.unitId));
                } else {
                    short flag = nvsui.value.flag[j];
                    NameValueUnitId nvui = null;
                    if (flag == 0) {
                        nvui = new NameValueUnitId(nvsui.valName, ODSHelper.createEmptyTS_Value(currentDt),
                                                   nvsui.unitId);
                    } else {
                        TS_Union u = ODSHelper.tsUnionSeq2tsUnion(nvsui.value.u, j);
                        nvui = new NameValueUnitId(nvsui.valName, new TS_Value(u, flag), nvsui.unitId);
                    }
                    currentValuesMap.computeIfAbsent(ODSHelper.asJLong(currentIid), v -> new ArrayList<>())
                                    .add(nvui);
                }
            }
        }
        return currentValuesMap;
    }
    
    private class NvsuiHelper {
        private final DataType dataType;
        private List<Short> flags = new ArrayList<>();
        
        private List<Blob> blobValues;
        private List<Boolean> booleanValues;
        private List<Byte> byteValues;
        private List<byte[]> byteStrValues;
        private List<T_COMPLEX> complexValues;
        private List<String> dateValues;
        private List<T_DCOMPLEX> dcomplexValues;
        private List<Double> doubleValues;
        private List<Integer> enumValues;
        private List<T_ExternalReference> extRefValues;
        private List<Float> floatValues;
        private List<Integer> longValues;
        private List<T_LONGLONG> longlongValues;
        private List<Short> shortValues;
        private List<String> stringValues;
        
        private List<boolean[]> booleanSeq;
        private List<byte[]> byteSeq;
        private List<byte[][]> byteStrSeq;
        private List<T_COMPLEX[]> complexSeq;
        private List<String[]> dateSeq;
        private List<T_DCOMPLEX[]> dcomplexSeq;
        private List<double[]> doubleSeq;
        private List<int[]> enumSeq;
        private List<T_ExternalReference[]> extRefSeq;
        private List<float[]> floatSeq;
        private List<int[]> longSeq;
        private List<T_LONGLONG[]> longlongSeq;
        private List<short[]> shortSeq;
        private List<String[]> stringSeq;
        
        NvsuiHelper(DataType dt) {
            this.dataType = dt;
            switch(dt.value()) {
                case DataType._DT_BLOB:
                    blobValues = new ArrayList<>();
                    break;
                case DataType._DT_BOOLEAN:
                    booleanValues = new ArrayList<>();
                    break;
                case DataType._DT_BYTE:
                    byteValues = new ArrayList<>();
                    break;
                case DataType._DT_BYTESTR:
                    byteStrValues = new ArrayList<>();
                    break;
                case DataType._DT_COMPLEX:
                    complexValues = new ArrayList<>();
                    break;
                case DataType._DT_DATE:
                    dateValues = new ArrayList<>();
                    break;
                case DataType._DT_DCOMPLEX:
                    dcomplexValues = new ArrayList<>();
                    break;
                case DataType._DT_DOUBLE:
                    doubleValues = new ArrayList<>();
                    break;
                case DataType._DT_ENUM:
                    enumValues = new ArrayList<>();
                    break;
                case DataType._DT_EXTERNALREFERENCE:
                    extRefValues = new ArrayList<>();
                    break;
                case DataType._DT_FLOAT:
                    floatValues = new ArrayList<>();
                    break;
                case DataType._DT_LONG:
                    longValues = new ArrayList<>();
                    break;
                case DataType._DT_LONGLONG:
                    longlongValues = new ArrayList<>();
                    break;
                case DataType._DT_SHORT:
                    shortValues = new ArrayList<>();
                    break;
                case DataType._DT_STRING:
                    stringValues = new ArrayList<>();
                    break;
                case DataType._DS_BOOLEAN:
                    booleanSeq = new ArrayList<>();
                    break;
                case DataType._DS_BYTE:
                    byteSeq = new ArrayList<>();
                    break;
                case DataType._DS_BYTESTR:
                    byteStrSeq = new ArrayList<>();
                    break;
                case DataType._DS_COMPLEX:
                    complexSeq = new ArrayList<>();
                    break;
                case DataType._DS_DATE:
                    dateSeq = new ArrayList<>();
                    break;
                case DataType._DS_DCOMPLEX:
                    dcomplexSeq = new ArrayList<>();
                    break;
                case DataType._DS_DOUBLE:
                    doubleSeq = new ArrayList<>();
                    break;
                case DataType._DS_ENUM:
                    enumSeq = new ArrayList<>();
                    break;
                case DataType._DS_EXTERNALREFERENCE:
                    extRefSeq = new ArrayList<>();
                    break;
                case DataType._DS_FLOAT:
                    floatSeq = new ArrayList<>();
                    break;
                case DataType._DS_LONG:
                    longSeq = new ArrayList<>();
                    break;
                case DataType._DS_LONGLONG:
                    longlongSeq = new ArrayList<>();
                    break;
                case DataType._DS_SHORT:
                    shortSeq = new ArrayList<>();
                    break;
                case DataType._DS_STRING:
                    stringSeq = new ArrayList<>();
                    break;
                default:
                    break;
            }
        }
        
        void addValue(Object value, short flag) {
            flags.add(flag);
            
            switch(dataType.value()) {
                case DataType._DT_BLOB:
                    blobValues.add((Blob) value);
                    break;
                case DataType._DT_BOOLEAN:
                    booleanValues.add((boolean) value);
                    break;
                case DataType._DT_BYTE:
                    byteValues.add((byte) value);
                    break;
                case DataType._DT_BYTESTR:
                    byteStrValues.add((byte[]) value);
                    break;
                case DataType._DT_COMPLEX:
                    T_COMPLEX complexCopy = new T_COMPLEX(((T_COMPLEX)value).r, ((T_COMPLEX)value).i);
                    complexValues.add(complexCopy);
                    break;
                case DataType._DT_DATE:
                    dateValues.add(value.toString());
                    break;
                case DataType._DT_DCOMPLEX:
                    T_DCOMPLEX dcomplexCopy = new T_DCOMPLEX(((T_DCOMPLEX)value).r, ((T_DCOMPLEX)value).i);
                    dcomplexValues.add(dcomplexCopy);
                    break;
                case DataType._DT_DOUBLE:
                    doubleValues.add((double) value);
                    break;
                case DataType._DT_ENUM:
                    enumValues.add((int) value);
                    break;
                case DataType._DT_EXTERNALREFERENCE:
                    T_ExternalReference extRefCopy = new T_ExternalReference(((T_ExternalReference) value).description,
                                                                            ((T_ExternalReference) value).mimeType,
                                                                            ((T_ExternalReference) value).location);
                    extRefValues.add(extRefCopy);
                    break;
                case DataType._DT_FLOAT:
                    floatValues.add((float) value);
                    break;
                case DataType._DT_LONG:
                    longValues.add((int) value);
                    break;
                case DataType._DT_LONGLONG:
                    longlongValues.add(new T_LONGLONG(((T_LONGLONG) value).high, ((T_LONGLONG) value).low));
                    break;
                case DataType._DT_SHORT:
                    shortValues.add((short) value);
                    break;
                case DataType._DT_STRING:
                    stringValues.add(value.toString());
                    break;
                case DataType._DS_BOOLEAN:
                    booleanSeq.add((boolean[])value);
                    break;
                case DataType._DS_BYTE:
                    byteSeq.add((byte[])value);
                    break;
                case DataType._DS_BYTESTR:
                    byteStrSeq.add((byte[][])value);
                    break;
                case DataType._DS_COMPLEX:
                    complexSeq.add((T_COMPLEX[])value);
                    break;
                case DataType._DS_DATE:
                    dateSeq.add((String[])value);
                    break;
                case DataType._DS_DCOMPLEX:
                    dcomplexSeq.add((T_DCOMPLEX[])value);
                    break;
                case DataType._DS_DOUBLE:
                    doubleSeq.add((double[])value);
                    break;
                case DataType._DS_ENUM:
                    enumSeq.add((int[])value);
                    break;
                case DataType._DS_EXTERNALREFERENCE:
                    extRefSeq.add((T_ExternalReference[])value);
                    break;
                case DataType._DS_FLOAT:
                    floatSeq.add((float[])value);
                    break;
                case DataType._DS_LONG:
                    longSeq.add((int[])value);
                    break;
                case DataType._DS_LONGLONG:
                    longlongSeq.add((T_LONGLONG[])value);
                    break;
                case DataType._DS_SHORT:
                    shortSeq.add((short[])value);
                    break;
                case DataType._DS_STRING:
                    stringSeq.add((String[])value);
                    break;
                default:
                    break;
            }
        }
        
        Object getValueFromNvui(NameValueUnitId nvui) {
            switch(nvui.value.u.discriminator().value()) {
                case DataType._DT_BLOB:
                    return nvui.value.u.blobVal();
                case DataType._DT_BOOLEAN:
                    return nvui.value.u.booleanVal();
                case DataType._DT_BYTE:
                    return nvui.value.u.byteVal();
                case DataType._DT_BYTESTR:
                    return nvui.value.u.bytestrVal();
                case DataType._DT_COMPLEX:
                    return nvui.value.u.complexVal();
                case DataType._DT_DATE:
                    return nvui.value.u.dateVal();
                case DataType._DT_DCOMPLEX:
                    return nvui.value.u.dcomplexVal();
                case DataType._DT_DOUBLE:
                    return nvui.value.u.doubleVal();
                case DataType._DT_ENUM:
                    return nvui.value.u.enumVal();
                case DataType._DT_EXTERNALREFERENCE:
                    return nvui.value.u.extRefVal();
                case DataType._DT_FLOAT:
                    return nvui.value.u.floatVal();
                case DataType._DT_LONG:
                    return nvui.value.u.longVal();
                case DataType._DT_LONGLONG:
                    return nvui.value.u.longlongVal();
                case DataType._DT_SHORT:
                    return nvui.value.u.shortVal();
                case DataType._DT_STRING:
                    return nvui.value.u.stringVal();
                case DataType._DS_BOOLEAN:
                    return nvui.value.u.booleanSeq();
                case DataType._DS_BYTE:
                    return nvui.value.u.byteSeq();
                case DataType._DS_BYTESTR:
                    return nvui.value.u.bytestrSeq();
                case DataType._DS_COMPLEX:
                    return nvui.value.u.complexSeq();
                case DataType._DS_DATE:
                    return nvui.value.u.dateSeq();
                case DataType._DS_DCOMPLEX:
                    return nvui.value.u.dcomplexSeq();
                case DataType._DS_DOUBLE:
                    return nvui.value.u.doubleSeq();
                case DataType._DS_ENUM:
                    return nvui.value.u.enumSeq();
                case DataType._DS_EXTERNALREFERENCE:
                    return nvui.value.u.extRefSeq();
                case DataType._DS_FLOAT:
                    return nvui.value.u.floatSeq();
                case DataType._DS_LONG:
                    return nvui.value.u.longSeq();
                case DataType._DS_LONGLONG:
                    return nvui.value.u.longlongSeq();
                case DataType._DS_SHORT:
                    return nvui.value.u.shortSeq();
                case DataType._DS_STRING:
                    return nvui.value.u.stringSeq();
                default:
                    return DataType.DT_UNKNOWN;
            }
        }
        
        public void fillNvsui(NameValueSeqUnitId nvsui) {
            // set values
            switch(dataType.value()) {
                case DataType._DT_BLOB:
                    nvsui.value.u.blobVal(blobValues.toArray(new Blob[0]));
                    break;
                case DataType._DT_BOOLEAN:
                    nvsui.value.u.booleanVal(ArrayUtils.toPrimitive(booleanValues.toArray(new Boolean[0])));
                    break;
                case DataType._DT_BYTE:
                    nvsui.value.u.byteVal(ArrayUtils.toPrimitive(byteValues.toArray(new Byte[0])));
                    break;
                case DataType._DT_BYTESTR:
                    nvsui.value.u.bytestrVal(byteStrValues.toArray(new byte[0][]));
                    break;
                case DataType._DT_COMPLEX:
                    nvsui.value.u.complexVal(complexValues.toArray(new T_COMPLEX[0]));
                    break;
                case DataType._DT_DATE:
                    nvsui.value.u.dateVal(dateValues.toArray(new String[0]));
                    break;
                case DataType._DT_DCOMPLEX:
                    nvsui.value.u.dcomplexVal(dcomplexValues.toArray(new T_DCOMPLEX[0]));
                    break;
                case DataType._DT_DOUBLE:
                    nvsui.value.u.doubleVal(ArrayUtils.toPrimitive(doubleValues.toArray(new Double[0])));
                    break;
                case DataType._DT_ENUM:
                    nvsui.value.u.enumVal(ArrayUtils.toPrimitive(enumValues.toArray(new Integer[0])));
                    break;
                case DataType._DT_EXTERNALREFERENCE:
                    nvsui.value.u.extRefVal(extRefValues.toArray(new T_ExternalReference[0]));
                    break;
                case DataType._DT_FLOAT:
                    nvsui.value.u.floatVal(ArrayUtils.toPrimitive(floatValues.toArray(new Float[0])));
                    break;
                case DataType._DT_LONG:
                    nvsui.value.u.longVal(ArrayUtils.toPrimitive(longValues.toArray(new Integer[0])));
                    break;
                case DataType._DT_LONGLONG:
                    nvsui.value.u.longlongVal(longlongValues.toArray(new T_LONGLONG[0]));
                    break;
                case DataType._DT_SHORT:
                    nvsui.value.u.shortVal(ArrayUtils.toPrimitive(shortValues.toArray(new Short[0])));
                    break;
                case DataType._DT_STRING:
                    nvsui.value.u.stringVal(stringValues.toArray(new String[0]));
                    break;
                case DataType._DS_BOOLEAN:
                    nvsui.value.u.booleanSeq(booleanSeq.toArray(new boolean[0][]));
                    break;
                case DataType._DS_BYTE:
                    nvsui.value.u.byteSeq(byteSeq.toArray(new byte[0][]));
                    break;
                case DataType._DS_BYTESTR:
                    nvsui.value.u.bytestrSeq(byteStrSeq.toArray(new byte[0][][]));
                    break;
                case DataType._DS_COMPLEX:
                    nvsui.value.u.complexSeq(complexSeq.toArray(new T_COMPLEX[0][]));
                    break;
                case DataType._DS_DATE:
                    nvsui.value.u.dateSeq(dateSeq.toArray(new String[0][]));
                    break;
                case DataType._DS_DCOMPLEX:
                    nvsui.value.u.dcomplexSeq(dcomplexSeq.toArray(new T_DCOMPLEX[0][]));
                    break;
                case DataType._DS_DOUBLE:
                    nvsui.value.u.doubleSeq(doubleSeq.toArray(new double[0][]));
                    break;
                case DataType._DS_ENUM:
                    nvsui.value.u.enumSeq(enumSeq.toArray(new int[0][]));
                    break;
                case DataType._DS_EXTERNALREFERENCE:
                    nvsui.value.u.extRefSeq(extRefSeq.toArray(new T_ExternalReference[0][]));
                    break;
                case DataType._DS_FLOAT:
                    nvsui.value.u.floatSeq(floatSeq.toArray(new float[0][]));
                    break;
                case DataType._DS_LONG:
                    nvsui.value.u.longSeq(longSeq.toArray(new int[0][]));
                    break;
                case DataType._DS_LONGLONG:
                    nvsui.value.u.longlongSeq(longlongSeq.toArray(new T_LONGLONG[0][]));
                    break;
                case DataType._DS_SHORT:
                    nvsui.value.u.shortSeq(shortSeq.toArray(new short[0][]));
                    break;
                case DataType._DS_STRING:
                    nvsui.value.u.stringSeq(stringSeq.toArray(new String[0][]));
                    break;
                default:
                    break;
            }
            
            // set flags
            nvsui.value.flag = ArrayUtils.toPrimitive(flags.toArray(new Short[0]));
        }
    }
}
