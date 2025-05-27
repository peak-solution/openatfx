package com.peaksolution.openatfx.api;

import java.io.File;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.asam.ods.SetType;

public interface OpenAtfxAPI {
    
    BaseModel getBaseModel();
    
    void writeAtfx(File file);
    
    /***************************************************************************************
     * context
     ***************************************************************************************/
    
    /**
     * Sets the given value in the context. Overwrites any previously set value for that key. Note that not all context
     * values in OpenAtfx can be set, some are fixed and cannot be changed!
     * 
     * @param contextVariable the context value to set.
     * @throws OpenAtfxException if the value would overwrite a context value that has a fixed value in OpenAtfx.
     */
    void setContext(NameValueUnit contextVariable);
    
    /**
     * Returns the complete context value set.
     * 
     * @return all available context values.
     */
    Map<String, NameValueUnit> getContext();
    
    /**
     * Returns the value of the given context key or null if not found.
     * 
     * @param key the context key to get the value for.
     * @return the found context value or null.
     */
    NameValueUnit getContext(String key);
    
    /***************************************************************************************
     * methods for instance data
     ***************************************************************************************/
    ByteOrder getByteOrder(long aidExtComp, long iidExtComp);
    void setRelatedInstances(long aid, long iid, String relName, Collection<Long> instIds, SetType type);
    public void removeRelatedInstances(long aid, long iid, String relationName, Collection<Long> otherIids);
    List<Long> getRelatedInstanceIds(long aid, long iid, String relationName);
    Instance createInstance(long aid, Collection<NameValueUnit> values);
    Instance getInstanceById(long aid, long iid);
    Collection<Instance> getInstances(long aid);
    Collection<Instance> getInstances(long aid, Collection<Long> iids);
    Collection<Instance> getChildren(long aid, long iid);
    void removeInstance(long aid, long iid);
    void setAttributeValues(long aid, long iid, Collection<NameValueUnit> values);
    
    /**
     * List all instance attribute names.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return Collection of attribute names.
     */
    Collection<String> listInstanceAttributes(long aid, long iid);
    
    void setInstanceAttributeValue(long aid, long iid, NameValueUnit nvu);
    
    /**
     * Returns the unit id for given unit name, if found.
     * 
     * @param unitName The name of the unit.
     * @return the found unit id.
     * @throws OpenAtfxException on invalid unitName or not found id.
     */
    long getUnitId(String unitName);

    /**
     * Returns the unit name for the given unit ID, if found.
     * 
     * @param unitId The id of the unit.
     * @return the found unit name.
     * @throws OpenAtfxException on invalid unitId or not found name.
     */
    String getUnitName(long unitId);
    
    /***************************************************************************************
     * methods for model data
     ***************************************************************************************/
    
    /** Enumerations **/
    
    /**
     * Creates an empty enumeration with the given enumName.
     * 
     * @param enumName the name to give the new enumeration.
     * @return the created enumeration definition.
     * @throws OpenAtfxException if given enumName is null or empty or if an enumeration with the given name already
     *             exists.
     */
    EnumerationDefinition createEnumeration(String enumName);
    
    /**
     * Lists names of all known enumerations, including base enumerations if parameter is set to true.
     * 
     * @param includeBaseRelations true, if base relation names should be included in return value.
     * @return the names of all available enumerations.
     */
    Collection<String> listEnumerationNames(boolean includeBaseRelations);
    
    /**
     * Returns the definition object of the enumeration with given name, if found.
     * 
     * @param enumName the name of the enumeration to get.
     * @return the found enumeration definition or null.
     * @throws OpenAtfxException if given enumName is null or empty.
     */
    EnumerationDefinition getEnumerationDefinition(String enumName);
    
    /**
     * Returns the item name of the given enumeration item.
     *  
     * @param enumName the name of the enumeration.
     * @param item the item number.
     * @return the requested item's name or null if item has not been found.
     * @throws OpenAtfxException if no enumeration with given name has been found.
     */
    String getEnumerationItemName(String enumName, long item);
    
