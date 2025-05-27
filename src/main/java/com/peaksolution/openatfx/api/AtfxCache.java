package com.peaksolution.openatfx.api;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.io.AtfxTagConstants;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Central cache holding all data for performance reasons.
 * 
 * @author Markus Renner
 */
class AtfxCache {
    private static final Logger LOG = LoggerFactory.getLogger(AtfxCache.class);
    
    private final BaseModel baseModel;
    private final ExtCompReader extCompReader;
    private final ExtCompWriter extCompWriter;
    
    /** enumerations */
    private final Collection<AtfxEnumeration> enumerations = new ArrayList<>();
    /** enumName -> aid -> attrNames */
    private final Map<String, Map<Long, Collection<String>>> enumerationUsages = new HashMap<>();
    private AtomicInteger nextEnumIndex = null;
    
    /** application elements */
    private final Map<Long, AtfxElement> aidToElement = new TreeMap<>();
    private final Map<String, Long> aeNameToAid = new HashMap<>();
    private final Map<String, Collection<Long>> beNameToAid = new HashMap<>();
    
    /** application relations*/
    private final Map<String, AtfxRelation> unassignedRelations = new HashMap<>();
    private final AtomicInteger tempRelNo = new AtomicInteger(-1);

    /** instance attribute values */
    private final Map<Long, Map<Long, Map<String, SingleValue>>> instanceAttrValueMap = new HashMap<>();
    
    /** instance attribute units */
    private final Map<Long, Map<String, String>> instanceAttrUnitMap = new HashMap<>();
    
    private final Map<Long, String> unitIids2UnitNames = new HashMap<>();

    /** instance elements */
    private final Map<Long, Map<Long, AtfxInstance>> instanceElementCache = new HashMap<>();

    /** The counters for ids */
    private AtomicInteger nextAid;
    
    private Map<Long, AtomicLong> nextIidsByAid = new HashMap<>();
    
    private boolean extendedCompatibilityMode = false;
    private String writeMode;
    
    /**
     * Constructor.
     * 
     * @param baseModel
     * @param extCompReader
     * @param extCompWriter
     */
    public AtfxCache(BaseModel baseModel, ExtCompReader extCompReader, ExtCompWriter extCompWriter) {
        this.baseModel = baseModel;
        this.extCompReader = extCompReader;
        this.extCompWriter = extCompWriter;

        this.nextAid = new AtomicInteger(1);
        this.nextEnumIndex = new AtomicInteger(baseModel.getEnumerations().size());
    }
    
    public void setExtendedCompatibilityMode(boolean on) {
        extendedCompatibilityMode = on;
    }
    
