package com.peaksolution.openatfx.api;

import java.util.Collection;

import org.asam.ods.RelationType;

public interface BaseElement {
    /**
     * Adds a new base attribute.
     * 
     * @param baseAttribute The base attribute to add.
     */
    public void addBaseAttribute(BaseAttribute baseAttribute);

    /**
     * Adds a new base relation.
     * 
     * @param baseRelation The base relation to add.
     */
    public void addBaseRelation(BaseRelation baseRelation);

    public String getType();

    public boolean isTopLevel();

    public Collection<BaseAttribute> getAttributes(String baPattern);
    
    public BaseAttribute getAttributeByName(String baName);

    public BaseRelation[] getRelations();

    public String[] listRelatedElementsByRelationship(Relationship brRelationship);

    public BaseElement[] getRelatedElementsByRelationship(Relationship brRelationship);

    public BaseRelation[] getRelationsByType(RelationType brRelationType);
    
    public BaseRelation getRelationByName(String baseRelationName, String targetBaseType);
}
