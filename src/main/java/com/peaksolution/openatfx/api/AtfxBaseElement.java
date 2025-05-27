package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.asam.ods.ErrorCode;
import org.asam.ods.RelationType;

import com.peaksolution.openatfx.util.PatternUtil;


/**
 * OpenAtfx base element.
 * 
 * @author Markus Renner
 */
public class AtfxBaseElement implements BaseElement {

    private final String type;
    private final boolean topLevel;
    private final List<BaseAttribute> baseAttributes;
    private final List<BaseRelation> baseRelations;

    /**
     * Constructor.
     * 
     * @param type The base type.
     * @param topLevel Whether the element is top level.
     */
    public AtfxBaseElement(String type, boolean topLevel) {
        this.type = type;
        this.topLevel = topLevel;
        this.baseAttributes = new ArrayList<>();
        this.baseRelations = new ArrayList<>();
    }

    /**
     * Adds a new base attribute.
     * 
     * @param baseAttribute The base attribute to add.
     */
    @Override
    public void addBaseAttribute(BaseAttribute baseAttribute) {
        this.baseAttributes.add(baseAttribute);
    }

    /**
     * Adds a new base relation.
     * 
     * @param baseRelation The base relation to add.
     */
    @Override
    public void addBaseRelation(BaseRelation baseRelation) {
        boolean relationExtended = false;
        // supports correct setup of relations even for base model read from older custom base model xml files, where
        // relations were defined multiple times if they had more than one possible target element
        for (BaseRelation existingRel : baseRelations) {
            if (existingRel.getName().equalsIgnoreCase(baseRelation.getName())
                    && existingRel.getInverseName().equalsIgnoreCase(baseRelation.getInverseName())
                    && existingRel instanceof AtfxBaseRelation) {
                ((AtfxBaseRelation) existingRel).addElement2(baseRelation.getElem2());
                relationExtended = true;
            }
        }
        if (!relationExtended) {
            this.baseRelations.add(baseRelation);
        }
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public boolean isTopLevel() {
        return this.topLevel;
    }

    @Override
    public Collection<BaseAttribute> getAttributes(String baPattern) {
        List<BaseAttribute> list = new ArrayList<>();
        for (BaseAttribute baseAttr : this.baseAttributes) {
            String name = baseAttr.getName();
            if (PatternUtil.nameFilterMatchCI(name, baPattern)) {
                list.add(baseAttr);
            }
        }
        return list;
    }
    
    /**
     *
     */
    public BaseAttribute getAttributeByName(String baName) {
        if (baName != null && !baName.isBlank()) {
            for (BaseAttribute baseAttr : this.baseAttributes) {
                String name = baseAttr.getName();
                if (baName.equalsIgnoreCase(name)) {
                    return baseAttr;
                }
            }
        }
        return null;
    }

    @Override
    public BaseRelation[] getRelations() {
        List<BaseRelation> list = new ArrayList<>();
        for (BaseRelation baseRelation : this.baseRelations) {
            list.add(baseRelation);
        }
        return list.toArray(new BaseRelation[0]);
    }

    @Override
    public String[] listRelatedElementsByRelationship(Relationship brRelationship) {
        List<String> list = new ArrayList<String>();
        for (BaseElement baseElement : getRelatedElementsByRelationship(brRelationship)) {
            list.add(baseElement.getType());
        }
        return list.toArray(new String[0]);
    }

    @Override
    public BaseElement[] getRelatedElementsByRelationship(Relationship brRelationship) {
        List<BaseElement> list = new ArrayList<>();
        for (BaseRelation baseRelation : this.baseRelations) {
            if (brRelationship == Relationship.ALL_REL) {
                list.addAll(baseRelation.getElem2());
            } else if (brRelationship == Relationship.INFO_REL) {
                if (baseRelation.getRelationship() == Relationship.INFO_FROM
                        || baseRelation.getRelationship() == Relationship.INFO_TO) {
                    list.addAll(baseRelation.getElem2());
                }
            } else {
                if (baseRelation.getRelationship() == brRelationship) {
                    list.addAll(baseRelation.getElem2());
                }
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    @Override
    public BaseRelation[] getRelationsByType(RelationType brRelationType) {
        List<BaseRelation> list = new ArrayList<>();
        for (BaseRelation baseRelation : this.baseRelations) {
            if (baseRelation.getRelationType() == brRelationType) {
                list.add(baseRelation);
            }
        }
        return list.toArray(new BaseRelation[0]);
    }
    
    @Override
    public BaseRelation getRelationByName(String baseRelationName, String targetBaseType) {
        for (BaseRelation currentRel : baseRelations) {
            if (currentRel.getName().equalsIgnoreCase(baseRelationName)) {
                if (targetBaseType == null) {
                    if (currentRel.getElem2().size() == 1) {
                        return currentRel;
                    } else {
                        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Requested base relation '"
                                + baseRelationName + "' from base element '" + type
                                + "' without providing target base name, but this relation has more than one possible target element!");
                    }
                } else {
                    for (BaseElement currentElem2 : currentRel.getElem2()) {
                        if (currentElem2.getType().equalsIgnoreCase(targetBaseType)) {
                            return currentRel;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "AtfxBaseElement [type=" + type + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtfxBaseElement other = (AtfxBaseElement) obj;
        return Objects.equals(type, other.type);
    }
}
