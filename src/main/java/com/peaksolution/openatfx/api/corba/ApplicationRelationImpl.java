package com.peaksolution.openatfx.api.corba;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelationPOA;
import org.asam.ods.BaseRelation;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;

import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplicationRelation</code>.
 * 
 * @author Christian Rechner
 */
class ApplicationRelationImpl extends ApplicationRelationPOA {
    private final CorbaAtfxCache corbaCache;
    private final AtfxRelation delegate;
    
    /**
     * Constructor.
     * 
     * @param corbaCache The CorbaAtfxCache.
     * @param atfxRelation The delegate for the AtfxRelation. 
     */
    public ApplicationRelationImpl(CorbaAtfxCache corbaCache, AtfxRelation atfxRelation) {
        this.corbaCache = corbaCache;
        this.delegate = atfxRelation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getBaseRelation()
     */
    public BaseRelation getBaseRelation() throws AoException {
        return corbaCache.getBaseRelation(delegate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setBaseRelation(org.asam.ods.BaseRelation)
     */
    public void setBaseRelation(BaseRelation baseRel) throws AoException {
        try {
            corbaCache.updateBaseRelation(delegate, baseRel);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getElem1()
     */
    public ApplicationElement getElem1() throws AoException {
        Element elem1 = delegate.getElement1();
        return elem1 == null ? null : corbaCache.getApplicationElementById(elem1.getId());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setElem1(org.asam.ods.ApplicationElement)
     */
    public void setElem1(ApplicationElement applElem) throws AoException {
        // update cache
        try {
            if (delegate.getElement1() != null && applElem == null) {
                this.corbaCache.removeApplicationRelationElem1(delegate.getElement1().getId(), delegate.getRelationName(), delegate.getRelNo());
            } else if (applElem != null) {
                long aid = ODSHelper.asJLong(applElem.getId());
                this.corbaCache.setApplicationRelationElem1(aid, this, delegate.getRelationName());
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getElem2()
     */
    public ApplicationElement getElem2() throws AoException {
        try {
            Relation invRelation = delegate.getInverseRelation();
            if (invRelation != null) {
                return corbaCache.getApplicationElementById(invRelation.getElement1().getId());
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setElem2(org.asam.ods.ApplicationElement)
     */
    public void setElem2(ApplicationElement applElem) throws AoException {
        corbaCache.setApplicationRelationElem2(delegate, ODSHelper.asJLong(applElem.getId()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationName()
     */
    public String getRelationName() throws AoException {
        try {
            return delegate.getRelationName();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationName(java.lang.String)
     */
    public void setRelationName(String arName) throws AoException {
        try {
            if (delegate.getAtfxElement1() != null) {
                delegate.getAtfxElement1().renameRelation(delegate.getRelNo(), delegate.getRelationName(), arName);
            } else {
                delegate.setRelationName(arName);
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationName()
     */
    public String getInverseRelationName() throws AoException {
        try {
            return delegate.getInverseRelationName();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setInverseRelationName(java.lang.String)
     */
    public void setInverseRelationName(String arInvName) throws AoException {
        try {
            AtfxRelation invRel = delegate.getInverseRelation();
            if (invRel != null) {
                delegate.getInverseRelation().getAtfxElement1().renameRelation(invRel.getRelNo(), invRel.getRelationName(), arInvName);
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationRange()
     */
    public RelationRange getRelationRange() throws AoException {
        try {
            return new RelationRange(delegate.getRelationRangeMin(), delegate.getRelationRangeMax());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationRange(org.asam.ods.RelationRange)
     */
    public void setRelationRange(RelationRange arRelationRange) throws AoException {
        try {
            delegate.setRelationRangeMin(arRelationRange.min);
            delegate.setRelationRangeMax(arRelationRange.max);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setInverseRelationRange(org.asam.ods.RelationRange)
     */
    public void setInverseRelationRange(RelationRange arRelationRange) throws AoException {
        try {
            delegate.getInverseRelation().setRelationRangeMin(arRelationRange.min);
            delegate.getInverseRelation().setRelationRangeMax(arRelationRange.max);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationship()
     */
    public Relationship getRelationship() throws AoException {
        try {
            com.peaksolution.openatfx.api.Relationship relship = delegate.getRelationship();
            if (relship != null) {
                return Relationship.from_int(relship.ordinal());
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationship()
     */
    public Relationship getInverseRelationship() throws AoException {
        return Relationship.from_int(delegate.getInverseRelation().getRelationship().ordinal());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationRange()
     */
    public RelationRange getInverseRelationRange() throws AoException {
        return new RelationRange(delegate.getInverseRelation().getRelationRangeMin(), delegate.getInverseRelation().getRelationRangeMax());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getRelationType()
     */
    public RelationType getRelationType() throws AoException {
        try {
            return delegate.getRelationType();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setRelationType(org.asam.ods.RelationType)
     */
    public void setRelationType(RelationType arRelationType) throws AoException {
        try {
            delegate.setRelationType(arRelationType);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
}
