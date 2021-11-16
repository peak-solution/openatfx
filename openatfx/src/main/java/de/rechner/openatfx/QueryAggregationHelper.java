package de.rechner.openatfx;

import java.util.List;

import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;

public class QueryAggregationHelper {
    private final AtfxCache atfxCache;
    private final QueryStructureExt query;
    private final long aid;
    private final List<SelAIDNameUnitId> aggregateSelects;
    
    public QueryAggregationHelper(AtfxCache atfxCache, QueryStructureExt query, long aid, List<SelAIDNameUnitId> aggregateSelects) {
        this.atfxCache = atfxCache;
        this.query = query;
        this.aid = aid;
        this.aggregateSelects = aggregateSelects;
    }

    public void fillElemResultSetExt(ElemResultSetExt erse, List<Long> filteredIids) throws AoException {
        if (query.anuSeq.length > aggregateSelects.size()) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                    "Invalid QueryStructureExt found: No other selects than the aggregate are supported!");
        }
        if (aggregateSelects.size() > 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                    "Invalid QueryStructureExt found: Only at most 1 Aggregate function is supported!");
        }
        
        erse.values = new NameValueSeqUnitId[query.anuSeq.length];
        for (int col = 0; col < erse.values.length; col++) {
            Integer attrNo = this.atfxCache.getAttrNoByName(aid, query.anuSeq[col].attr.aaName);
            ApplicationRelation ar = atfxCache.getRelationByName(aid, query.anuSeq[col].attr.aaName);
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = query.anuSeq[col].attr.aaName;
            erse.values[col].value = new TS_ValueSeq();
            erse.values[col].unitId = new T_LONGLONG(0, 0);
            
            AggrFunc aggrFunc = query.anuSeq[col].aggregate;
            if (attrNo == null) {
                TS_ValueSeq valueSeq = atfxCache.getRelatedInstanceIds(aid, filteredIids, ar);
                erse.values[col].value = handleAggregate(aggrFunc, valueSeq);
            } else {
                TS_ValueSeq valueSeq = atfxCache.getInstanceValues(aid, attrNo, filteredIids);
                erse.values[col].value = handleAggregate(aggrFunc, valueSeq);
            }
        }
    }
    
    private TS_ValueSeq handleAggregate(AggrFunc aggrFunc, TS_ValueSeq value) throws AoException {
        if (AggrFunc.MAX == aggrFunc) {
            DataType dt = value.u.discriminator();
            Object max = null;
            for (int i = 0; i < ODSHelper.tsUnionSeqLength(value.u); i++) {
                TS_Value currentValue = ODSHelper.tsValueSeq2tsValue(value, i);
                if (currentValue.flag == 0) {
                    continue;
                }
                max = getMaxValue(ODSHelper.tsValue2jObject(currentValue), max);
            }
            
            if (max == null) {
                TS_Value empty = ODSHelper.createEmptyTS_Value(dt);
                return ODSHelper.tsValue2tsValueSeq(empty, false);
            }
            
            String aggregateValue = null;
            if (max instanceof T_LONGLONG) {
                aggregateValue = String.valueOf(ODSHelper.asJLong((T_LONGLONG)max));
            } else {
                aggregateValue = max.toString();
            }
            TS_Union union = ODSHelper.string2tsUnion(dt, aggregateValue);
            TS_UnionSeq seq = ODSHelper.tsUnion2tsUnionSeq(union, false);
            return new TS_ValueSeq(seq, new short[] {15});
        }
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Aggregation of type " + aggrFunc.toString() + " not supported!");
    }
    
    private Object getMaxValue(Object value, Object aggregateValue) throws AoException {
        if (aggregateValue == null) {
            return value;
        }
        if (value instanceof Short) {
            if ((Short) value > (Short) aggregateValue) {
                aggregateValue = value;
            }
        } else if (value instanceof Integer) {
            if ((Integer) value > (Integer) aggregateValue) {
                aggregateValue = value;
            }
        } else if (value instanceof Long) {
            if ((Long) value > (Long) aggregateValue) {
                aggregateValue = value;
            }
        } else if (value instanceof T_LONGLONG) {
            long longVal = ODSHelper.asJLong((T_LONGLONG)value);
            long longAggrVal = ODSHelper.asJLong((T_LONGLONG)aggregateValue);
            if (longVal > longAggrVal) {
                aggregateValue = value;
            }
        } else if (value instanceof Float) {
            if ((Float) value > (Float) aggregateValue) {
                aggregateValue = value;
            }
        } else if (value instanceof Double) {
            if ((Double) value > (Double) aggregateValue) {
                aggregateValue = value;
            }
        } else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Aggregation for values of datatype " + value.getClass() + " not supported!");
        }
        return aggregateValue;
    }
}
