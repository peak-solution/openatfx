package de.rechner.openatfx_mdf4.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;


/**
 * Cache for the ASAM-ODS application model.
 * 
 * @author Christian Rechner
 */
public class ODSModelCache {

    private static final Log LOG = LogFactory.getLog(ODSModelCache.class);

    // cached ODS AoSession (ODS interface)
    private AoSession aoSession = null;

    // cached ODS context parameter
    private NameValue[] context = null;

    // cached ODS BaseStructure (ODS interface)
    private BaseStructure baseStructure = null;

    // cached ODS ApplicationStructure (ODS interface)
    private ApplicationStructure applicationStructure = null;

    // cached application elements (ODS interface)
    private Map<String, ApplicationElement> applicationElemCache;

    // cached application attributes (ODS interface)
    private Map<String, ApplicationAttribute[]> applicationAttrCache;

    // cached application relations (ODS interface)
    private Map<ApplicationRelKey, ApplicationRelation> applicationRelCache;

    // cached enumeration definitions (ODS interface)
    private Map<String, EnumerationDefinition> enumDefCache;

    // cached applElemAccess (ODS interface)
    private ApplElemAccess applElemAccess = null;

    // cached applicationStructureValue (ODS interface)
    private ApplicationStructureValue applicationStructureValue = null;

    // cached applElemns (ODS struct)
    private ApplElem[] applElems = null;

    private Map<String, ApplElem> aeName2applElemMap = null;

    private Map<Long, ApplElem> aid2applElemMap = null;

    // cached applRels (ODS struct)
    private ApplRel[] applRels = null;

    // cached enumeration values
    private EnumerationStructure[] enumerationStructure = null;

    // cached enumeration attributes
    private EnumerationAttributeStructure[] enumerationAttributes = null;

    // map containing the enum definition <aid,aaName,enumName>
    private Map<Long, Map<String, String>> enumerationAttributeMap = null;

    private Map<String, Map<Integer, String>> enumIndexToValueMap = null;

    private Map<String, Map<String, Integer>> enumValueToIndexMap = null;

    /**
     * Constructor.
     * 
     * @param aoSession the ODS session
     */
    public ODSModelCache(AoSession aoSession) {
        if (aoSession == null) {
            throw new IllegalArgumentException("Parameter aoSession must not be null");
        }
        this.applicationElemCache = new HashMap<String, ApplicationElement>();
        this.applicationAttrCache = new HashMap<String, ApplicationAttribute[]>();
        this.applicationRelCache = new HashMap<ApplicationRelKey, ApplicationRelation>();
        this.enumDefCache = new HashMap<String, EnumerationDefinition>();
        this.aoSession = aoSession;
    }

    /**
     * Returns a Java long from ODS T_LONGLONG.
     * 
     * @param ll ODS T_LONGLONG value
     * @return Java long with the same value as ll
     */
    private static long asJLong(T_LONGLONG ll) {
        long tmp;
        if (ll.low >= 0) {
            tmp = (long) ll.high * 0x100000000L + (long) ll.low;
        } else {
            tmp = (long) (ll.high + 1) * 0x100000000L + (long) ll.low;
        }
        return (tmp);
    }

    /**
     * Return ODS T_LONGLONG from Java long.
     * 
     * @param v Java long value
     * @return ODS T_LONGLONG with the same value as v
     */
    private static T_LONGLONG asODSLongLong(long v) {
        return new T_LONGLONG((int) ((v >> 32) & 0xffffffffL), (int) (v & 0xffffffffL));
    }

    /*******************************************************************************************************************
     * Methods for accessing cached ODS objects.
     ******************************************************************************************************************/

    /**
     * Returns the ODS session.
     * 
     * @return the ODS session
     */
    public final AoSession getAoSession() {
        return aoSession;
    }

    /**
     * Returns the cached ODS server context.
     * 
     * @return array containg context parameter
     * @throws AoException if something went wrong
     */
    public final NameValue[] getContext() throws AoException {
        if (this.context == null) {
            NameValueIterator iter = getAoSession().getContext("*");
            int cnt = iter.getCount();
            this.context = new NameValue[cnt];
            for (int i = 0; i < cnt; i++) {
                this.context[i] = iter.nextOne();
            }
            LOG.debug("Context loaded");
        }
        return this.context;
    }

