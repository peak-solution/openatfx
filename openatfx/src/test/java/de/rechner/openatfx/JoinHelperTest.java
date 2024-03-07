package de.rechner.openatfx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.RelationRange;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.rechner.openatfx.util.ODSHelper;


public class JoinHelperTest {

    private AtfxCache atfxCache;
    private T_LONGLONG aid1 = new T_LONGLONG(0, 42);
    private long aid1L = ODSHelper.asJLong(aid1);
    private T_LONGLONG aid2 = new T_LONGLONG(42, 815);
    private long aid2L = ODSHelper.asJLong(aid2);
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
        atfxCache = new AtfxCache(null);

        ApplicationElement applElem1 = Mockito.mock(ApplicationElement.class);
        Mockito.when(applElem1.getId()).thenReturn(aid1);
        ApplicationAttribute idAttr1 = Mockito.mock(ApplicationAttribute.class);
        Mockito.when(idAttr1.getName()).thenReturn(idAttrName);
        Mockito.when(applElem1.getAttributeByBaseName("id")).thenReturn(idAttr1);
        Mockito.when(applElem1.getAttributeByName(idAttrName)).thenReturn(idAttr1);
        atfxCache.addApplicationElement(aid1L, "AoAny", applElem1);
        atfxCache.addApplicationAttribute(aid1L, 1, idAttr1);
        for (Long iid : iids1) {
            atfxCache.addInstance(aid1L, iid);
        }

        ApplicationElement applElem2 = Mockito.mock(ApplicationElement.class);
        Mockito.when(applElem2.getId()).thenReturn(aid2);
        ApplicationAttribute idAttr2 = Mockito.mock(ApplicationAttribute.class);
        Mockito.when(idAttr2.getName()).thenReturn(idAttrName);
        Mockito.when(applElem2.getAttributeByBaseName("id")).thenReturn(idAttr2);
        Mockito.when(applElem2.getAttributeByName(idAttrName)).thenReturn(idAttr2);
        ApplicationAttribute nameAttr2 = Mockito.mock(ApplicationAttribute.class);
        Mockito.when(nameAttr2.getName()).thenReturn(nameAttrName);
        Mockito.when(applElem2.getAttributeByName(nameAttrName)).thenReturn(nameAttr2);
        atfxCache.addApplicationElement(aid2L, "AoAny", applElem2);
        atfxCache.addApplicationAttribute(aid2L, 1, idAttr2);
        atfxCache.addApplicationAttribute(aid2L, 2, nameAttr2);
        for (int i = 0; i < iids2.length; i++) {
            long iid = iids2[i];
            atfxCache.addInstance(aid2L, iid);

            TS_Value nameValue = ODSHelper.createStringNV(nameAttrName, names2[i]).value;
            atfxCache.setInstanceValue(aid2L, iid, atfxCache.getAttrNoByName(aid2L, nameAttrName), nameValue);
        }

