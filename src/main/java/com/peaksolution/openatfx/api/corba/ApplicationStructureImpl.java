package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructurePOA;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.api.AtfxElement;
import com.peaksolution.openatfx.api.AtfxRelation;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;
import com.peaksolution.openatfx.util.ODSHelper;
import com.peaksolution.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.ApplicationStructure</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
class ApplicationStructureImpl extends ApplicationStructurePOA {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStructureImpl.class);

    private final AoSession aoSession;
    private final OpenAtfxAPIImplementation api;
    private final CorbaAtfxCache corbaCache;

    /**
     * Constructor.
     * 
     * @param corbaCache The ATFX cache.
     * @param api The OpenAtfxAPI.
     * @param aoSession The AoSession.
     * @param baseStructure The BaseStructure.
     * @throws AoException Error creating application structure.
     */
    public ApplicationStructureImpl(CorbaAtfxCache corbaCache, OpenAtfxAPIImplementation api, AoSession aoSession,
            BaseStructure baseStructure) throws AoException {
        this.corbaCache = corbaCache;
        this.api = api;
        this.aoSession = aoSession;
    }
    
    public void loadInitialData(BaseStructure baseStructure, AoSessionImpl session) throws AoException {
        try {
            // load enumerations
            for (String enumName : api.listEnumerationNames(true)) {
                com.peaksolution.openatfx.api.EnumerationDefinition enumDef = api.getEnumerationDefinition(enumName);
                corbaCache.mapEnumeration(enumDef);
            }
            
            // load elements
            for (AtfxElement currentElement : api.getAtfxElements()) {
                BaseElement baseElement = baseStructure.getElementByType(currentElement.getType());
                corbaCache.mapElement(currentElement, baseElement, session.getApplicationStructure());
            }
            
            // load relations
            for (AtfxElement currentElement : api.getAtfxElements()) {
                for (AtfxRelation currentRelation : currentElement.getAtfxRelations()) {
                    corbaCache.mapApplicationRelation(currentRelation, currentRelation.getInverseRelation());
                }
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getSession()
     */
    public AoSession getSession() throws AoException {
        return this.aoSession;
    }

    /***************************************************************************************
     * methods for enumeration definitions
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#createEnumerationDefinition(java.lang.String)
     */
    public EnumerationDefinition createEnumerationDefinition(String enumName) throws AoException {
        try {
            com.peaksolution.openatfx.api.EnumerationDefinition existingEnumDef = api.getEnumerationDefinition(enumName);
            // check for existing enum name
            if (existingEnumDef != null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "EnumerationDefinition with name '" + enumName + "' already exists");
            }
            
            // create enumeration definition
            com.peaksolution.openatfx.api.EnumerationDefinition createdEnum = api.createEnumeration(enumName);
            return corbaCache.mapEnumeration(createdEnum);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listEnumerations()
     */
    public String[] listEnumerations() throws AoException {
        return api.listEnumerationNames(true).toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getEnumerationDefinition(java.lang.String)
     */
    public EnumerationDefinition getEnumerationDefinition(String enumName) throws AoException {
        EnumerationDefinition foundEnum = corbaCache.getEnumerationDefinition(enumName);
        if (foundEnum != null) {
            return foundEnum;
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "EnumerationDefinition '" + enumName
                + "' not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#removeEnumerationDefinition(java.lang.String)
     */
    public void removeEnumerationDefinition(String enumName) throws AoException {
        // lookup enumeration to remove
        EnumerationDefinition enumDefToDelete = getEnumerationDefinition(enumName);
        
        // remove
        try {
            api.removeEnumeration(enumName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        corbaCache.remove(enumDefToDelete);
    }

    /***************************************************************************************
     * methods for application elements
     ***************************************************************************************/
    
    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#createElement(org.asam.ods.BaseElement)
     */
    public ApplicationElement createElement(BaseElement baseElem) throws AoException {
        // check baseElem not null
        if (baseElem == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "baseElem must not be empty");
        }
        try {
            // check if element with empty name already exists
            if (api.getElementByName("") != null) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "Creating a new application element with already having other elements with empty name is not allowed!");
            }
    
            // create application element
            AtfxElement createdElement = api.createAtfxElement(baseElem.getType(), "");
            return corbaCache.mapElement(createdElement, baseElem, _this());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listElements(java.lang.String)
     */
    public String[] listElements(String aePattern) throws AoException {
        return api.getElements(aePattern).stream().map(Element::getName).toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElements(java.lang.String)
     */
    public ApplicationElement[] getElements(String aePattern) throws AoException {
        Collection<ApplicationElement> elements = new ArrayList<>();
        for (Element currentElement : api.getElements(aePattern)) {
            elements.add(corbaCache.getApplicationElementById(currentElement.getId()));
        }
        return elements.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listElementsByBaseType(java.lang.String)
     */
    public String[] listElementsByBaseType(String aeType) throws AoException {
        Collection<String> list = new HashSet<>();
        for (Element currentElement : api.getElementsByBaseType(aeType)) {
            list.add(corbaCache.getApplicationElementNameById(currentElement.getId()));
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElementsByBaseType(java.lang.String)
     */
    public ApplicationElement[] getElementsByBaseType(String aeType) throws AoException {
        List<ApplicationElement> list = new ArrayList<>();
        for (Element currentElement : api.getElementsByBaseType(aeType)) {
            list.add(corbaCache.getApplicationElementById(currentElement.getId()));
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listTopLevelElements(java.lang.String)
     */
    public String[] listTopLevelElements(String aeType) throws AoException {
        List<String> list = new ArrayList<>();
        for (ApplicationElement applElem : getTopLevelElements(aeType)) {
            list.add(applElem.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getTopLevelElements(java.lang.String)
     */
    public ApplicationElement[] getTopLevelElements(String bePattern) throws AoException {
        List<ApplicationElement> list = new ArrayList<>();
        for (Element currentElement : api.getElements()) {
            if (currentElement.isTopLevelElement() && PatternUtil.nameFilterMatch(currentElement.getType(), bePattern)) {
                // handle special case with environment or self as father
                boolean skip = false;
                for (Relation currentRelation : currentElement.getRelations()) {
                    if (com.peaksolution.openatfx.api.Relationship.FATHER == currentRelation.getRelationship()
                            && ("AoEnvironment".equalsIgnoreCase(currentRelation.getElement2().getType())
                                    || currentElement.getName().equalsIgnoreCase(currentRelation.getElement2().getName()))) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                list.add(corbaCache.getApplicationElementById(currentElement.getId()));
            }
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElementById(org.asam.ods.T_LONGLONG)
     */
    public ApplicationElement getElementById(T_LONGLONG aeId) throws AoException {
        ApplicationElement ae = corbaCache.getApplicationElementById(ODSHelper.asJLong(aeId));
        if (ae == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "ApplicationElement with id="
                    + ODSHelper.asJLong(aeId) + " not found");
        }
        return ae;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElementByName(java.lang.String)
     */
    public ApplicationElement getElementByName(String aeName) throws AoException {
        // check aeName length
        if (aeName == null || aeName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "aeName must not be empty");
        }
        ApplicationElement ae = corbaCache.getApplicationElementByName(aeName);
        if (ae == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "ApplicationElement '" + aeName
                    + "' not found");
        }
        return ae;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#removeElement(org.asam.ods.ApplicationElement)
     */
    public void removeElement(ApplicationElement applElem) throws AoException {
        // check for null ae
        if (applElem == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "ApplicationElement must not be null");
        }
        long aid = ODSHelper.asJLong(applElem.getId());
        // remove application element
        corbaCache.removeApplicationElement(aid, applElem);
        LOG.debug("Removed application element aid={}", aid);
    }

    /***************************************************************************************
     * methods for application relations
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#createRelation()
     */
    public ApplicationRelation createRelation() throws AoException {
        AtfxRelation newRelation = api.createAtfxRelation(null, null, null, null, null, (short)0, (short)0);
        return corbaCache.mapApplicationRelation(newRelation, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getRelations(org.asam.ods.ApplicationElement,
     *      org.asam.ods.ApplicationElement)
     */
    public ApplicationRelation[] getRelations(ApplicationElement applElem1, ApplicationElement applElem2)
            throws AoException {
        if (applElem1 == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                                  "Parameter applElem1 must not be null!");
        }
        if (applElem2 == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                                  "Parameter applElem2 must not be null!");
        }

        long aidElem1 = ODSHelper.asJLong(applElem1.getId());
        long aidElem2 = ODSHelper.asJLong(applElem2.getId());

        List<ApplicationRelation> list = new ArrayList<>();
        for (ApplicationRelation rel : this.corbaCache.getApplicationRelations(aidElem1)) {
            long relAidElem2 = ODSHelper.asJLong(rel.getElem2().getId());
            if (aidElem2 == relAidElem2) {
                list.add(rel);
            }
        }

        return list.toArray(new ApplicationRelation[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#removeRelation(org.asam.ods.ApplicationRelation)
     */
    public void removeRelation(ApplicationRelation applRel) throws AoException {
        if (applRel == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Parameter applRel may not be null!");
        }
        // remove from cache
        this.corbaCache.removeApplicationRelation(applRel);
    }

    /***************************************************************************************
     * methods for instance elements
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getInstancesById(org.asam.ods.ElemId[])
     */
    public InstanceElement[] getInstancesById(ElemId[] ieIds) throws AoException {
        List<InstanceElement> list = new ArrayList<InstanceElement>();
        for (ElemId elemId : ieIds) {
            ApplicationElement ae = getElementById(elemId.aid);
            list.add(ae.getInstanceById(elemId.iid));
        }
        return list.toArray(new InstanceElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getInstanceByAsamPath(java.lang.String)
     */
    public InstanceElement getInstanceByAsamPath(String asamPath) throws AoException {
        InstanceElement currentInstance = null;

        Element envElement = api.getUniqueElementByBaseType("aoenvironment");
        // split by '/' considering escaping
        for (String p : asamPath.split("(?<!\\\\)/")) {

            // split by ']' considering escaping
            String[] xAr = p.split("(?<!\\\\)]");
            if (xAr.length != 2) {
                continue;
            }
            String aeName = PatternUtil.unEscapeNameForASAMPath(xAr[0].substring(1, xAr[0].length()));

            // split by ';' considering escaping
            String[] yAr = xAr[1].split("(?<!\\\\);");
            if (yAr.length < 1) {
                continue;
            }
            String ieName = PatternUtil.unEscapeNameForASAMPath(yAr[0]);
            String ieVersion = yAr.length == 2 ? PatternUtil.unEscapeNameForASAMPath(yAr[1]) : "";

            // skip environment instance
            if (envElement != null && aeName.equals(envElement.getName())) {
                continue;
            }

            // top level instance
            if (currentInstance == null) {
                InstanceElementIterator iter = getElementByName(aeName).getInstances("*");
                currentInstance = getSingleInstanceByNameAndVersion(iter, ieName, ieVersion);
                iter.destroy();
            }
            // child instance
            else {
                InstanceElementIterator iter = currentInstance.getRelatedInstancesByRelationship(Relationship.CHILD,
                                                                                                 "*");
                currentInstance = getSingleInstanceByNameAndVersion(iter, ieName, ieVersion);
                iter.destroy();
            }
        }

        if (currentInstance == null) {
            throw new AoException(ErrorCode.AO_INVALID_ASAM_PATH, SeverityFlag.ERROR, 0, "Invalid ASAMPath: "
                    + asamPath);
        }

        LOG.debug("Found instance for ASAMPath: '" + asamPath + "'");
        return currentInstance;
    }

    /**
     * Lookup given array of instances for an instance with given name and version.
     * <p>
     * Only a single instance will be returned. If none or multiple instances found, and exception will be thrown.
     * 
     * @param instances Collection of instances.
     * @param name The name to lookup.
     * @param version The version to lookup.
     * @return The instance.
     * @throws AoException Error reading instance values or none or multiple instances found.
     */
    private InstanceElement getSingleInstanceByNameAndVersion(InstanceElementIterator iter, String name, String version)
            throws AoException {
        List<InstanceElement> found = new ArrayList<InstanceElement>();
        for (int i = 0; i < iter.getCount(); i++) {
            InstanceElement ie = iter.nextOne();
            if (ie.getName().equals(name)) {
                if (version != null && version.length() > 0) {
                    String v = ODSHelper.getStringVal(ie.getValueByBaseName("version"));
                    if (v.equals(version)) {
                        found.add(ie);
                    }
                } else {
                    found.add(ie);
                }
            }
        }
        if (found.size() < 1) {
            throw new AoException(ErrorCode.AO_INVALID_ASAM_PATH, SeverityFlag.ERROR, 0, "No instance found for name='"
                    + name + "',version='" + version + "'");
        } else if (found.size() > 1) {
            throw new AoException(ErrorCode.AO_INVALID_ASAM_PATH, SeverityFlag.ERROR, 0,
                                  "Multiple instances found for name='" + name + "',version='" + version + "'");
        }
        return found.get(0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#createInstanceRelations(org.asam.ods.ApplicationRelation,
     *      org.asam.ods.InstanceElement[], org.asam.ods.InstanceElement[])
     */
    public void createInstanceRelations(ApplicationRelation applRel, InstanceElement[] elemList1,
            InstanceElement[] elemList2) throws AoException {
        // TODO Auto-generated method stub

    }

    /***************************************************************************************
     * method for application model check
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#check()
     */
    public void check() throws AoException {
        Set<String> relNames = new HashSet<>();
        Set<String> invRelNames = new HashSet<>();

        for (ApplicationElement ae : this.corbaCache.getApplicationElements()) {
            long aid = ODSHelper.asJLong(ae.getId());

            // check for base element
            if (ae.getBaseElement() == null) {
                throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                      "Application element has not base element. aid=" + aid);
            }

            // check for empty name
            String aeName = ae.getName();
            if ((aeName.length() < 1) || (aeName.length() > 30)) {
                throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                      "Application element name length must be between 1 and 30 characters. aid=" + aid);
            }

            // check mandatory base attributes
            for (BaseAttribute ba : ae.getBaseElement().getAttributes("*")) {
                if (ba.isObligatory() && (ae.getAttributeByBaseName(ba.getName()) == null)) {
                    try {
                        // throws exception when attribute not found
                        ae.getAttributeByBaseName(ba.getName());
                    } catch (AoException aoe) {
                        throw new AoException(ErrorCode.AO_MISSING_ATTRIBUTE, SeverityFlag.ERROR, 0,
                                              "Application element is missing mandatory base attribute. aid=" + aid
                                                      + ",baseAttr=" + ba.getName());
                    }
                }
            }

            // check application attributes
            Set<String> baseAttrs = new HashSet<>();
            for (ApplicationAttribute aa : ae.getAttributes("*")) {
                String aaName = aa.getName();
                // check application attribute name length
                if ((aaName.length() < 1) || (aaName.length() > 30)) {
                    throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                          "Attribute name length must be between 1 and 30 characters. aid=" + aid);
                }
                // check base attribute
                String baName = aa.getBaseAttribute() == null ? null : aa.getBaseAttribute().getName();
                if (baName != null && baseAttrs.contains(baName)) {
                    throw new AoException(ErrorCode.AO_DUPLICATE_BASE_ATTRIBUTE, SeverityFlag.ERROR, 0,
                                          "A base attribute may only be used once within an application element. aid="
                                                  + aid + ",baName=" + baName);
                }
            }

            // check application relations
            for (ApplicationRelation rel : ae.getAllRelations()) {
                String relName = rel.getRelationName();
                String invRelName = rel.getInverseRelationName();

                // elem1 / elem2
                if (rel.getElem1() == null) {
                    throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                                          "Elem1 of relation not set. aid=" + aid + ",relName=" + relName
                                                  + ",invRelname=" + invRelName);
                }
                if (rel.getElem2() == null) {
                    throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0,
                                          "Elem2 of relation not set. aid=" + aid + ",relName=" + relName
                                                  + ",invRelname=" + invRelName);
                }

                if ((relName.length() < 1) || (relName.length() > 30)) {
                    throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                          "Relation name length must be between 1 and 30 characters. aid=" + aid
                                                  + ",relName=" + relName + ",invRelname=" + invRelName);
                }
                if ((invRelName.length() < 1) || (invRelName.length() > 30)) {
                    throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                          "Inverse relation name length must be between 1 and 30 characters. aid="
                                                  + aid + ",relName=" + relName + ",invRelname=" + invRelName);
                }
                relNames.add("[" + rel.getElem1().getName() + "]" + relName);
                invRelNames.add("[" + rel.getElem2().getName() + "]" + invRelName);

                // check if base types of application elements matches the base types of the relations
                BaseRelation baseRel = rel.getBaseRelation();
                if (baseRel != null) {
                    String relType1 = rel.getElem1().getBaseElement().getType();
                    String bTypeElem1 = baseRel.getElem1().getType();
                    if (!relType1.equals(bTypeElem1)) {
                        throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                              "Wrong base relation targets! Expected '" + relType1 + "', found '"
                                                      + bTypeElem1 + "' for relation '" + rel.getRelationName() + "'");
                    }

                    String relType2 = rel.getElem2().getBaseElement().getType();
                    String bTypeElem2 = baseRel.getElem2().getType();
                    if (!relType2.equals(bTypeElem2)) {
                        throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                              "Wrong base relation targets! Expected '" + relType2 + "', found '"
                                                      + bTypeElem2 + "' for relation '" + rel.getRelationName() + "'");
                    }
                }
            }
        }

        // check if every relation has an inverse relation
        relNames.removeAll(invRelNames);
        if (!relNames.isEmpty()) {
            throw new AoException(ErrorCode.AO_MISSING_RELATION, SeverityFlag.ERROR, 0,
                                  "No inverse relations found for application relations: " + relNames);
        }
    }
}