    /**
     * Returns the cached ODS base structure.
     * 
     * @return the cached BaseStructure
     * @throws AoException if something went wrong
     */
    public final BaseStructure getBaseStructure() throws AoException {
        if (baseStructure == null) {
            baseStructure = getAoSession().getBaseStructure();
            LOG.debug("BaseStructure loaded");
        }
        return baseStructure;
    }

    /**
     * Returns an ODS base element by given name.
     * 
     * @param beName The base element name
     * @return The base element object
     * @throws AoException error getting base element
     */
    public final BaseElement getBaseElement(String beName) throws AoException {
        return getBaseStructure().getElementByType(beName);
    }

    /**
     * Returns the ODS base relation by given base element names.
     * 
     * @param beNameFrom first base element name
     * @param beNameTo second base element name
     * @return the ODS base relation
     * @throws AoException application elements or relation not found
     */
    public final BaseRelation getBaseRelation(String beNameFrom, String beNameTo) throws AoException {
        BaseElement beFrom = getBaseElement(beNameFrom);
        BaseElement beTo = getBaseElement(beNameTo);
        return getBaseStructure().getRelation(beFrom, beTo);
    }

    /**
     * Returns the cached ODS application structure.
     * 
     * @return the cached application structure
     * @throws AoException if something went wrong
     */
    public final ApplicationStructure getApplicationStructure() throws AoException {
        if (applicationStructure == null) {
            applicationStructure = getAoSession().getApplicationStructure();
            LOG.debug("ApplicationStructure loaded");
        }
        return applicationStructure;
    }

    /**
     * Returns the cached ODS application structure value.
     * 
     * @return the cached applicationStructureValue
     * @throws AoException if something went wrong
     */
    public final ApplicationStructureValue getApplicationStructureValue() throws AoException {
        if (applicationStructureValue == null) {
            applicationStructureValue = getAoSession().getApplicationStructureValue();
            LOG.debug("ApplicationStructureValue loaded");
        }
        return applicationStructureValue;
    }

    /**
     * Returns an application element of specified type.
     * 
     * @param aeName the name of the application element to retrieve
     * @return the cached application element
     * @throws AoException application element not found
     */
    public final ApplicationElement getApplicationElement(String aeName) throws AoException {
        ApplicationElement ae = applicationElemCache.get(aeName);
        if (ae == null) {
            ApplicationStructure as = getApplicationStructure();
            ae = as.getElementByName(aeName);
            applicationElemCache.put(aeName, ae);
            LOG.debug("ApplicationElement [name=" + aeName + "] loaded");
        }
        return ae;
    }

    /**
     * Returns an array containing all application attributes for an application element.
     * 
     * @param aeName the application element name
     * @return the ODS <code>ApplicationAttribute</code>
     * @throws AoException if something went wrong
     */
    public final ApplicationAttribute[] getApplicationAttributes(String aeName) throws AoException {
        ApplicationAttribute[] attrs = applicationAttrCache.get(aeName);
        if (attrs == null) {
            attrs = getApplicationElement(aeName).getAttributes("*");
            applicationAttrCache.put(aeName, attrs);
            LOG.debug("ApplicationAttributes [aeName=" + aeName + "] loaded");
        }
        return attrs;
    }

