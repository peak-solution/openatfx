package de.rechner.openatfx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.asam.ods.AoException;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelValue;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


class QueryBuilder {

    private final AtfxCache atfxCache;

    private HashMap<Long, Set<Long>> currentResultMatrix; // <aid, <iids>>

    /**
     * @param atfxCache
     */
    public QueryBuilder(AtfxCache atfxCache) {
        this.atfxCache = atfxCache;
        this.currentResultMatrix = new HashMap<Long, Set<Long>>();
    }

    public void addAid(long aid) {
        if (!currentResultMatrix.containsKey(aid)) {
            // fill with instance ids
            Set<Long> iids = new TreeSet<Long>(atfxCache.getInstanceIds(aid));
            this.currentResultMatrix.put(aid, iids);
        }
    }

    public void applySelValue(SelValue selValue) throws AoException {
        long aid = ODSHelper.asJLong(selValue.attr.attr.aid);
        String aaName = selValue.attr.attr.aaName;

        // apply filter
        Set<Long> resultIids = new HashSet<Long>();
        for (long iid : this.currentResultMatrix.get(aid)) {
            // attribute exists?
            TS_Value value = this.atfxCache.getInstanceValue(aid, iid, aaName);
            if (value == null) {
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0, "Attribute not found!");
            }
            boolean filterMatch = filterMatch(value, selValue.value, selValue.oper);
        }
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
