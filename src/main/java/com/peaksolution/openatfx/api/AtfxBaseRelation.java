package com.peaksolution.openatfx.api;

import java.util.Collection;
import java.util.Objects;

import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;


/**
 * OpenAtfx base relation.
 * 
 * @author Markus Renner
 */
public class AtfxBaseRelation implements BaseRelation {

    private final BaseElement elem1;
    private final Collection<BaseElement> elem2;
    private final String relationName;
    private final String inverseRelationName;
    private final boolean mandatory;
    private final RelationRange relationRange;
    private final RelationRange inverseRelationRange;
    private final Relationship relationship;
    private final Relationship inverseRelationship;
    private final RelationType relationType;

    /**
     * Constructor.
     * 
     * @param elem1 The first base element.
     * @param elem2 The second base element(s).
     * @param relationName The relation name.
     * @param inverseRelationName The inverse relation name.
     * @param mandatory The mandatory flag.
     * @param relationRange The relation range.
     * @param inverseRelationRange The inverse relation range.
     * @param relationship The relationship.
     * @param inverseRelationship The inverse relationship.
     * @param relationType The relation type.
     */
    public AtfxBaseRelation(BaseElement elem1, Collection<BaseElement> elem2, String relationName,
            String inverseRelationName, boolean mandatory, RelationRange relationRange,
            RelationRange inverseRelationRange, Relationship relationship,
            Relationship inverseRelationship, RelationType relationType) {
        this.elem1 = elem1;
        this.elem2 = elem2;
        this.relationName = relationName;
        this.inverseRelationName = inverseRelationName;
        this.mandatory = mandatory;
        this.relationRange = relationRange;
        this.inverseRelationRange = inverseRelationRange;
        this.relationship = relationship;
        this.inverseRelationship = inverseRelationship;
        this.relationType = relationType;
    }
    
    /**
     * @param relationTargetBaseName
     * @return
     */
    private String checkAndHandleDifferentlyNamedInverseRelations(String relationTargetBaseName) {
        String identifiedInverseRelationName = inverseRelationName;
        if (identifiedInverseRelationName == null || identifiedInverseRelationName.isBlank()) {
            if (elem1.getType().equalsIgnoreCase("AoTest") || elem1.getType().equalsIgnoreCase("AoSubTest")) {
                if (relationTargetBaseName.equalsIgnoreCase("AoMeasurement")) {
                    return "test";
                } else  {
                    return "parent_test";
                }
            }
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "AtfxBaseRelation.checkAndHandleDifferentlyNamedInverseRelations() called on unexpected base element "
                                                + elem1.getType());
        }
        return identifiedInverseRelationName;
    }

    @Override
    public BaseElement getElem1() {
        return this.elem1;
    }

    @Override
    public Collection<BaseElement> getElem2() {
        return this.elem2;
    }
    
    public void addElement2(Collection<BaseElement> elem2) {
        this.elem2.addAll(elem2);
    }

    @Override
    public String getName() {
        return this.relationName;
    }
    
    @Override
    public String getInverseName() {
        return inverseRelationName;
    }

    @Override
    public String getInverseName(String relationTargetBaseName) {
        return checkAndHandleDifferentlyNamedInverseRelations(relationTargetBaseName);
    }
    
    @Override
    public boolean isMandatory() {
        return this.mandatory;
    }

    @Override
    public RelationRange getRelationRange() {
        return this.relationRange;
    }

    @Override
    public RelationRange getInverseRelationRange() {
        return this.inverseRelationRange;
    }

    @Override
    public Relationship getRelationship() {
        return this.relationship;
    }

    @Override
    public Relationship getInverseRelationship() {
        return this.inverseRelationship;
    }

    @Override
    public RelationType getRelationType() {
        return this.relationType;
    }

    @Override
    public String toString() {
        return "AtfxBaseRelation [relationName=" + relationName + ", inverseRelationName=" + getInverseName()
                + ", elem1=" + elem1.getType() + ", elem2=" + elem2.stream().map(BaseElement::getType).toList()
                + ", relationType=" + relationType + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(elem1, getInverseName(), relationName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtfxBaseRelation other = (AtfxBaseRelation) obj;
        return Objects.equals(elem1, other.elem1) && Objects.equals(getInverseName(), other.getInverseName())
                && Objects.equals(relationName, other.relationName);
    }
}
