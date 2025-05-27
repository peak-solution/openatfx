package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.asam.ods.ErrorCode;

import com.peaksolution.openatfx.io.AtfxTagConstants;


public class AtfxInstance implements Instance {

    private final AtfxCache atfxCache;
    private final AtfxElement element;

    private long iid;
    private String name = "";
    private Map<Integer, NameValueUnit> attrValuesByAttrNo = new HashMap<>();
    private Map<String, NameValueUnit> instanceAttrValues = new HashMap<>();
    private Map<Relation, Collection<Long>> relationValues = new HashMap<>();

    public AtfxInstance(AtfxCache atfxCache, AtfxElement element, Collection<NameValueUnit> values) {
        this.atfxCache = atfxCache;
        this.element = element;

        for (NameValueUnit nvu : values) {
            Attribute attr = element.getAttributeByName(nvu.getValName());
            attrValuesByAttrNo.put(attr.getAttrNo(), nvu);
            if ("id".equalsIgnoreCase(attr.getBaseName())) {
                this.iid = nvu.getValue().getLongValue();
            } else if ("name".equalsIgnoreCase(attr.getBaseName())) {
                this.name = nvu.getValue().stringVal();

            }
        }
    }

    @Override
    public long getAid() {
        return element.getId();
    }

    @Override
    public long getIid() {
        return iid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String iaName) {
        name = iaName;
        Attribute nameAttr = element.getAttributeByBaseName("name");

        // create value
        NameValueUnit nvu = new NameValueUnit(nameAttr.getName(), DataType.DT_STRING, iaName);

        // set value
        attrValuesByAttrNo.put(nameAttr.getAttrNo(), nvu);
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public String getElementName() {
        return getElement().getName();
    }

    @Override
    public void setAttributeValues(Collection<NameValueUnit> values) {
        // trick for 'AoLocalColumn': sort the attribute 'sequence_representation' BEFORE the attribute 'values'. This
        // is needed for the write_mode 'file'.
        Collection<NameValueUnit> valuesToSet = values;
        if (element.getType().equalsIgnoreCase("aolocalcolumn")) {
            List<NameValueUnit> list = new ArrayList<>(values);
            Collections.sort(list, (NameValueUnit o1, NameValueUnit o2) -> {
                Attribute currentAttr = element.getAttributeByName(o2.getValName());
                boolean isSeqRepVal = false;
                if (currentAttr.getBaseName() != null) {
                    isSeqRepVal = currentAttr.getBaseName().equalsIgnoreCase("sequence_representation");
                }
                return isSeqRepVal ? 1 : 0;
            });
            valuesToSet = list;
        }

        for (NameValueUnit nvu : valuesToSet) {
            // This method can also be called from the CORBA layer, therefore it cannot be expected that the NVUs contain
            // information whether or not they are instance or application attributes. This has to be checked now in order
            // to choose the correct method to set the value.
            AtfxAttribute attr = element.getAttributeByName(nvu.getValName());
            if (attr == null) {
                setInstanceValue(nvu);
            } else {
                setAttributeValue(nvu);
            }
        }
    }

    @Override
    public Collection<String> listInstanceAttributes() {
        return instanceAttrValues.keySet();
    }

    @Override
    public Collection<NameValueUnit> getInstanceAttributes() {
        return instanceAttrValues.values();
    }

    @Override
    public NameValueUnit getInstanceAttribute(String attrName) {
        return instanceAttrValues.get(attrName);
    }
    
    public void setValue(NameValueUnit nvu) {
        AtfxAttribute attr = element.getAttributeByName(nvu.getValName());
        if (attr == null) {
            setInstanceValue(nvu);
        } else {
            setAttributeValue(nvu);
        }
    }

    @Override
    public void setAttributeValue(NameValueUnit nvu) {
        setValue(nvu, false);
    }

    @Override
    public void setInstanceValue(NameValueUnit nvu) {
        setValue(nvu, true);
    }

    private void setValue(NameValueUnit nvu, boolean isInstanceAttribute) {
        // check for empty name
        if (nvu.getValName() == null || nvu.getValName().isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "Empty attribute name is not allowed");
        }

        Attribute attr = element.getAttributeByName(nvu.getValName());

        // instance attribute?
        if (isInstanceAttribute) {
            // check for existing application attribute
            if (attr != null) {
                throw new OpenAtfxException(ErrorCode.AO_DUPLICATE_NAME,
                                            "Cannot set instance attribute, since an application attribute with name '"
                                                    + nvu.getValName() + "' already exists");
            }
            // check data type
            DataType dt = nvu.getValue().discriminator();
            if ((dt != DataType.DT_STRING) && (dt != DataType.DT_FLOAT) && (dt != DataType.DT_DOUBLE)
                    && (dt != DataType.DT_BYTE) && (dt != DataType.DT_SHORT) && (dt != DataType.DT_LONG)
                    && (dt != DataType.DT_LONGLONG) && (dt != DataType.DT_DATE)) {
                throw new OpenAtfxException(ErrorCode.AO_INVALID_DATATYPE,
                                            "DataType is no allowed for InstanceAttributes: " + dt);
            }

            if (nvu.hasValidValue()) {
                instanceAttrValues.put(nvu.getValName(), nvu);
            } else {
                instanceAttrValues.remove(nvu.getValName());
            }
            return;
        } else if (attr == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No attribute '" + nvu.getValName() + "' found at "
                    + element + " to set the value for!");
        }

