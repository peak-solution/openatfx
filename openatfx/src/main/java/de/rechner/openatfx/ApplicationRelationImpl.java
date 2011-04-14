package de.rechner.openatfx;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationPOA;
import org.asam.ods.BaseRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplicationRelation</code>.
 * 
 * @author Christian Rechner
 */
class ApplicationRelationImpl extends ApplicationRelationPOA {

    private final AtfxCache atfxCache;

    private BaseRelation baseRelation;
    private ApplicationElement elem1;
    private ApplicationElement elem2;
    private String relationName;
    private String inverseRelationName;
    private RelationRange relationRange;
    private RelationType relationType;

    private ApplicationRelation inverseRelation;

    /**
     * Constructor.
     * 
     * @param atfxCache The ATFX cache.
     */
    public ApplicationRelationImpl(AtfxCache atfxCache) {
        this.atfxCache = atfxCache;
        this.relationType = RelationType.INFO;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getBaseRelation()
     */
    public BaseRelation getBaseRelation() throws AoException {
        return this.baseRelation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setBaseRelation(org.asam.ods.BaseRelation)
     */
    public void setBaseRelation(BaseRelation baseRel) throws AoException {
        // set default values for base relation
        if (baseRel != null) {
            // only set the relation range if there is none set
            if (this.relationRange == null) {
                this.relationRange = baseRel.getRelationRange();
            }
            this.relationType = baseRel.getRelationType();
        }
        this.baseRelation = baseRel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getElem1()
     */
    public ApplicationElement getElem1() throws AoException {
        return this.elem1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setElem1(org.asam.ods.ApplicationElement)
     */
    public void setElem1(ApplicationElement applElem) throws AoException {
        // update cache
        if (this.elem1 != null && applElem == null) {
            long aid = ODSHelper.asJLong(this.elem1.getId());
            this.atfxCache.removeApplicationRelationElem1(aid, _this());
        } else if (applElem != null) {
            long aid = ODSHelper.asJLong(applElem.getId());
            this.atfxCache.setApplicationRelationElem1(aid, _this());
        }

        this.elem1 = applElem;
        this.inverseRelation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getElem2()
     */
    public ApplicationElement getElem2() throws AoException {
        return this.elem2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setElem2(org.asam.ods.ApplicationElement)
     */
    public void setElem2(ApplicationElement applElem) throws AoException {
        this.elem2 = applElem;
        this.inverseRelation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationName()
     */
    public String getRelationName() throws AoException {
        return this.relationName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationName(java.lang.String)
     */
    public void setRelationName(String arName) throws AoException {
        this.relationName = arName;
        if (this.inverseRelation != null) {
            this.inverseRelation.setInverseRelationName(arName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationName()
     */
    public String getInverseRelationName() throws AoException {
        return this.inverseRelationName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setInverseRelationName(java.lang.String)
     */
    public void setInverseRelationName(String arInvName) throws AoException {
        this.inverseRelationName = arInvName;
        if (this.inverseRelation != null) {
            this.inverseRelation.setRelationName(arInvName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationRange()
     */
    public RelationRange getRelationRange() throws AoException {
        return this.relationRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationRange(org.asam.ods.RelationRange)
     */
    public void setRelationRange(RelationRange arRelationRange) throws AoException {
        this.relationRange = arRelationRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setInverseRelationRange(org.asam.ods.RelationRange)
     */
    public void setInverseRelationRange(RelationRange arRelationRange) throws AoException {
        // this.inverseRelationRange = arRelationRange;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationship()
     */
    public Relationship getRelationship() throws AoException {
        // if base relation, return the relationship
        if (this.baseRelation != null) {
            return this.baseRelation.getRelationship();
        }
        // m:n
        else if (getRelationRange() == null) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, "RelationRange was null");
        } else if (getRelationRange().max == -1 && getInverseRelationRange().max == -1) {
            return Relationship.INFO_REL;
        }
        // 0:1 or 1:0
        else if (getRelationRange().min == 1 || getRelationRange().min == 0) {
            return Relationship.INFO_FROM;
        } else {
            return Relationship.INFO_TO;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationship()
     */
    public Relationship getInverseRelationship() throws AoException {
        return this.atfxCache.getInverseRelation(_this()).getRelationship();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationRange()
     */
    public RelationRange getInverseRelationRange() throws AoException {
        return this.atfxCache.getInverseRelation(_this()).getRelationRange();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationType()
     */
    public RelationType getRelationType() throws AoException {
        if (this.baseRelation != null) {
            return this.baseRelation.getRelationType();
        }
        return this.relationType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationType(org.asam.ods.RelationType)
     */
    public void setRelationType(RelationType arRelationType) throws AoException {
        if (this.baseRelation != null) {
            throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                  "Unable to change relation type for an application relation derived from base relation");
        }
        this.relationType = arRelationType;
    }

}