    /**
     * Returns the ODS application attribute for an application element.
     * 
     * @param aeName the application element name
     * @param aaName the application attribute name
     * @return the ODS <code>ApplicationAttribute</code>
     * @throws AoException if something went wrong
     */
    public final ApplicationAttribute getApplicationAttribute(String aeName, String aaName) throws AoException {
        for (ApplicationAttribute attr : getApplicationAttributes(aeName)) {
            if (attr.getName().equals(aaName)) {
                return attr;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute [aeName=" + aeName
                + ",aaName=" + aaName + "] not found");
    }

    /**
     * Returns an application relation between two elements.
     * 
     * @param ae1Name the source application element
     * @param ae2Name the target application element
     * @return the ODS application relation
     * @throws AoException if something went wrong
     */
    public final ApplicationRelation getApplicationRelation(String ae1Name, String ae2Name) throws AoException {
        // lookup in cache for application relation
        ApplicationRelKey appRelKey = new ApplicationRelKey(ae1Name, ae2Name);
        ApplicationRelation rel = applicationRelCache.get(appRelKey);
        if (rel != null) {
            return rel;
        }

        // retrieve relation
        ApplicationStructure as = getApplicationStructure();
        ApplicationElement ae1 = getApplicationElement(ae1Name);
        ApplicationElement ae2 = getApplicationElement(ae2Name);
        ApplicationRelation[] ars = as.getRelations(ae1, ae2);

        // check if relation exist
        if (ars.length < 1) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 1,
                                  "ApplicationRelation between [ae1Name=" + ae1Name + ",ae2Name=" + ae2Name
                                          + "] not found");
        }

        // check if multiple relations between two elements exist
        if (ars.length > 1) {
            isSelfRelation(ae1Name, ae2Name, ars);
        }

        // put to cache
        applicationRelCache.put(appRelKey, ars[0]);

        LOG.debug("ApplicationRelation between [ae1Name=" + ae1Name + ",ae2Name=" + ae2Name + "] loaded");
        return ars[0];
    }

