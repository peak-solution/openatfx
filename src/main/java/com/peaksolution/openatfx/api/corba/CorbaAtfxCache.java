package com.peaksolution.openatfx.api.corba;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationAttributeHelper;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationElementHelper;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationHelper;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationDefinitionHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementHelper;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementIteratorHelper;
import org.asam.ods.NameValueUnit;
import org.asam.ods.ODSWriteTransfer;
import org.asam.ods.ODSWriteTransferHelper;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.api.AtfxAttribute;
import com.peaksolution.openatfx.api.AtfxElement;
import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.Attribute;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.Instance;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.api.SingleValue;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Central cache holding all corba data for performance reasons.
 * 
 * @author Markus Renner
 */
public class CorbaAtfxCache {
    private static final Logger LOG = LoggerFactory.getLogger(CorbaAtfxCache.class);
    
    private final OpenAtfxAPIImplementation api;
    private final POA modelPOA;
    private final BaseStructure baseStructure;
    private final IFileHandler fileHandler;
    private final AoSessionImpl session;
    private POA instancePOA;
    
    /** enumeration definitions */
    private final List<EnumerationDefinition> enumerationDefinitions = new ArrayList<>();
    
    /** application elements */
    private final Map<Long, ApplicationElement> elementByAid = new HashMap<>();
    
    /** application attributes */
    private final Map<Long, Map<Integer, ApplicationAttribute>> attributeByNoByAid = new HashMap<>();

    /** application relations */
    private final Map<Long, Map<Integer, ApplicationRelation>> relsByNoByAid = new HashMap<>();

    /** instance relations */
    private final Map<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>> instanceRelMap; // <aid,<iid,<applRel,relInstIds>>>

    /** instance element CORBA object references */
    private final Map<Long, Map<Long, InstanceElement>> instanceElementCache; // <aid,<iid,<InstanceElement>>>

    /** the instance iterator references */
    private final Map<Long, InstanceElement[]> instanceIteratorElementCache;
    private final Map<Long, Integer> instanceIteratorPointerCache;
    private long nextInstanceIteratorId = 0;
    
    /**
     * cache for ODSWriteTransferImplementations, mapped by the aid and iid of the respective ODSFile instance
     */
    private final Map<Long, Map<Long, ODSWriteTransfer>> writeTransferCache;

    /**
     * COnstructor.
     * 
     * @param api The OpenAtfxAPI.
     * @param modelPOA
     * @param baseStructure The CORBA base structure for base model lookups.
     * @param fileHandler 
     */
    public CorbaAtfxCache(OpenAtfxAPIImplementation api, POA modelPOA, BaseStructure baseStructure, IFileHandler fileHandler, AoSessionImpl session) {
        this.api = api;
        this.modelPOA = modelPOA;
        this.baseStructure = baseStructure;
        this.fileHandler = fileHandler;
        this.session = session;
        
        this.instanceRelMap = new HashMap<>();
        this.instanceElementCache = new HashMap<>();
        this.instanceIteratorElementCache = new HashMap<>();
        this.instanceIteratorPointerCache = new HashMap<>();
        this.writeTransferCache = new HashMap<>();
    }
    
    public void setInstancePOA(POA instancePOA) {
        this.instancePOA = instancePOA;
    }
    
    public IFileHandler getFileHandler() {
        return fileHandler;
    }
    
    AoSessionImpl getSessionImpl() {
        return session;
    }
    
    /***********************************************************************************
     * enumeration definitions
     ***********************************************************************************/
    
    private EnumerationDefinition transformToEnumerationDefinition(com.peaksolution.openatfx.api.EnumerationDefinition newEnum) throws AoException {
        EnumerationDefinitionImpl enumDefImpl = new EnumerationDefinitionImpl(newEnum);
        EnumerationDefinition enumDef;
        try {
            enumDef = EnumerationDefinitionHelper.narrow(modelPOA.servant_to_reference(enumDefImpl));
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        enumerationDefinitions.add(enumDef);
        return enumDef;
    }
    
    public EnumerationDefinition mapEnumeration(com.peaksolution.openatfx.api.EnumerationDefinition atfxEnum) throws AoException {
        return transformToEnumerationDefinition(atfxEnum);
    }
    
    public EnumerationDefinition getEnumerationDefinition(String enumName) throws AoException {
        // check enum name length
        if (enumName == null || enumName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "enumName must not be empty");
        }
        for (EnumerationDefinition enumDef : this.enumerationDefinitions) {
            if (enumDef.getName().equals(enumName)) {
                return enumDef;
            }
        }
        return null;
    }
    
