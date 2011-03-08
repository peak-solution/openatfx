package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.AttrResultSet;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelOpcode;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Helper class to provide query functionality in memory.
 * <ul>
 * <li>Step 1: perform joins, collect instance ids</li>
 * <li>Step 2: apply filters to each row</li>
 * <li>Step x: collect result data and convert to column data</li>
 * </ul>
 * 
 * @author Christian Rechner
 */
class QueryBuilder {

    private final AtfxCache atfxCache;

    private final Set<Long> columns;
    private List<SortedMap<Long, Long>> rows;

    /**
     * Constructor.
     * 
     * @param atfxCache The cache.
     * @param startAid The application element id to start with building the query.
     */
    public QueryBuilder(AtfxCache atfxCache, long startAid) {
        this.atfxCache = atfxCache;
        this.rows = new ArrayList<SortedMap<Long, Long>>();
        this.columns = new TreeSet<Long>();

        columns.add(startAid);
        Set<Long> instIids = atfxCache.getInstanceIds(startAid);
        for (Long iid : new TreeSet<Long>(instIids)) {
            SortedMap<Long, Long> m = new TreeMap<Long, Long>();
            m.put(startAid, iid);
            rows.add(m);
        }
    }

    /**
     * Constructor.
     * 
     * @param atfxCache The cache.
     */
    public QueryBuilder(AtfxCache atfxCache) {
        this.atfxCache = atfxCache;
        this.rows = new ArrayList<SortedMap<Long, Long>>();
        this.columns = new TreeSet<Long>();
    }

    /******************************************************************
     * methods for performing the joins
     * 
     * @throws AoException
     ******************************************************************/

    public void addJoinDefs(JoinDef[] joinDefs) throws AoException {
        // TODO: sort joins
        for (JoinDef joinDef : joinDefs) {
            applyJoinDef(joinDef);
        }
    }

