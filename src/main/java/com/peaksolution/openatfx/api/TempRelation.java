package com.peaksolution.openatfx.api;

import java.util.Objects;

class TempRelation {
    public String fromName;
    public String toName;
    public String baseRelationName;
    public String relationName;
    public String inverseRelationName;
    public String min;
    public String max;
    
    @Override
    public int hashCode() {
        return Objects.hash(fromName, relationName);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TempRelation other = (TempRelation) obj;
        return fromName.equals(other.fromName) && Objects.equals(relationName, other.relationName);
    }
}
