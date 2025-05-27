package com.peaksolution.openatfx.api;

import java.util.Collection;

public interface Instance {

    Element getElement();
    
    String getElementName();
    
    long getAid();
    
    long getIid();
    
    void setName(String ieName);
    
    String getName();

    void setAttributeValue(NameValueUnit nvu);
    
    NameValueUnit getValue(int attrNo);
    
    NameValueUnit getValue(String aaName);
    
    Collection<NameValueUnit> getValues(boolean includeAllODSValues);
    
    NameValueUnit getValueByBaseName(String baName);

    Collection<String> listInstanceAttributes();
    
    Collection<NameValueUnit> getInstanceAttributes();
    
    NameValueUnit getInstanceAttribute(String attrName);
    
    void renameInstanceAttribute(String oldName, String newName);
    
    void setInstanceValue(NameValueUnit nvu);

    boolean doesAttributeExist(String aaName, String baName, boolean isRequired);

    void setAttributeValues(Collection<NameValueUnit> attrsList);
    
    void addRelationValue(Relation relation, Collection<Long> otherIids);

    Collection<Instance> getRelatedInstancesByRelationship(Relationship child, String string);
    
    Boolean removeRelatedIids(Relation applRel, Collection<Long> iidsToRemove);

    boolean hasValidValue(String attrName, String baseAttrName);
}
