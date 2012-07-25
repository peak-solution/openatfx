package de.rechner.openatfx.exporter;

import org.asam.ods.ElemId;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


class ElemIdMap {

    private final long aid;
    private final long iid;

    public ElemIdMap(T_LONGLONG aid, T_LONGLONG iid) {
        this.aid = ODSHelper.asJLong(aid);
        this.iid = ODSHelper.asJLong(iid);
    }

    public ElemIdMap(ElemId elemId) {
        this.aid = ODSHelper.asJLong(elemId.aid);
        this.iid = ODSHelper.asJLong(elemId.iid);
    }

    public long getAid() {
        return aid;
    }

    public long getIid() {
        return iid;
    }

    public ElemId getElemId() {
        return new ElemId(ODSHelper.asODSLongLong(this.aid), ODSHelper.asODSLongLong(this.iid));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (aid ^ (aid >>> 32));
        result = prime * result + (int) (iid ^ (iid >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ElemIdMap other = (ElemIdMap) obj;
        if (aid != other.aid)
            return false;
        if (iid != other.iid)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ElemIdMap [aid=" + aid + ", iid=" + iid + "]";
    }

}
