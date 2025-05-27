package com.peaksolution.openatfx.basestructure;

import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseElementPOA;
import org.asam.ods.BaseRelation;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;

import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.BaseElement</code>.
 * 
 * @author Christian Rechner
 */
class BaseElementImpl extends BaseElementPOA {

    private final com.peaksolution.openatfx.api.BaseElement delegate;
    private final List<BaseAttribute> baseAttributes;
    private final List<BaseRelation> baseRelations;

    /**
     * Constructor.
     * 
     * @param delegate The atfx model element delegate.
     */
    public BaseElementImpl(OpenAtfxAPI api, com.peaksolution.openatfx.api.BaseElement delegate) {
        this.delegate = delegate;
        this.baseAttributes = new ArrayList<BaseAttribute>();
        this.baseRelations = new ArrayList<BaseRelation>();
    }

    /**
     * Adds a new base attribute.
     * 
     * @param baseAttribute The base attribute to add.
     */
    public void addBaseAttribute(BaseAttribute baseAttribute) {
        this.baseAttributes.add(baseAttribute);
    }

    /**
     * Adds a new base relation.
     * 
     * @param baseRelation The base relation to add.
     */
    public void addBaseRelation(BaseRelation baseRelation) {
        this.baseRelations.add(baseRelation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#getType()
     */
    public String getType() throws AoException {
        return delegate.getType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#isTopLevel()
     */
    public boolean isTopLevel() throws AoException {
        return delegate.isTopLevel();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#listAttributes(java.lang.String)
     */
    public String[] listAttributes(String baPattern) throws AoException {
        return delegate.getAttributes(baPattern).stream().map(com.peaksolution.openatfx.api.BaseAttribute::getName)
                       .toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#getAttributes(java.lang.String)
     */
    public BaseAttribute[] getAttributes(String baPattern) throws AoException {
        List<BaseAttribute> list = new ArrayList<BaseAttribute>();
        for (BaseAttribute baseAttr : this.baseAttributes) {
            String name = baseAttr.getName();
            if (PatternUtil.nameFilterMatchCI(name, baPattern)) {
                list.add(baseAttr);
            }
        }
        return list.toArray(new BaseAttribute[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#getAllRelations()
     */
    public BaseRelation[] getAllRelations() throws AoException {
        List<BaseRelation> list = new ArrayList<BaseRelation>();
        for (BaseRelation baseRelation : this.baseRelations) {
            list.add(baseRelation);
        }
        return list.toArray(new BaseRelation[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#listRelatedElementsByRelationship(org.asam.ods.Relationship)
     */
    public String[] listRelatedElementsByRelationship(Relationship brRelationship) throws AoException {
        List<String> list = new ArrayList<>();
        for (BaseElement baseElement : getRelatedElementsByRelationship(brRelationship)) {
            list.add(baseElement.getType());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#getRelatedElementsByRelationship(org.asam.ods.Relationship)
     */
    public BaseElement[] getRelatedElementsByRelationship(Relationship brRelationship) throws AoException {
        List<BaseElement> list = new ArrayList<>();
        for (BaseRelation baseRelation : this.baseRelations) {
            if (brRelationship == Relationship.ALL_REL) {
                list.add(baseRelation.getElem2());
            } else if (brRelationship == Relationship.INFO_REL) {
                if (baseRelation.getRelationship() == Relationship.INFO_FROM
                        || baseRelation.getRelationship() == Relationship.INFO_TO) {
                    list.add(baseRelation.getElem2());
                }
            } else {
                if (baseRelation.getRelationship() == brRelationship) {
                    list.add(baseRelation.getElem2());
                }
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseElementOperations#getRelationsByType(org.asam.ods.RelationType)
     */
    public BaseRelation[] getRelationsByType(RelationType brRelationType) throws AoException {
        List<BaseRelation> list = new ArrayList<BaseRelation>();
        for (BaseRelation baseRelation : this.baseRelations) {
            if (baseRelation.getRelationType() == brRelationType) {
                list.add(baseRelation);
            }
        }
        return list.toArray(new BaseRelation[0]);
    }

}
