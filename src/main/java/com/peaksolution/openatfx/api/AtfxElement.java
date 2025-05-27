package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.asam.ods.ErrorCode;
import org.asam.ods.RelationType;

import com.peaksolution.openatfx.util.PatternUtil;

public class AtfxElement implements Element {
    private final long aid;
    private final BaseElement baseElement;
    private final boolean isExtendedCompatibilityMode;
    
    private String name;
    private AtomicInteger nextAttrNo = new AtomicInteger(1);
    
    // attributes
    private Map<Integer, AtfxAttribute> attrsByNr = new HashMap<>();
    private Map<String, Integer> aaNameToAttrNr = new HashMap<>();
    private Map<String, Integer> baNameToAttrNr = new HashMap<>();
    
    // relations
    private Map<Integer, AtfxRelation> relsByNr = new HashMap<>();
    private Map<String, Integer> arNameToRelNr = new HashMap<>();
    private Map<String, Collection<Integer>> brNameToRelNrs = new HashMap<>();
    
    public AtfxElement(long aid, BaseElement baseElement, String name, boolean isExtendedCompatibilityMode) {
        this.aid = aid;
        this.baseElement = baseElement;
        this.isExtendedCompatibilityMode = isExtendedCompatibilityMode;
        this.name = name;
    }
    
    @Override
    public long getId() {
        return aid;
    }
    
    @Override
    public String getType() {
        return baseElement.getType();
    }
    
    @Override
    public BaseElement getBaseElement() {
        return baseElement;
    }
    
    @Override
    public boolean isTopLevelElement() {
        return baseElement.isTopLevel();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Integer getAttrNoByName(String aaName) {
        return aaNameToAttrNr.get(aaName);
    }
    
    @Override
    public Integer getAttrNoByBaseName(String baName) {
        return baNameToAttrNr.get(baName.toLowerCase());
    }
    
    @Override
    public AtfxAttribute getAttributeByNo(int attrNo) {
        return attrsByNr.get(attrNo);
    }
    
    @Override
    public Collection<Attribute> getAttributes() {
        return getAttributes("*");
    }
    
    public Collection<AtfxAttribute> getAtfxAttributes() {
        return attrsByNr.values();
    }
    
    public Collection<AtfxAttribute> getAtfxAttributes(String pattern) {
        Collection<AtfxAttribute> attributes = new ArrayList<>();
        for (AtfxAttribute attr : attrsByNr.values()) {
            if (PatternUtil.nameFilterMatch(attr.getName(), pattern)) {
                attributes.add(attr);
            }
        }
        return attributes;
    }
    
    @Override
    public Collection<Attribute> getAttributes(String pattern) {
        Collection<Attribute> attributes = new ArrayList<>();
        for (AtfxAttribute attr : attrsByNr.values()) {
            if (PatternUtil.nameFilterMatch(attr.getName(), pattern)) {
                attributes.add(attr);
            }
        }
        return attributes;
    }
    
    @Override
    public AtfxAttribute getAttributeByName(String aaName) {
        // check aaName length
        if (aaName == null || aaName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "aaName must not be empty");
        }
        Integer attrNo = getAttrNoByName(aaName);
        if (attrNo != null) {
            return attrsByNr.get(attrNo);
        }
        return null;
    }
    
