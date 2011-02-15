package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationElementHelper;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationHelper;
import org.asam.ods.ApplicationStructurePOA;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationDefinitionHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.ApplicationStructure</code>.
 * 
 * @author Christian Rechner
 */
class ApplicationStructureImpl extends ApplicationStructurePOA {

    private static final Log LOG = LogFactory.getLog(ApplicationStructureImpl.class);

    private final POA poa;
    private final AoSession aoSession;
    private final AtfxCache atfxCache;

    /** enumeration definitions */
    private final List<EnumerationDefinition> enumerationDefinitions;
    private final Set<String> readOnlyEnumDefs;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param atfxCache The ATFX cache.
     * @param aoSession The AoSession.
     * @throws AoException Error creating application structure.
     */
    public ApplicationStructureImpl(POA poa, AtfxCache atfxCache, AoSession aoSession) throws AoException {
        this.poa = poa;
        this.atfxCache = atfxCache;
        this.aoSession = aoSession;
        this.enumerationDefinitions = new ArrayList<EnumerationDefinition>();
        this.readOnlyEnumDefs = new HashSet<String>();

        // fill enumeration definitions from base enumerations
        for (BaseElement baseElement : getSession().getBaseStructure().getElements("*")) {
            for (BaseAttribute baseAttribute : baseElement.getAttributes("*")) {
                DataType dt = baseAttribute.getDataType();
                if (dt == DataType.DT_ENUM || dt == DataType.DS_ENUM) {
                    EnumerationDefinition baseEnumDef = baseAttribute.getEnumerationDefinition();
                    if (!this.readOnlyEnumDefs.contains(baseEnumDef.getName())) {
                        this.enumerationDefinitions.add(baseEnumDef);
                        this.readOnlyEnumDefs.add(baseEnumDef.getName());
                    }
                }
            }
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
        // check enum name length
        if (enumName == null || enumName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "enumName must not be empty");
        }
        // check for existing enum name
        for (EnumerationDefinition enumDef : this.enumerationDefinitions) {
            if (enumDef.getName().equals(enumName)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "EnumerationDefinition with name '" + enumName + "' already exists");
            }
        }
        // create enumeration definition
        try {
            int index = getMaxEnumDefIndex() + 1;
            EnumerationDefinitionImpl enumDefImpl = new EnumerationDefinitionImpl(_this(), index, enumName);
            EnumerationDefinition enumDef = EnumerationDefinitionHelper.narrow(poa.servant_to_reference(enumDefImpl));
            this.enumerationDefinitions.add(enumDef);
            return enumDef;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Returns the maximum index of all existing enumeration definitions.
     * 
     * @return The maximum index of all enumeration definitions.
     * @throws AoException Error retrieving max index.
     */
    private int getMaxEnumDefIndex() throws AoException {
        int max = -1;
        for (EnumerationDefinition enumDef : this.enumerationDefinitions) {
            if (enumDef.getIndex() > max) {
                max = enumDef.getIndex();
            }
        }
        return max;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listEnumerations()
     */
    public String[] listEnumerations() throws AoException {
        List<String> list = new ArrayList<String>();
        for (EnumerationDefinition enumDef : this.enumerationDefinitions) {
            list.add(enumDef.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getEnumerationDefinition(java.lang.String)
     */
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
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "EnumerationDefinition '" + enumName
                + "' not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#removeEnumerationDefinition(java.lang.String)
     */
    public void removeEnumerationDefinition(String enumName) throws AoException {
        // check enum name length
        if (enumName == null || enumName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "enumName must not be empty");
        }
        // check if enumeration definition is read only
        if (this.readOnlyEnumDefs.contains(enumName)) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "EnumerationDefinition '"
                    + enumName + "' is read only");
        }
        // check if enumeration is in use
        for (EnumerationAttributeStructure eas : getSession().getEnumerationAttributes()) {
            if (eas.enumName.equals(enumName)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "EnumerationDefinition '"
                        + enumName + "' may not be deleted because it is in use by attribute '" + eas.aaName + "'");
            }
        }

        // lookup enumeration to remove
        EnumerationDefinition enumDefToDelete = null;
        for (EnumerationDefinition enumDef : this.enumerationDefinitions) {
            if (enumDef.getName().equals(enumName)) {
                enumDefToDelete = enumDef;
                break;
            }
        }
        if (enumDefToDelete == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "EnumerationDefinition '"
                    + enumName + "' not found");
        }
        // remove
        this.enumerationDefinitions.remove(enumDefToDelete);
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
        // check if attribute already exists
        if (this.atfxCache.getApplicationElementByName("") != null) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Creating a new application element with already having other elements with empty name is not allowed!");
        }

