package de.rechner.openatfx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationHelper;
import org.asam.ods.ApplicationRelationPOA;
import org.asam.ods.BaseRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplicationRelation</code>.
 * 
 * @author Christian Rechner
 */
class ApplicationRelationImpl extends ApplicationRelationPOA {

    private static final Log LOG = LogFactory.getLog(ApplicationRelationImpl.class);

    private final POA modelPOA;
    private final AtfxCache atfxCache;

    private BaseRelation baseRelation;
    private ApplicationElement elem1;
    private RelationRange relationRange;
    private String relationName;
    private RelationType relationType;

    private ApplicationRelationImpl inverseRelation;

    /**
     * Constructor.
     * 
     * @param atfxCache The ATFX cache.
     */
    public ApplicationRelationImpl(POA modelPOA, AtfxCache atfxCache) {
        this.modelPOA = modelPOA;
        this.atfxCache = atfxCache;

        // default values as specified in ASAM ODS specification CH10
        this.baseRelation = null;
        this.elem1 = null;
        this.relationRange = new RelationRange((short) -2, (short) -2);
        this.relationName = "AUTOGEN";
        this.relationType = RelationType.INFO;
    }

    /**
     * Returns the inverse relation.
     * 
     * @return The inverse relation.
     */
    public ApplicationRelationImpl getInverseRelation() {
        return this.inverseRelation;
    }

    /**
     * Sets the inverse relation.
     * 
     * @param inverseRelation The inverse relation.
     */
    public void setInverseRelation(ApplicationRelationImpl inverseRelation) {
        this.inverseRelation = inverseRelation;
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
            if (this.relationRange.min == -2 && this.relationRange.max == -2) {
                setRelationRange(baseRel.getRelationRange());
            }
            this.relationType = baseRel.getRelationType();
        } else {
            this.relationType = RelationType.INFO;
        }

        // set base relation to inverse relation
        BaseRelation invBaseRel = null;
        if (baseRel != null) {
            invBaseRel = getInverseBaseRelation(baseRel);
            // only set the inverse relation range if there is none set
            if (getInverseRelationRange().min == -2 && getInverseRelationRange().max == -2) {
                setInverseRelationRange(invBaseRel.getRelationRange());
            }
            this.inverseRelation.relationType = baseRel.getRelationType();
        } else {
            this.relationType = RelationType.INFO;
        }

        this.baseRelation = baseRel;
        this.inverseRelation.baseRelation = invBaseRel;
    }

    /**
     * Returns the inverse relation for given base relation.
     * 
     * @param baseRel The base relation.
     * @return The inverse base relation.
     * @throws AoException Inverse base relation not found.
     */
    private static BaseRelation getInverseBaseRelation(BaseRelation baseRel) throws AoException {
        for (BaseRelation invBaseRel : baseRel.getElem2().getAllRelations()) {
            if (invBaseRel.getElem2().getType().equals(baseRel.getElem1().getType())
                    && invBaseRel.getElem1().getType().equals(baseRel.getElem2().getType())
                    && invBaseRel.getRelationName().equals(baseRel.getInverseRelationName())) {
                return invBaseRel;
            }
        }
        throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                              "Unable to find inverse relation for base relation: " + baseRel.getRelationName());
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
        try {
            ApplicationRelation rel = ApplicationRelationHelper.narrow(this.modelPOA.servant_to_reference(this));

            // update cache
            if (this.elem1 != null && applElem == null) {
                long aid = ODSHelper.asJLong(this.elem1.getId());
                this.atfxCache.removeApplicationRelationElem1(aid, rel);
            } else if (applElem != null) {
                long aid = ODSHelper.asJLong(applElem.getId());
                this.atfxCache.setApplicationRelationElem1(aid, rel);
            }
            this.elem1 = applElem;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getElem2()
     */
    public ApplicationElement getElem2() throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        return this.inverseRelation.getElem1();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setElem2(org.asam.ods.ApplicationElement)
     */
    public void setElem2(ApplicationElement applElem) throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        this.inverseRelation.setElem1(applElem);
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
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationName()
     */
    public String getInverseRelationName() throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        return this.inverseRelation.getRelationName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#setInverseRelationName(java.lang.String)
     */
    public void setInverseRelationName(String arInvName) throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        this.inverseRelation.setRelationName(arInvName);
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
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        this.inverseRelation.setRelationRange(arRelationRange);
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

        RelationRange relRange = getRelationRange();
        RelationRange invRelRange = getInverseRelationRange();

        // m:n or 1:1
        if (relRange.max == invRelRange.max) {
            return Relationship.INFO_REL;
        }
        // 0:n or 1:n
        else if (relRange.max == 1 && invRelRange.max == -1) {
            return Relationship.INFO_TO;
        }
        // m:0 or m:1
        else if (relRange.max == -1 && invRelRange.max == 1) {
            return Relationship.INFO_FROM;
        }

        throw new AoException(ErrorCode.AO_INVALID_RELATION_RANGE, SeverityFlag.ERROR, 0,
                              "Unable to obtain the relationship by relationrange " + relRange.min + ":"
                                      + relationRange.max + "=>" + invRelRange.min + ":" + invRelRange.max);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationship()
     */
    public Relationship getInverseRelationship() throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        return this.inverseRelation.getRelationship();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationRelationOperations#getInverseRelationRange()
     */
    public RelationRange getInverseRelationRange() throws AoException {
        if (this.inverseRelation == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Inverse relation not found!");
        }
        return this.inverseRelation.getRelationRange();
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