    @Override
    public AtfxAttribute getAttributeByBaseName(String baName) {
        // check baName length
        if (baName == null || baName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "baName must not be empty");
        }
        // get application attribute
        Integer attrNo = baNameToAttrNr.get(baName.toLowerCase());
        if (attrNo != null) {
            return attrsByNr.get(attrNo);
        }
        return null;
    }
    
    public void updateBaseAttribute(int attrNo, BaseAttribute baseAttribute) {
        AtfxAttribute attribute = attrsByNr.get(attrNo);
        if (attribute != null) {
            String previousBaseName = attribute.getBaseName();
            baNameToAttrNr.remove(previousBaseName);
            attribute.setBaseAttribute(baseAttribute);
            if (baseAttribute != null) {
                baNameToAttrNr.put(baseAttribute.getName(), attrNo);
            }
        }
    }
    
    @Override
    public Collection<Relation> getRelations() {
        Collection<Relation> relations = new ArrayList<>();
        for (AtfxRelation rel : relsByNr.values()) {
            relations.add(rel);
        }
        return relations;
    }
    
    public Collection<AtfxRelation> getAtfxRelations() {
        return relsByNr.values();
    }

    public Collection<AtfxRelation> getRelationsByBaseName(String baseRelName) {
        List<AtfxRelation> list = new ArrayList<>();
        Collection<Integer> relNos = brNameToRelNrs.get(baseRelName);
        if (relNos != null) {
            for (Integer relNo : relNos) {
                list.add(relsByNr.get(relNo));
            }
        }
        return list;
    }
    
    @Override
    public AtfxRelation getRelationByName(String relName) {
        String relationName = relName;
        if (relationName == null) {
            relationName = OpenAtfxConstants.DEF_RELNAME_EMPTY;
        }
        Integer relNo = arNameToRelNr.get(relationName);
        if (relNo != null) {
            return relsByNr.get(relNo);
        }
        return null;
    }
    
    public AtfxRelation getRelationByNo(int relNo) {
        return relsByNr.get(relNo);
    }
    
    public AtfxAttribute createAttribute(String attrName, BaseAttribute baseAttribute, DataType dataType, long unitId, String enumName,
            Integer length, boolean isObligatory, boolean isUnique, boolean isAutogenerated) {
        Integer existingAttrNr = arNameToRelNr.get(attrName);
        if (existingAttrNr != null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Tried to add attribute '" + attrName + "' to " + this
                    + ", but an attribute with that name already exists!");
        }
        
        int valueLength = 0;
        if (length != null) {
            valueLength = length;
        }
        
        int attrNr = nextAttrNo.getAndIncrement();
        AtfxAttribute newAttribute = new AtfxAttribute(this, attrNr, attrName, baseAttribute, dataType, unitId, enumName, valueLength,
                                                       isUnique, isObligatory, isAutogenerated, isExtendedCompatibilityMode);
        attrsByNr.put(attrNr, newAttribute);
        aaNameToAttrNr.put(attrName, attrNr);
        if (baseAttribute != null) {
            baNameToAttrNr.put(baseAttribute.getName().toLowerCase(), attrNr);
        }
        return newAttribute;
    }
    
    public AtfxAttribute createAttributeFromBaseAttribute(BaseAttribute baseAttribute, String attrName) {
        return createAttribute(attrName, baseAttribute, baseAttribute.getDataType(), 0,
                        baseAttribute.getEnumName(), null, baseAttribute.isObligatory(),
                        baseAttribute.isUnique(), baseAttribute.isAutogenerated());
    }
    
    @Override
    public void removeAttribute(String aaName) {
        // check for existing attribute
        AtfxAttribute attribute = getAttributeByName(aaName);
        if (attribute == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Attribute with name '" + aaName + "' not found at " + this);
        }
        
        // check for non obligatory base attribute
        BaseAttribute baseAttribute = attribute.getBaseAttribute();
        if (baseAttribute != null && baseAttribute.isObligatory()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "Removing attribute derived from obligatory base attribute not allowed!");
        }
        
        Integer attrNo = getAttrNoByName(aaName);
        aaNameToAttrNr.remove(aaName);
        if (baseAttribute != null) {
            baNameToAttrNr.remove(baseAttribute.getName().toLowerCase());
        }
        attrsByNr.remove(attrNo);
    }
    
    public AtfxRelation createRelation(AtfxElement toElement, BaseRelation baseRelation, String name, String inverseName, short minOccurs,
            short maxOccurs, Relationship relationShipToSet, RelationType relationTypeToSet) {
        Integer existingRelNr = arNameToRelNr.get(name);
        if (existingRelNr != null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Tried to add new relation '" + name + "' to " + this
                    + ", but a relation with that name already exists!");
        }
        
        AtfxRelation newRelation = new AtfxRelation(this, toElement, baseRelation, name, inverseName, minOccurs,
                                                    maxOccurs, relationShipToSet, relationTypeToSet);
        addRelation(newRelation);
        return newRelation;
    }
    
    /**
     * @param relation
     */
    @Override
    public void addRelation(Relation relation) {
        if (!(relation instanceof AtfxRelation)) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "addRelation() does not support other relation than of type AtfxRelation, but received: "
                                                + relation.getClass().getSimpleName());
        }
        
        AtfxRelation atfxRelation = (AtfxRelation)relation;
        int relNo = nextAttrNo.getAndIncrement();
        atfxRelation.setElement1(this);
        atfxRelation.setRelNo(relNo);
        relsByNr.put(relNo, atfxRelation);
        arNameToRelNr.put(relation.getRelationName(), relNo);
        BaseRelation baseRelation = relation.getBaseRelation();
        if (baseRelation != null) {
            brNameToRelNrs.computeIfAbsent(baseRelation.getName(), v -> new HashSet<>()).add(relNo);
        }
    }
    
    @Override
    public void updateBaseRelation(String relName, BaseRelation baseRelation) {
        AtfxRelation relation = getRelationByName(relName);
        if (relation != null) {
            String oldBaseRelName = relation.getBaseName();
            if (oldBaseRelName != null) {
                brNameToRelNrs.remove(oldBaseRelName);
            }
            
            relation.setBaseRelation(baseRelation);
            if (baseRelation != null) {
                brNameToRelNrs.computeIfAbsent(baseRelation.getName(), v -> new HashSet<>()).add(relation.getRelNo());
            }
        }
    }
    
    @Override
    public void removeRelation(String relationName) {
        Integer relNo = arNameToRelNr.remove(relationName);
        Relation relation = relsByNr.get(relNo);
        if (relation.getBaseRelation() != null) {
            brNameToRelNrs.remove(relation.getBaseRelation().getName());
        }
        relation.setElement1(null);
        relsByNr.remove(relNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtfxElement other = (AtfxElement) obj;
        return aid == other.aid;
    }

    @Override
    public String toString() {
        return "Element [aid=" + aid + ", baseName=" + baseElement == null ? "" : baseElement.getType() + ", name=" + name + "]";
    }

    public void renameAttribute(int attrNo, String oldAaName, String newAaName) {
        AtfxAttribute attr = getAttributeByNo(attrNo);
        if (attr != null) {
            attr.setName(newAaName);
            aaNameToAttrNr.remove(oldAaName);
            aaNameToAttrNr.put(newAaName, attrNo);
        }
    }
    
    public void renameRelation(int relNo, String oldArName, String newArName) {
        AtfxRelation rel = getRelationByNo(relNo);
        if (rel != null) {
            rel.setRelationName(newArName);
            arNameToRelNr.remove(oldArName);
            arNameToRelNr.put(newArName, relNo);
            
            AtfxRelation invRel = rel.getInverseRelation();
            if (invRel != null) {
                invRel.setInverseRelationName(newArName);
            }
        }
    }
}