        // create application element
        try {
            long aid = this.atfxCache.nextAid();
            ApplicationElementImpl aeImpl = new ApplicationElementImpl(poa, this.atfxCache, _this(), baseElem, aid);
            ApplicationElement ae = ApplicationElementHelper.narrow(poa.servant_to_reference(aeImpl));
            this.atfxCache.addApplicationElement(aid, baseElem.getType(), ae);

            // mandatory base attributes are created automatically
            for (BaseAttribute baseAttribute : baseElem.getAttributes("*")) {
                if (baseAttribute.isObligatory()) {
                    ApplicationAttribute aa = ae.createAttribute();
                    aa.setName(baseAttribute.getName());
                    aa.setBaseAttribute(baseAttribute);
                }
            }

            return ae;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listElements(java.lang.String)
     */
    public String[] listElements(String aePattern) throws AoException {
        List<String> list = new ArrayList<String>();
        for (ApplicationElement applElem : getElements(aePattern)) {
            list.add(applElem.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElements(java.lang.String)
     */
    public ApplicationElement[] getElements(String aePattern) throws AoException {
        List<ApplicationElement> list = new ArrayList<ApplicationElement>();
        for (ApplicationElement applElem : this.atfxCache.getApplicationElements()) {
            if (PatternUtil.nameFilterMatch(applElem.getName(), aePattern)) {
                list.add(applElem);
            }
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listElementsByBaseType(java.lang.String)
     */
    public String[] listElementsByBaseType(String aeType) throws AoException {
        List<String> list = new ArrayList<String>();
        for (ApplicationElement applElem : getElementsByBaseType(aeType)) {
            list.add(applElem.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getElementsByBaseType(java.lang.String)
     */
    public ApplicationElement[] getElementsByBaseType(String aeType) throws AoException {
        List<ApplicationElement> list = new ArrayList<ApplicationElement>();
        for (ApplicationElement applElem : this.atfxCache.getApplicationElements()) {
            BaseElement baseElem = applElem.getBaseElement();
            if (PatternUtil.nameFilterMatchCI(baseElem.getType(), aeType)) {
                list.add(applElem);
            }
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#listTopLevelElements(java.lang.String)
     */
    public String[] listTopLevelElements(String aeType) throws AoException {
        List<String> list = new ArrayList<String>();
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
        List<ApplicationElement> list = new ArrayList<ApplicationElement>();
        for (ApplicationElement applElem : this.atfxCache.getApplicationElements()) {
            BaseElement baseElem = applElem.getBaseElement();

            // no top level
            if (!baseElem.isTopLevel()) {
                continue;
            }

            // handle special case with environment or self as father
            ApplicationElement[] fatherElems = applElem.getRelatedElementsByRelationship(Relationship.FATHER);

            if (fatherElems.length > 0) {
                ApplicationElement fatherElem = fatherElems[0];
                boolean fatherIsEnvironment = fatherElem.getBaseElement().getType().equals("AoEnvironment");
                boolean fatherIsSelf = fatherElem.getName().equals(applElem.getName());
                if (!fatherIsEnvironment && !fatherIsSelf) {
                    continue;
                }
            }

            if (PatternUtil.nameFilterMatch(baseElem.getType(), bePattern)) {
                list.add(applElem);
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
        ApplicationElement ae = this.atfxCache.getApplicationElementById(ODSHelper.asJLong(aeId));
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
        ApplicationElement ae = this.atfxCache.getApplicationElementByName(aeName);
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
        // remove all application relations
        for (ApplicationRelation applRel : applElem.getAllRelations()) {
            removeRelation(applRel);
        }
        // remove all application attributes
        for (ApplicationAttribute applAttr : applElem.getAttributes("*")) {
            this.atfxCache.removeApplicationAttribute(aid, applAttr.getName());
        }
        // remove application element
        this.atfxCache.removeApplicationElement(aid);
        LOG.debug("Removed application element aid=" + aid);
        // deactivate CORBA object
        try {
            byte[] id = poa.reference_to_id(applElem);
            poa.deactivate_object(id);
        } catch (WrongAdapter e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ObjectNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
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
        try {
            ApplicationRelationImpl arImpl = new ApplicationRelationImpl(this.atfxCache);
            ApplicationRelation ar = ApplicationRelationHelper.narrow(poa.servant_to_reference(arImpl));
            this.atfxCache.addApplicationRelation(ar);
            return ar;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationStructureOperations#getRelations(org.asam.ods.ApplicationElement,
     *      org.asam.ods.ApplicationElement)
     */
    public ApplicationRelation[] getRelations(ApplicationElement applElem1, ApplicationElement applElem2)
            throws AoException {
        List<ApplicationRelation> list = new ArrayList<ApplicationRelation>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(ODSHelper.asJLong(applElem1.getId()))) {
            if (rel.getElem1().getName().equals(applElem1.getName())
                    && rel.getElem2().getName().equals(applElem2.getName())) {
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
        // lookup relation and inverse relation
        ApplicationRelation relToRemove = null;
        ApplicationRelation invRelToRemove = null;
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations()) {
            String relName = rel.getRelationName();
            if (relName.equals(applRel.getRelationName())) {
                relToRemove = rel;
            } else if (relName.equals(applRel.getInverseRelationName())) {
                invRelToRemove = rel;
            }
        }

        // remove relation and inverse relation from cache
        if (relToRemove != null) {
            this.atfxCache.removeApplicationRelation(relToRemove);
        }
        if (invRelToRemove != null) {
            this.atfxCache.removeApplicationRelation(invRelToRemove);
        }
        // deactivate CORBA object
        try {
            byte[] id = poa.reference_to_id(applRel);
            poa.deactivate_object(id);
        } catch (WrongAdapter e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ObjectNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
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
        Pattern pattern = Pattern.compile("\\[(.*)\\]([^;]*);?(.*)");
        String[] strAr = asamPath.split("(?<!\\\\)/");
        for (String str : strAr) {
            // if (str.isEmpty()) {
            // continue;
            // }
            Matcher m = pattern.matcher(str);
            if (!m.matches()) {
                throw new AoException(ErrorCode.AO_INVALID_ASAM_PATH, SeverityFlag.ERROR, 0,
                                      "Unable to parse ASAM path: " + asamPath);
            }
            String aeName = m.group(1);
            String ieName = m.group(2);
            String version = m.group(3);

            System.out.println("AE: " + aeName);
            System.out.println("IE: " + ieName);
            System.out.println("VE: " + version);
            System.out.println("-");
        }
        System.out.println("-----------------------------------------------");

        // TODO Auto-generated method stub
        return null;
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
        Set<String> relNames = new HashSet<String>();
        Set<String> invRelNames = new HashSet<String>();

        for (ApplicationElement ae : this.atfxCache.getApplicationElements()) {
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
                if (ba.isObligatory() && (this.atfxCache.getApplicationAttributeByBaName(aid, ba.getName()) == null)) {
                    throw new AoException(ErrorCode.AO_MISSING_ATTRIBUTE, SeverityFlag.ERROR, 0,
                                          "Application element is missing mandatory base attribute. aid=" + aid
                                                  + ",baseAttr=" + ba.getName());
                }
            }

            // check application attributes
            Set<String> baseAttrs = new HashSet<String>();
            for (ApplicationAttribute aa : this.atfxCache.getApplicationAttributes(aid)) {
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
            for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(aid)) {
                String relName = rel.getRelationName();
                String invRelName = rel.getInverseRelationName();
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
