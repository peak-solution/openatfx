package com.peaksolution.openatfx.api.corba;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.SetType;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.UnitTestFileHandler;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.NameValueUnit;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.util.ODSHelper;

@ExtendWith(GlassfishCorbaExtension.class)
class JoinHelperTest {

    private OpenAtfxAPIImplementation api;
    private long aid1L;
    private long aid2L;
    private long[] iids1 = new long[] { 4, 7, 29, 998743 };
    private long[] iids2 = new long[] { 457, 79, 1234, 4567, 988 };
    private String[] names2 = new String[] { "one", "two", "three", "four", "five" };
    private String joinRelName = "joinRelation";
    private String invJoinRelName = "invJoinRelation";
    private String m2nJoinRelName = "m2nJoinRelation";
    private String invM2NJoinRelName = "m2nInvJoinRelation";
    private String idAttrName = "Id";
    private String nameAttrName = "Name";
    private int expectedNrOfJoinedInstanceEntries = 0;
    private int expectedNrOfM2NJoinedInstanceEntries = 0;

    @BeforeEach
    public void setUp() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationElementImpl.class.getResource("/com/peaksolution/openatfx/example.atfx");
        CorbaAtfxReader corbaReader = AoServiceFactory.getInstance().newCorbaReader(orb, new UnitTestFileHandler(), new File(url.getFile()).toPath());
        api = corbaReader.getApi();
        
        String elem1Name = "TestElement1";
        Element element1 = api.createElement("AoAny", elem1Name);
        aid1L = element1.getId();
        api.renameAttribute(aid1L, "id", idAttrName);
        api.renameAttribute(aid1L, "name", nameAttrName);
        for (Long iid : iids1) {
            NameValueUnit idNvu = new NameValueUnit(idAttrName, com.peaksolution.openatfx.api.DataType.DT_LONGLONG, iid);
            NameValueUnit nameNvu = new NameValueUnit(nameAttrName, com.peaksolution.openatfx.api.DataType.DT_STRING, "Name_" + iid);
            api.createInstance(aid1L, Arrays.asList(idNvu, nameNvu));
        }

        String elem2Name = "TestElement2";
        Element element2 = api.createElement("AoAny", elem2Name);
        aid2L = element2.getId();
        api.renameAttribute(aid2L, "id", idAttrName);
        api.renameAttribute(aid2L, "name", nameAttrName);
        int counter = 0;
        for (Long iid : iids2) {
            NameValueUnit idNvu = new NameValueUnit(idAttrName, com.peaksolution.openatfx.api.DataType.DT_LONGLONG, iid);
            NameValueUnit nameNvu = new NameValueUnit(nameAttrName, com.peaksolution.openatfx.api.DataType.DT_STRING, names2[counter]);
            api.createInstance(aid2L, Arrays.asList(idNvu, nameNvu));
            counter++;
        }
        
        Relation joinRel = api.createRelation(element1, element2, null, joinRelName, invJoinRelName, (short) 0, (short) -1);
        api.createRelation(element2, element1, null, invJoinRelName, joinRelName, (short) 0, (short) 1);
        
        Relation m2nJoinRel = api.createRelation(element1, element2, null, m2nJoinRelName, invM2NJoinRelName, (short) 0, (short) -1);
        api.createRelation(element2, element1, null, invM2NJoinRelName, m2nJoinRelName, (short) 0, (short) -1);

        // no. of instances: n(aid1) = 4; n(aid2) = 5
        // iids1 = {4, 7, 29, 998743}
        // iids2 = {457, 79, 1234, 4567, 988}

        // overview related instance for join relation
        // 1/1 -> 2/2
        // 1/2 -> none
        // 1/3 -> 2/1
        // -> 2/3
        // -> 2/5
        // 1/4 -> none
        api.setRelatedInstances(aid1L, iids1[0], joinRel.getRelationName(), Arrays.asList(iids2[1]), SetType.UPDATE);
        api.setRelatedInstances(aid1L, iids1[2], joinRel.getRelationName(), Arrays.asList(iids2[0], iids2[2], iids2[4]), SetType.INSERT);
        expectedNrOfJoinedInstanceEntries = 4;

