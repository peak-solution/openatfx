package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asam.ods.AggrFunc;
import org.asam.ods.DataType;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.Instance;
import com.peaksolution.openatfx.api.NameValueUnit;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.api.SingleValue;
import com.peaksolution.openatfx.util.ODSHelper;

public class QueryAggregationHelper {
    private final OpenAtfxAPIImplementation api;
    private final QueryStructureExt query;
    private final long aid;
    private final List<SelAIDNameUnitId> aggregateSelects;
    
    public QueryAggregationHelper(OpenAtfxAPIImplementation api, QueryStructureExt query, long aid, List<SelAIDNameUnitId> aggregateSelects) {
        this.api = api;
        this.query = query;
        this.aid = aid;
        this.aggregateSelects = aggregateSelects;
    }

    public void fillElemResultSetExt(ElemResultSetExt erse, List<Long> filteredIids) throws OpenAtfxException {
        if (query.anuSeq.length > aggregateSelects.size()) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Invalid QueryStructureExt found: No other selects than the aggregate are supported!");
        }
        if (aggregateSelects.size() > 1) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Invalid QueryStructureExt found: Only at most 1 Aggregate function is supported!");
        }

        erse.values = new NameValueSeqUnitId[query.anuSeq.length];
        for (int col = 0; col < erse.values.length; col++) {
            Relation ar = api.getRelationByName(aid, query.anuSeq[col].attr.aaName);
            erse.values[col] = new NameValueSeqUnitId();
            erse.values[col].valName = query.anuSeq[col].attr.aaName;
            erse.values[col].value = new TS_ValueSeq();
            erse.values[col].unitId = new T_LONGLONG(0, 0);

            AggrFunc aggrFunc = query.anuSeq[col].aggregate;
            Attribute attr = api.getAttribute(aid, query.anuSeq[col].attr.aaName);
            // relation
            if (attr == null) {
                List<Long> relIids = api.getRelatedInstanceIds(aid, filteredIids, ar);
                List<TS_Value> values = new ArrayList<>();
                for (Long relIid : relIids) {
                    SingleValue val = new SingleValue(com.peaksolution.openatfx.api.DataType.DT_LONGLONG, relIid);
                    values.add(ODSHelper.mapTSValue(val));
                }
                TS_ValueSeq valueSeq = ODSHelper.tsValue2tsValueSeq(values.toArray(new TS_Value[0]), DataType.DT_LONGLONG);
                erse.values[col].value = handleAggregate(aggrFunc, valueSeq);
            // attribute
            } else {
                Map<Long, Instance> filteredInstancesById = new HashMap<>();
                for (Instance instance : api.getInstances(aid, filteredIids)) {
                    filteredInstancesById.put(instance.getIid(), instance); 
                }
                List<TS_Value> values = new ArrayList<>();
                for (long filteredIid : filteredIids) {
                    Instance currentInstance = filteredInstancesById.get(filteredIid);
                    NameValueUnit nvu = currentInstance.getValue(attr.getName());
                    values.add(ODSHelper.mapTSValue(nvu.getValue()));
                }
                TS_ValueSeq valueSeq = ODSHelper.tsValue2tsValueSeq(values.toArray(new TS_Value[0]), ODSHelper.mapDataType(attr.getDataType()));
                erse.values[col].value = handleAggregate(aggrFunc, valueSeq);
            }
        }
    }
    
    private TS_ValueSeq handleAggregate(AggrFunc aggrFunc, TS_ValueSeq value) throws OpenAtfxException {
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
        throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                    "Aggregation of type " + aggrFunc.toString() + " not supported!");
    }
    
    private Object getMaxValue(Object value, Object aggregateValue) throws OpenAtfxException {
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
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Aggregation for values of datatype " + value.getClass() + " not supported!");
        }
        return aggregateValue;
    }
}