    /**
     * Adds an item/value entry to the enumeration with given enumName.
     * 
     * @param enumName the name of the enumeration to extend.
     * @param item the item (index) value.
     * @param value the text value.
     * @throws OpenAtfxException if a given value is empty or if given enumeration is a base enumeration
     */
    void addEnumerationItem(String enumName, long item, String value);
    
    /**
     * Removes the enumeration with the given enumName, if found.
     * 
     * @param enumName the name of the enumeration to remove.
     * @throws OpenAtfxException if given enumeration is a base enumeration.
     */
    void removeEnumeration(String enumName);
    
    /** Elements **/
    
    Element createElement(String basetype, String aeName);
    
    Collection<Element> getElements();
    Collection<Element> getElements(String pattern);
    
    Element getUniqueElementByBaseType(String aeType);
    Collection<Element> getElementsByBaseType(String aeType);
    Element getElementById(long aid);
    Element getElementByName(String aeName);
    String renameElement(long aid, String aeName);
    void removeElement(long aid);
    
    /** Attributes **/
    
    Attribute createAttributeFromBaseAttribute(long aid, String attrName, String baseAttrName);
    
    /**
     * Creates a new model attribute with given details.
     * 
     * @param aid
     * @param name
     * @param baseName
     * @param dataType
     * @param length
     * @param unitId
     * @param enumName
     * @param obligatory
     * @param unique
     * @param autogenerated
     * @return
     */
    Attribute createAttribute(long aid, String name, String baseName, DataType dataType, Integer length,
            long unitId, String enumName, Boolean obligatory, Boolean unique, Boolean autogenerated);
    
    /**
     * Creates a new model attribute with given details. Note that this method should only be used as convenience method
     * in the case when all unit instances are already known, to not have to identify the unit id first. For atfx
     * initialization cases call the method taking the unit id instead.
     * 
     * @param aid
     * @param name
     * @param baseName
     * @param dataType
     * @param length
     * @param unitName
     * @param enumName
     * @param obligatory
     * @param unique
     * @param autogenerated
     * @return
     */
    Attribute createAttribute(long aid, String name, String baseName, DataType dataType, Integer length,
            String unitName, String enumName, Boolean obligatory, Boolean unique, Boolean autogenerated);
    
    /**
     * Updates the values of the attribute with given name of element with given aid. Call this method if you do not
     * need to update the unit.
     * 
     * @param aid
     * @param name
     * @param dataType
     * @param length
     * @param enumName
     * @param obligatory
     * @param unique
     */
    void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName, Boolean obligatory,
            Boolean unique);
    
    /**
     * Updates the values of the attribute with given name of element with given aid. Call this method if you need to
     * update the unit and only have its new name.
     * 
     * @param aid
     * @param name
     * @param dataType
     * @param length
     * @param enumName
     * @param unitName
     * @param obligatory
     * @param unique
     */
    void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName, String unitName,
            Boolean obligatory, Boolean unique);
    
    /**
     * Updates the values of the attribute with given name of element with given aid. Call this method if you need to
     * update the unit and have its iid.
     * 
     * @param aid
     * @param name
     * @param dataType
     * @param length
     * @param enumName
     * @param unitId
     * @param obligatory
     * @param unique
     */
    void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName, long unitId,
            Boolean obligatory, Boolean unique);
    
    void renameAttribute(long aid, String oldAttrName, String newAttrName);
    void removeAttribute(long aid, String attrName);
    
    /** Relations **/
    
    Relation createRelationFromBaseRelation(long aidFrom, long aidTo, String baseRelationName, String relationName,
            String inverseRelationName);
    Relation createRelation(Element element1, Element element2, BaseRelation baseRelation, String relName,
            String inverseRelName, Short min, Short max);
    Relation getRelationByBaseName(long aid, String baseRelationName);
    Relation getRelationByName(long aid, String relationName);
    Relation getInverseRelation(long aid, String relationName);
    void removeRelation(long aid, String relName);
    
    /***************************************************************************************
     * methods for base model
     ***************************************************************************************/
    
    String getBaseModelVersion();
    
    BaseElement getBaseElement(String basetype);

    BaseRelation getBaseRelation(String baseType1, String baseType2);
}
