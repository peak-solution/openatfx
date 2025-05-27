package com.peaksolution.openatfx.api;

import java.util.Collection;


public interface Element {

    long getId();

    String getName();

    String getType();
    
    boolean isTopLevelElement();
    
    BaseElement getBaseElement();

    /**
     * Returns all attributes of this element. This method is a convenience access and identical to calling
     * getAttributes("*").
     * 
     * @return all attributes.
     */
    Collection<Attribute> getAttributes();

    /**
     * Returns all attributes of this element for which the name matches the given pattern.
     * 
     * @param pattern the attribute name pattern to match.
     * @return all matching attributes.
     */
    Collection<Attribute> getAttributes(String pattern);

    Integer getAttrNoByBaseName(String baName);

    Integer getAttrNoByName(String aaName);

    void removeAttribute(String aaName);

    Attribute getAttributeByNo(int attrNo);
    
    Attribute getAttributeByBaseName(String string);

    Attribute getAttributeByName(String aaName);
    
    /**
     * @param relation
     */
    void addRelation(Relation relation);

    Collection<Relation> getRelations();

    Relation getRelationByName(String relName);

    void removeRelation(String relationName);

    void updateBaseRelation(String relName, BaseRelation baseRelation);
}