        ApplicationRelation joinRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(joinRel.getRelationName()).thenReturn(joinRelName);
        Mockito.when(joinRel.getRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        Mockito.when(joinRel.getElem1()).thenReturn(applElem1);
        Mockito.when(joinRel.getElem2()).thenReturn(applElem2);
        ApplicationRelation invJoinRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(invJoinRel.getRelationName()).thenReturn(invJoinRelName);
        Mockito.when(invJoinRel.getRelationRange()).thenReturn(new RelationRange((short) 0, (short) 1));
        Mockito.when(invJoinRel.getElem1()).thenReturn(applElem2);
        Mockito.when(invJoinRel.getElem2()).thenReturn(applElem1);
        Mockito.when(joinRel.getInverseRelationRange()).thenReturn(new RelationRange((short) 0, (short) 1));
        Mockito.when(invJoinRel.getInverseRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        atfxCache.addApplicationRelation(joinRel, invJoinRel);

        ApplicationRelation m2nJoinRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(m2nJoinRel.getRelationName()).thenReturn(m2nJoinRelName);
        Mockito.when(m2nJoinRel.getRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        Mockito.when(m2nJoinRel.getElem1()).thenReturn(applElem1);
        Mockito.when(m2nJoinRel.getElem2()).thenReturn(applElem2);
        ApplicationRelation invM2NJoinRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(invM2NJoinRel.getRelationName()).thenReturn(invM2NJoinRelName);
        Mockito.when(invM2NJoinRel.getRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        Mockito.when(invM2NJoinRel.getElem1()).thenReturn(applElem2);
        Mockito.when(invM2NJoinRel.getElem2()).thenReturn(applElem1);
        Mockito.when(m2nJoinRel.getInverseRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        Mockito.when(invM2NJoinRel.getInverseRelationRange()).thenReturn(new RelationRange((short) 0, (short) -1));
        atfxCache.addApplicationRelation(m2nJoinRel, invM2NJoinRel);

        Mockito.when(applElem1.getAllRelations()).thenReturn(new ApplicationRelation[] { joinRel, m2nJoinRel });
        Mockito.when(applElem2.getAllRelations()).thenReturn(new ApplicationRelation[] { invJoinRel, invM2NJoinRel });

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
        atfxCache.createInstanceRelations(aid1L, iids1[0], joinRel, Arrays.asList(iids2[1]));
        atfxCache.createInstanceRelations(aid1L, iids1[2], joinRel, Arrays.asList(iids2[0], iids2[2], iids2[4]));
        expectedNrOfJoinedInstanceEntries = 4;

        // overview related instances for m2n join relation
        // 1/1 <-> 2/2
        // 1/1 <-> 2/3
        // 1/2 <-> 2/1
        // 1/3 <-> 2/1
        // 1/3 <-> 2/2
        // 1/3 <-> 2/5
        // 1/4 <-> none
        atfxCache.createInstanceRelations(aid1L, iids1[0], m2nJoinRel, Arrays.asList(iids2[1], iids2[2]));
        atfxCache.createInstanceRelations(aid1L, iids1[1], m2nJoinRel, Arrays.asList(iids2[0]));
        atfxCache.createInstanceRelations(aid1L, iids1[2], m2nJoinRel, Arrays.asList(iids2[0], iids2[1], iids2[4]));
        expectedNrOfM2NJoinedInstanceEntries = 6;
    }

    @Test
    public void testJoin_simpleJoin() throws Exception {
        JoinDef join = new JoinDef(aid1, aid2, joinRelName, null);
        JoinHelper helper = new JoinHelper(atfxCache, join);
        ElemResultSetExt[] erses = prepareResult();
        Map<Long, NameValueSeqUnitId[]> valuesForAid = new HashMap<>();
        for (ElemResultSetExt erse : erses) {
            valuesForAid.put(ODSHelper.asJLong(erse.aid), erse.values);
        }

        ElemResultSetExt[] result = helper.join(erses);

        assertThat(result).isEqualTo(erses);
    }

    @Test
    public void testJoin_m2nJoin() throws Exception {
        JoinDef join = new JoinDef(aid1, aid2, m2nJoinRelName, null);
        JoinHelper helper = new JoinHelper(atfxCache, join);
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
    public void testJoin_m2nJoin_sequenceAttrWithPartiallyInvalidValues() throws Exception {
        JoinDef join = new JoinDef(aid1, aid2, m2nJoinRelName, null);
        JoinHelper helper = new JoinHelper(atfxCache, join);

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

                for (NameValueSeqUnitId nvsui : erse.values) {
                    if (idAttrName.equals(nvsui.valName)) {
                        T_LONGLONG[] longlongVal = nvsui.value.u.longlongVal();
                        long[] vals = ODSHelper.asJLong(longlongVal);
                        assertThat(vals).hasSize(expectedNrOfM2NJoinedInstanceEntries)
                                        .containsExactlyInAnyOrder(new long[] { iids2[1], iids2[2], iids2[0], iids2[0],
                                                iids2[1], iids2[4] });
                    } else if ("TestAttr".equals(nvsui.valName)) {
                        T_LONGLONG[][] longlongSeq = nvsui.value.u.longlongSeq();
                        assertThat(longlongSeq).hasNumberOfRows(expectedNrOfM2NJoinedInstanceEntries);
                        assertThat(longlongSeq).contains(testValues[1], Index.atIndex(0));
                        assertThat(longlongSeq).contains(testValues[2], Index.atIndex(1));
                        assertThat(longlongSeq).contains(testValues[0], Index.atIndex(2));
                        assertThat(longlongSeq).contains(testValues[1], Index.atIndex(3));
                        assertThat(longlongSeq).contains(testValues[0], Index.atIndex(4));
                        assertThat(longlongSeq).contains(testValues[4], Index.atIndex(5));
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
        ElemResultSetExt erse1 = new ElemResultSetExt(aid1, nvsuis1);

        // element 2 nvsuis
        short[] idFlags2 = new short[] { (short) 15, 15, 15, 15, 15 };
        short[] testFlags2 = new short[] { (short) 15, 15, 0, 0, 15 };

        NameValueSeqUnitId idNvsui2 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids2, idFlags2, null);
        NameValueSeqUnitId testNvsui2 = createNvsui("TestAttr", DataType.DS_LONGLONG, testValues2, testFlags2, null);
        NameValueSeqUnitId[] nvsuis2 = new NameValueSeqUnitId[] { idNvsui2, testNvsui2 };
        ElemResultSetExt erse2 = new ElemResultSetExt(aid2, nvsuis2);

        return new ElemResultSetExt[] { erse1, erse2 };
    }

    private ElemResultSetExt[] prepareResult() {
        // element 1 nvsuis
        short[] idFlags1 = new short[iids1.length];
        Arrays.fill(idFlags1, (short) 15);
        NameValueSeqUnitId idNvsui1 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids1, idFlags1, null);
        NameValueSeqUnitId[] nvsuis1 = new NameValueSeqUnitId[] { idNvsui1 };
        ElemResultSetExt erse1 = new ElemResultSetExt(aid1, nvsuis1);

        // element 2 nvsuis
        short[] idFlags2 = new short[iids2.length];
        Arrays.fill(idFlags2, (short) 15);
        NameValueSeqUnitId idNvsui2 = createNvsui(idAttrName, DataType.DT_LONGLONG, iids2, idFlags2, null);
        NameValueSeqUnitId nameNvsui2 = createNvsui(nameAttrName, DataType.DT_STRING, names2, idFlags2, null);
        NameValueSeqUnitId[] nvsuis2 = new NameValueSeqUnitId[] { idNvsui2, nameNvsui2 };
        ElemResultSetExt erse2 = new ElemResultSetExt(aid2, nvsuis2);

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