        // overview related instances for m2n join relation
        // 1/1 <-> 2/2
        // 1/1 <-> 2/3
        // 1/2 <-> 2/1
        // 1/3 <-> 2/1
        // 1/3 <-> 2/2
        // 1/3 <-> 2/5
        // 1/4 <-> none
        api.setRelatedInstances(aid1L, iids1[0], m2nJoinRel.getRelationName(), Arrays.asList(iids2[1], iids2[2]), SetType.APPEND);
        api.setRelatedInstances(aid1L, iids1[1], m2nJoinRel.getRelationName(), Arrays.asList(iids2[0]), SetType.APPEND);
        api.setRelatedInstances(aid1L, iids1[2], m2nJoinRel.getRelationName(), Arrays.asList(iids2[0], iids2[1], iids2[4]), SetType.APPEND);
        expectedNrOfM2NJoinedInstanceEntries = 6;
    }

    @Test
    void testJoin_simpleJoin() throws Exception {
        JoinDef join = new JoinDef(ODSHelper.asODSLongLong(aid1L), ODSHelper.asODSLongLong(aid2L), joinRelName, null);
        JoinHelper helper = new JoinHelper(api, join);
        ElemResultSetExt[] erses = prepareResult();
        Map<Long, NameValueSeqUnitId[]> valuesForAid = new HashMap<>();
        for (ElemResultSetExt erse : erses) {
            valuesForAid.put(ODSHelper.asJLong(erse.aid), erse.values);
        }

        ElemResultSetExt[] result = helper.join(erses);

        assertThat(result).isEqualTo(erses);
    }

    @Test
    void testJoin_m2nJoin() throws Exception {
        JoinDef join = new JoinDef(ODSHelper.asODSLongLong(aid1L), ODSHelper.asODSLongLong(aid2L), m2nJoinRelName, null);
        JoinHelper helper = new JoinHelper(api, join);
        ElemResultSetExt[] erses = prepareResult();
        Map<Long, NameValueSeqUnitId[]> valuesForAid = new HashMap<>();
        for (ElemResultSetExt erse : erses) {
            valuesForAid.put(ODSHelper.asJLong(erse.aid), erse.values);
        }

        ElemResultSetExt[] result = helper.join(erses);

        assertThat(result).hasSize(2);
        for (ElemResultSetExt erse : result) {
            long currentAid = ODSHelper.asJLong(erse.aid);

            if (aid1L == currentAid) {
                int expectedSize = valuesForAid.get(aid1L).length;
                assertThat(erse.values).hasSize(expectedSize);

                for (NameValueSeqUnitId nvsui : erse.values) {
                    if (idAttrName.equals(nvsui.valName)) {
                        T_LONGLONG[] longlongVal = nvsui.value.u.longlongVal();
                        long[] vals = ODSHelper.asJLong(longlongVal);
                        assertThat(vals).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                        .containsExactlyInAnyOrder(new long[] { iids1[0], iids1[0], iids1[1], iids1[2],
                                                iids1[2], iids1[2] });
                    }
                }
            } else if (aid2L == currentAid) {
                int expectedSize = valuesForAid.get(aid2L).length;
                assertThat(erse.values).hasSize(expectedSize);

                for (NameValueSeqUnitId nvsui : erse.values) {
                    if (idAttrName.equals(nvsui.valName)) {
                        T_LONGLONG[] longlongVal = nvsui.value.u.longlongVal();
                        long[] vals = ODSHelper.asJLong(longlongVal);
                        assertThat(vals).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                        .containsExactlyInAnyOrder(new long[] { iids2[1], iids2[2], iids2[0], iids2[0],
                                                iids2[1], iids2[4] });
                    } else if (nameAttrName.equals(nvsui.valName)) {
                        String[] stringVal = nvsui.value.u.stringVal();
                        assertThat(stringVal).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                             .containsExactlyInAnyOrder(new String[] { names2[1], names2[2], names2[0],
                                                     names2[0], names2[1], names2[4] });
                    }
                }
            }
        }
    }

    @Test
    void testJoin_m2nJoin_sequenceAttrWithPartiallyInvalidValues() throws Exception {
        JoinDef join = new JoinDef(ODSHelper.asODSLongLong(aid1L), ODSHelper.asODSLongLong(aid2L), m2nJoinRelName, null);
        JoinHelper helper = new JoinHelper(api, join);

        T_LONGLONG[][] testValues = new T_LONGLONG[][] { new T_LONGLONG[] { ODSHelper.asODSLongLong(1L) },
                new T_LONGLONG[] { ODSHelper.asODSLongLong(2L) }, new T_LONGLONG[0], new T_LONGLONG[0],
                new T_LONGLONG[] { ODSHelper.asODSLongLong(3L) } };

        ElemResultSetExt[] erses = prepareResult_withSequenceValues_partiallyInvalid(testValues);
        Map<Long, NameValueSeqUnitId[]> valuesForAid = new HashMap<>();
        for (ElemResultSetExt erse : erses) {
            valuesForAid.put(ODSHelper.asJLong(erse.aid), erse.values);
        }

        ElemResultSetExt[] result = helper.join(erses);

        assertThat(result).hasSize(2);
        for (ElemResultSetExt erse : result) {
            long currentAid = ODSHelper.asJLong(erse.aid);

            if (aid1L == currentAid) {
                int expectedSize = valuesForAid.get(aid1L).length;
                assertThat(erse.values).hasSize(expectedSize);

                for (NameValueSeqUnitId nvsui : erse.values) {
                    if (idAttrName.equals(nvsui.valName)) {
                        T_LONGLONG[] longlongVal = nvsui.value.u.longlongVal();
                        long[] vals = ODSHelper.asJLong(longlongVal);
                        assertThat(vals).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                        .containsExactlyInAnyOrder(new long[] { iids1[0], iids1[0], iids1[1], iids1[2],
                                                iids1[2], iids1[2] });
                    }
                }
            } else if (aid2L == currentAid) {
                int expectedSize = valuesForAid.get(aid2L).length;
                assertThat(erse.values).hasSize(expectedSize);

                long[] iids = null;
                for (NameValueSeqUnitId nvsui : erse.values) {
                    if (idAttrName.equals(nvsui.valName)) {
                        T_LONGLONG[] longlongVal = nvsui.value.u.longlongVal();
                        iids = ODSHelper.asJLong(longlongVal);
                        assertThat(iids).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                        .containsExactlyInAnyOrder(new long[] { iids2[1], iids2[2], iids2[0], iids2[0],
                                                iids2[1], iids2[4] });
                    }
                }
                for (NameValueSeqUnitId nvsui : erse.values) {
                    
                    if ("TestAttr".equals(nvsui.valName)) {
                        int instanceCounter = 0;
                        T_LONGLONG[][] longlongSeq = nvsui.value.u.longlongSeq();
                        assertThat(longlongSeq).hasNumberOfRows(expectedNrOfM2NJoinedInstanceEntries);
                        for (T_LONGLONG[] instanceValues : longlongSeq) {
                            long currentIid = iids[instanceCounter];
                            if (currentIid == iids2[0]) {
                                assertThat(instanceValues).isEqualTo(testValues[0]);
                            } else if (currentIid == iids2[1]) {
                                assertThat(instanceValues).isEqualTo(testValues[1]);
                            } else if (currentIid == iids2[2]) {
                                assertThat(instanceValues).isEqualTo(testValues[2]);
                            } else if (currentIid == iids2[3]) {
                                assertThat(instanceValues).isEqualTo(testValues[3]);
                            } else if (currentIid == iids2[4]) {
                                assertThat(instanceValues).isEqualTo(testValues[4]);
                            }
                            instanceCounter++;
                        }
                    }
                }
            }
        }
    }

    private ElemResultSetExt[] prepareResult_withSequenceValues_partiallyInvalid(T_LONGLONG[][] testValues2) {
        // element 1 nvsuis
        short[] idFlags1 = new short[iids1.length];
        Arrays.fill(idFlags1, (short) 15);
        NameValueSeqUnitId idNvsui1 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids1, idFlags1, null);
        NameValueSeqUnitId[] nvsuis1 = new NameValueSeqUnitId[] { idNvsui1 };
        ElemResultSetExt erse1 = new ElemResultSetExt(ODSHelper.asODSLongLong(aid1L), nvsuis1);

        // element 2 nvsuis
        short[] idFlags2 = new short[] { (short) 15, 15, 15, 15, 15 };
        short[] testFlags2 = new short[] { (short) 15, 15, 0, 0, 15 };

        NameValueSeqUnitId idNvsui2 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids2, idFlags2, null);
        NameValueSeqUnitId testNvsui2 = createNvsui("TestAttr", DataType.DS_LONGLONG, testValues2, testFlags2, null);
        NameValueSeqUnitId[] nvsuis2 = new NameValueSeqUnitId[] { idNvsui2, testNvsui2 };
        ElemResultSetExt erse2 = new ElemResultSetExt(ODSHelper.asODSLongLong(aid2L), nvsuis2);

        return new ElemResultSetExt[] { erse1, erse2 };
    }

    private ElemResultSetExt[] prepareResult() {
        // element 1 nvsuis
        short[] idFlags1 = new short[iids1.length];
        Arrays.fill(idFlags1, (short) 15);
        NameValueSeqUnitId idNvsui1 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids1, idFlags1, null);
        NameValueSeqUnitId[] nvsuis1 = new NameValueSeqUnitId[] { idNvsui1 };
        ElemResultSetExt erse1 = new ElemResultSetExt(ODSHelper.asODSLongLong(aid1L), nvsuis1);

        // element 2 nvsuis
        short[] idFlags2 = new short[iids2.length];
        Arrays.fill(idFlags2, (short) 15);
        NameValueSeqUnitId idNvsui2 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids2, idFlags2, null);
        NameValueSeqUnitId nameNvsui2 = createNvsui(nameAttrName, DataType.DT_STRING, names2, idFlags2, null);
        NameValueSeqUnitId[] nvsuis2 = new NameValueSeqUnitId[] { idNvsui2, nameNvsui2 };
        ElemResultSetExt erse2 = new ElemResultSetExt(ODSHelper.asODSLongLong(aid2L), nvsuis2);

        return new ElemResultSetExt[] { erse1, erse2 };
    }

    private NameValueSeqUnitId createNvsui(String valName, DataType dt, Object values, short[] flags, T_LONGLONG unit) {
        TS_UnionSeq u = new TS_UnionSeq();
        switch (dt.value()) {
            case DataType._DT_BLOB:
                u.blobVal((Blob[]) values);
            break;
            case DataType._DT_BOOLEAN:
                u.booleanVal((boolean[]) values);
            break;
            case DataType._DT_BYTE:
                u.byteVal((byte[]) values);
            break;
            case DataType._DT_BYTESTR:
                u.bytestrVal((byte[][]) values);
            break;
            case DataType._DT_COMPLEX:
                u.complexVal((T_COMPLEX[]) values);
            break;
            case DataType._DT_DATE:
                u.dateVal((String[]) values);
            break;
            case DataType._DT_DCOMPLEX:
                u.dcomplexVal((T_DCOMPLEX[]) values);
            break;
            case DataType._DT_DOUBLE:
                u.doubleVal((double[]) values);
            break;
            case DataType._DT_ENUM:
                u.enumVal((int[]) values);
            break;
            case DataType._DT_EXTERNALREFERENCE:
                u.extRefVal((T_ExternalReference[]) values);
            break;
            case DataType._DT_FLOAT:
                u.floatVal((float[]) values);
            break;
            case DataType._DT_LONG:
                u.longVal((int[]) values);
            break;
            case DataType._DT_LONGLONG:
                List<T_LONGLONG> longlongValues = Arrays.stream((long[]) values).boxed().map(ODSHelper::asODSLongLong)
                                                        .collect(Collectors.toList());
                u.longlongVal(longlongValues.toArray(new T_LONGLONG[0]));
            break;
            case DataType._DT_SHORT:
                u.shortVal((short[]) values);
            break;
            case DataType._DT_STRING:
                u.stringVal((String[]) values);
            break;
            case DataType._DS_BOOLEAN:
                u.booleanSeq((boolean[][]) values);
            break;
            case DataType._DS_BYTE:
                u.byteSeq((byte[][]) values);
            break;
            case DataType._DS_BYTESTR:
                u.bytestrSeq((byte[][][]) values);
            break;
            case DataType._DS_COMPLEX:
                u.complexSeq((T_COMPLEX[][]) values);
            break;
            case DataType._DS_DATE:
                u.dateSeq((String[][]) values);
            break;
            case DataType._DS_DCOMPLEX:
                u.dcomplexSeq((T_DCOMPLEX[][]) values);
            break;
            case DataType._DS_DOUBLE:
                u.doubleSeq((double[][]) values);
            break;
            case DataType._DS_ENUM:
                u.enumSeq((int[][]) values);
            break;
            case DataType._DS_EXTERNALREFERENCE:
                u.extRefSeq((T_ExternalReference[][]) values);
            break;
            case DataType._DS_FLOAT:
                u.floatSeq((float[][]) values);
            break;
            case DataType._DS_LONG:
                u.longSeq((int[][]) values);
            break;
            case DataType._DS_LONGLONG:
                u.longlongSeq((T_LONGLONG[][]) values);
            break;
            case DataType._DS_SHORT:
                u.shortSeq((short[][]) values);
            break;
            case DataType._DS_STRING:
                u.stringSeq((String[][]) values);
            break;
            default:
            break;
        }

        TS_ValueSeq seq = new TS_ValueSeq(u, flags);
        return new NameValueSeqUnitId(valName, seq, unit == null ? new T_LONGLONG(0, 0) : unit);
    }
}
