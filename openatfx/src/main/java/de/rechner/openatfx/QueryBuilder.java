package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.AttrResultSet;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSet;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelOpcode;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * <ul>
 * <li>Step 1: perform joins, collect instance ids</li>
 * </ul>
 * 
 * @author Christian Rechner
 */
class QueryBuilder {

    private final AtfxCache atfxCache;

    private final Set<Long> columns;
    private final List<Map<Long, Long>> rows;

    /**
     * @param atfxCache
     * @param startAid
     */
    public QueryBuilder(AtfxCache atfxCache, long startAid) {
        this.atfxCache = atfxCache;
        this.rows = new ArrayList<Map<Long, Long>>();
        this.columns = new TreeSet<Long>();

        columns.add(startAid);
        Set<Long> instIids = atfxCache.getInstanceIds(startAid);
        for (Long iid : new TreeSet<Long>(instIids)) {
            Map<Long, Long> m = new TreeMap<Long, Long>();
            m.put(startAid, iid);
            rows.add(m);
        }
    }

    /**
     * Returns the query result for an ODS 'query'.
     * 
     * @param anuSeq
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
    public ResultSetExt[] getResultSetExt() throws AoException {
        return null;
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

    private TS_ValueSeq getTsValueSeq(long aid, String colName) throws AoException {
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

        // build list
        
        // DT_BOOLEAN
        // if (dt == DataType.DT_LONGLONG) {
        // boolean[] bAr = new boolean[rows.size()];
        // }
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
        sb.append("Row    | ");
        for (Long aid : columns) {
            sb.append(this.atfxCache.getApplicationElementNameById(aid));
            sb.append("[");
            sb.append(aid);
            sb.append("]");
        }
        sb.append(" | ");
        sb.append("\n");
        sb.append("--------------------------------\n");
        // rows
        int rowNo = 0;
        for (Map<Long, Long> map : rows) {
            sb.append(String.format("%6d", rowNo));
            sb.append(" | ");
            for (Long aid : columns) {
                sb.append(String.format("%7d", map.get(aid)));
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
