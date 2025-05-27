package com.peaksolution.openatfx.api;

import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;

public class AtfxRelation implements Relation {
    private int relNo;
    private AtfxElement from;
    private AtfxElement to;
    private BaseRelation baseRelation;
    private String inverseName = "";
    private int inverseRelNo;
    private Relationship relationship;
    // default values as specified in ASAM ODS specification CH10
    private String relationName = OpenAtfxConstants.DEF_RELNAME_EMPTY;
    private short rangeMin = -2;
    private short rangeMax = -2;
    private RelationType relationType = RelationType.INFO;

    public AtfxRelation(int tempRelNo) {
        this.relNo = tempRelNo;
    }
    
    public AtfxRelation(AtfxElement from, AtfxElement to, BaseRelation baseRelation, String relationName, String inverseName,
            short rangeMin, short rangeMax, Relationship relationship, RelationType relationType) {
        this.from = from;
        this.to = to;
        setBaseRelation(baseRelation);
        if (relationName != null) {
            this.relationName = relationName;
        }
        if (inverseName != null) {
            if (to != null) {
                AtfxRelation invRel = to.getRelationByName(inverseName);
                if (invRel != null) {
                    this.inverseRelNo = invRel.getRelNo();
                }
            }
            this.inverseName = inverseName;
        }
        if (rangeMin != -2) {
            this.rangeMin = rangeMin;
        }
        if (rangeMax != -2) {
            this.rangeMax = rangeMax;
        }
        this.relationship = relationship;
        if (relationType != null) {
            this.relationType = relationType;
        }
    }
    
    public void setRelNo(int relNo) {
        this.relNo = relNo;
    }
    
    @Override
    public int getRelNo() {
        return relNo;
    }
    
    @Override
    public AtfxRelation getInverseRelation() {
        if (to == null) {
            return null;
        }
        
        AtfxRelation rel = to.getRelationByNo(inverseRelNo);
        if (rel == null) {
            rel = to.getRelationByName(inverseName);
            if (rel != null) {
                this.inverseRelNo = rel.getRelNo();
            }
        }
        
        return rel;
    }
    
    @Override
    public void setBaseRelation(BaseRelation baseRel) {
        // set default values for base relation
        if (baseRel != null) {
            // only set the relation range if there is none set
            if (getRelationRangeMin() == -2 && getRelationRangeMax() == -2) {
                RelationRange relRange = baseRel.getRelationRange();
                setRelationRangeMin(relRange.min);
                setRelationRangeMax(relRange.max);
            }
            setRelationType(baseRel.getRelationType());
            setRelationship(baseRel.getRelationship());
        } else {
            setRelationType(RelationType.INFO);
        }

        this.baseRelation = baseRel;
    }

    @Override
    public BaseRelation getBaseRelation() {
        return baseRelation;
    }
    
    public void setRelationName(String newRelationName) {
        this.relationName = newRelationName;
    }

    @Override
    public String getRelationName() {
        return relationName;
    }
    
    @Override
    public String getBaseName() {
        if (baseRelation != null) {
            return baseRelation.getName();
        }
        return null;
    }
    
    @Override
    public void setElement1(Element element1) {
        this.from = (AtfxElement)element1;
    }

    @Override
    public Element getElement1() {
        return from;
    }
    
    public AtfxElement getAtfxElement1() {
        return from;
    }

    @Override
    public void setElement2(Element element2) {
        this.to = (AtfxElement)element2;
    }
    
    @Override
    public Element getElement2() throws OpenAtfxException {
        return to;
    }
    
    public AtfxElement getAtfxElement2() {
        return to;
    }
    
    void setInverseRelNo(int invRelNo) {
        this.inverseRelNo = invRelNo;
    }
    
    void setInverseRelationName(String inverseName) {
        if (inverseName == null) {
            this.inverseName = "";
        } else {
            this.inverseName = inverseName;
        }
    }

    @Override
    public String getInverseRelationName() {
        return inverseName;
    }

    @Override
    public void setRelationRangeMin(short min) {
        this.rangeMin = min;
    }
    
    @Override
    public short getRelationRangeMin() {
        return rangeMin;
    }
    
    @Override
    public void setRelationRangeMax(short max) {
        this.rangeMax = max;
    }

    @Override
    public short getRelationRangeMax() {
        return rangeMax;
    }
    
    public void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    @Override
    public Relationship getRelationship() {
        return relationship;
    }
    
    @Override
    public void setRelationType(RelationType relType) {
        relationType = relType;
    }

    @Override
    public RelationType getRelationType() {
        return relationType;
    }

    @Override
    public String toString() {
        String baseRelationString = baseRelation == null ? "" : baseRelation.getName();
        String toString = to == null ? "" : to.toString();
        return "Relation [relationName=" + relationName + ", baseRelationName=" + baseRelationString + ", from="
                + from + ", to=" + toString + ", inverseName=" + inverseName + ", rangeMin=" + rangeMin
                + ", rangeMax=" + rangeMax + "]";
    }
}