    public boolean isExtendedCompatibilityMode() {
        return extendedCompatibilityMode;
    }
    
    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
    }

    /**
     * Returns the next free application element id.
     * 
     * @return The application element id.
     */
    public long nextAid() {
        return nextAid.getAndIncrement();
    }

    /**
     * Returns the next free instance element id for an application element.
     * 
     * @param aid The application element id.
     * @return The instance element id.
     */
    public long nextIid(long aid) {
        return nextIidsByAid.computeIfAbsent(aid, v -> new AtomicLong(1)).getAndIncrement();
    }
    
    /**
     * @return the instance of the {@link ExtCompReader}.
     */
    ExtCompReader getExtCompReader() {
        return extCompReader;
    }

    /***********************************************************************************
     * application elements
     ***********************************************************************************/

    /**
     * Adds an application element to the cache.
     * 
     * @param ae The application element.
     */
    public void addApplicationElement(AtfxElement ae) {
        long aid = ae.getId();
        this.aidToElement.put(aid, ae);
        String type = ae.getType();
        if (type != null && !type.isBlank()) {
            this.beNameToAid.computeIfAbsent(ae.getType().toLowerCase(), v -> new ArrayList<>()).add(aid);
        }
        this.aeNameToAid.put(ae.getName(), aid);
        this.instanceAttrValueMap.put(aid, new TreeMap<>());
        this.instanceAttrUnitMap.put(aid, new TreeMap<>());
        this.instanceElementCache.put(aid, new TreeMap<>());
    }

    /**
     * Returns the list of all application elements.
     * 
     * @return All application elements.
     */
    public Collection<AtfxElement> getElements() {
        return aidToElement.values();
    }

    /**
     * Returns an application element by given id.
     * 
     * @param aid The application element id.
     * @return The application element, null if not found.
     */
    public AtfxElement getElementById(long aid) {
        return aidToElement.get(aid);
    }
    
    /**
     * Returns the application element for given name.
     * 
     * @param aeName The application element name.
     * @return The application element, null if not found.
     */
    public AtfxElement getElementByName(String aeName) {
        Long aid = aeNameToAid.get(aeName);
        if (aid != null) {
            return getElementById(aid);
        }
        
        for (Entry<String, Long> entry : aeNameToAid.entrySet()) {
            if (aeName.equalsIgnoreCase(entry.getKey())) {
                LOG.warn("Requested element with name '{}', which was not found, but an element named '{}' exists!"
                        + " Application element names are case sensitive, so this might be an error in your file's model!",
                         aeName, entry.getKey());
            }
        }
        
        return null;
    }
    
    /**
     * Convenience method for expectedly unique elements, regarding baseType.
     * 
     * @param baseType
     * @return
     */
    public AtfxElement getUniqueElementByBasetype(String baseType) {
        Collection<AtfxElement> elementsForBaseType = getElementsByBasetype(baseType);
        if (elementsForBaseType.size() > 1) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "No unique element for type '" + baseType
                    + "' could be identified. Try with method getElementsByBasetype() instead.");
        }
        if (elementsForBaseType.isEmpty()) {
            return null;
        }
        return elementsForBaseType.iterator().next();
    }
    
    /**
     * Returns any elements derived from given baseType.
     * 
     * @param baseType
     * @return
     */
    public Collection<AtfxElement> getElementsByBasetype(String baseType) {
        Collection<Long> aids = beNameToAid.get(baseType);
        Collection<AtfxElement> elements = new ArrayList<>();
        if (aids != null) {
            for (Long aid : aids) {
                if (aid != null) {
                    AtfxElement element = getElementById(aid);
                    if (element != null) {
                        elements.add(element);
                    }
                }
            }
        }
        return elements;
    }

    /**
     * Returns the application element name by id.
     * 
     * @param aid The application element id.
     * @return The application element name.
     */
    public String getElementNameById(long aid) {
        Element element = getElementById(aid);
        if (element != null) {
            return element.getName();
        }
        return null;
    }

    /**
     * Rename an application element.
     * 
     * @param aid The application element id.
     * @param newAeName The new name.
     * @return the old element name if aid was found, null otherwise
     */
    public String renameElement(long aid, String newAeName) {
        AtfxElement ae = this.aidToElement.get(aid);
        if (ae == null) {
            return null;
        }
        String oldName = ae.getName();
        ae.setName(newAeName);
        this.aeNameToAid.remove(oldName);
        this.aeNameToAid.put(newAeName, aid);
        return oldName;
    }

    /**
     * Removed an application element from the cache.
     * 
     * @param aid The application element id.
     */
    public void removeApplicationElement(long aid) {
        AtfxElement element = aidToElement.remove(aid);
        if (element == null) {
            return;
        }
        
        this.aeNameToAid.remove(element.getName());
        String type = element.getType();
        if (type != null && !type.isBlank()) {
            Collection<Long> aidsForType = beNameToAid.get(type.toLowerCase());
            aidsForType.remove(aid);
        }
        this.instanceAttrValueMap.remove(aid);
        this.instanceAttrUnitMap.remove(aid);
        this.instanceElementCache.remove(aid);
    }

    /***********************************************************************************
     * application attributes
     ***********************************************************************************/

    public AtfxAttribute createAttribute(long aid, String name, BaseAttribute baseAttribute, DataType dataType,
            String unitName, Long unitId, String enumName, int length, boolean isObligatory, boolean isUnique,
            boolean isAutogenerated) {
        AtfxElement element = getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Element with aid " + aid
                    + " not found for creation of attribute with name '" + name + "'!");
        }
        
        DataType dt = dataType;
        if (extendedCompatibilityMode && baseAttribute != null && "id".equalsIgnoreCase(baseAttribute.getName())
                && DataType.DT_LONG == dataType) {
            dt = DataType.DT_LONGLONG;
        }
        if (enumName != null && !enumName.isBlank()) {
            enumerationUsages.computeIfAbsent(enumName, v -> new HashMap<>()).computeIfAbsent(aid, v -> new HashSet<>()).add(name);
        }
        
        long finalUnitId = 0;
        if (unitId != null) {
            finalUnitId = unitId;
        } else if (unitName != null) {
            long identifiedUnitId = getUnitId(unitName);
            if (identifiedUnitId < 1) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "The unit '" + unitName + "' is not yet known and cannot be resolved. Did you want to use createAttribute() overload with the unit's id?");
            }
        }
        return element.createAttribute(name, baseAttribute, dt, finalUnitId, enumName, length, isObligatory, isUnique, isAutogenerated);
    }
    
    public AtfxAttribute createAttributeFromBaseAttribute(long aid, String name, BaseAttribute baseAttribute) {
        String enumName = baseAttribute.getEnumName();
        if (enumName != null && !enumName.isBlank()) {
            enumerationUsages.computeIfAbsent(enumName, v -> new HashMap<>()).computeIfAbsent(aid, v -> new HashSet<>()).add(name);
        }
        AtfxElement element = getElementById(aid);
        return element.createAttributeFromBaseAttribute(baseAttribute, name);
    }

    /**
     * Returns the ordered list of all available application attribute names.
     * 
     * @param aid The application element id.
     * @return Collection of attribute names.
     */
    public Collection<String> listApplicationAttributes(long aid) {
        AtfxElement element = getElementById(aid);
        return element.getAttributes().stream().map(Attribute::getName).toList();
    }
    
    public AtfxAttribute getAttribute(long aid, int attrNo) {
        return getElementById(aid).getAttributeByNo(attrNo);
    }

    public Integer getAttrNoByName(long aid, String aaName) {
        return getElementById(aid).getAttrNoByName(aaName);
    }

    /**
     * Returns the attribute index number for given base attribute name.
     * 
     * @param aid The application element id.
     * @param baName The base attribute name.
     * @return The attribute index number, null if no application attribute found for base attribute.
     */
    public Integer getAttrNoByBaName(long aid, String baName) {
        return getElementById(aid).getAttrNoByBaseName(baName);
    }

    /**
     * Returns the ordered list of all available application attributes.
     * 
     * @param aid The application element id.
     * @return Collection of application attributes.
     */
    public Collection<Attribute> getAttributes(long aid) {
        return getElementById(aid).getAttributes();
    }

    /**
     * If application attribute name changes, all value keys have to be altered.
     * 
     * @param aid The application element id.
     * @param attrNo The attribute number.
     * @param oldAaName The old application attribute name.
     * @param newAaName The new application attribute name.
     */
    public void renameAttribute(long aid, int attrNo, String oldAaName, String newAaName) {
        AtfxElement element = getElementById(aid);
        element.renameAttribute(attrNo, oldAaName, newAaName);
    }

    /**
     * Removes an application attribute.
     * 
     * @param aid The application element id.
     * @param aaName The application attribute name.
     */
    public void removeAttribute(long aid, String aaName) {
        AtfxElement element = getElementById(aid);
        if (element != null) {
            Attribute attr = element.getAttributeByName(aaName);
            if (attr != null) {
                String enumName = attr.getEnumName();
                if (enumName != null && !enumName.isBlank()) {
                    enumerationUsages.get(enumName).get(aid).remove(aaName);
                }
                element.removeAttribute(aaName);
            }
        }
    }
    
    /**
     * @param aid
     * @param attrName
     * @param baseAttribute
     */
    public void updateBaseAttribute(long aid, String attrName, BaseAttribute baseAttribute) {
        AtfxElement element = getElementById(aid);
        if (element != null) {
            Attribute attribute = element.getAttributeByName(attrName);
            if (attribute != null) {
                element.updateBaseAttribute(attribute.getAttrNo(), baseAttribute);
            }
        }
    }

    /***********************************************************************************
     * application relations
     ***********************************************************************************/

    /**
     * Returns all application relations of an application element.
     * 
     * @param aid The application element id.
     * @return Collection of application relations.
     */
    public Collection<Relation> getModelRelations(long aid) {
        return getElementById(aid).getRelations();
    }

    /**
     * Returns an application relation by given relation name.
     * 
     * @param aid The application element id.
     * @param relName The application relation name.
     * @return The application relation, null if relation not found.
     * @throws AoException Error getting relation name.
     */
    public AtfxRelation getModelRelationByName(long aid, String relName) {
        return getElementById(aid).getRelationByName(relName);
    }

    /**
     * @param aid
     * @param bRelName
     * @return
     */
    public AtfxRelation getModelRelationByBaseName(long aid, String bRelName) {
        AtfxElement element = getElementById(aid);
        Collection<AtfxRelation> rels = element.getRelationsByBaseName(bRelName);
        if (rels.size() == 1) {
            return rels.iterator().next();
        } else if (rels.size() > 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Couldn't uniquely identify a relation with base name '"
                    + bRelName + "' at " + element);
        }
        return null;
    }
    
    /**
     * @param name
     * @param baseRelation
     * @param fromAid
     * @param toAid
     * @param relationship
     * @param minOccurs
     * @param maxOccurs
     * @param inverseName
     * @return
     */
    public AtfxRelation addModelRelation(String name, BaseRelation baseRelation, long fromAid, long toAid,
            Relationship relationship, short minOccurs, short maxOccurs, String inverseName) {
        if (fromAid == 0) {
            // creation via CORBA call, has to prepare completely empty relation "stub"
            if (unassignedRelations.containsKey(OpenAtfxConstants.DEF_RELNAME_EMPTY)) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION,
                                            "Could not create more than one temporary uninitialized relation with name '" + name
                                                    + "'! Make sure you have finalized any such relation before you create another one!");
            }
            AtfxRelation newRel = new AtfxRelation(tempRelNo.getAndDecrement());
            unassignedRelations.computeIfAbsent(newRel.getRelationName(), v -> newRel);
            return newRel;
        }
        
        AtfxElement fromElement = getElementById(fromAid);
        if (fromElement == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "The root element with aid " + fromAid
                                        + " of relation '" + name + "' could not be found!");
        }
        AtfxElement toElement = getElementById(toAid);
        
        Relationship relationShipToSet = null;
        RelationType relationTypeToSet = RelationType.INFO;
        if (baseRelation != null) {
            relationShipToSet = baseRelation.getRelationship();
            relationTypeToSet = baseRelation.getRelationType();
        } else {
            if (relationship == Relationship.CHILD || relationship == Relationship.FATHER) {
                relationTypeToSet = RelationType.FATHER_CHILD;
            } else if (relationship == Relationship.SUBTYPE || relationship == Relationship.SUPERTYPE) {
                relationTypeToSet = RelationType.INHERITANCE;
            } else if (relationship == null) {
                AtfxRelation inverseRelation = getModelRelationByName(toAid, inverseName);
                // the info relationships can only be set when the inverse relation is already available,
                // in which case the inverse relation is updated and the relationship for the the current
                // relation is identified for following setting at relation creation
                if (inverseRelation != null) {
                    short inverseMaxOccurs = inverseRelation.getRelationRangeMax();
                    // m:n or 1:1
                    if (maxOccurs == inverseMaxOccurs) {
                        inverseRelation.setRelationship(Relationship.INFO_REL);
                        relationShipToSet = Relationship.INFO_REL;
                    }
                    // 0:n or 1:n
                    else if (maxOccurs == 1 && inverseMaxOccurs == -1) {
                        inverseRelation.setRelationship(Relationship.INFO_FROM);
                        relationShipToSet = Relationship.INFO_TO;
                    }
                    // m:0 or m:1
                    else if (maxOccurs == -1 && inverseMaxOccurs == 1) {
                        inverseRelation.setRelationship(Relationship.INFO_TO);
                        relationShipToSet = Relationship.INFO_FROM;
                    }
                }
            } else {
                relationShipToSet = relationship;
            }
        }
        
        return fromElement.createRelation(toElement, baseRelation, name, inverseName, minOccurs, maxOccurs,
                                          relationShipToSet, relationTypeToSet);
    }
    
    /**
     * @param relationToRemove
     */
    public void removeModelRelation(Relation relationToRemove) {
        Element element1 = relationToRemove.getElement1();
        if (element1 != null) {
            element1.removeRelation(relationToRemove.getRelationName());
        }
        // remove inverse relation
        Element element2 = relationToRemove.getElement2();
        if (element2 != null) {
            element2.removeRelation(relationToRemove.getInverseRelationName());
        }
    }
    
    /**
     * @param aid
     * @param relationName
     * @return
     */
    public AtfxRelation detachModelRelation(long aid, String relationName) {
        AtfxElement element = getElementById(aid);
        if (element == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Element with aid " + aid + " not found!");
        }
        AtfxRelation relation = getModelRelationByName(aid, relationName);
        if (relation == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Relation with name '" + relationName + "' not found at " + element);
        }
        
        element.removeRelation(relationName);
        
        relation.setElement1(null);
        unassignedRelations.put(relationName, relation);
        return relation;
    }
    
    /**
     * @param aid
     * @param relationName
     */
    public void setModelRelationElem1(long aid, String relationName) {
        AtfxRelation rel = unassignedRelations.remove(relationName);
        if (rel == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Relation with name '" + relationName + "' not found in pool of unassigned relations!");
        }
        AtfxElement element = getElementById(aid);
        element.addRelation(rel);
    }
    
    public AtfxRelation setModelRelationElem2(long aid1, long aid2, String relationName) {
        AtfxElement element = getElementById(aid1);
        if (element != null) {
            AtfxRelation relation = element.getRelationByName(relationName);
            if (relation != null) {
                Element element2 = getElementById(aid2);
                if (element2 != null) {
                    Element previouslyRelatedElem2 = relation.getElement2();
                    if (previouslyRelatedElem2 != null) {
                        previouslyRelatedElem2.removeRelation(relation.getInverseRelationName());
                    }
                    relation.setElement2(element2);
                    
                    Relation inverseRelation = relation.getInverseRelation();
                    if (inverseRelation != null) {
                        inverseRelation.setElement1(element2);
                        element2.addRelation(relation.getInverseRelation());
                    } else {
                        // create missing inverse relation
                        BaseRelation inverseBaseRelation = null;
                        if (relation.getBaseRelation() != null) {
                            inverseBaseRelation = element2.getBaseElement()
                                                          .getRelationByName(relation.getBaseRelation()
                                                                                     .getInverseName(element2.getType()),
                                                                             element.getType());
                        }
                        AtfxRelation invRelation = addModelRelation(null, inverseBaseRelation, aid2, aid1, null, (short)-2, (short)-2, relationName);
                        relation.setInverseRelNo(invRelation.getRelNo());
                        return invRelation;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * @param aid
     * @param relName
     * @param baseRelation
     * @param inverseBaseRelation
     */
    public void updateBaseRelation(long aid, String relName, BaseRelation baseRelation, BaseRelation inverseBaseRelation) {
        Element element = getElementById(aid);
        if (element != null) {
            element.updateBaseRelation(relName, baseRelation);
            Relation relation = element.getRelationByName(relName);
            if (relation != null) {
                Relation inverseRelation = relation.getInverseRelation();
                relation.getElement2().updateBaseRelation(inverseRelation.getRelationName(), inverseBaseRelation);
            }
        }
    }

    /***********************************************************************************
     * instance elements
     ***********************************************************************************/

    /**
     * Adds an instance element to the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return The created instance.
     */
    public Instance addInstance(long aid, Collection<NameValueUnit> nvus) {
        AtfxElement element = getElementById(aid);
        AtfxInstance newInstance = new AtfxInstance(this, element, nvus);
        long iid = newInstance.getIid();
        
        // map unit if instance is an AoUnit
        if (OpenAtfxConstants.BE_UNIT.equalsIgnoreCase(element.getType())) {
            String unitName = newInstance.getName();
            if (unitName != null && !unitName.isBlank()) {
                addUnitMapping(iid, unitName);
            }
        }
        
        this.instanceElementCache.computeIfAbsent(aid, v -> new HashMap<>()).put(iid, newInstance);

        this.instanceAttrValueMap.get(aid).put(iid, new LinkedHashMap<>());
        return newInstance;
    }

    /**
     * Returns an instance element by given instance id.
     * 
     * @param instancePOA The POA for lazy creation of the CORBA object.
     * @param aid The application element id.
     * @param iid The instance id.
     * @return The instance element.
     * @throws OpenAtfxException if not found.
     */
    public AtfxInstance getInstance(long aid, long iid) {
        Map<Long, AtfxInstance> instanceMap = instanceElementCache.get(aid);
        if (instanceMap != null) {
            AtfxInstance instance = instanceMap.get(iid);
            if (instance != null) {
                return instance;
            }
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Could not find any instance for aid=" + aid + " and iid=" + iid);
    }

    public static byte[] toByta(long[] data) {
        if (data == null) {
            return null;
        }
        byte[] byts = new byte[data.length * 8];
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(toByta(data[i]), 0, byts, i * 8, 8);
        }
        return byts;
    }

    private static byte[] toByta(long data) {
        return new byte[] { (byte) ((data >> 56) & 0xff), (byte) ((data >> 48) & 0xff), (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff), };
    }

    private static long toLong(byte[] data) {
        if (data == null || data.length != 8) {
            return 0x0;
        }
        return ((long) (0xff & data[0]) << 56 | (long) (0xff & data[1]) << 48 | (long) (0xff & data[2]) << 40
                | (long) (0xff & data[3]) << 32 | (long) (0xff & data[4]) << 24 | (long) (0xff & data[5]) << 16
                | (long) (0xff & data[6]) << 8 | (long) (0xff & data[7]) << 0);
    }

    public static long[] toLongA(byte[] data) {
        if (data == null || data.length % 8 != 0) {
            return null;
        }
        long[] lngs = new long[data.length / 8];
        for (int i = 0; i < lngs.length; i++) {
            lngs[i] = toLong(new byte[] { data[(i * 8)], data[(i * 8) + 1], data[(i * 8) + 2], data[(i * 8) + 3],
                    data[(i * 8) + 4], data[(i * 8) + 5], data[(i * 8) + 6], data[(i * 8) + 7], });
        }
        return lngs;
    }

    /**
     * Returns all instance elements for an application element.
     * 
     * @param aid The application element id.
     * @return Collection if instance elements.
     * @throws OpenAtfxException if not found. 
     */
    public Collection<AtfxInstance> getInstances(long aid) {
        Map<Long, AtfxInstance> instanceMap = instanceElementCache.get(aid);
        if (instanceMap != null) {
            return instanceMap.values();
        }
        return Collections.emptyList();
    }

    /**
     * Removes an instance element from the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @throws OpenAtfxException 
     */
    public void removeInstance(long aid, long iid) {
        // remove relations
        for (Relation applRel : getModelRelations(aid)) {
            removeInstanceRelations(aid, iid, applRel, getRelatedInstanceIds(aid, iid, applRel));
        }
        // remove instance values
        this.instanceAttrValueMap.get(aid).remove(iid);
        this.instanceElementCache.get(aid).remove(iid);
    }

    /**
     * Returns whether an instance element exists.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return True, if instance exists, otherwise false.
     */
    public boolean instanceExists(long aid, long iid) {
        Map<Long, AtfxInstance> instanceMap = instanceElementCache.get(aid);
        if (instanceMap != null) {
            AtfxInstance instance = instanceMap.get(iid);
            if (instance != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the environment instance.
     * 
     * @param modelPOA
     * @param instancePOA
     * @return The environment instance, null if not application element derived from 'AoEnviroment' exists or no
     *         instance available.
     * @throws OpenAtfxException if something went wrong
     */
    public Instance getEnvironmentInstance() {
        String envElementName = getEnvironmentApplicationElementName();
        if (envElementName != null && !envElementName.isBlank()) {
            Long envAid = aeNameToAid.get(envElementName);
            Collection<AtfxInstance> ieAr = this.getInstances(envAid);
            if (!ieAr.isEmpty()) {
                return ieAr.iterator().next();
            }
        }
        return null;
    }

    /**
     * Returns the name of the first found environment application element.
     * 
     * @return the name of the first found environment AE or null, if none exist
     */
    public String getEnvironmentApplicationElementName() {
        Collection<Long> environmentAids = beNameToAid.get("aoenvironment");
        if (environmentAids != null && !environmentAids.isEmpty()) {
            AtfxElement envElement = aidToElement.get(environmentAids.iterator().next());
            if (envElement != null) {
                return envElement.getName();
            }
        }
        return null;
    }

    /***********************************************************************************
     * instance values
     ***********************************************************************************/

    /**
     * Sets an instance value.
     * 
     * @param aid The application element id.
     * @param iid The instance element id.
     * @param attrNo The application attribute number.
     * @param value The value.
     * @throws OpenAtfxException 
     */
    public void setInstanceValue(long aid, long iid, int attrNo, NameValueUnit value) {
        Attribute attr = getAttribute(aid, attrNo);
        Element unitElement = null;
        Collection<AtfxElement> unitElements = getElementsByBasetype("aounit");
        if (!unitElements.isEmpty()) {
            unitElement = unitElements.iterator().next();
        }
        
        // check if attribute is 'values' of 'AoLocalColumn', then special handling
        if (attr.isLocalColumnValuesAttr()) {

            // read sequence representation and write_mode
            int seqRepAttrNo = getAttrNoByBaName(aid, AtfxTagConstants.LC_SEQ_REP);
            AtfxAttribute seqRepAttr = getAttribute(aid, seqRepAttrNo);
            NameValueUnit seqRepValue = getInstanceValue(aid, seqRepAttrNo, iid);
            int seqRep = -1;
            if (DataType.DT_ENUM == seqRepValue.getValue().discriminator()) {
                seqRep = seqRepValue.getValue().enumVal();
            } else if (DataType.DT_LONG == seqRepValue.getValue().discriminator()) {
                // tolerate if sequence representation comes as DT_LONG
                seqRep = seqRepValue.getValue().longVal();
            }
            
            // ***************************************************
            // seqRep implicit_constant=1,implicit_linear=2,implicit_saw=3,formula=6
            if ((seqRep == 1) || (seqRep == 2) || (seqRep == 3) || (seqRep == 6)) {
                // return;
            }

            // ***************************************************
            // write mode 'file', then write to external component
            else if (writeMode.equalsIgnoreCase("file")) {
                seqRep = ODSHelper.seqRepComp2seqRepExtComp(seqRep);
                extCompWriter.writeValues(iid, value.getValue());
                setInstanceValue(aid, iid, seqRepAttrNo, new NameValueUnit(seqRepAttr.getName(), DataType.DT_ENUM, seqRep));
                return;
            }

            // ***************************************************
            // write mode 'database', then write to XML (memory)
            else if (writeMode.equalsIgnoreCase("database")) {
                seqRep = ODSHelper.seqRepExtComp2seqRepComp(seqRep);
                setInstanceValue(aid, iid, seqRepAttrNo, new NameValueUnit(seqRepAttr.getName(), DataType.DT_ENUM, seqRep));
            }
        }

        // check if attribute is 'flags' of 'AoLocalColumn', then special handling
        else if (attr.isLocalColumnFlagsAttr()) {
            // check if values are referenced from external component
            AtfxRelation relLcExtComp = getModelRelationByBaseName(aid, "external_component");
            Collection<Long> extCompIids = getRelatedInstanceIds(aid, iid, relLcExtComp);
            if (extCompIids.size() == 1) {
                long extCompIid = extCompIids.iterator().next();
                extCompWriter.writeFlags(extCompIid, value.getValue().shortSeq());
                return;
            }
        }
        
        // check if attribute 'name' of 'AoUnit', then store for unit name resolution
        else if (unitElement != null && unitElement.getId() == aid
                && unitElement.getAttributeByBaseName("name").getAttrNo() == attrNo) {
            unitIids2UnitNames.computeIfAbsent(iid, v -> value.getValue().stringVal());
        }

        // store instance value
        getInstance(aid, iid).setAttributeValue(value);
    }

    /**
     * Calls getInstanceValue(...) with resolveFlagsWithGlobalFlag=true.
     * 
     * @param aid
     * @param attrNo
     * @param iid
     * @return
     */
    NameValueUnit getInstanceValue(long aid, int attrNo, long iid) {
        return getInstanceValue(aid, attrNo, iid, true);
    }
    
    /**
     * Returns a value of an instance element.
     * 
     * @param aid The application element id.
     * @param attrNo The application attribute number.
     * @param iid The instance id.
     * @param resolveFlagsWithGlobalFlag If true, instead of returning empty flags, fills them with the global flag if possible.
     * @return The value, null if not found.
     * @throws OpenAtfxException Error getting value.
     */
    NameValueUnit getInstanceValue(long aid, int attrNo, long iid, boolean resolveFlagsWithGlobalFlag) {
        AtfxElement element = getElementById(aid);
        Attribute attr = getAttribute(aid, attrNo);
        AtfxInstance instance = getInstance(aid, iid);
        
        boolean isLcValuesAttr = attr.isLocalColumnValuesAttr();
        boolean isLcFlagsAttr = attr.isLocalColumnFlagsAttr();
        DataType dt = attr.getDataType();
        
        // read values from memory
        NameValueUnit nvu = instance.getValueInternal(attrNo);
        boolean isValid = nvu != null && nvu.hasValidValue();
        
        // read generation parameters from values if null
        if (!isValid && attr.isLocalColumnGenParamsAttr()) {
            int seqRepAttrNo = getAttrNoByBaName(aid, AtfxTagConstants.LC_SEQ_REP);
            int seqRep = getInstanceValue(aid, seqRepAttrNo, iid).getValue().enumVal();
            // implicit_constant=1,implicit_linear=2,implicit_saw=3,formula=4
            if (nvu == null && (seqRep == 1 || seqRep == 2 || seqRep == 3 || seqRep == 4)) {
                NameValueUnit valuesNvu = instance.getValueByBaseName(AtfxTagConstants.LC_VALUES);
                // generation parameters for 'implicit_constant' may be datatype DT_STRING
                DataType discriminator = valuesNvu.getValue().discriminator();
                if (DataType.DS_STRING == discriminator || DataType.DS_DATE == discriminator
                        || DataType.DS_BOOLEAN == discriminator || DataType.DS_BYTESTR == discriminator
                        || DataType.DS_COMPLEX == discriminator || DataType.DS_DCOMPLEX == discriminator) {
                    Attribute genParamsAttr = element.getAttributeByBaseName(AtfxTagConstants.LC_GEN_PARAMS);
                    return new NameValueUnit(genParamsAttr.getName(), DataType.DS_DOUBLE, null);
                }
                return convertToGenerationParameters(valuesNvu);
            }
        }

        // read values from external component file
        if (!isValid && isLcValuesAttr) {
            dt = getDataTypeForLocalColumnValues(iid);
            int seqRepAttrNo = getAttrNoByBaName(aid, AtfxTagConstants.LC_SEQ_REP);
            int seqRep = getInstanceValue(aid, seqRepAttrNo, iid).getValue().enumVal();
            // external_component=7,raw_linear_external=8,raw_polynomial_external=9,raw_linear_calibrated_external=11,raw_rational_external=13
            if (seqRep == 7 || seqRep == 8 || seqRep == 9 || seqRep == 11 || seqRep == 13) {
                return convertToNameValueUnit(attr, extCompReader.readValues(iid, dt));
            }
        }
        // read flags from external component file
        if (!isValid && isLcFlagsAttr) {
            SingleValue flags = extCompReader.readFlags(iid);
            if (flags != null) {
                return convertToNameValueUnit(attr, flags);
            }
        }
        
        // adjust datatype in case for internal values
        // and raw datatype differs from measurement quantity datatype
        if (isLcValuesAttr && isValid) {
            java.lang.Object jValue =  nvu.getValue().getValue();
            DataType valuesDt = null;
            if (jValue instanceof float[]) {
                valuesDt = DataType.DS_FLOAT;
            } else if (jValue instanceof double[]) {
                valuesDt = DataType.DS_DOUBLE;
            } else if (jValue instanceof byte[]) {
                valuesDt = DataType.DS_BYTE;
            } else if (jValue instanceof short[]) {
                valuesDt = DataType.DS_SHORT;
            } else if (jValue instanceof int[]) {
                valuesDt = DataType.DS_LONG;
            } else if (jValue instanceof long[]) {
                valuesDt = DataType.DS_LONGLONG;
            }
            
            if (valuesDt != null && dt != valuesDt) {
                nvu = new NameValueUnit(nvu.getValName(), valuesDt, jValue);
            }
        }
        
        // if flags attribute could neither be read from external component file or from the attribute in atfx,
        // return the global flag value
        else if (isLcFlagsAttr && !isValid) {
            int globalFlagAttrNo = getAttrNoByBaName(aid, AtfxTagConstants.LC_GLOBAL_FLAG);
            short globalFlag = getInstanceValue(aid, globalFlagAttrNo, iid).getValue().shortVal();
            
            // don't get the flags array's length from the values' union length!
            // this can easily cause high memory usage for example in mdf source 
            // files because all values would have to be read and cached
            int nrOfValues = 0;
            Relation parentMatrixRelation = getModelRelationByBaseName(aid, AtfxTagConstants.SUB_MATRIX);
            if (parentMatrixRelation != null) {
                List<Long> parentMatrixIds = getRelatedInstanceIds(aid, iid, parentMatrixRelation);
                if (parentMatrixIds.size() == 1) {
                    long matrixAid = parentMatrixRelation.getElement2().getId();
                    int nrOfRowsAttrNo = getAttrNoByBaName(matrixAid, AtfxTagConstants.MAT_ATTR_NROFROWS);
                    nrOfValues = getInstanceValue(matrixAid, nrOfRowsAttrNo, parentMatrixIds.get(0)).getValue().longVal();
                }
            } else {
                nrOfValues = getInstanceValue(aid, getAttrNoByBaName(aid, "values"), iid).getValueLength();
            }
            
            short[] flags = new short[nrOfValues];
            Arrays.fill(flags, globalFlag);
            nvu = new NameValueUnit(attr.getName(), attr.getDataType(), flags);
            isValid = true;
        }

        return !isValid ? new NameValueUnit(attr.getName(), dt, null) : nvu;
    }

    /**
     * Returns all values of an instance attribute of a given list of instances.
     * 
     * @param aid the application element id
     * @param attrNo the attribute number
     * @param iids the array of instance ids
     * @return collection containing all values, or null, if one or more of the instances do not possess the instance
     *         attribute.
     * @throws OpenAtfxException if the conversion from tsValue array to tsValueSeq fails
     */
    public Collection<NameValueUnit> getInstanceValues(long aid, int attrNo, Collection<Long> iids) throws OpenAtfxException {
        Attribute aa = getAttribute(aid, attrNo);

        // datatype
        boolean lcValuesAttr = aa.isLocalColumnValuesAttr();
        DataType dt = aa.getDataType();

        List<NameValueUnit> list = new ArrayList<>();
        DataType dtForValuesConsistencyCheck = null;
        for (long iid : iids) {
            dt = lcValuesAttr ? getDataTypeForLocalColumnValues(iid) : aa.getDataType();
            if (lcValuesAttr)
            {
                if (dtForValuesConsistencyCheck == null)
                {
                    dtForValuesConsistencyCheck = dt;
                }
                else if (dtForValuesConsistencyCheck != dt)
                {
                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                "Invalid query, please make sure you queried only local column values of the same datatype!");
                }
            }
            list.add(getInstanceValue(aid, attrNo, iid));
        }

        return list;
    }

    /**
     * Returns the instance of the related measurement quantity for a local column instance.
     * 
     * @param lcIid The local column instance id.
     * @return The measurement quantity instance id.
     * @throws OpenAtfxException Error getting instance.
     */
    private long getMeaQuantityInstance(long lcIid) throws OpenAtfxException {
        long lcAid = getUniqueElementByBasetype("aolocalcolumn").getId();
        AtfxRelation relMeaQua = getModelRelationByBaseName(lcAid, "measurement_quantity");
        Collection<Long> set = getRelatedInstanceIds(lcAid, lcIid, relMeaQua);
        if (set.size() != 1) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "None or multiple related instances found for base relation 'measurement_quantity' for local column with iid="
                                                + lcIid);
        }
        return set.iterator().next();
    }

    /**
     * In case this InstanceElement is from the ApplicationElement derived from "AoLocalColumn", the datatype of the
     * related "AoMeasurementQuantity" instance is returned.
     * 
     * @throws OpenAtfxException Error getting datatype.
     */
    DataType getDataTypeForLocalColumnValues(long lcIid) {
        long lcAid = getUniqueElementByBasetype("aolocalcolumn").getId();
        int seqRepAttrNo = getAttrNoByBaName(lcAid, "sequence_representation");
        NameValueUnit seqRepValue = getInstanceValue(lcAid, seqRepAttrNo, lcIid);
        int seqRepEnumVal = seqRepValue.getValue().enumVal();
        NameValueUnit dtValue = null;
        
        // first check whether to use the local column's raw datatype if available
        // (compare ODS chapter 4.4.5 (near the end))
        if (seqRepEnumVal == 4 || seqRepEnumVal == 5 || seqRepEnumVal >= 8) {
            Integer rawDatatypeAttrNo = getAttrNoByBaName(lcAid, "raw_datatype");
            if (rawDatatypeAttrNo != null) {
                NameValueUnit rawDtValue = getInstanceValue(lcAid, rawDatatypeAttrNo, lcIid);
                if (rawDtValue.getValue().enumVal() != 0)
                {
                    dtValue = getInstanceValue(lcAid, rawDatatypeAttrNo, lcIid);
                }
            }
        }
        // otherwise take the datatype from the AoMeasurementQuantity
        if (dtValue == null)
        {
            AtfxElement meaQuantityElement = getUniqueElementByBasetype("aomeasurementquantity");
            if (meaQuantityElement != null) {
                long meaQuaAid = meaQuantityElement.getId();
                long meaQuaIid = getMeaQuantityInstance(lcIid);
                int attrNo = getAttrNoByBaName(meaQuaAid, "datatype");
                dtValue = getInstanceValue(meaQuaAid, attrNo, meaQuaIid);
            }
        }
        
        if (dtValue != null && dtValue.isValid() && dtValue.getValue().discriminator() == DataType.DT_ENUM) {
            int val = dtValue.getValue().enumVal();
            if (val == 1) { // DT_STRING
                return DataType.DS_STRING;
            } else if (val == 2) { // DT_SHORT
                return DataType.DS_SHORT;
            } else if (val == 3) { // DT_FLOAT
                return DataType.DS_FLOAT;
            } else if (val == 4) { // DT_BOOLEAN
                return DataType.DS_BOOLEAN;
            } else if (val == 5) { // DT_BYTE
                return DataType.DS_BYTE;
            } else if (val == 6) { // DT_LONG
                return DataType.DS_LONG;
            } else if (val == 7) { // DT_DOUBLE
                return DataType.DS_DOUBLE;
            } else if (val == 8) { // DT_LONGLONG
                return DataType.DS_LONGLONG;
            } else if (val == 10) { // DT_DATE
                return DataType.DS_DATE;
            } else if (val == 11) { // DT_BYTESTR
                return DataType.DS_BYTESTR;
            } else if (val == 13) { // DT_COMPLEX
                return DataType.DS_COMPLEX;
            } else if (val == 14) { // DT_DCOMPLEX
                return DataType.DS_DCOMPLEX;
            } else if (val == 28) { // DT_EXTERNALREFERENCE
                return DataType.DS_EXTERNALREFERENCE;
            } else if (val == 30) { // DT_ENUM
                return DataType.DS_ENUM;
            }
        }
        throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR,
                                    "No valid datatype could be identified for local column with iid=" + lcIid);
    }

    /***********************************************************************************
     * instance relations
     ***********************************************************************************/

    /**
     * Creates instance relation.
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIids The target instance element ids.
     * @throws OpenAtfxException Error creating instance relation.
     */
    public void connectInstances(long aid, long iid, Relation applRel, Collection<Long> otherIids) {
        if (otherIids.isEmpty()) {
            return;
        }
        
        // add relation
        AtfxInstance instance1 = getInstance(aid, iid);
        instance1.addRelationValue(applRel, otherIids);
        
        // add inverse relation
        Element element2 = applRel.getElement2();
        for (Long iid2 : otherIids) {
            AtfxInstance instance2 = getInstance(element2.getId(), iid2);
            if (applRel.getInverseRelation() != null) {
                instance2.addRelationValue(applRel.getInverseRelation(), Arrays.asList(iid));
            }
        }
    }

    /**
     * Removes an instance relation
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIids The target instance element id.
     * @throws OpenAtfxException Error removing instance relation.
     */
    public void removeInstanceRelations(long aid, long iid, Relation applRel, Collection<Long> otherIids) {
        if (otherIids.isEmpty()) {
            return;
        }

        // remove relation
        AtfxInstance instance = getInstance(aid, iid);
        instance.removeRelatedIids(applRel, otherIids);
        
        // remove inverse relation
        if (otherIids != null && !otherIids.isEmpty()) {
            for (long removedIid : otherIids) {
                Instance inverseInstance = getInstance(applRel.getElement2().getId(), removedIid);
                inverseInstance.removeRelatedIids(applRel.getInverseRelation(), Arrays.asList(iid));
            }
        }
    }

    /**
     * Returns the instance ids of the related instances by given application relation.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param applRel The application relation.
     * @return Set of instance ids.
     */
    public List<Long> getRelatedInstanceIds(long aid, long iid, Relation applRel) {
        List<Long> relatedIids = new ArrayList<>();
        NameValueUnit nvu = getInstance(aid, iid).getValue(applRel.getRelationName());
        if (nvu != null) {
            DataType dt = nvu.getValue().discriminator();
            if (DataType.DS_LONG == dt) {
                Arrays.stream(nvu.getValue().longSeq()).mapToLong(Long::valueOf).forEach(relatedIids::add);
            } else if (DataType.DS_LONGLONG == dt) {
                Arrays.stream(nvu.getValue().longlongSeq()).forEach(relatedIids::add);
            } else if (DataType.DT_LONG == dt) {
                return Arrays.asList(Long.valueOf(nvu.getValue().longVal()));
            } else if (DataType.DT_LONGLONG == dt) {
                return Arrays.asList(nvu.getValue().longlongVal());
            } else {
                throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Unexpected datatype encountered at "
                        + applRel + ": " + dt);
            }
        }
        return relatedIids;
    }
    
//    public TS_ValueSeq getRelatedInstanceIds(long aid, List<Long> iids, String relationName) throws OpenAtfxException {
//        return getRelatedInstanceIds(aid, iids, getModelRelationByName(aid, relationName));
//    }
//
//    /**
//     * Returns the instance ids of the related instances by given application relation.
//     * @param aid The application element id.
//     * @param iids The instance ids.
//     * @param applRel The application relation. Only N to 1 relations allowed.
//     * @return TS_ValueSeq with the related instance ids
//     * @throws OpenAtfxException 
//     */
//    public TS_ValueSeq getRelatedInstanceIds(long aid, List<Long> iids, AtfxRelation applRel) throws OpenAtfxException {
//        if (applRel == null) {
//            throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Application relation cannot be null!");
//        }
//        
//        // first get related ids
//        Map<Long, List<Long>> relatedIidsBySourceIid = new HashMap<>();
//        for (long iid : iids) {
//            relatedIidsBySourceIid.put(iid, getRelatedInstanceIds(aid, iid, applRel));
//        }
//        
//        boolean isSequenceType = applRel.getRelationRangeMax() == -1;
//        DataType dt = isSequenceType ? DataType.DS_LONGLONG : DataType.DT_LONGLONG;
//        
//        // for better performance finding the respective index in the TS_Value for each iid
//        Map<Long, Integer> indexByIid = new HashMap<>();
//        for (int i = 0; i < iids.size(); i++) {
//            indexByIid.put(iids.get(i), i);
//        }
//        
//        // create values
//        TS_Value[] tsValues = new TS_Value[iids.size()];
//        for (Entry<Long, List<Long>> entry : relatedIidsBySourceIid.entrySet())
//        {
//            int index = indexByIid.get(entry.getKey());
//            List<Long> relatedIids = entry.getValue();
//            if (relatedIids.isEmpty())
//            {
//                tsValues[index] = ODSHelper.createEmptyTS_Value(dt);
//            }
//            else if (isSequenceType)
//            {
//                List<Long> ids = new ArrayList<>();
//                for (Long relatedIid : entry.getValue())
//                {
//                    ids.add(relatedIid);
//                }
//                tsValues[index] = ODSHelper.jObject2tsValue(dt, ids.toArray(new Long[0]));
//            }
//            else
//            {
//                tsValues[index] = ODSHelper.jObject2tsValue(dt, ODSHelper.asODSLongLong(entry.getValue().get(0)));
//            }
//        }
//        return ODSHelper.tsValue2tsValueSeq(tsValues, dt);
//    }
    
    public ByteOrder getByteOrder(long aidExtComp, long iidExtComp)
    {
        Instance extComp = getInstance(aidExtComp, iidExtComp);
        NameValueUnit typeNvu = extComp.getValueByBaseName("value_type");
        int valueType = typeNvu.getValue().enumVal();
        
        // dt_short_beo [7], dt_long_beo [8], dt_longlong_beo [9], ieeefloat4_beo [10], ieeefloat8_beo [11],
        // dt_boolean_flags_beo [15], dt_byte_flags_beo [16], dt_string_flags_beo [17], dt_bytestr_beo [18],
        // dt_sbyte_flags_beo [20], dt_ushort_beo [22], dt_ulong_beo [24], dt_string_utf8_beo [26]
        // dt_bit_int_beo [28], dt_bit_uint_beo [30], dt_bit_float_beo [32]
        if ((valueType == 7) || (valueType == 8) || (valueType == 9) || (valueType == 10) || (valueType == 11)
                || (valueType == 15) || (valueType == 16) || (valueType == 17) || (valueType == 18)
                || (valueType == 20) || (valueType == 22) || (valueType == 24) || (valueType == 26)
                || (valueType == 28) || (valueType == 30) || (valueType == 32)) {
            return ByteOrder.BIG_ENDIAN;
        }
        return ByteOrder.LITTLE_ENDIAN;
    }
    
    public Collection<AtfxEnumeration> getEnumerations() {
        return enumerations;
    }
    
    public AtfxEnumeration createEnumeration(String enumName) {
        if (enumName == null || enumName.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "enumName must not be empty");
        }
        
        for (AtfxEnumeration currentEnum : enumerations) {
            if (currentEnum.getName().equals(enumName)) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Enumeration with name '" + enumName + "' already exists, cannot create enumeration!");
            }
        }
        
        AtfxEnumeration newEnum = new AtfxEnumeration(nextEnumIndex.getAndIncrement(), enumName, this);
        enumerations.add(newEnum);
        return newEnum;
    }
    
    public void addEnumerationItem(String enumName, long item, String value) {
        if (item < 0) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "item for enumItem to add must not be < 0!");
        } else if (value == null || value.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "value for enumItem to add must not be empty!");
        }
        EnumerationDefinition enumeration = getEnumeration(enumName);
        if (enumeration == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration '" + enumName + "' not found!");
        }
        enumeration.addItem(item, value);
    }
    
    public void removeEnumeration(String enumName) {
        AtfxEnumeration foundEnum = null;
        for (AtfxEnumeration currentEnum : enumerations) {
            if (currentEnum.getName().equals(enumName)) {
                foundEnum = currentEnum;
                break;
            }
        }
        if (foundEnum != null) {
            Map<Long, Collection<String>> enumUsageEntry = enumerationUsages.get(enumName);
            if (enumUsageEntry != null) {
                for (Entry<Long, Collection<String>> entry : enumUsageEntry.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "Enumeration '"
                                + enumName + "' may not be deleted because it is in use by attribute(s) '" + Arrays.toString(entry.getValue().toArray()) + "'");
                    }
                }
            }
            enumerations.remove(foundEnum);
        }
    }
    
    public EnumerationDefinition getEnumeration(String enumName) {
        if (enumName == null || enumName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "enumName must not be empty");
        }
        
        // check base enums
        for (EnumerationDefinition enumDef : baseModel.getEnumerations()) {
            if (enumDef.getName().equals(enumName)) {
                return enumDef;
            }
        }
        
        // check application enums
        for (AtfxEnumeration currentEnum : enumerations) {
            if (currentEnum.getName().equals(enumName)) {
                return currentEnum;
            }
        }
        return null;
    }
    
