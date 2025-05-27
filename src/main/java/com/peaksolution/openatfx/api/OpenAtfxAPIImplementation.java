package com.peaksolution.openatfx.api;

import java.io.File;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.asam.ods.ErrorCode;
import org.asam.ods.SetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.util.PatternUtil;


public class OpenAtfxAPIImplementation implements OpenAtfxAPI {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAtfxAPIImplementation.class);
    private static final String ERROR_MSG_NO_VALID_AID = " is no valid AID in this model!";

    /** context variables */
    private final Map<String, NameValueUnit> context;

    private AtfxCache atfxCache;
    private final BaseModel baseModel;

    public OpenAtfxAPIImplementation(BaseModel baseModel) {
        this.baseModel = baseModel;
        this.context = new LinkedHashMap<>();
    }

    public void init(Collection<NameValueUnit> context) {
        ExtCompReader extCompReader = new ExtCompReader(this);
        ExtCompWriter extCompWriter = new ExtCompWriter(this);

        this.atfxCache = new AtfxCache(baseModel, extCompReader, extCompWriter);
        for (NameValueUnit nvu : context) {
            initContext(nvu);
        }
    }
    
    @Override
    public BaseModel getBaseModel() {
        return baseModel;
    }
    
    @Override
    public void writeAtfx(File file) {
        AtfxWriter.getInstance().writeXML(file, this);
    }

    /***********************************************************************************
     * context
     ***********************************************************************************/

    @Override
    public Map<String, NameValueUnit> getContext() {
        return context;
    }

    @Override
    public NameValueUnit getContext(String key) {
        return context.get(key);
    }

    /**
     * Call this method to initialize the context without checking overwrite. Use for setting of static context only!
     * 
     * @param contextVariable
     */
    void initContext(NameValueUnit contextVariable) {
        this.getContext().put(contextVariable.getValName(), new NameValueUnit(contextVariable));
    }

    @Override
    public void setContext(NameValueUnit contextVariable) {
        String valName = contextVariable.getValName();
        // check if readonly context
        if (ApiFactory.STATIC_CONTEXT.containsKey(valName) && this.getContext().containsKey(valName)) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR,
                                        "Context '" + valName + "' is readonly");
        }
        this.getContext().put(valName, new NameValueUnit(contextVariable));

        // set extended compatibility mode in atfx cache
        if (OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE.equals(valName)) {
            NameValueUnit nvu = getContext().get(OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE);
            boolean compatibilityMode = false;
            if (DataType.DT_STRING == nvu.getValue().discriminator()) {
                compatibilityMode = Boolean.parseBoolean(nvu.getValue().stringVal());
            } else if (DataType.DT_BOOLEAN == nvu.getValue().discriminator()) {
                compatibilityMode = nvu.getValue().booleanVal();
            }
            atfxCache.setExtendedCompatibilityMode(compatibilityMode);
        }
    }

    public void removeContext(String varPattern) {
        // check if readonly context should be removed
        for (NameValueUnit nv : this.getContext().values()) {
            if (PatternUtil.nameFilterMatch(nv.getValName(), varPattern)
                    && ApiFactory.STATIC_CONTEXT.containsKey(nv.getValName())) {
                throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR,
                                            "Unable to remove readonly context '" + nv.getValName() + "'");
            }
        }
        // remove matching context
        List<String> toRemove = new ArrayList<>();
        for (NameValueUnit nv : this.getContext().values()) {
            if (PatternUtil.nameFilterMatch(nv.getValName(), varPattern)) {
                toRemove.add(nv.getValName());
            }
        }
        for (String valName : toRemove) {
            this.getContext().remove(valName);
        }
    }

    /***************************************************************************************
     * methods for model data
     ***************************************************************************************/

    @Override
    public String getBaseModelVersion() {
        return baseModel.getVersion();
    }

    @Override
    public BaseElement getBaseElement(String basetype) {
        for (BaseElement currentBaseElement : baseModel.getElements("*")) {
            if (basetype.equalsIgnoreCase(currentBaseElement.getType())) {
                return currentBaseElement;
            }
        }
        return null;
    }

    @Override
    public BaseRelation getBaseRelation(String baseType1, String baseType2) {
        for (BaseRelation rel : getBaseElement(baseType1).getRelations()) {
            for (BaseElement elem2 : rel.getElem2()) {
                if (elem2.getType().equalsIgnoreCase(baseType2)) {
                    return rel;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<String> listEnumerationNames(boolean includeBaseRelations) {
        Collection<String> enumNames = new TreeSet<>();
        if (includeBaseRelations) {
            baseModel.getEnumerations().stream().map(EnumerationDefinition::getName).forEach(enumNames::add);
        }
        atfxCache.getEnumerations().stream().map(EnumerationDefinition::getName).forEach(enumNames::add);
        return enumNames;
    }

    @Override
    public EnumerationDefinition getEnumerationDefinition(String enumName) {
        EnumerationDefinition foundEnum = baseModel.getEnumDef(enumName);
        if (foundEnum == null) {
            foundEnum = atfxCache.getEnumeration(enumName);
        }
        return foundEnum;
    }

    @Override
    public String getEnumerationItemName(String enumName, long item) {
        EnumerationDefinition enumDef = getEnumerationDefinition(enumName);
        if (enumDef == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No enumeration found with name " + enumName);
        }

        return enumDef.getItemName(item);
    }

    @Override
    public EnumerationDefinition createEnumeration(String enumName) {
        // check for existing enum name
        EnumerationDefinition foundBaseEnum = baseModel.getEnumDef(enumName);
        if (foundBaseEnum != null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "Base enumeration with name '" + enumName
                    + "' exists, cannot create enumeration!");
        }
        return atfxCache.createEnumeration(enumName);
    }

    @Override
    public void addEnumerationItem(String enumName, long value, String name) {
        EnumerationDefinition foundEnum = baseModel.getEnumDef(enumName);
        if (foundEnum != null) {
            throw new OpenAtfxException(ErrorCode.AO_ACCESS_DENIED, "Changing base enumerations is not allowed");
        }
        atfxCache.addEnumerationItem(enumName, value, name);
    }

    @Override
    public void removeEnumeration(String enumName) {
        EnumerationDefinition baseEnum = baseModel.getEnumDef(enumName);
        if (baseEnum != null) {
            throw new OpenAtfxException(ErrorCode.AO_ACCESS_DENIED,
                                        "Removing base enumeration '" + enumName + "' is not allowed!");
        }

        atfxCache.removeEnumeration(enumName);
    }

    @Override
    public Collection<Element> getElements() {
        Collection<Element> elements = new ArrayList<>();
        elements.addAll(atfxCache.getElements());
        return elements;
    }
    
    @Override
    public Collection<Element> getElements(String pattern) {
        Collection<Element> elements = new ArrayList<>();
        for (Element currentElement : atfxCache.getElements()) {
            if (PatternUtil.nameFilterMatch(currentElement.getName(), pattern)) {
                elements.add(currentElement);
            }
        }
        return elements;
    }

    @Override
    public Element createElement(String basetype, String aeName) throws OpenAtfxException {
        return createAtfxElement(basetype, aeName);
    }
    
    @Override
    public String renameElement(long aid, String aeName) {
        return atfxCache.renameElement(aid, aeName);
    }

    @Override
    public Attribute createAttributeFromBaseAttribute(long aid, String attrName, String baseAttrName) {
        AtfxElement element = atfxCache.getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "createAttributeFromBaseAttribute(): " + aid + ERROR_MSG_NO_VALID_AID);
        }
        BaseElement baseElement = baseModel.getElementByType(element.getType());
        Collection<BaseAttribute> baseAttrs = baseElement.getAttributes(baseAttrName);
        if (baseAttrs.size() == 1) {
            return element.createAttributeFromBaseAttribute(baseAttrs.iterator().next(), attrName);
        }

        throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                    "'" + baseAttrName + "' specifies no valid base attribute for " + element + "!");
    }

    @Override
    public Attribute createAttribute(long aid, String name, String baseName, DataType dataType, Integer length,
            String unitName, String enumName, Boolean obligatory, Boolean unique, Boolean autogenerated) {
        return createAttribute(aid, name, baseName, dataType, length, unitName, null, enumName, obligatory, unique, autogenerated);
    }
    
    @Override
    public Attribute createAttribute(long aid, String name, String baseName, DataType dataType, Integer length,
            long unitId, String enumName, Boolean obligatory, Boolean unique, Boolean autogenerated) {
        return createAttribute(aid, name, baseName, dataType, length, null, unitId, enumName, obligatory, unique, autogenerated);
    }
    
    private Attribute createAttribute(long aid, String name, String baseName, DataType dataType, Integer length,
            String unitName, Long unitId, String enumName, Boolean obligatory, Boolean unique, Boolean autogenerated) {
        boolean isObligatory = obligatory != null && obligatory;
        boolean isUnique = unique != null && unique;
        boolean isAutogenerated = autogenerated != null && autogenerated;
        
        BaseAttribute baseAttribute = null;
        if (baseName != null && !baseName.isBlank()) {
            AtfxElement element = atfxCache.getElementById(aid);
            if (element != null) {
                baseAttribute = element.getBaseElement().getAttributeByName(baseName);
            }
        }
        
        return atfxCache.createAttribute(aid, name, baseAttribute, dataType, unitName, unitId, enumName, length, isObligatory, isUnique,
                                  isAutogenerated);
    }

    @Override
    public void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName,
            String unitName, Boolean obligatory, Boolean unique) {
        updateAttribute(aid, name, dataType, length, enumName, unitName, null, true, obligatory, unique);
    }
    
    @Override
    public void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName,
            long unitId, Boolean obligatory, Boolean unique) {
        updateAttribute(aid, name, dataType, length, enumName, null, unitId, true, obligatory, unique);
    }
    
    @Override
    public void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName,
            Boolean obligatory, Boolean unique) {
        updateAttribute(aid, name, dataType, length, enumName, null, null, false, obligatory, unique);
    }
    
    private void updateAttribute(long aid, String name, DataType dataType, Integer length, String enumName,
            String unitName, Long unitId, boolean updateUnit, Boolean obligatory, Boolean unique) {
        Element element = atfxCache.getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, aid + ERROR_MSG_NO_VALID_AID);
        }
        Attribute existingAttribute = element.getAttributeByName(name);
        if (existingAttribute == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Attribute '" + name + "' not found for " + element
                    + "! Use renameAttribute() in case you tried to change the name!");
        }

        boolean isBaseAttributeDerived = existingAttribute.getBaseName() != null;

        DataType newDataType = dataType;
        if (newDataType != DataType.DT_UNKNOWN && existingAttribute.getDataType() != newDataType) {
            DataType oldDataType = existingAttribute.getDataType();
            existingAttribute.setDataType(newDataType);
            LOG.debug("Changed DataType of {} from {} to {}", existingAttribute, oldDataType, newDataType);
        }

        if (obligatory != null && !existingAttribute.isObligatory() == obligatory) {
            if (isBaseAttributeDerived && existingAttribute.isObligatory()) {
                LOG.warn("Tried to reduce obligatory flag of {}, but that is not allowed for attributes derived from a base attribute!",
                         existingAttribute);
            } else {
                existingAttribute.setObligatory(obligatory);
                LOG.debug("Changed obligatory flag of {} to {}", existingAttribute, obligatory);
            }
        }

        if (unique != null && !existingAttribute.isUnique() == unique) {
            if (isBaseAttributeDerived) {
                LOG.warn("Tried to change unique flag of {}, but that is not allowed for attributes derived from a base attribute!",
                         existingAttribute);
            } else {
                existingAttribute.setUnique(unique);
                LOG.debug("Changed unique flag of {} to {}", existingAttribute, unique);
            }
        }

        if (length != null && length > 0) {
            int oldLength = existingAttribute.getLength();
            existingAttribute.setLength(length);
            LOG.debug("Changed length of {} from {} to {}", existingAttribute, oldLength, length);
        }

        if (enumName != null && !enumName.isBlank()) {
            String oldEnumName = existingAttribute.getEnumName();
            if (isBaseAttributeDerived) {
                if (!oldEnumName.equals(enumName)) {
                    LOG.warn("Tried to re-define enumeration '{}' of {} with enum name '{}', but that is not allowed for attributes derived from a base attribute!",
                         oldEnumName, existingAttribute, enumName);
                }
            } else {
                existingAttribute.setEnumName(enumName);
                LOG.debug("Changed enumName of {} from {} to {}", existingAttribute, oldEnumName, enumName);
            }
        }

        if (updateUnit) {
            long finalUnitId = 0;
            if (unitId != null) {
                finalUnitId = unitId;
            } else if (unitName != null && !unitName.isBlank()) {
                finalUnitId = getUnitId(unitName);
            }
            long oldUnitId = existingAttribute.getUnitId();
            existingAttribute.setUnitId(finalUnitId);
            if (oldUnitId != finalUnitId) {
                LOG.debug("Changed unitId of {} from {} to {}", existingAttribute, oldUnitId, finalUnitId);
            }
        }
    }
    
    @Override
    public void renameAttribute(long aid, String oldAttrName, String newAttrName) {
        Integer attrNo = atfxCache.getAttrNoByName(aid, oldAttrName);
        atfxCache.renameAttribute(aid, attrNo, oldAttrName, newAttrName);
    }
    
    @Override
    public void removeAttribute(long aid, String attrName) {
        atfxCache.removeAttribute(aid, attrName);
    }

    @Override
    public Element getElementById(long aid) {
        return atfxCache.getElementById(aid);
    }

    @Override
    public Element getElementByName(String aeName) throws OpenAtfxException {
        return atfxCache.getElementByName(aeName);
    }

    @Override
    public Collection<Element> getElementsByBaseType(String aeType) throws OpenAtfxException {
        List<Element> list = new ArrayList<>();
        for (Element applElem : this.atfxCache.getElements()) {
            if (PatternUtil.nameFilterMatchCI(applElem.getType(), aeType)) {
                list.add(applElem);
            }
        }
        return list;
    }

    @Override
    public Element getUniqueElementByBaseType(String aeType) {
        Collection<Element> aes = getElementsByBaseType(aeType);
        if (aes.size() != 1) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR,
                                        "None or multiple application elements of type '" + aeType + "' found");
        }
        return aes.iterator().next();
    }
    
    @Override
    public void removeElement(long aid) {
        this.atfxCache.removeApplicationElement(aid);
    }

    public Attribute getAttribute(long aid, String aaName) {
        return getElementById(aid).getAttributeByName(aaName);
    }

    @Override
    public Relation getRelationByBaseName(long aid, String baseRelationName) {
        return atfxCache.getModelRelationByBaseName(aid, baseRelationName);
    }

    @Override
    public Relation getRelationByName(long aid, String relationName) {
        return atfxCache.getModelRelationByName(aid, relationName);
    }
    
    @Override
    public Relation getInverseRelation(long aid, String relationName) {
        Relation orgRelation = getRelationByName(aid, relationName);
        if (orgRelation == null) {
            Element element = getElementById(aid);
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Could not find relation with name '" + relationName
                    + "' at " + element + " to get inverse relation for!");
        }
        return orgRelation.getInverseRelation();
    }

    @Override
    public Relation createRelationFromBaseRelation(long aidFrom, long aidTo, String baseRelationName,
            String relationName, String inverseRelationName) {
        Element fromElement = getElementById(aidFrom);
        Element toElement = getElementById(aidTo);
        if (fromElement == null || fromElement.getType() == null || fromElement.getType().isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No base element found for aid " + aidFrom
                    + " to create the relation '" + relationName + "' from.");
        }
        Relation alreadyExistingRelation = fromElement.getRelationByName(relationName);
        if (alreadyExistingRelation != null) {
            LOG.debug("Relation '{}' to create at {} already exists, creation skipped and existing relation returned.", relationName, fromElement);
            return alreadyExistingRelation;
        }

        // create relation
        BaseElement fromBaseElement = getBaseElement(fromElement.getType());
        BaseRelation baseRelation = null;
        for (BaseRelation baseRel : fromBaseElement.getRelations()) {
            if (baseRel.getName().equalsIgnoreCase(baseRelationName)) {
                baseRelation = baseRel;
                break;
            }
        }
        if (baseRelation == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No base relation '" + baseRelationName
                    + "' found to create relation at " + fromElement);
        }

        AtfxRelation createdRelation = atfxCache.addModelRelation(relationName, baseRelation, fromElement.getId(),
                                                                        toElement == null ? 0 : toElement.getId(),
                                                                        baseRelation.getRelationship(),
                                                                        baseRelation.getRelationRange().min,
                                                                        baseRelation.getRelationRange().max,
                                                                        inverseRelationName);

        if (toElement != null) {
            // create inverse relation
            BaseElement toBaseElement = getBaseElement(toElement.getType());
            BaseRelation inverseBaseRelation = null;
            for (BaseRelation baseRel : toBaseElement.getRelations()) {
                if (baseRel.getInverseName(toBaseElement.getType()).equalsIgnoreCase(baseRelationName)) {
                    inverseBaseRelation = baseRel;
                    break;
                }
            }
            if (inverseBaseRelation == null) {
                throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Did not find any inverse base relation for " + baseRelation + " at " + toElement);
            }
            atfxCache.addModelRelation(inverseRelationName, inverseBaseRelation, toElement.getId(),
                                             fromElement.getId(), inverseBaseRelation.getRelationship(),
                                             inverseBaseRelation.getRelationRange().min,
                                             inverseBaseRelation.getRelationRange().max, relationName);
        }

        return createdRelation;
    }

    @Override
    public Relation createRelation(Element element1, Element element2, BaseRelation baseRelation, String relName,
            String inverseRelName, Short min, Short max) {
        return createAtfxRelation(element1, element2, baseRelation, relName, inverseRelName, min, max);
    }
    
    @Override
    public void removeRelation(long aid, String relName) {
        Relation relationToRemove = getRelationByName(aid, relName);
        if (relationToRemove == null) {
            Element parentElement = getElementById(aid);
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                        "Relation with name '" + relName + "' to remove not found at " + parentElement);
        }
        
        atfxCache.removeModelRelation(relationToRemove);
    }

    /***************************************************************************************
     * methods for instance data
     ***************************************************************************************/

    @Override
    public long getUnitId(String unitName) {
        if (unitName == null || unitName.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "unitName in call to getUnitId() may not be empty!");
        }
        long unitId = atfxCache.getUnitId(unitName);
        if (unitId >= 0) {
            return unitId;
        }
        throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "No mapped unitId found for name '" + unitName + "'!");
    }
    
    @Override
    public String getUnitName(long unitId) {
        if (unitId == 0) {
            return "";
        }
        
        if (unitId < 0) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "unitId in call to getUnitName() may not be < 0!");
        }
        String unitName = atfxCache.getUnitString(unitId);
        if (unitName != null) {
            return unitName;
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No mapped unitName found for id " + unitId + "!");
    }

    @Override
    public void setRelatedInstances(long aid, long iid, String relName, Collection<Long> instIds, SetType type) {
        // check 'ElemId'
        if (!this.atfxCache.instanceExists(aid, iid)) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                        "Instance not found ElemId aid=" + aid + ",iid=" + iid);
        }

        // check 'relName'
        AtfxRelation applRel = this.atfxCache.getModelRelationByName(aid, relName);
        if (applRel == null) {
            LOG.warn("Relation not found: aid={}, relName={}", aid, relName);
            return;
        }

        Element relatedElement = applRel.getElement2();
        long otherAid = relatedElement.getId();

        // check 'instIds' and create relation
        if (type == SetType.INSERT || type == SetType.UPDATE || type == SetType.APPEND) {
            Collection<Long> otherIidsToSet = new ArrayList<>(instIds.size());
            for (Long otherIid : instIds) {
                if (!this.atfxCache.instanceExists(otherAid, otherIid)) { // throw not found error
                    String sourceAeName = atfxCache.getElementNameById(aid);
                    String targetAeName = atfxCache.getElementNameById(otherAid);
                    throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                                "Target InstanceElement not found: Source[aid=" + aid + ",aeName="
                                                        + sourceAeName + ",iid=" + iid + "] -> Target[aid=" + otherAid
                                                        + ",aeName=" + targetAeName + ",iid=" + otherIid + "]");
                }
                otherIidsToSet.add(otherIid);
            }
            this.atfxCache.connectInstances(aid, iid, applRel, otherIidsToSet);
        }
        // remove relations
        else if (type == SetType.REMOVE) {
            Collection<Long> otherIidsToRemove = new ArrayList<>(instIds.size());
            for (Long otherIid : instIds) {
                otherIidsToRemove.add(otherIid);
            }
            this.atfxCache.removeInstanceRelations(aid, iid, applRel, otherIidsToRemove);
        }
    }
    
    @Override
    public void removeRelatedInstances(long aid, long iid, String relationName, Collection<Long> otherIids) {
        Instance instance = getInstanceById(aid, iid);
        if (instance == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Instance with aid=" + aid + " and iid=" + iid + " not found");
        }
        Relation relation = instance.getElement().getRelationByName(relationName);
        instance.removeRelatedIids(relation, otherIids);
        
        for (Long otherIid : otherIids) {
            Instance otherInstance = getInstanceById(relation.getElement2().getId(), otherIid);
            otherInstance.removeRelatedIids(relation.getInverseRelation(), Arrays.asList(iid));
        }
    }

    @Override
    public Instance createInstance(long aid, Collection<NameValueUnit> values) {
        Integer idAttrNo = atfxCache.getAttrNoByBaName(aid, "id");
        if (idAttrNo == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                        "No application attribute of base attribute 'id' found for aid=" + aid);
        }
        Attribute idAttribute = atfxCache.getAttribute(aid, idAttrNo);
        boolean idValueContained = false;
        
        for (NameValueUnit nvu : values) {
            if (nvu.getValName().equalsIgnoreCase(idAttribute.getName())) {
                idValueContained = true;
                break;
            }
        }

        Collection<NameValueUnit> nvus = new ArrayList<>(values);
        if (!idValueContained) {
            long iid = this.atfxCache.nextIid(aid);
            NameValueUnit idNvu = new NameValueUnit(idAttribute.getName(), idAttribute.getDataType(), iid);
            nvus.add(idNvu);
        }
        return atfxCache.addInstance(aid, nvus);
    }

    @Override
    public Collection<Instance> getInstances(long aid) {
        return new ArrayList<>(atfxCache.getInstances(aid));
    }

    @Override
    public AtfxInstance getInstanceById(long aid, long iid) {
        AtfxInstance ie = this.atfxCache.getInstance(aid, iid);
        if (ie == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                        "Instance with aid=" + aid + " and iid=" + iid + " not found");
        }
        return ie;
    }

    @Override
    public Collection<Instance> getInstances(long aid, Collection<Long> iids) {
        Collection<Instance> instances = new ArrayList<>();
        for (Long iid : iids) {
            instances.add(this.atfxCache.getInstance(aid, iid));
        }
        return instances;
    }

    @Override
    public void removeInstance(long aid, long iid) {
        atfxCache.removeInstance(aid, iid);
    }

    @Override
    public ByteOrder getByteOrder(long aidExtComp, long iidExtComp) {
        return atfxCache.getByteOrder(aidExtComp, iidExtComp);
    }
    
    public List<Long> getRelatedInstanceIds(long aid, long iid, Relation relation) {
        return atfxCache.getRelatedInstanceIds(aid, iid, relation);
    }
    
    public List<Long> getRelatedInstanceIds(long aid, Collection<Long> iids, Relation relation) {
        Set<Long> relIids = new HashSet<>();
        for (Long iid : iids) {
            relIids.addAll(atfxCache.getRelatedInstanceIds(aid, iid, relation));
        }
        return new ArrayList<>(relIids);
    }

    @Override
    public List<Long> getRelatedInstanceIds(long aid, long iid, String relationName) {
        return atfxCache.getRelatedInstanceIds(aid, iid, getRelationByName(aid, relationName));
    }

    @Override
    public Collection<Instance> getChildren(long aid, long iid) {
        Element element = getElementById(aid);
        Collection<Relation> childRelations = new ArrayList<>();
        for (Relation relation : element.getRelations()) {
            if (Relationship.CHILD == relation.getRelationship()) {
                childRelations.add(relation);
            }
        }

        Collection<Instance> children = new ArrayList<>();
        for (Relation childRelation : childRelations) {
            List<Long> relatedIids = getRelatedInstanceIds(aid, iid, childRelation);
            for (Long childIid : relatedIids) {
                if (childIid != null) {
                    children.add(getInstanceById(childRelation.getElement2().getId(), childIid));
                }
            }
        }
        return children;
    }

    @Override
    public void setAttributeValues(long aid, long iid, Collection<NameValueUnit> values) {
        for (NameValueUnit nvu : values) {
            Integer attrNo = atfxCache.getAttrNoByName(aid, nvu.getValName());
            if (attrNo == null) {
                Element element = getElementById(aid);
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                            "Attribut '" + nvu.getValName() + "' not found at " + element);
            }
            atfxCache.setInstanceValue(aid, iid, attrNo, nvu);
        }
        Instance instance = getInstanceById(aid, iid);
        instance.setAttributeValues(values);
    }

    /**
     * List all instance attribute names.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return Collection of attribute names.
     */
    @Override
    public Collection<String> listInstanceAttributes(long aid, long iid) {
        return getInstanceById(aid, iid).listInstanceAttributes();
    }

    @Override
    public void setInstanceAttributeValue(long aid, long iid, NameValueUnit nvu) {
        Instance instance = getInstanceById(aid, iid);
        instance.setInstanceValue(nvu);
    }
    
    void addUnitMapping(long id, String name) {
        atfxCache.addUnitMapping(id, name);
    }

    /***************************************************************************************
     * methods required for internal usage in CORBA layer
     ***************************************************************************************/
    
    public AtfxElement createAtfxElement(String basetype, String aeName) {
        for (BaseElement currentBaseElement : baseModel.getElements("*")) {
            if (basetype.equalsIgnoreCase(currentBaseElement.getType())) {
                AtfxElement newElement = new AtfxElement(atfxCache.nextAid(), currentBaseElement, aeName, isExtendedCompatibilityMode());
                atfxCache.addApplicationElement(newElement);

                long aid = newElement.getId();
                for (BaseAttribute currentBaseAttr : currentBaseElement.getAttributes("*")) {
                    if (currentBaseAttr.isMandatory()) {
                        atfxCache.createAttributeFromBaseAttribute(aid, currentBaseAttr.getName(), currentBaseAttr);
                    }
                }

                return newElement;
            }
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Couldn't create new Element '" + aeName
                + "' because provided base type '" + basetype + "' could not be found in base model!");
    }
    
    public Collection<AtfxElement> getAtfxElements() {
        Collection<AtfxElement> elements = new ArrayList<>();
        elements.addAll(atfxCache.getElements());
        return elements;
    }
    
    public AtfxElement getAtfxElement(long aid) {
        return atfxCache.getElementById(aid);
    }
    
    public AtfxAttribute createAtfxAttribute(long aid) {
        return atfxCache.createAttribute(aid, null, null, null, null, null, null, 0, false, false, false);
    }
    
    public AtfxRelation createAtfxRelation(Element element1, Element element2, BaseRelation baseRelation, String relName,
            String inverseRelName, Short min, Short max) {
        return atfxCache.addModelRelation(relName, baseRelation, element1 == null ? 0 : element1.getId(),
                                                element2 == null ? 0 : element2.getId(), null, min,
                                                max, inverseRelName);
    }
    
    public void removeRelationElem1(long aid, String relationName) {
        atfxCache.detachModelRelation(aid, relationName);
    }

    public void setRelationElem1(long aid, String relationName) {
        atfxCache.setModelRelationElem1(aid, relationName);
    }
    
    public AtfxRelation setRelationElem2(long aid1, long aid2, String relationName) {
        return atfxCache.setModelRelationElem2(aid1, aid2, relationName);
    }
    
    public void updateBaseAttribute(long aid, String attrName, String baseAttrName) {
        Element element = getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Element with aid " + aid + " not found");
        }
        BaseElement baseElement = baseModel.getElementByType(element.getType());
        if (baseElement == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No BaseElement found at " + element);
        }
        Attribute attribute = element.getAttributeByName(attrName);
        if (attribute == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Attribute with name '" + attrName + "' not found at " + element);
        }
        
        BaseAttribute baseAttribute = null;
        if (baseAttrName != null && !baseAttrName.isBlank()) {
            baseAttribute = baseElement.getAttributeByName(baseAttrName);
            if (baseAttribute == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "BaseAttribute with name '" + baseAttrName + "' not found at " + baseElement);
            }
        }
        atfxCache.updateBaseAttribute(aid, attrName, baseAttribute);
    }
    
    public void updateBaseRelation(long aid, String relationName, String baseRelationName) {
        Element element = getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Element with aid " + aid + " not found");
        }
        BaseElement baseElement = baseModel.getElementByType(element.getType());
        if (baseElement == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No BaseElement found at " + element);
        }
        Relation relation = element.getRelationByName(relationName);
        if (relation == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Relation with name '" + relationName + "' not found at " + element);
        }
        
        BaseRelation baseRelation = baseElement.getRelationByName(baseRelationName, relation.getElement2().getType());
        if (baseRelation == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "BaseRelation with name '" + baseRelation + "' not found at " + baseElement);
        }
        for (BaseElement currentElement2 : baseRelation.getElem2()) {
            if (relation.getElement2().getType().equalsIgnoreCase(currentElement2.getType())) {
                BaseRelation inverseBaseRelation = currentElement2.getRelationByName(baseRelation.getInverseName(currentElement2.getType()),
                                                                                     element.getType());
                atfxCache.updateBaseRelation(aid, relationName, baseRelation, inverseBaseRelation);
            }
        }
    }
    
    public DataType getDataTypeForLocalColumnValues(long lcIid) {
        return atfxCache.getDataTypeForLocalColumnValues(lcIid);
    }
    
    public boolean isExtendedCompatibilityMode() {
        NameValueUnit nvu = getContext(OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE);
        if (nvu != null && nvu.hasValidValue()) {
            DataType dt = nvu.getValue().discriminator();
            if (DataType.DT_STRING == dt) {
                return Boolean.parseBoolean(nvu.getValue().stringVal());
            } else if (DataType.DT_BOOLEAN == dt) {
                return nvu.getValue().booleanVal();
            }
        }
        return false;
    }
}
