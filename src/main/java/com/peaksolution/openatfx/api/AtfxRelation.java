package com.peaksolution.openatfx.api;
import com.peaksolution.datamodel.BaseRelation;
import com.peaksolution.datamodel.DefaultRelation;
import com.peaksolution.datamodel.RelationType;
import com.peaksolution.datamodel.Relationship;

public class AtfxRelation extends DefaultRelation {
    private int inverseRelNo;

    public AtfxRelation(int tempRelNo) {
        super(tempRelNo, OpenAtfxConstants.DEF_RELNAME_EMPTY);
    }
    
    public AtfxRelation(AtfxElement from, AtfxElement to, BaseRelation baseRelation, String relationName, String inverseName,
            short rangeMin, short rangeMax, Relationship relationship, RelationType relationType) {
        super(0, relationName != null ? relationName : OpenAtfxConstants.DEF_RELNAME_EMPTY);
        setElement1(from);
        setElement2(to);
        setBaseRelation(baseRelation);
        if (inverseName != null) {
            if (to != null) {
                AtfxRelation invRel = to.getRelationByName(inverseName);
                if (invRel != null) {
                    this.inverseRelNo = invRel.getRelNo();
                }
            }
            setInverseRelationName(inverseName);
        }
        if (rangeMin != -2) {
            setRelationRangeMin(rangeMin);
        }
        if (rangeMax != -2) {
            setRelationRangeMax(rangeMax);
        }
        setRelationship(relationship);
        if (relationType != null) {
            setRelationType(relationType);
        }
    }

    void setInverseRelNo(int invRelNo) {
        this.inverseRelNo = invRelNo;
    }
    
    @Override
    public AtfxRelation getInverseRelation() {
        AtfxElement to = getAtfxElement2();
        if (to == null) {
            return null;
        }
        
        AtfxRelation rel = to.getRelationByNo(inverseRelNo);
        if (rel == null) {
            rel = to.getRelationByName(getInverseRelationName());
            if (rel != null) {
                this.inverseRelNo = rel.getRelNo();
            }
        }
        
        return rel;
    }

    public AtfxElement getAtfxElement1() {
        return (AtfxElement) getElement1();
    }

    @Override
    public long getElement1Id() {
        AtfxElement from = getAtfxElement1();
        return from == null ? -1 : from.getId();
    }

    public AtfxElement getAtfxElement2() {
        return (AtfxElement) getElement2();
    }

    @Override
    public long getElement2Id() {
        AtfxElement to = getAtfxElement2();
        return to == null ? -1 : to.getId();
    }

    @Override
    public String toString() {
        BaseRelation baseRelation = getBaseRelation();
        String baseRelationString = baseRelation == null ? "" : baseRelation.getName();
        AtfxElement to = getAtfxElement2();
        String toString = to == null ? "" : to.toString();
        return "Relation [relationName=" + getRelationName() + ", baseRelationName=" + baseRelationString + ", from="
                + getAtfxElement1() + ", to=" + toString + ", inverseName=" + getInverseRelationName() + ", rangeMin="
                + getRelationRangeMin() + ", rangeMax=" + getRelationRangeMax() + "]";
    }
}