    /**
     * Checks for self relation.
     * 
     * @param ae1Name Application element name 1.
     * @param ae2Name Application element name 2.
     * @param ars Application relations.
     * @throws AoException Thrown if a self relation is detected.
     */
    private void isSelfRelation(String ae1Name, String ae2Name, ApplicationRelation[] ars) throws AoException {
        if (ars.length != 2 && !ars[0].getRelationName().equals(ars[1].getInverseRelationName())) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 1,
                                  "More than one relation between [ae1Name=" + ae1Name + ",ae2Name=" + ae2Name
                                          + "] found");

        }
    }

    /**
     * Returns an application relation between two elements with given name.
     * 
     * @param ae1Name the source application element
     * @param ae2Name the target application element
     * @param relName the relation name
     * @return the ODS application relation
     * @throws AoException if something went wrong
     */
    public final ApplicationRelation getApplicationRelation(String ae1Name, String ae2Name, String relName)
            throws AoException {

        // lookup in cache for application relation
        ApplicationRelKey appRelKey = new ApplicationRelKey(ae1Name, ae2Name);
        ApplicationRelation rel = applicationRelCache.get(appRelKey);
        if ((rel != null) && (rel.getRelationName().equals(relName))) {
            return rel;
        }

        // retrieve relation
        ApplicationStructure as = getApplicationStructure();
        ApplicationElement ae1 = getApplicationElement(ae1Name);
        ApplicationElement ae2 = getApplicationElement(ae2Name);
        ApplicationRelation[] ars = as.getRelations(ae1, ae2);

        // get relation with right name
        for (ApplicationRelation ar : ars) {
            if (ar.getRelationName().equals(relName)) {
                LOG.debug("ApplicationRelation between [ae1Name=" + ae1Name + ",ae2Name=" + ae2Name + ",relName="
                        + relName + "] loaded");
                applicationRelCache.put(appRelKey, ar);
                return ar;
            }
        }

        throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 1, "ApplicationRelation  [ae1Name="
                + ae1Name + ",ae2Name=" + ae2Name + ",relName=" + relName + "] not found");
    }

    /**
     * Returns an ODS enumeration definition by given name.
     * 
     * @param enumName the enumeration name
     * @return the ODS <code>EnumerationDefinition</code> or null if the <code>EnumerationDefinition</code> does not
     *         exits *
     * @throws AoException if something is wrong
     */
    public final EnumerationDefinition getEnumerationDefinition(String enumName) throws AoException {
        EnumerationDefinition enumDef = enumDefCache.get(enumName);
        if (enumDef == null) {
            ApplicationStructure as = getApplicationStructure();
            try {
                enumDef = as.getEnumerationDefinition(enumName);
                enumDefCache.put(enumName, enumDef);
            } catch (AoException aoe) {
                LOG.debug("EnumerationDefinition [name=" + enumName + "] not found!");
                return null;
            }

            LOG.debug("EnumerationDefinition [name=" + enumName + "] loaded");
        }
        return enumDef;
    }

    /**
     * Returns the cached ODS applElemAccess.
     * 
     * @return the cached applElemAccess
     * @throws AoException if something went wrong
     */
    public final ApplElemAccess getApplElemAccess() throws AoException {
        if (applElemAccess == null) {
            applElemAccess = getAoSession().getApplElemAccess();
            LOG.debug("ApplElemAccess loaded");
        }
        return applElemAccess;
    }

    /*******************************************************************************************************************
     * methods for accessing cached ODS structs.
     ******************************************************************************************************************/

    /**
     * Returns all structs of ODS <code>ApplElem</code>.
     * 
     * @return the <code>ApplElem</code>
     * @throws AoException if something went wrong
     */
    public final ApplElem[] getApplElems() throws AoException {
        if (this.applElems == null) {
            this.applElems = getApplicationStructureValue().applElems;
        }
        return applElems;
    }

    /**
     * Returns the map between application element name and application element structure.
     * 
     * @return The map.
     * @throws AoException Error loading application element mapping.
     */
    private Map<String, ApplElem> getAeName2applElemMap() throws AoException {
        if (this.aeName2applElemMap == null) {
            this.aeName2applElemMap = new HashMap<String, ApplElem>();
            for (ApplElem applElem : getApplElems()) {
                this.aeName2applElemMap.put(applElem.aeName, applElem);
            }
        }
        return this.aeName2applElemMap;
    }

    /**
     * Returns the map between application element id and application element structure.
     * 
     * @return The map.
     * @throws AoException Error loading application element mapping.
     */
    private Map<Long, ApplElem> getAid2applElemMap() throws AoException {
        if (this.aid2applElemMap == null) {
            this.aid2applElemMap = new HashMap<Long, ApplElem>();
            for (ApplElem applElem : getApplElems()) {
                this.aid2applElemMap.put(asJLong(applElem.aid), applElem);
            }
        }
        return this.aid2applElemMap;
    }

    /**
     * Returns all ODS <code>ApplElems</code> by base name.
     * 
     * @param beName the base element name
     * @return array of <code>ApplElemns</code>
     * @throws AoException if something went wrong
     */
    public final ApplElem[] getApplElemsByBaseName(String beName) throws AoException {
        List<ApplElem> list = new LinkedList<ApplElem>();
        for (ApplElem applElem : getApplElems()) {
            if (applElem.beName.equals(beName)) {
                list.add(applElem);
            }
        }
        return list.toArray(new ApplElem[list.size()]);
    }

    /**
     * Returns all application element names by given base name
     * 
     * @param beName the base element name
     * @return list of application element names
     * @throws AoException if something went wrong
     */
    public final String[] getAeNamesByBaseName(String beName) throws AoException {
        List<String> list = new LinkedList<String>();
        for (ApplElem applElem : getApplElemsByBaseName(beName)) {
            list.add(applElem.aeName);
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns an ODS <code>ApplElem</code> by given base name. If none or multiple were found, an exception is thrown.
     * 
     * @param beName the base element name
     * @return the <code>ApplElem</code> if found
     * @throws AoException none or multiple application elements by given base name have been found
     */
    public final ApplElem getApplElemByBaseName(String beName) throws AoException {
        ApplElem[] elems = getApplElemsByBaseName(beName);
        if (elems.length < 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "AE for base name [beName=" + beName
                    + "] not found");
        } else if (elems.length > 1) {
            throw new AoException(ErrorCode.AO_NOT_UNIQUE, SeverityFlag.ERROR, 01,
                                  "Multiple AEs for base name [beName=" + beName + " found");
        }
        return elems[0];
    }

    /**
     * Returns an ODS <code>ApplElem</code> by given aid.
     * 
     * @param aid the id of the application element
     * @return the <code>ApplElem</code>
     * @throws AoException application element by given id not found
     */
    public final ApplElem getApplElem(long aid) throws AoException {
        ApplElem applElem = getAid2applElemMap().get(aid);
        if (applElem != null) {
            return applElem;
        }
        throw new AoException(ErrorCode.AO_INVALID_ELEMENT, SeverityFlag.ERROR, 0, "AE [aid=" + aid + "] not found");
    }

    /**
     * Returns an ODS <code>ApplElem</code> by given aid as ODS <code>T_LONGLONG</code>.
     * 
     * @param aid the id of the application element
     * @return the <code>ApplElem</code>
     * @throws AoException application element by given id not found
     */
    public final ApplElem getApplElem(T_LONGLONG aid) throws AoException {
        return getApplElem(asJLong(aid));
    }

    /**
     * Returns an ODS <code>ApplElem</code> by given name.
     * 
     * @param aeName the name of the application element
     * @return the <code>ApplElem</code>
     * @throws AoException application element with given name not found
     */
    public final ApplElem getApplElem(String aeName) throws AoException {
        ApplElem applElem = getAeName2applElemMap().get(aeName);
        if (applElem != null) {
            return applElem;
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "AE [aeName=" + aeName + "] not found!");
    }

    /**
     * Checks whether an application element with specified name exists.
     * 
     * @param aeName the name of the ApplicationElement
     * @return true, if the ApplicationElement exists, otherwise false
     * @throws AoException if something went wrong
     */
    public final boolean applElemExists(String aeName) throws AoException {
        return getAeName2applElemMap().containsKey(aeName);
    }

    /**
     * Creates an ODS <code>org.asam.ods.ElemId</code> by given application element name and instance id.
     * 
     * @param aeName the application element name
     * @param instanceId the instance id
     * @return the <code>org.asam.ods.ElemId</code>
     * @throws AoException application element not found
     */
    public final ElemId createElemId(String aeName, long instanceId) throws AoException {
        T_LONGLONG aid = getApplElem(aeName).aid;
        T_LONGLONG iid = asODSLongLong(instanceId);
        return new ElemId(aid, iid);
    }

    /**
     * Returns an ODS <code>ApplAttr</code> by given name.
     * 
     * @param aeName the name of the application element
     * @param aaName name of the application attribute
     * @return the <code>ApplAttr</code>, null if not found
     * @throws AoException application attribute not found
     */
    public final ApplAttr getApplAttr(String aeName, String aaName) throws AoException {
        ApplElem applElem = getApplElem(aeName);
        for (ApplAttr applAttr : applElem.attributes) {
            if (applAttr.aaName.equals(aaName)) {
                return applAttr;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute [aeName=" + aeName
                + ",aaName=" + aaName + "] not found!");
    }

    /**
     * Checks whether an application attribute exists.
     * 
     * @param aeName the name of the application element
     * @param aaName the name of the application attribute
     * @return true, if the application attribute exists, otherwise false
     * @throws AoException error accessing the application structure
     */
    public final boolean applAttrExists(String aeName, String aaName) throws AoException {
        if (!applElemExists(aeName)) {
            return false;
        }
        ApplElem applElem = getApplElem(aeName);
        for (ApplAttr applAttr : applElem.attributes) {
            if (applAttr.aaName.equals(aaName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an application attribute definition by given base base element and base attribute name.
     * 
     * @param beName the base element name
     * @param baName the base attribute name
     * @return the application atttribute definition
     * @throws AoException none or multiple attributes found
     */
    public final ApplAttr getApplAttrByBaseName(String beName, String baName) throws AoException {
        ApplElem applElem = getApplElemByBaseName(beName);
        for (ApplAttr applAttr : applElem.attributes) {
            if (applAttr.baName.equals(baName)) {
                return applAttr;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplAttr [beName=" + beName + ",baName="
                + baName + "] not found!");
    }

    /**
     * Returns all application relations as strucuture.
     * 
     * @return array of application relation structures
     * @throws AoException error accessing the application structure
     */
    public final ApplRel[] getApplRels() throws AoException {
        if (applRels == null) {
            applRels = getApplicationStructureValue().applRels;
        }
        return applRels;
    }

    /**
     * Checks whether an application relation exists.
     * 
     * @param elem1 the first application element
     * @param elem2 the target application element
     * @param relName the relations name
     * @return true, if the relation exists, otherwise false
     * @throws AoException if something went wrong
     */
    public final boolean applRelExists(String elem1, String elem2, String relName) throws AoException {
        long aidElem1 = asJLong(getApplElem(elem1).aid);
        long aidElem2 = asJLong(getApplElem(elem2).aid);
        for (ApplRel applRel : getApplRels()) {
            if ((asJLong(applRel.elem1) == aidElem1) && (asJLong(applRel.elem2) == aidElem2)
                    && (applRel.arName.equals(relName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all ApplRels for an ODS application element.
     * 
     * @param aeName the name of the application element
     * @return list of ApplRels
     * @throws AoException if something went wrong
     */
    public final ApplRel[] getApplRelsForAe(String aeName) throws AoException {
        List<ApplRel> list = new LinkedList<ApplRel>();
        long aid = asJLong(getApplElem(aeName).aid);
        for (ApplRel applRel : getApplRels()) {
            long aidelem1 = asJLong(applRel.elem1);
            if (aid == aidelem1) {
                list.add(applRel);
            }
        }
        return list.toArray(new ApplRel[list.size()]);
    }

    /**
     * Returns all application relations as structure for an application element by given base element name.
     * 
     * @param beName the base element name
     * @return array of application relation structures
     * @throws AoException none or multiple application elements with given basename found
     */
    public final ApplRel[] getApplRelsByBaseName(String beName) throws AoException {
        ApplElem applElem = getApplElemByBaseName(beName);
        long aid = asJLong(applElem.aid);

        List<ApplRel> list = new LinkedList<ApplRel>();
        for (ApplRel applRel : getApplRels()) {
            long elem1Aid = asJLong(applRel.elem1);
            if (elem1Aid == aid) {
                list.add(applRel);
            }
        }

        return list.toArray(new ApplRel[list.size()]);
    }

    /**
     * Returns an application relation as structure for an application element found by given base element and base
     * relation name.
     * 
     * @param beName the base element name
     * @param brName the base relation name
     * @return the ApplRel structure
     * @throws AoException none or multiple application elements with given base element found or application relation
     *             not existing
     */
    public final ApplRel getApplRelByBaseName(String beName, String brName) throws AoException {
        for (ApplRel applRel : getApplRelsByBaseName(beName)) {
            if (applRel.brName.equals(brName)) {
                return applRel;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplRel [beName=" + beName + ",brName="
                + brName + "] not found!");
    }

    /*******************************************************************************************************************
     * Methods for accessing enumeration values
     ******************************************************************************************************************/

    /**
     * Returns the enumeration structure.
     * 
     * @return The enumeration structure.
     * @throws AoException Error getting enumeration structure.
     */
    public EnumerationStructure[] getEnumerationStructure() throws AoException {
        if (this.enumerationStructure == null) {
            this.enumerationStructure = getAoSession().getEnumerationStructure();
        }
        return this.enumerationStructure;
    }

    /**
     * Returns the enumeration attribute structure.
     * 
     * @return The enumeration attributes.
     * @throws AoException Error getting enumeration attributes.
     */
    public EnumerationAttributeStructure[] getEnumerationAttributes() throws AoException {
        if (this.enumerationAttributes == null) {
            this.enumerationAttributes = getAoSession().getEnumerationAttributes();
        }
        return this.enumerationAttributes;
    }

    /**
     * Returns the name of the enumeration definition associated to an application attribute.
     * 
     * @param aeName The application element name.
     * @param aaName The application attribute name.
     * @return The name of the enumeration definition.
     * @throws AoException No enumeration definition defined at the attribute.
     */
    public String getEnumName(String aeName, String aaName) throws AoException {
        long aid = asJLong(getApplElem(aeName).aid);
        // fill cache
        if (this.enumerationAttributeMap == null) {
            this.enumerationAttributeMap = new HashMap<Long, Map<String, String>>();
            for (EnumerationAttributeStructure eas : getEnumerationAttributes()) {
                Map<String, String> attrMap = this.enumerationAttributeMap.get(aid);
                if (attrMap == null) {
                    attrMap = new HashMap<String, String>();
                    this.enumerationAttributeMap.put(aid, attrMap);
                }
                attrMap.put(eas.aaName, eas.enumName);
            }
        }
        // lookup
        Map<String, String> attrMap = this.enumerationAttributeMap.get(aid);
        if (attrMap != null) {
            String enumName = attrMap.get(aaName);
            if (enumName != null) {
                return enumName;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                              "No enumeration definition found for [aeName=" + aeName + ",aaName=" + aaName + "]");
    }

    /**
     * Returns the enumeration item for a enumeration value.
     * 
     * @param enumName The name of the enumeration definition.
     * @param enumValue The enumeration value.
     * @return The enumeration item.
     * @throws AoException Enumeration definition or value not found.
     */
    public final int getEnumItem(String enumName, String enumValue) throws AoException {
        // fill cache
        if (this.enumValueToIndexMap == null) {
            this.enumValueToIndexMap = new HashMap<String, Map<String, Integer>>();
            for (EnumerationStructure es : getEnumerationStructure()) {
                Map<String, Integer> map = new HashMap<String, Integer>();
                for (EnumerationItemStructure item : es.items) {
                    map.put(item.itemName, item.index);
                }
                this.enumValueToIndexMap.put(es.enumName, map);
            }
        }

        // lookup
        Map<String, Integer> map = this.enumValueToIndexMap.get(enumName);
        if (map != null) {
            Integer item = map.get(enumValue);
            if (item != null) {
                return item;
            }
        }

        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                              "Enumeration item not found for [enumName=" + enumName + ",enumValue=" + enumValue + "]");
    }

    /**
     * Returns the enumeration value for an enumeration item.
     * 
     * @param enumName The name of the enumeration definition.
     * @param enumItem The enumeration item.
     * @return The enumeration value.
     * @throws AoException Enumeration definition or item not found.
     */
    public final String getEnumValue(String enumName, int enumItem) throws AoException {
        // fill cache
        if (this.enumIndexToValueMap == null) {
            this.enumIndexToValueMap = new HashMap<String, Map<Integer, String>>();
            for (EnumerationStructure es : getEnumerationStructure()) {
                Map<Integer, String> map = new HashMap<Integer, String>();
                for (EnumerationItemStructure item : es.items) {
                    map.put(item.index, item.itemName);
                }
                this.enumIndexToValueMap.put(es.enumName, map);
            }
        }

        // lookup
        Map<Integer, String> map = this.enumIndexToValueMap.get(enumName);
        if (map != null) {
            String value = map.get(enumItem);
            if (value != null) {
                return value;
            }
        }

        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                              "Enumeration value not found for [enumName=" + enumName + ",enumItem=" + enumItem + "]");
    }

    /**
     * Unique key for an application relation.
     */
    private static class ApplicationRelKey {

        private final String ae1name;

        private final String ae2name;

        /**
         * Creates a new ApplicationRelKey.
         * 
         * @param ae1name the source application element name
         * @param ae2name the target application element name
         */
        public ApplicationRelKey(String ae1name, String ae2name) {
            this.ae1name = ae1name;
            this.ae2name = ae2name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ae1name == null) ? 0 : ae1name.hashCode());
            result = prime * result + ((ae2name == null) ? 0 : ae2name.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ApplicationRelKey other = (ApplicationRelKey) obj;
            if (ae1name == null) {
                if (other.ae1name != null) {
                    return false;
                }
            } else if (!ae1name.equals(other.ae1name)) {
                return false;
            }
            if (ae2name == null) {
                if (other.ae2name != null) {
                    return false;
                }
            } else if (!ae2name.equals(other.ae2name)) {
                return false;
            }
            return true;
        }

    }

}