    /**
     * The LEFT join ae has to be in the current join list!
     * 
     * @param joinDef
     * @throws AoException
     */
    private void applyJoinDef(JoinDef joinDef) throws AoException {
        List<SortedMap<Long, Long>> newRows = new ArrayList<SortedMap<Long, Long>>();
        long fromAid = ODSHelper.asJLong(joinDef.fromAID);
        long toAid = ODSHelper.asJLong(joinDef.toAID);
        // check if fromAID is already in matrix
        if (!columns.contains(fromAid)) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "application element not exists in query builder matrix fromAID=" + fromAid);
        }
        columns.add(toAid);

        // fetch related instance ids
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(fromAid, joinDef.refName);
        if (applRel == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationRelation 'elem1="
                    + fromAid + ",elem2=" + toAid + ",relName=" + joinDef.refName + "' not found");
        }

        for (SortedMap<Long, Long> row : rows) {
            long fromIid = row.get(fromAid);
            // TODO: handle outer JOIN
            for (long relIid : atfxCache.getRelatedInstanceIds(fromAid, fromIid, applRel)) {
                SortedMap<Long, Long> newRow = new TreeMap<Long, Long>(row);
                newRow.put(toAid, relIid);
                newRows.add(newRow);
            }
        }

        this.rows = newRows;
    }

    /******************************************************************
     * methods for building the result set.
     ******************************************************************/

    /**
     * Returns the query result for an ODS 'query'.
     * 
     * @param anuSeq The attributes to select.
     * @return
     * @throws AoException
     */
    public ElemResultSet[] getElemResultSet(AIDNameUnitId[] anuSeq) throws AoException {
        if (columns.size() == 1) {
            long aid = columns.iterator().next();
            ElemResultSet resSet = new ElemResultSet();
            resSet.aid = ODSHelper.asODSLongLong(aid);
            resSet.attrValues = new AttrResultSet[anuSeq.length];

            // attributes
            for (int i = 0; i < anuSeq.length; i++) {
                String aaName = anuSeq[i].attr.aaName;
                resSet.attrValues[i] = new AttrResultSet();
                resSet.attrValues[i].attrValues = new NameValueSeqUnitId();
                resSet.attrValues[i].attrValues.unitId = ODSHelper.asODSLongLong(0);
                resSet.attrValues[i].attrValues.valName = aaName;
                resSet.attrValues[i].attrValues.value = getTsValueSeq(aid, aaName);
            }

            return new ElemResultSet[] { resSet };
        }
        throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                              "None or multiple result ApplElems not allowed");
    }

    /**
     * Returns the query result for an ODS 'extended query'.
     * 
     * @return
     * @throws AoException
     */
    public ResultSetExt[] getResultSetExt(SelAIDNameUnitId[] anuSeq) throws AoException {
        Map<Long, List<SelAIDNameUnitId>> map = groupSelAIDNameUnitIdByAid(anuSeq);

        // one ElemResultSetExt per requested aid
        ResultSetExt resSet = new ResultSetExt();
        resSet.firstElems = new ElemResultSetExt[map.size()];
        Long[] aids = map.keySet().toArray(new Long[0]);
        for (int i = 0; i < aids.length; i++) {
            resSet.firstElems[i] = new ElemResultSetExt();
            resSet.firstElems[i].aid = ODSHelper.asODSLongLong(aids[i]);
            // attributes
            SelAIDNameUnitId[] attrs = map.get(aids[i]).toArray(new SelAIDNameUnitId[0]);
            resSet.firstElems[i].values = new NameValueSeqUnitId[attrs.length];
            for (int x = 0; x < attrs.length; x++) {
                resSet.firstElems[i].values[x] = new NameValueSeqUnitId();
                resSet.firstElems[i].values[x].valName = attrs[x].attr.aaName;
                resSet.firstElems[i].values[x].unitId = attrs[x].unitId;
                resSet.firstElems[i].values[x].value = getTsValueSeq(aids[i], attrs[x].attr.aaName);
                // check for non supported aggregate functions
                if (attrs[i].aggregate != AggrFunc.NONE) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "Aggregate functions are not yet supported");
                }
            }
        }

        return new ResultSetExt[] { resSet };
    }

    /**
     * Groups given array of select attributes by their application element id.
     * 
     * @param anuSeq Array of attributes.
     * @return Map with the aid as key.
     */
    private Map<Long, List<SelAIDNameUnitId>> groupSelAIDNameUnitIdByAid(SelAIDNameUnitId[] anuSeq) {
        Map<Long, List<SelAIDNameUnitId>> map = new LinkedHashMap<Long, List<SelAIDNameUnitId>>();
        for (SelAIDNameUnitId anu : anuSeq) {
            long aid = ODSHelper.asJLong(anu.attr.aid);
            List<SelAIDNameUnitId> l = map.get(aid);
            if (l == null) {
                l = new ArrayList<SelAIDNameUnitId>();
                map.put(aid, l);
            }
            l.add(anu);
        }
        return map;
    }

    /**
     * Returns the value for a 'cell'; for a row number, an application element and the column name.
     * 
     * @param rowNo The row number.
     * @param aid The application element id.
     * @param colName The column name, may be an application attribute name or a relation id column.
     * @return The value.
     * @throws AoException Error getting value.
     */
    private TS_Value getValue(int rowNo, long aid, String colName) throws AoException {
        long iid = this.rows.get(rowNo).get(aid);

        // application attribute column
        ApplicationAttribute applAttr = this.atfxCache.getApplicationAttributeByName(aid, colName);
        if (applAttr != null) {
            TS_Value value = this.atfxCache.getInstanceValue(aid, iid, colName);
            if (value == null) {
                value = ODSHelper.createEmptyTS_Value(applAttr.getDataType());
                this.atfxCache.setInstanceValue(aid, iid, colName, value);
            }
            return value;
        }

        // relation column
        ApplicationRelation applRel = this.atfxCache.getApplicationRelationByName(aid, colName);
        if (applRel != null) {
            Set<Long> relsIids = this.atfxCache.getRelatedInstanceIds(aid, iid, applRel);
            if (relsIids.size() < 1) {
                return ODSHelper.createEmptyTS_Value(DataType.DT_LONGLONG);
            }
            return ODSHelper.createLongLongNV("", relsIids.iterator().next()).value;
        }

        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Column '" + colName + "' not existing");
    }

    /**
     * Converts a list reates a <code>org.asam.ods.TS_ValueSeq</code> column data object
     * 
     * @param aid
     * @param colName
     * @return
     * @throws AoException
     */
    private TS_ValueSeq getTsValueSeq(long aid, String colName) throws AoException {
        // determine datatype
        DataType dt = DataType.DT_LONGLONG;
        ApplicationAttribute applAttr = this.atfxCache.getApplicationAttributeByName(aid, colName);
        if (applAttr != null) {
            dt = applAttr.getDataType();
        }

        TS_ValueSeq valueSeq = new TS_ValueSeq();
        valueSeq.flag = new short[rows.size()];
        valueSeq.u = new TS_UnionSeq();
        // collect values from cache
        List<TS_Value> tsValueList = new ArrayList<TS_Value>();
        for (int rowNo = 0; rowNo < this.rows.size(); rowNo++) {
            TS_Value tsValue = getValue(rowNo, aid, colName);
            tsValueList.add(tsValue);
            valueSeq.flag[rowNo] = tsValue.flag;
        }

        return createTsValueSeq(tsValueList, dt);
    }

    /**
     * Converts a list of <code>org.asam.ods.TS_Value</code> objects to a <code>org.asam.ods.TS_ValueSeq</code> result
     * column object.
     * <p>
     * All value must have the same data type.
     * 
     * @param values The list of values.
     * @param dt The data type.
     * @return The TS_ValueSeq object.
     */
    private TS_ValueSeq createTsValueSeq(List<TS_Value> values, DataType dt) {
        TS_ValueSeq valueSeq = new TS_ValueSeq();
        valueSeq.flag = new short[rows.size()];
        valueSeq.u = new TS_UnionSeq();

        // DT_BLOB
        if (dt == DataType.DT_BLOB) {
            Blob[] ar = new Blob[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.blobVal();
            }
            valueSeq.u.blobVal(ar);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            boolean[] ar = new boolean[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.booleanVal();
            }
            valueSeq.u.booleanVal(ar);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            byte[] ar = new byte[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.byteVal();
            }
            valueSeq.u.byteVal(ar);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            byte[][] ar = new byte[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.bytestrVal();
            }
            valueSeq.u.bytestrVal(ar);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            T_COMPLEX[] ar = new T_COMPLEX[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.complexVal();
            }
            valueSeq.u.complexVal(ar);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            String[] ar = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.dateVal();
            }
            valueSeq.u.dateVal(ar);
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            T_DCOMPLEX[] ar = new T_DCOMPLEX[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.dcomplexVal();
            }
            valueSeq.u.dcomplexVal(ar);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            double[] ar = new double[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.doubleVal();
            }
            valueSeq.u.doubleVal(ar);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            int[] ar = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.enumVal();
            }
            valueSeq.u.enumVal(ar);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference[] ar = new T_ExternalReference[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.extRefVal();
            }
            valueSeq.u.extRefVal(ar);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            float[] ar = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.floatVal();
            }
            valueSeq.u.floatVal(ar);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            int[] ar = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.longVal();
            }
            valueSeq.u.longVal(ar);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            T_LONGLONG[] ar = new T_LONGLONG[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.longlongVal();
            }
            valueSeq.u.longlongVal(ar);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            short[] ar = new short[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.shortVal();
            }
            valueSeq.u.shortVal(ar);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            String[] ar = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.stringVal();
            }
            valueSeq.u.stringVal(ar);
        }
        // DS_BOOLEAN
        else if (dt == DataType.DS_BOOLEAN) {
            boolean[][] ar = new boolean[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.booleanSeq();
            }
            valueSeq.u.booleanSeq(ar);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            byte[][] ar = new byte[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.byteSeq();
            }
            valueSeq.u.byteSeq(ar);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            byte[][][] ar = new byte[values.size()][][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.bytestrSeq();
            }
            valueSeq.u.bytestrSeq(ar);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            T_COMPLEX[][] ar = new T_COMPLEX[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.complexSeq();
            }
            valueSeq.u.complexSeq(ar);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            String[][] ar = new String[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.dateSeq();
            }
            valueSeq.u.dateSeq(ar);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            T_DCOMPLEX[][] ar = new T_DCOMPLEX[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.dcomplexSeq();
            }
            valueSeq.u.dcomplexSeq(ar);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            double[][] ar = new double[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.doubleSeq();
            }
            valueSeq.u.doubleSeq(ar);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            int[][] ar = new int[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.enumSeq();
            }
            valueSeq.u.enumSeq(ar);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            T_ExternalReference[][] ar = new T_ExternalReference[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.extRefSeq();
            }
            valueSeq.u.extRefSeq(ar);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            float[][] ar = new float[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.floatSeq();
            }
            valueSeq.u.floatSeq(ar);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            int[][] ar = new int[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.longSeq();
            }
            valueSeq.u.longSeq(ar);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            T_LONGLONG[][] ar = new T_LONGLONG[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.longlongSeq();
            }
            valueSeq.u.longlongSeq(ar);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            short[][] ar = new short[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.shortSeq();
            }
            valueSeq.u.shortSeq(ar);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            String[][] ar = new String[values.size()][];
            for (int i = 0; i < values.size(); i++) {
                ar[i] = values.get(i).u.stringSeq();
            }
            valueSeq.u.stringSeq(ar);
        }

        return valueSeq;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        // headline
        sb.append("Row    |");
        for (Long aid : columns) {
            sb.append(String.format(" %6s[%2d] |", this.atfxCache.getApplicationElementNameById(aid), aid));
        }
        sb.append("\n");
        sb.append("-------------------------------------------------------------\n");
        // rows
        int rowNo = 0;
        for (Map<Long, Long> map : rows) {
            sb.append(String.format("%6d", rowNo));
            sb.append(" | ");
            for (Long aid : columns) {
                sb.append(String.format("%10d", map.get(aid)));
                sb.append(" | ");
            }
            sb.append("\n");
            rowNo++;
        }
        return sb.toString();
    }

    private boolean filterMatch(TS_Value value, TS_Value filter, SelOpcode selOpCode) throws AoException {
        if (value.u.discriminator() != filter.u.discriminator()) {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0, "Invalid datatype!");
        }
        // EQ
        if (selOpCode == SelOpcode.EQ) {
            return filterMatchEQ(value, filter);
        }
        // unsupported SelOpcode
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "unsupproted SelOpcode: ");
        }
    }

    private boolean filterMatchEQ(TS_Value value, TS_Value filter) throws AoException {
        DataType dt = value.u.discriminator();
        boolean ret = false;
        // flags unequal
        if (value.flag != filter.flag) {
            ret = false;
        }
        // flags both null
        else if (value.flag == 0 && filter.flag == 0) {
            ret = true;
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            ret = (value.u.booleanVal() == filter.u.booleanVal());
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            ret = (value.u.byteVal() == filter.u.byteVal());
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            ret = (value.u.enumVal() == filter.u.enumVal());
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            ret = (value.u.longVal() == filter.u.longVal());
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            ret = (ODSHelper.asJLong(value.u.longlongVal()) == ODSHelper.asJLong(filter.u.longlongVal()));
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            ret = (value.u.shortVal() == filter.u.shortVal());
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            ret = PatternUtil.nameFilterMatch(value.u.stringVal(), filter.u.stringVal());
        }
        // unsupported datatype
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "unsupproted datatype: ");
        }
        return ret;
    }

}