        if (attr.isBaseAttrDerived() && attr.getBaseName().equalsIgnoreCase("name")) {
            name = nvu.getValue().stringVal();
        }

        // check if id has been updated, not allowed!
        Integer attrNoToUpdate = element.getAttrNoByName(attr.getName());
        Integer idAttrNo = element.getAttrNoByBaseName("id");
        if (idAttrNo != null && idAttrNo.equals(attrNoToUpdate)) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Updating the id of an instance is not allowed!");
        }
        if (nvu.getValue().getFlag() != (short) 15) {
            attrValuesByAttrNo.remove(attr.getAttrNo());
        } else {
            attrValuesByAttrNo.put(attr.getAttrNo(), nvu);
        }
    }
    
    @Override
    public boolean hasValidValue(String attrName, String baseAttrName) {
        Integer attrNo = null;
        if (attrName != null) {
            attrNo = getElement().getAttrNoByName(attrName);
        } else if (baseAttrName != null) {
            attrNo = getElement().getAttrNoByBaseName(baseAttrName);
        }
        
        if (attrNo != null) {
            NameValueUnit nvu = attrValuesByAttrNo.get(attrNo);
            if (nvu != null && nvu.hasValidValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doesAttributeExist(String aaName, String baName, boolean isRequired) {
        Attribute attr = null;
        if (baName != null && !baName.isBlank()) {
            // check on base name if provided
            attr = element.getAttributeByBaseName(baName.trim());
        } else if (aaName != null && !aaName.isBlank()) {
            // check if instance attribute
            if (instanceAttrValues.containsKey(aaName)) {
                return true;
            }

            // get attribute
            attr = element.getAttributeByName(aaName.trim());
        }
        if (isRequired && attr == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Attribute with provided base name '" + baName
                    + "' or attribute name '" + aaName + "' not found at Element " + element);
        }

        return attr != null;
    }
    
    NameValueUnit getValueInternal(int attrNo) {
        return attrValuesByAttrNo.get(attrNo);
    }

    @Override
    public NameValueUnit getValue(String valueName) {
        if (valueName == null) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "valueName argument may not be null!");
        }
        String valName = valueName.trim();
        
        // check if instance attribute
        NameValueUnit nvu = instanceAttrValues.get(valName);
        if (nvu != null) {
            return nvu;
        }

        // get attribute
        Attribute attr = element.getAttributeByName(valName);
        if (attr != null) {
            return atfxCache.getInstanceValue(element.getId(), attr.getAttrNo(), iid);
        }

        // get relation
        Relation rel = element.getRelationByName(valName);
        if (rel != null) {
            Collection<Long> relIids = relationValues.get(rel);
            if (relIids != null) {
                if (rel.getRelationRangeMax() == -1) {
                    return new NameValueUnit(valName, DataType.DS_LONGLONG,
                                             relIids.stream().mapToLong(Long::longValue).toArray());
                } else if (!relIids.isEmpty()) {
                    return new NameValueUnit(valName, DataType.DT_LONGLONG, relIids.iterator().next());
                }
            }
        }
        return null;
    }
    
    @Override
    public NameValueUnit getValue(int number) {
        Attribute attr = element.getAttributeByNo(number);
        if (attr != null) {
            return getValue(attr.getName());
        }
        Relation rel = element.getRelationByNo(number);
        if (rel != null) {
            return getValue(rel.getRelationName());
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Did not find any attribute or relation at " + element + " with provided number " + number);
    }

    @Override
    public Collection<NameValueUnit> getValues(boolean includeAllODSValues) {
        Collection<NameValueUnit> nvus = new ArrayList<>();
        // only filter the requested attribute values
        Set<String> filteredODSBaseNames = new HashSet<>(Arrays.asList(AtfxTagConstants.BA_ID, AtfxTagConstants.BA_MIME_TYPE));
        for (Attribute currentAttr : element.getAttributes()) {
            if (includeAllODSValues || !filteredODSBaseNames.contains(currentAttr.getBaseName())) {
                nvus.add(getValue(currentAttr.getName()));
            }
        }
        return nvus;
    }

    @Override
    public NameValueUnit getValueByBaseName(String baName) {
        Integer attr = element.getAttrNoByBaseName(baName);
        if (attr == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No base attribute '" + baName + "' found at " + this);
        }
        return getValue(attr);
    }

    @Override
    public void addRelationValue(Relation applRel, Collection<Long> otherIids) {
        Collection<Long> relatedIids = relationValues.computeIfAbsent(applRel, v -> new HashSet<>());
        if (!relatedIids.isEmpty() && applRel.getRelationRangeMax() != -1) {
            relatedIids.clear();
        }
        relatedIids.addAll(otherIids);
    }

    @Override
    public Collection<Instance> getRelatedInstancesByRelationship(Relationship child, String string) {
        throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                    "getRelatedInstancesByRelationship() at AtfxInstance is not yet implemented!");
    }

    @Override
    public Boolean removeRelatedIids(Relation applRel, Collection<Long> iidsToRemove) {
        Collection<Long> relatedIids = relationValues.get(applRel);
        return relatedIids.removeAll(iidsToRemove);
    }
    
    @Override
    public void renameInstanceAttribute(String oldName, String newName) {
        // check if attribute exists
        if (getInstanceAttribute(oldName) == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                        "Instance attribute '" + oldName + "' not found at " + this);
        }
        // check for empty name
        if (newName == null || newName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_DUPLICATE_NAME, "Empty instance attribute name is not allowed!");
        }
        // check for existing instance attribute
        if (getInstanceAttribute(newName) != null) {
            throw new OpenAtfxException(ErrorCode.AO_DUPLICATE_NAME,
                                        "An instance attribute with name '" + newName + "' already exists at " + this);
        }

        NameValueUnit value = instanceAttrValues.get(oldName);
        instanceAttrValues.remove(oldName);
        instanceAttrValues.put(newName, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element.getId(), iid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AtfxInstance other = (AtfxInstance) obj;
        return element.getId() == other.getElement().getId() && iid == other.iid;
    }

    @Override
    public String toString() {
        return "AtfxInstance [element=" + element + ", iid=" + iid + ", name=" + name + "]";
    }
}