    public boolean remove(EnumerationDefinition enumDef) throws AoException {
        deleteCorbaObject(enumDef);
        return enumerationDefinitions.remove(enumDef);
    }
    
    /***********************************************************************************
     * application elements
     ***********************************************************************************/
    
    private ApplicationElement transformToApplicationElement(AtfxElement atfxElement, BaseElement baseElem, ApplicationStructure as) throws AoException {
        ApplicationElementImpl aeImpl = new ApplicationElementImpl(this.modelPOA, this.instancePOA, this.api, this,
                                                                   as, baseElem, atfxElement);
        try {
            ApplicationElement ae = ApplicationElementHelper.narrow(modelPOA.servant_to_reference(aeImpl));
            elementByAid.put(atfxElement.getId(), ae);
            return ae;
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        
    }
    
    public ApplicationElement mapElement(AtfxElement atfxElement, BaseElement baseElem, ApplicationStructure as) throws AoException {
        BaseElement baseElement = baseElem;
        if (baseElem == null && atfxElement.getType() != null && !atfxElement.getType().isBlank()) {
            baseElement = baseStructure.getElementByType(atfxElement.getType());
        }
        ApplicationElement ae = transformToApplicationElement(atfxElement, baseElement, as);
        
        for (AtfxAttribute currentAttribute : atfxElement.getAtfxAttributes()) {
            mapApplicationAttribute(atfxElement.getId(), currentAttribute);
        }
        return ae;
    }

    /**
     * Returns the list of all application elements.
     * 
     * @return All application elements.
     */
    public Collection<ApplicationElement> getApplicationElements() {
        return this.elementByAid.values();
    }

    /**
     * Returns an application element by given id.
     * 
     * @param aid The application element id.
     * @return The application element, null if not found.
     */
    public ApplicationElement getApplicationElementById(long aid) {
        return this.elementByAid.get(aid);
    }

    /**
     * Returns the application element name by id.
     * 
     * @param aid The application element id.
     * @return The application element name.
     */
    public String getApplicationElementNameById(long aid) {
        return api.getElementById(aid).getName();
    }

    /**
     * Returns the application element for given name.
     * 
     * @param aeName The application element name.
     * @return The application element, null if not found.
     */
    public ApplicationElement getApplicationElementByName(String aeName) {
        Element element = api.getElementByName(aeName);
        if (element != null) {
            return this.elementByAid.get(element.getId());
        }
        return null;
    }

    /**
     * Rename an application element.
     * 
     * @param aid The application element id.
     * @param newAeName The new name.
     */
    public void renameApplicationElement(long aid, String newAeName) {
        api.renameElement(aid, newAeName);
    }
    
    /**
     * Removed an application element from the cache.
     * 
     * @param aid The application element id.
     * @param element The element to remove.
     * @throws AoException 
     */
    public void removeApplicationElement(long aid, ApplicationElement element) throws AoException {
        // remove and deactivate CORBA objects for attributes
        Map<Integer, ApplicationAttribute> attrsByNo = attributeByNoByAid.remove(aid);
        if (attrsByNo != null) {
            for (Entry<Integer, ApplicationAttribute> entry : attrsByNo.entrySet()) {
                deleteCorbaObject(entry.getValue());
            }
        }
        
        // remove and deactivate CORBA objects for relations
        for (ApplicationRelation rel : element.getAllRelations()) {
            removeApplicationRelation(rel);
        }
        
        try {
            api.removeElement(aid);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        // deactivate CORBA object for element
        deleteCorbaObject(element);
        
        this.elementByAid.remove(aid);
        this.instanceRelMap.remove(aid);
        this.instanceElementCache.remove(aid);
    }
    
    /**
     * @param basetype
     * @return
     * @throws AoException
     */
    public Collection<Long> getAidsByBaseType(String basetype) throws AoException {
        try {
            return api.getElementsByBaseType(basetype).stream().map(Element::getId).toList();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /***********************************************************************************
     * application attributes
     ***********************************************************************************/

    private ApplicationAttribute transformToApplicationAttribute(long aid, AtfxAttribute atfxAttribute) throws AoException {
        try {
            ApplicationAttributeImpl aaImpl = new ApplicationAttributeImpl(this, atfxAttribute);
            ApplicationAttribute aa = ApplicationAttributeHelper.narrow(modelPOA.servant_to_reference(aaImpl));
            attributeByNoByAid.computeIfAbsent(aid, v -> new HashMap<>()).put(atfxAttribute.getAttrNo(), aa);
            return aa;
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    public ApplicationAttribute mapApplicationAttribute(long aid, AtfxAttribute atfxAttribute) throws AoException {
        return transformToApplicationAttribute(aid, atfxAttribute);
    }
    
    public ApplicationAttribute addApplicationAttribute(long aid, ApplicationAttribute aa) throws AoException {
        T_LONGLONG corbaUnitId = aa.getUnit();
        long unitId = 0;
        if (corbaUnitId != null) {
            unitId = ODSHelper.asJLong(corbaUnitId);
        }
        AtfxAttribute newAttribute = api.createAtfxAttribute(aid);
        newAttribute.setName(aa.getName());
        newAttribute.setDataType(ODSHelper.mapDataType(aa.getDataType()));
        newAttribute.setLength(aa.getLength());
        newAttribute.setUnitId(unitId);
        newAttribute.setEnumName(aa.getEnumerationDefinition().getName());
        newAttribute.setObligatory(aa.isObligatory());
        newAttribute.setUnique(aa.isUnique());
        newAttribute.setAutogenerated(aa.isAutogenerated());
        updateBaseAttribute(aid, newAttribute.getName(), aa.getBaseAttribute().getName());
        
        return mapApplicationAttribute(aid, newAttribute);
    }
    
    public void updateBaseAttribute(long aid, String attrName, String baseAttrName) throws AoException {
        try {
            api.updateBaseAttribute(aid, attrName, baseAttrName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    /**
     * @param attribute
     * @param aaDataType
     * @throws AoException
     */
    public void updateApplicationAttributeDataType(AtfxAttribute attribute, DataType aaDataType) throws AoException {
        // clear for existing instance values
        try {
            com.peaksolution.openatfx.api.DataType dt = ODSHelper.mapDataType(aaDataType);
            for (Instance instance : this.api.getInstances(attribute.getAid())) {
                new com.peaksolution.openatfx.api.NameValueUnit(attribute.getName(), dt, null, getUnitName(attribute.getUnitId()));
                this.api.setAttributeValues(attribute.getAid(), instance.getIid(), Arrays.asList());
            }
            attribute.setDataType(dt);
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * @param aid
     * @param attrName
     * @return
     */
    public ApplicationAttribute getApplicationAttribute(long aid, String attrName) {
        Element element = api.getElementById(aid);
        if (element != null) {
            Attribute attribute = element.getAttributeByName(attrName);
            if (attribute != null) {
                Map<Integer, ApplicationAttribute> attrMap = attributeByNoByAid.get(aid);
                if (attrMap != null) {
                    return attrMap.get(attribute.getAttrNo());
                }
            }
        }
        return null;
    }
    
    /**
     * @param aid
     * @param baseName
     * @return
     */
    public ApplicationAttribute getApplicationAttributeByBaseName(long aid, String baseName) {
        Element element = api.getElementById(aid);
        if (element != null) {
            Attribute attribute = element.getAttributeByBaseName(baseName);
            if (attribute != null) {
                Map<Integer, ApplicationAttribute> attrMap = attributeByNoByAid.get(aid);
                if (attrMap != null) {
                    return attrMap.get(attribute.getAttrNo());
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the ordered list of all available application attributes.
     * 
     * @param aid The application element id.
     * @return Collection of application attributes.
     */
    public Collection<ApplicationAttribute> getApplicationAttributes(long aid) {
        Map<Integer, ApplicationAttribute> attrMap = attributeByNoByAid.get(aid);
        if (attrMap != null) {
            return attrMap.values();
        }
        return Collections.emptyList();
    }
    
    /**
     * @param aid
     * @param oldAttrName
     * @param newAttrName
     * @throws AoException
     */
    public void renameApplicationAttribute(long aid, String oldAttrName, String newAttrName) throws AoException {
        try {
            api.renameAttribute(aid, oldAttrName, newAttrName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    /**
     * @param unitName
     * @return
     * @throws AoException
     */
    public T_LONGLONG getUnitId(String unitName) throws AoException {
        try {
            return ODSHelper.asODSLongLong(api.getUnitId(unitName));
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    /**
     * @param aaUnit
     * @return
     * @throws AoException
     */
    public String getUnitName(T_LONGLONG aaUnit) throws AoException {
        return getUnitName(ODSHelper.asJLong(aaUnit));
    }
    
    public String getUnitName(long unitId) throws AoException {
        try {
            return api.getUnitName(unitId);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * Removes an application attribute from the cache.
     * 
     * @param aid The application element id.
     * @param aaName The application attribute name.
     * @throws AoException
     */
    public void removeApplicationAttribute(long aid, String aaName) throws AoException {
        int attrNo = api.getElementById(aid).getAttributeByName(aaName).getAttrNo();
        try {
            api.removeAttribute(aid, aaName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        attributeByNoByAid.get(aid).remove(attrNo);
    }

    /***********************************************************************************
     * application relations
     ***********************************************************************************/

    private ApplicationRelation transformToApplicationRelation(AtfxRelation relation, AtfxRelation inverseRelation) throws AoException {
        try {
            // check if ApplicationRelation already exists
            if (relation.getElement1() != null) {
                Map<Integer, ApplicationRelation> existingElementRelations = relsByNoByAid.get(relation.getElement1().getId());
                if (existingElementRelations != null) {
                    ApplicationRelation existingRelation = existingElementRelations.get(relation.getRelNo());
                    if (existingRelation != null) {
                        return existingRelation;
                    }
                }
            }
            
            ApplicationRelationImpl arImpl = new ApplicationRelationImpl(this, relation);
            ApplicationRelation ar = ApplicationRelationHelper.narrow(modelPOA.servant_to_reference(arImpl));
            
            int relNo = relation.getRelNo();
            if (relNo < 0) {
                return ar;
            }
            relsByNoByAid.computeIfAbsent(relation.getElement1().getId(), v -> new HashMap<>()).put(relNo, ar);
            if (inverseRelation != null) {
                ApplicationRelationImpl arInvImpl = new ApplicationRelationImpl(this, inverseRelation);
                ApplicationRelation arInv = ApplicationRelationHelper.narrow(modelPOA.servant_to_reference(arInvImpl));
                relsByNoByAid.computeIfAbsent(relation.getElement2().getId(), v -> new HashMap<>()).put(inverseRelation.getRelNo(), arInv);
            }
            return ar;
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    public ApplicationRelation mapApplicationRelation(AtfxRelation relation, AtfxRelation inverseRelation) throws AoException {
        return transformToApplicationRelation(relation, inverseRelation);
    }
    
    /**
     * Returns all application relations of an application element.
     * 
     * @param aid The application element id.
     * @return Collection of application relations.
     */
    public Collection<ApplicationRelation> getApplicationRelations(long aid) {
        Map<Integer, ApplicationRelation> relMap = relsByNoByAid.get(aid);
        if (relMap != null) {
            return relMap.values();
        }
        return Collections.emptyList();
    }

    /**
     * Returns an application relation by given relation name.
     * 
     * @param aid The application element id.
     * @param relName The application relation name.
     * @return The application relation, null if relation not found.
     * @throws AoException Error getting relation name.
     */
    public ApplicationRelation getApplicationRelationByName(long aid, String relName) throws AoException {
        try {
            Relation rel = api.getRelationByName(aid, relName);
            return getApplicationRelationByRelNo(aid, rel.getRelNo());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    public ApplicationRelation getApplicationRelationByBaseName(long aid, String bRelName) throws AoException {
        try {
            Relation rel = api.getRelationByBaseName(aid, bRelName);
            return getApplicationRelationByRelNo(aid, rel.getRelNo());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    private ApplicationRelation getApplicationRelationByRelNo(long aid, int relNo) {
        Map<Integer, ApplicationRelation> relMap = relsByNoByAid.get(aid);
        if (relMap != null) {
            return relMap.get(relNo);
        }
        return null;
    }

    /**
     * Sets the application element
     * 
     * @param aid
     * @param applRel
     * @param relationName
     * @throws AoException 
     */
    public void setApplicationRelationElem1(long aid, ApplicationRelationImpl applRel, String relationName) throws AoException {
        try {
            api.setRelationElem1(aid, relationName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        try {
            Relation delegate = api.getRelationByName(aid, relationName);
            ApplicationRelation rel = ApplicationRelationHelper.narrow(this.modelPOA.servant_to_reference(applRel));
            relsByNoByAid.computeIfAbsent(aid, v -> new HashMap<>()).put(delegate.getRelNo(), rel);
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }
    
    public void setApplicationRelationElem2(AtfxRelation relation, long aid2) throws AoException {
        try {
            if (relation.getElement1() == null) {
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0, "For a newly created relation set the element1 before the element2!");
            }
            
            if (relation.getElement2() != null) {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Changing the element2 of a relation is not supported, if it has already been set!");
            }
            
            AtfxRelation newInverseRelation = api.setRelationElem2(relation.getElement1().getId(), aid2, relation.getRelationName());
            ApplicationRelationImpl arInvImpl = new ApplicationRelationImpl(this, newInverseRelation);
            ApplicationRelation arInv = ApplicationRelationHelper.narrow(modelPOA.servant_to_reference(arInvImpl));
            relsByNoByAid.computeIfAbsent(relation.getElement2().getId(), v -> new HashMap<>()).put(newInverseRelation.getRelNo(), arInv);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        catch (WrongPolicy | ServantNotActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * @param aid
     * @param relationName
     * @param relNo
     * @throws AoException
     */
    public void removeApplicationRelationElem1(long aid, String relationName, int relNo) throws AoException {
        try {
            api.removeRelationElem1(aid, relationName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        Map<Integer, ApplicationRelation> relsForAidByNo = relsByNoByAid.get(aid);
        if (relsForAidByNo != null) {
            relsForAidByNo.remove(relNo);
        }
    }
    
    /**
     * Removes an application relation from the cache.
     * 
     * @param applRel The application relation.
     * @throws AoException 
     */
    public void removeApplicationRelation(ApplicationRelation applRel) throws AoException {
        Relation relation = api.getRelationByName(ODSHelper.asJLong(applRel.getElem1().getId()), applRel.getRelationName());
        relsByNoByAid.get(relation.getElement1().getId()).remove(relation.getRelNo());
        ApplicationRelation invApplRel = getInverseApplicationRelation(relation);
        relsByNoByAid.get(relation.getElement2().getId()).remove(relation.getInverseRelation().getRelNo());
        
        // remove existing instance relations if any exists
        Map<Long, Map<ApplicationRelation, Set<Long>>> relsByIid = instanceRelMap.get(relation.getElement1().getId());
        if (relsByIid != null) {
            for (Entry<Long, Map<ApplicationRelation, Set<Long>>> iidEntry : relsByIid.entrySet()) {
                iidEntry.getValue().remove(applRel);
            }
        }
        // remove inverse instance relations
        relsByIid = instanceRelMap.get(relation.getElement2().getId());
        if (relsByIid != null) {
            for (Entry<Long, Map<ApplicationRelation, Set<Long>>> iidEntry : relsByIid.entrySet()) {
                iidEntry.getValue().remove(invApplRel);
            }
        }
        
        try {
            api.removeRelation(relation.getElement1().getId(), relation.getRelationName());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        // deactivate CORBA objects
        deleteCorbaObject(invApplRel);
        deleteCorbaObject(applRel);
    }

    /**
     * @param relation
     * @param baseRel
     * @throws AoException
     */
    public void updateBaseRelation(AtfxRelation relation, BaseRelation baseRel) throws AoException {
        try {
            if (baseRel != null) {
                Element element1 = relation.getElement1();
                if (element1 != null) {
                    long aid = element1.getId();
                    api.updateBaseRelation(aid, relation.getRelationName(), baseRel.getRelationName());
                } else {
                    LOG.warn("Tried to update base relation before element 1 was set to relation: {}", relation);
                }
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    /**
     * Returns the inverse relation for given application relation.
     * 
     * @param applRel The application relation.
     * @return The inverse application relation.
     * @throws AoException Unable to find inverse application relation.
     */
    public ApplicationRelation getInverseApplicationRelation(Relation applRel) throws AoException {
        try {
            long aid1 = applRel.getElement1().getId();
            long aid2 = applRel.getElement2().getId();
            Relation inverseRelation = api.getInverseRelation(aid1, applRel.getRelationName());
            return getApplicationRelationByRelNo(aid2, inverseRelation.getRelNo());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }
    
    /**
     * Returns the inverse relation for given base relation.
     * 
     * @param baseRel The base relation.
     * @return The inverse base relation.
     * @throws AoException Inverse base relation not found.
     */
    public BaseRelation getInverseBaseRelation(BaseRelation baseRel) throws AoException {
        for (BaseRelation invBaseRel : baseRel.getElem2().getAllRelations()) {
            if (invBaseRel.getElem2().getType().equals(baseRel.getElem1().getType())
                    && invBaseRel.getElem1().getType().equals(baseRel.getElem2().getType())
                    && invBaseRel.getRelationName().equals(baseRel.getInverseRelationName())) {
                return invBaseRel;
            }
        }
        throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                              "Unable to find inverse relation for base relation: " + baseRel.getRelationName());
    }

    /***********************************************************************************
     * instance elements
     ***********************************************************************************/

    /**
     * Adds an instance element to the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public void addInstanceElement(long aid, long iid) {
        this.instanceRelMap.get(aid).put(iid, new HashMap<>());
        for (ApplicationRelation rel : getApplicationRelations(aid)) {
            this.instanceRelMap.get(aid).get(iid).put(rel, new TreeSet<>());
        }
    }

    /**
     * Returns an instance element by given instance id.
     * 
     * @param instancePOA The POA for lazy creation of the CORBA object.
     * @param aid The application element id.
     * @param iid The instance id.
     * @return The instance element, null if not found.
     * @throws AoException Error lazy create CORBA instance element.
     */
    public InstanceElement getInstanceById(POA instancePOA, long aid, long iid) throws AoException {
        Instance existingInstance = null;
        try {
            existingInstance = api.getInstanceById(aid, iid);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        if (existingInstance != null) {
            Map<Long, InstanceElement> instanceById = instanceElementCache.computeIfAbsent(aid, v -> new HashMap<>());
            InstanceElement ie = instanceById.get(iid);
            if (ie == null) {
                byte[] oid = toBytArray(new long[] { 0, aid, iid }); // 0=InstanceElement
                org.omg.CORBA.Object obj;
                try {
                    obj = instancePOA.create_reference_with_id(oid, InstanceElementHelper.id());
                } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
                    LOG.error(e.getMessage(), e);
                    throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
                }
                ie = InstanceElementHelper.unchecked_narrow(obj);
                instanceById.put(iid, ie);
            }
            return ie;
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                              "Instance not found [aid=" + aid + ",iid=" + iid + "]");
    }

    public static byte[] toBytArray(long[] data) {
        if (data == null) {
            return new byte[0];
        }
        byte[] byts = new byte[data.length * 8];
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(toByteArray(data[i]), 0, byts, i * 8, 8);
        }
        return byts;
    }

    private static byte[] toByteArray(long data) {
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
            return new long [0];
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
     * @param instancePOA The instance POA.
     * @param aid The application element id.
     * @return Collection if instance elements.
     * @throws AoException Error lazy create CORBA instance element.
     */
    public InstanceElement[] getInstances(POA instancePOA, long aid) throws AoException {
        Collection<Instance> instances = null;
        try {
            instances = api.getInstances(aid);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        InstanceElement[] ies = new InstanceElement[instances.size()];
        int i = 0;
        for (Instance instance : instances) {
            ies[i] = getInstanceById(instancePOA, aid, instance.getIid());
            i++;
        }
        return ies;
    }
    
    /**
     * @param aid
     * @param iid
     * @param attrName
     * @param baseAttrName
     * @return
     */
    public NameValueUnit getInstanceValue(long aid, long iid, String attrName, String baseAttrName) {
        Instance instance = api.getInstanceById(aid, iid);
        if (baseAttrName != null && !baseAttrName.isBlank()) {
            return ODSHelper.mapNvu(instance.getValueByBaseName(baseAttrName));
        } else if (attrName != null && !attrName.isBlank()) {
            return ODSHelper.mapNvu(instance.getValue(attrName));
        }
        return null;
    }
    
    /**
     * Returns all values of an instance attribute of a given list of instances.
     * 
     * @param aid the application element id
     * @param attrName the attribute name
     * @param iids the array of instance ids
     * @return TS_ValueSeq containing all values, or null, if one or more of the instances do not possess the instance
     *         attribute.
     * @throws AoException if the conversion from tsValue array to tsValueSeq fails
     */
    public TS_ValueSeq getInstanceValues(long aid, String attrName, Collection<Long> iids) throws AoException {
        Element element = api.getElementById(aid);
        Attribute aa = element.getAttributeByName(attrName);

        // datatype
        boolean lcValuesAttr = isLocalColumnValuesAttr(element, aa.getBaseName());
        com.peaksolution.openatfx.api.DataType dt = aa.getDataType();

        List<TS_Value> list = new ArrayList<>();
        com.peaksolution.openatfx.api.DataType dtForValuesConsistencyCheck = null;
        for (Instance currentInstance : api.getInstances(aid, iids)) {
            dt = lcValuesAttr ? api.getDataTypeForLocalColumnValues(currentInstance.getIid()) : aa.getDataType();
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
            com.peaksolution.openatfx.api.NameValueUnit nvu = currentInstance.getValue(attrName);
            list.add(ODSHelper.mapTSValue(nvu == null ? new SingleValue(dt) : nvu.getValue()));
        }

        if (list.isEmpty()) {
            if (lcValuesAttr) {
                TS_UnionSeq u = new TS_UnionSeq();
                u.__default();
                return new TS_ValueSeq(u, new short[0]);
            }
            // when no iids were passed and the empty value sequence is generated here a datatype DT_UNKNOWN would
            // cause an exception
            return ODSHelper.tsValue2tsValueSeq(new TS_Value[0], ODSHelper.mapDataType(dt));
        } else {
            return ODSHelper.tsValue2tsValueSeq(list.toArray(new TS_Value[0]), ODSHelper.mapDataType(dt));
        }
    }
    
    private boolean isLocalColumnValuesAttr(Element element, String baseAttrName) {
        return "aolocalcolumn".equalsIgnoreCase(element.getType()) && "values".equalsIgnoreCase(baseAttrName);
    }

    /**
     * Removes an instance element from the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @throws AoException
     */
    public void removeInstanceElement(long aid, long iid) throws AoException {
        // remove relations
        for (ApplicationRelation applRel : getApplicationRelations(aid)) {
            removeInstanceRelations(aid, iid, applRel, getRelatedInstanceIds(aid, iid, applRel));
        }
        // remove instance values
        if (instanceRelMap.containsKey(aid)) {
            this.instanceRelMap.get(aid).remove(iid);
        }
        if (instanceElementCache.containsKey(aid)) {
            this.instanceElementCache.get(aid).remove(iid);
        }
        
        try {
            api.removeInstance(aid, iid);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * Returns the environment instance.
     * 
     * @param instancePOA
     * @return The environment instance, null if not application element derived from 'AoEnviroment' exists or no
     *         instance available.
     * @throws AoException if something went wrong
     */
    public InstanceElement getEnvironmentInstance(POA instancePOA) throws AoException {
        Collection<Element> envElements = null;
        try {
            envElements = api.getElementsByBaseType("aoenvironment");
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        
        if (envElements != null && !envElements.isEmpty()) {
            Element env = envElements.iterator().next();
            InstanceElement[] ieAr = this.getInstances(instancePOA, env.getId());
            if (ieAr.length > 0) {
                return ieAr[0];
            }
        }
        return null;
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
     * @throws AoException Error creating instance relation.
     */
    public void createInstanceRelations(long aid, long iid, ApplicationRelation applRel, Collection<Long> otherIids)
            throws AoException {
        if (otherIids.isEmpty()) {
            return;
        }
        
        try {
            api.setRelatedInstances(aid, iid, applRel.getRelationName(), otherIids, SetType.INSERT);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * Removes an instance relation
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIids The target instance element id.
     * @throws AoException Error removing instance relation.
     */
    public void removeInstanceRelations(long aid, long iid, ApplicationRelation applRel, Collection<Long> otherIids)
            throws AoException {
        if (otherIids.isEmpty()) {
            return;
        }
        
        try {
            api.removeRelatedInstances(aid, iid, applRel.getRelationName(), otherIids);
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    /**
     * Returns the instance ids of the related instances by given application relation.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param applRel The application relation.s
     * @return Set of instance ids.
     * @throws AoException Error getting inverse relation.
     */
    public List<Long> getRelatedInstanceIds(long aid, long iid, ApplicationRelation applRel) throws AoException {
        try {
            return api.getRelatedInstanceIds(aid, iid, applRel.getRelationName());
        } catch (OpenAtfxException e) {
            throw e.toAoException();
        }
    }

    public synchronized InstanceElementIterator newInstanceElementIterator(POA instancePOA, InstanceElement[] instances)
            throws AoException {
        nextInstanceIteratorId++;
        this.instanceIteratorElementCache.put(nextInstanceIteratorId, instances);
        this.instanceIteratorPointerCache.put(nextInstanceIteratorId, 0);

        byte[] oid = toBytArray(new long[] { 3, nextInstanceIteratorId, 0 }); // 3=InstanceElementIterator
        org.omg.CORBA.Object obj;
        try {
            obj = instancePOA.create_reference_with_id(oid, InstanceElementIteratorHelper.id());
        } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        return InstanceElementIteratorHelper.unchecked_narrow(obj);
    }

    public InstanceElement[] getIteratorInstances(long id) {
        return this.instanceIteratorElementCache.get(id);
    }

    public int getIteratorPointer(long id) {
        return this.instanceIteratorPointerCache.get(id);
    }

    public void setIteratorPointer(long id, int pointer) {
        this.instanceIteratorPointerCache.put(id, pointer);
    }

    public void removeInstanceIterator(long id) {
        this.instanceIteratorElementCache.remove(id);
        this.instanceIteratorPointerCache.remove(id);
    }
    
    public ODSWriteTransfer newWriteTransfer(POA modelPOA, POA instancePOA, long aid, long iid, String fileName, InstanceElementImpl fileInstance) throws AoException {
        if (getWriteTransfer(aid, iid) != null)
        {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Cannot create ODSFileTransfer for file (aid=" + aid + ",iid=" + iid
                                          + "), because transfer instance already exists!");
        }
        
        ODSWriteTransfer writeTransfer = null;
        try {
            String fileRoot = null;
            try {
                fileRoot = api.getContext().get("FILE_ROOT_EXTREF").getValue().stringVal();
            } catch (OpenAtfxException oae) {
                throw oae.toAoException();
            }
            
            String filePath = Paths.get(fileRoot).resolve(fileName).toString();
            OdsWriteTransfer transfer = new OdsWriteTransfer(filePath, fileInstance);
            writeTransfer = ODSWriteTransferHelper.narrow(modelPOA.servant_to_reference(transfer));
        } catch (Throwable e) { // weird behaviour using openJDK, thus expecting exception
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
        
        writeTransferCache.computeIfAbsent(aid, v -> new HashMap<>()).put(iid, writeTransfer);
        return writeTransfer;
    }
    
    public ODSWriteTransfer getWriteTransfer(long aid, long iid) {
        Map<Long, ODSWriteTransfer> map = writeTransferCache.get(aid);
        if (map != null) {
            return map.get(iid);
        }
        return null;
    }
    
    public BaseElement getBaseElement(String aeName) throws AoException {
        ApplicationElement ae = getApplicationElementByName(aeName);
        if (ae != null) {
            return ae.getBaseElement();
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                              "No element found for aeName '" + aeName + "'!");
    }

    /**
     * @param attribute
     * @return
     * @throws AoException
     */
    public BaseAttribute getBaseAttribute(AtfxAttribute attribute) throws AoException {
        if (attribute.isBaseAttrDerived()) {
            BaseAttribute[] baseAttrs = getApplicationElementById(attribute.getAid()).getBaseElement().getAttributes(attribute.getBaseName());
            if (baseAttrs.length > 0) {
                return baseAttrs[0];
            }
        }
        return null;
    }
    
    /**
     * Takes the possibility into account, that a base relation may have multiple target base elements and identifies
     * the correct CORBA base relation for the given AtfxRelation. This special handling is required, because CORBA base
     * relations can only handle a single target base element where in modern models the base relation contains the
     * information about all possible target base elements. So in CORBA there are two base relations while in modern
     * models there is one base relation with two target base elements for example.
     * 
     * @param relation
     * @return
     * @throws AoException
     */
    public BaseRelation getBaseRelation(AtfxRelation relation) throws AoException {
        com.peaksolution.openatfx.api.BaseRelation atfxBaseRelation = relation.getBaseRelation();
        if (atfxBaseRelation != null) {
            // iterate over all elem2s of the base relation if more than one
            if (atfxBaseRelation.getElem2().size() > 1) {
                for (com.peaksolution.openatfx.api.BaseElement baseElement2 : atfxBaseRelation.getElem2()) {
                    // filter the base element 2 that matches the actual relation's base element 2
                    if (relation.getAtfxElement2().getType().equalsIgnoreCase(baseElement2.getType())) {
                        // identify all relations between the exact two base elements of the actual relation
                        for (BaseRelation currentBaseRel : baseStructure.getRelations(baseStructure.getElementByType(atfxBaseRelation.getElem1()
                                                                                                                               .getType()),
                                                                                baseStructure.getElementByType(baseElement2.getType()))) {
                            // filter the one base relation between these elements that matches the name of the original base relation
                            if (currentBaseRel.getRelationName().equalsIgnoreCase(atfxBaseRelation.getName())) {
                                return currentBaseRel;
                            }
                        }
                    }
                }
            } else {
                return baseStructure.getRelations(baseStructure.getElementByType(atfxBaseRelation.getElem1().getType()),
                                                  baseStructure.getElementByType(atfxBaseRelation.getElem2().iterator().next().getType()))[0];
            }
        }
        return null;
    }
    
    public void deleteCorbaObject(org.omg.CORBA.Object object) throws AoException {
        try {
            byte[] id = modelPOA.reference_to_id(object);
            modelPOA.deactivate_object(id);
        } catch (WrongAdapter | WrongPolicy | ObjectNotActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }
}