//    public Enumeration getEnumForAttr(long aid, String attrName) {
//        Map<String, Enumeration> enumByAttrName = attrToEnumMap.get(aid);
//        if (enumByAttrName != null) {
//            return enumByAttrName.get(attrName);
//        }
//        return null;
//    }
//    
//    public long getEnumItemForAttrValue(long aid, String attrName, String attrValue) throws OpenAtfxException {
//        Enumeration identifiedEnum = getEnumForAttr(aid, attrName);
//        if (identifiedEnum != null) {
//            for (Item item : identifiedEnum.getItem()) {
//                if (item.getName().equals(attrValue)) {
//                    return item.getValue();
//                }
//            }
//        }
//        Element element = aidToElement.get(aid);
//        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No enum item with value '" + attrValue
//                + "' could be identified for attribute '" + attrName + "' at " + element);
//    }

    void addUnitMapping(long id, String name) {
        unitIids2UnitNames.put(id, name);
    }
    
    public long getUnitId(String unitName) {
        for (Entry<Long, String> entry : unitIids2UnitNames.entrySet()) {
            if (unitName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public String getUnitString(long unitId) {
        return unitIids2UnitNames.get(unitId);
    }
    
    public String getLcValuesAaName() {
        Collection<Long> localColumnElements = beNameToAid.get("aolocalcolumn");
        if (localColumnElements != null && !localColumnElements.isEmpty()) {
            AtfxElement lcElement = aidToElement.get(localColumnElements.iterator().next());
            Attribute valuesAttr = lcElement.getAttributeByBaseName("values");
            if (valuesAttr != null) {
                return valuesAttr.getName();
            }
        }
        return null;
    }
    
    private NameValueUnit convertToGenerationParameters(NameValueUnit source) {
        DataType sourceDt = source.getValue().discriminator();
        if (DataType.DS_DOUBLE == sourceDt) {
            return source;
        }
        
        SingleValue value = new SingleValue();
        double[] ar = new double[source.getValue().getLength()];
        if ((sourceDt == DataType.DS_SHORT)) {
            short[] seq = source.getValue().shortSeq();
            for (int i = 0; i < ar.length; i++) {
                ar[i] = seq[i];
            }
        } else if ((sourceDt == DataType.DS_LONG)) {
            int[] seq = source.getValue().longSeq();
            for (int i = 0; i < ar.length; i++) {
                ar[i] = seq[i];
            }
        } else if ((sourceDt == DataType.DS_BYTE)) {
            byte[] seq = source.getValue().byteSeq();
            for (int i = 0; i < ar.length; i++) {
                ar[i] = seq[i];
            }
        } else if ((sourceDt == DataType.DS_LONGLONG)) {
            long[] seq = source.getValue().longlongSeq();
            for (int i = 0; i < ar.length; i++) {
                ar[i] = seq[i];
            }
        } else if ((sourceDt == DataType.DS_FLOAT)) {
            float[] seq = source.getValue().floatSeq();
            for (int i = 0; i < ar.length; i++) {
                ar[i] = seq[i];
            }
        } else {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, "Unable to convert value from datatype '" + sourceDt
                    + "' to datatype '" + DataType.DS_DOUBLE + "'");
        }
        value.doubleSeq(ar);
        return new NameValueUnit(source, value);
    }
    
    private NameValueUnit convertToNameValueUnit(Attribute attr, SingleValue value) {
        return new NameValueUnit(attr.getName(), value, getUnitString(attr.getUnitId()));
    }
}
