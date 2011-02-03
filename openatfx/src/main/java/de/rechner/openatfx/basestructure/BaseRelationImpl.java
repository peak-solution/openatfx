package de.rechner.openatfx.basestructure;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelationPOA;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;


/**
 * Implementation of <code>org.asam.ods.BaseRelation</code>.
 * 
 * @author Christian Rechner
 */
class BaseRelationImpl extends BaseRelationPOA {

    private final BaseElement elem1;
    private final BaseElement elem2;
    private final String relationName;
    private final String inverseRelationName;
    private final RelationRange relationRange;
    private final RelationRange inverseRelationRange;
    private final Relationship relationship;
    private final Relationship inverseRelationship;
    private final RelationType relationType;

    /**
     * Constructor.
     * 
     * @param elem1 The first base element.
     * @param elem2 The second base element.
     * @param relationName The relation name.
     * @param inverseRelationName The inverse relation name.
     * @param relationRange The relation range.
     * @param inverseRelationRange The inverse relation range.
     * @param relationship The relationship.
     * @param inverseRelationship The inverse relationship.
     * @param relationType The relation type.
     */
    public BaseRelationImpl(BaseElement elem1, BaseElement elem2, String relationName, String inverseRelationName,
            RelationRange relationRange, RelationRange inverseRelationRange, Relationship relationship,
            Relationship inverseRelationship, RelationType relationType) {
        this.elem1 = elem1;
        this.elem2 = elem2;
        this.relationName = relationName;
        this.inverseRelationName = inverseRelationName;
        this.relationRange = relationRange;
        this.inverseRelationRange = inverseRelationRange;
        this.relationship = relationship;
        this.inverseRelationship = inverseRelationship;
        this.relationType = relationType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getElem1()
     */
    public BaseElement getElem1() throws AoException {
        return this.elem1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getElem2()
     */
    public BaseElement getElem2() throws AoException {
        return this.elem2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getRelationName()
     */
    public String getRelationName() throws AoException {
        return this.relationName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getInverseRelationName()
     */
    public String getInverseRelationName() throws AoException {
        return this.inverseRelationName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getRelationRange()
     */
    public RelationRange getRelationRange() throws AoException {
        return this.relationRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getInverseRelationRange()
     */
    public RelationRange getInverseRelationRange() throws AoException {
        return this.inverseRelationRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getRelationship()
     */
    public Relationship getRelationship() throws AoException {
        return this.relationship;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getInverseRelationship()
     */
    public Relationship getInverseRelationship() throws AoException {
        return this.inverseRelationship;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseRelationOperations#getRelationType()
     */
    public RelationType getRelationType() throws AoException {
        return this.relationType;
    }

}
