package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementHelper;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.omg.PortableServer.POA;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Central cache holding all data for performance reasons.
 * 
 * @author Christian Rechner
 */
class AtfxCache {

    /** application elements */
    private final Map<String, ApplicationElement> nameToAeMap; // <aeName, ApplicationElement>
    private final Map<String, Set<Long>> beToAidMap; // <beName, aid>
    private final Map<Long, String> aidToAeNameMap;
    private final Map<Long, ApplicationElement> aidToAeMap;

    /** application attributes */
    private final Map<Long, Map<String, ApplicationAttribute>> applicationAttributeMap; // <aid,<aaName,ApplicationAttribute>>
    private final Map<Long, Map<String, String>> baNameToAaNameMap; // <aid,<baName,aaName>>

    /** application relations */
    private final List<ApplicationRelation> applicationRelations;
    private final Map<Long, List<ApplicationRelation>> applicationRelationMap; // <aid,<applRels>

    /** instance relations */
    private final Map<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>> instanceRelMap; // <aid,<iid,<applRel,relInstIds>>>

    /** instance values */
    private final Map<Long, Map<Long, Map<String, TS_Value>>> instanceValueMap; // <aid,<iid,<aaName,value>>>

    /** instance attribute values */
    private final Map<Long, Map<Long, Map<String, TS_Value>>> instanceAttrValueMap; // <aid,<iid,<attrName,value>>>

    /** instance element CORBA object references */
    private final Map<Long, Map<Long, InstanceElement>> instanceElementCache; // <aid,<iid,<InstanceElement>>>

    /** The counters for ids */
    private int nextAid;

    /**
     * Constructor.
     */
    public AtfxCache() {
        this.nameToAeMap = new HashMap<String, ApplicationElement>();
        this.beToAidMap = new HashMap<String, Set<Long>>();
        this.aidToAeMap = new TreeMap<Long, ApplicationElement>();
        this.aidToAeNameMap = new HashMap<Long, String>();
        this.applicationAttributeMap = new HashMap<Long, Map<String, ApplicationAttribute>>();
        this.applicationRelationMap = new HashMap<Long, List<ApplicationRelation>>();
        this.baNameToAaNameMap = new HashMap<Long, Map<String, String>>();
        this.applicationRelations = new ArrayList<ApplicationRelation>();
        this.instanceRelMap = new HashMap<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>>();
        this.instanceValueMap = new HashMap<Long, Map<Long, Map<String, TS_Value>>>();
        this.instanceAttrValueMap = new HashMap<Long, Map<Long, Map<String, TS_Value>>>();
        this.instanceElementCache = new HashMap<Long, Map<Long, InstanceElement>>();
        this.nextAid = 1;
    }

    /**
     * Returns the next free application element id.
     * 
     * @return The application element id.
     */
    public long nextAid() {
        return this.nextAid++;
    }

    /**
     * Returns the next free instance element id for an application element.
     * 
     * @param aid The application element id.
     * @return The instance element id.
     */
    public long nextIid(long aid) {
        Set<Long> iids = this.instanceValueMap.get(aid).keySet();
        if (iids.size() < 1) {
            return 1;
        }
        return Collections.max(iids) + 1;
    }

    /***********************************************************************************
     * application elements
     ***********************************************************************************/

    /**
     * Adds an application element to the cache.
     * 
     * @param aid The application element id.
     * @param beName The base element name.
     * @param aeName The application element name.
     * @param ae The application element.
     */
    public void addApplicationElement(long aid, String beName, ApplicationElement ae) {
        this.aidToAeMap.put(aid, ae);
        this.aidToAeNameMap.put(aid, "");
        this.nameToAeMap.put("", ae);
        this.baNameToAaNameMap.put(aid, new HashMap<String, String>());
        this.applicationAttributeMap.put(aid, new LinkedHashMap<String, ApplicationAttribute>());
        this.applicationRelationMap.put(aid, new ArrayList<ApplicationRelation>());
        this.instanceRelMap.put(aid, new HashMap<Long, Map<ApplicationRelation, Set<Long>>>());
        this.instanceValueMap.put(aid, new TreeMap<Long, Map<String, TS_Value>>());
        this.instanceAttrValueMap.put(aid, new TreeMap<Long, Map<String, TS_Value>>());
        this.instanceElementCache.put(aid, new TreeMap<Long, InstanceElement>());

        Set<Long> applElems = this.beToAidMap.get(beName.toLowerCase());
        if (applElems == null) {
            applElems = new HashSet<Long>();
            this.beToAidMap.put(beName.toLowerCase(), applElems);
        }
        applElems.add(aid);
    }

    /**
     * Returns the list of all application elements.
     * 
     * @return All application elements.
     */
    public Collection<ApplicationElement> getApplicationElements() {
        return this.aidToAeMap.values();
    }

    /**
     * Returns an application element by given id.
     * 
     * @param aid The application element id.
     * @return The application element, null if not found.
     */
    public ApplicationElement getApplicationElementById(long aid) {
        return this.aidToAeMap.get(aid);
    }

    /**
     * Returns the application element name by id.
     * 
     * @param aid The application element id.
     * @return The application element name.
     */
    public String getApplicationElementNameById(long aid) {
        return this.aidToAeNameMap.get(aid);
    }

    /**
     * Returns the application element for given name.
     * 
     * @param aeName The application element name.
     * @return The application element, null if not found.
     */
    public ApplicationElement getApplicationElementByName(String aeName) {
        return this.nameToAeMap.get(aeName);
    }

    /**
     * Rename an application element.
     * 
     * @param aid The application element id.
     * @param newAeName The new name.
     */
    public void renameApplicationElement(long aid, String newAeName) {
        String oldName = this.aidToAeNameMap.get(aid);
        ApplicationElement ae = this.aidToAeMap.get(aid);
        this.aidToAeNameMap.put(aid, newAeName);
        this.nameToAeMap.remove(oldName);
        this.nameToAeMap.put(newAeName, ae);
    }

    /**
     * Removed an application element from the cache.
     * 
     * @param aid The application element id.
     */
    public void removeApplicationElement(long aid) {
        this.nameToAeMap.remove(aidToAeNameMap.get(aid));
        this.aidToAeNameMap.remove(aid);
        this.aidToAeMap.remove(aid);
        this.baNameToAaNameMap.remove(aid);
        this.applicationAttributeMap.remove(aid);
        this.applicationRelationMap.remove(aid);
        this.instanceRelMap.remove(aid);
        this.instanceValueMap.remove(aid);
        this.instanceAttrValueMap.remove(aid);
        this.instanceElementCache.remove(aid);
    }

    /**
     * Return the application element ids for given base element type.
     * 
     * @param baseType The base type (must be written in lowercase!)
     * @return Set of application element ids, may be null.
     */
    public Set<Long> getAidsByBaseType(String baseType) {
        return this.beToAidMap.get(baseType);
    }

    /***********************************************************************************
     * application attributes
     ***********************************************************************************/

    /**
     * Adds an application attribute to the cache.
     * 
     * @param aid The application element id.
     * @param aa The application attribute.
     */
    public void addApplicationAttribute(long aid, ApplicationAttribute aa) {
        this.applicationAttributeMap.get(aid).put("", aa);
    }

    /**
     * Returns the ordered list of all available application attribute names.
     * 
     * @param aid The application element id.
     * @return Collection of attribute names.
     */
    public Collection<String> listApplicationAttributes(long aid) {
        return this.applicationAttributeMap.get(aid).keySet();
    }

    /**
     * Returns an application attribute by name.
     * 
     * @param aid The application element id.
     * @param aaName The application attribute name.
     * @return The application attribute, null if not found.
     */
    public ApplicationAttribute getApplicationAttributeByName(long aid, String aaName) {
        return this.applicationAttributeMap.get(aid).get(aaName);
    }

    /**
     * Returns the ordered list of all available application attributes.
     * 
     * @param aid The application element id.
     * @return Collection of application attributes.
     */
    public Collection<ApplicationAttribute> getApplicationAttributes(long aid) {
        return this.applicationAttributeMap.get(aid).values();
    }

    /**
     * If application attribute name changes, all value keys has to be altered.
     * 
     * @param aid The application element id.
     * @param oldAaName The old application attribute name.
     * @param newAaName The new application attribute name.
     */
    public void renameApplicationAttribute(long aid, String oldAaName, String newAaName) {
        // update application attribute map
        Map<String, ApplicationAttribute> applAttrMap = this.applicationAttributeMap.get(aid);
        applAttrMap.put(newAaName, applAttrMap.remove(oldAaName));

        // invalidate instanceValueMap
        for (Map<String, TS_Value> map : this.instanceValueMap.get(aid).values()) {
            if (map.containsKey(oldAaName)) {
                map.put(newAaName, map.get(oldAaName));
            }
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
        // remove from base attribute map
        String baName = null;
        for (Entry<String, String> entry : this.baNameToAaNameMap.get(aid).entrySet()) {
            if (entry.getValue().equals(aaName)) {
                baName = entry.getKey();
                break;
            }
        }
        this.baNameToAaNameMap.get(aid).remove(baName);

        // remove from application attribute map
        this.applicationAttributeMap.get(aid).remove(aaName);

        // remove from instance value map
        Map<Long, Map<String, TS_Value>> ieValueMap = this.instanceValueMap.get(aid);
        for (Map<String, TS_Value> v : ieValueMap.values()) {
            if (v != null) {
                v.remove(aaName);
            }
        }
    }

    /**
     * Sets the base attribute name for an application attribute.
     * 
     * @param aid The application element id.
     * @param baName The base attribute name, null for no base attribute
     * @param aaName The application attribute name.
     */
    public void setAaNameForBaName(long aid, String baName, String aaName) {
        if (baName == null || baName.length() < 1) {
            this.baNameToAaNameMap.get(aid).remove(baName);
        } else {
            this.baNameToAaNameMap.get(aid).put(baName, aaName);
        }
    }

    /**
     * Returns the application attribute name for given base attribute name.
     * <p>
     * The lookup will be performed case insensitive!
     * 
     * @param aid The application element id.
     * @param baName The base attribute name.
     * @return The application attribute name, null if not found.
     */
    public String getAaNameByBaName(long aid, String baName) {
        return this.baNameToAaNameMap.get(aid).get(baName.toLowerCase());
    }

    /**
     * Returns the application attribute for given base attribute name.
     * <p>
     * The lookup will be performed case insensitive!
     * 
     * @param aid The application element id.
     * @param baName The base attribute name.
     * @return The application attribute, null if not found.
     */
    public ApplicationAttribute getApplicationAttributeByBaName(long aid, String baName) {
        String aaName = getAaNameByBaName(aid, baName);
        return this.applicationAttributeMap.get(aid).get(aaName);
    }

    /***********************************************************************************
     * application relations
     ***********************************************************************************/

    /**
     * Returns all application relations.
     * 
     * @return Collection of application relations.
     */
    public Collection<ApplicationRelation> getApplicationRelations() {
        return this.applicationRelations;
    }

    /**
     * Returns all application relations of an application element.
     * 
     * @param aid The application element id.
     * @return Collection of application relations.
     */
    public Collection<ApplicationRelation> getApplicationRelations(long aid) throws AoException {
        return this.applicationRelationMap.get(aid);
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
        for (ApplicationRelation ar : this.applicationRelationMap.get(aid)) {
            if (ar.getRelationName().equals(relName)) {
                return ar;
            }
        }
        return null;
    }

    /**
     * Sets the application element
     * 
     * @param aid
     * @param applRel
     */
    public void setApplicationRelationElem1(long aid, ApplicationRelation applRel) {
        this.applicationRelationMap.get(aid).add(applRel);
    }

    public void removeApplicationRelationElem1(long aid, ApplicationRelation applRel) {
        this.applicationRelationMap.get(aid).remove(applRel);
    }

    public void addApplicationRelation(ApplicationRelation applRel) throws AoException {
        this.applicationRelations.add(applRel);
        // prepare application relation maps for all aids
        for (Map<Long, Map<ApplicationRelation, Set<Long>>> map : this.instanceRelMap.values()) {
            for (Map<ApplicationRelation, Set<Long>> iMap : map.values()) {
                iMap.put(applRel, new TreeSet<Long>());
            }
        }
    }

    public void removeApplicationRelation(ApplicationRelation applRel) {
        this.applicationRelations.remove(applRel);
        for (List<ApplicationRelation> relList : this.applicationRelationMap.values()) {
            if (relList.contains(applRel)) {
                relList.remove(applRel);
            }
        }
        // remove application relation maps for all aids
        for (Map<Long, Map<ApplicationRelation, Set<Long>>> map : this.instanceRelMap.values()) {
            for (Map<ApplicationRelation, Set<Long>> iMap : map.values()) {
                iMap.remove(applRel);
            }
        }
    }

    /**
     * Returns the inverse relation for given application relation.
     * 
     * @param applRel The application relation.
     * @return The inverse application relation.
     * @throws AoException Unable to find inverse application relation.
     */
    public ApplicationRelation getInverseRelation(ApplicationRelation applRel) throws AoException {
        if (applRel.getElem2() != null) {
            long elem1Aid = ODSHelper.asJLong(applRel.getElem1().getId());
            long elem2Aid = ODSHelper.asJLong(applRel.getElem2().getId());
            for (ApplicationRelation invRel : this.applicationRelationMap.get(elem2Aid)) {
                if ((elem1Aid == ODSHelper.asJLong(invRel.getElem2().getId()))
                        && applRel.getRelationName().equals(invRel.getInverseRelationName())) {
                    return invRel;
                }
            }
        }
        throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                              "Unable to find inverse relation for '" + applRel.getRelationName() + "'");
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
    public void addInstance(long aid, long iid) throws AoException {
        this.instanceRelMap.get(aid).put(iid, new HashMap<ApplicationRelation, Set<Long>>());
        for (ApplicationRelation rel : this.getApplicationRelations(aid)) {
            this.instanceRelMap.get(aid).get(iid).put(rel, new TreeSet<Long>());
        }

        this.instanceValueMap.get(aid).put(iid, new HashMap<String, TS_Value>());
        this.instanceAttrValueMap.get(aid).put(iid, new LinkedHashMap<String, TS_Value>());
    }

    /**
     * Returns an instance element by given instance id.
     * 
     * @param poa The POA for lazy creation of the CORBA object.
     * @param aid The application element id.
     * @param iid The instance id.
     * @return The instance element, null if not found.
     * @throws AoException Error lazy create CORBA instance element.
     */
    public InstanceElement getInstanceById(POA instancePOA, long aid, long iid) throws AoException {
        if (this.instanceExists(aid, iid)) {
            InstanceElement ie = this.instanceElementCache.get(aid).get(iid);
            if (ie == null) {
                byte[] oid = toByta(new long[] { aid, iid });
                org.omg.CORBA.Object obj = instancePOA.create_reference_with_id(oid, InstanceElementHelper.id());
                ie = InstanceElementHelper.narrow(obj);
                this.instanceElementCache.get(aid).put(iid, ie);
            }
            return ie;
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Instance not found [aid=" + aid + ",iid="
                + iid + "]");
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
        return (long) ((long) (0xff & data[0]) << 56 | (long) (0xff & data[1]) << 48 | (long) (0xff & data[2]) << 40
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
     * @param instancePOA The instance POA.
     * @param aid The application element id.
     * @return Collection if instance elements.
     * @throws AoException Error lazy create CORBA instance element.
     */
    public InstanceElement[] getInstances(POA instancePOA, long aid) throws AoException {
        Set<Long> iids = this.instanceValueMap.get(aid).keySet();
        InstanceElement[] ies = new InstanceElement[iids.size()];
        int i = 0;
        for (long iid : iids) {
            ies[i] = getInstanceById(instancePOA, aid, iid);
            i++;
        }
        return ies;
    }

    /**
     * Removes an instance element from the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @throws AoException
     */
    public void removeInstance(long aid, long iid) throws AoException {
        // remove relations
        for (ApplicationRelation applRel : getApplicationRelations(aid)) {
            removeInstanceRelations(aid, iid, applRel, getRelatedInstanceIds(aid, iid, applRel));
        }
        // remove instance values
        this.instanceValueMap.get(aid).remove(iid);
        this.instanceAttrValueMap.get(aid).remove(iid);
        this.instanceRelMap.get(aid).remove(iid);
        this.instanceElementCache.get(aid).remove(iid);
    }

    /**
     * Returns all instance ids for given application element id.
     * 
     * @param aid The application element id.
     * @return The instance ids.
     */
    public Set<Long> getInstanceIds(long aid) {
        return this.instanceValueMap.get(aid).keySet();
    }

    /**
     * Returns whether an instance element exists.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return True, if instance exists, otherwise false.
     */
    public boolean instanceExists(long aid, long iid) {
        return this.instanceValueMap.get(aid).containsKey(iid);
    }

    /**
     * Returns the environment instance.
     * 
     * @param poa any valid POA
     * @return The environment instance, null if not application element derived from 'AoEnviroment' exists or no
     *         instance available.
     * @throws AoException if something went wrong
     */
    public InstanceElement getEnvironmentInstance(POA modelPOA, POA instancePOA) throws AoException {
        Set<Long> envAidSet = this.beToAidMap.get("aoenvironment");
        if (envAidSet != null && !envAidSet.isEmpty()) {
            long envAid = envAidSet.iterator().next();
            InstanceElement[] ieAr = this.getInstances(instancePOA, envAid);
            if (ieAr.length > 0) {
                return ieAr[0];
            }
        }
        return null;
    }

    /**
     * Returns the name of the first found environment application element.
     * 
     * @param poa any valid POA
     * @return the name of the first found environment AE or null, if none exist
     * @throws AoException if something went wrong
     */
    public String getEnvironmentApplicationElementName(POA poa) throws AoException {
        Set<Long> envAidSet = this.beToAidMap.get("aoenvironment");
        if (envAidSet != null && !envAidSet.isEmpty()) {
            long envAid = envAidSet.iterator().next();
            ApplicationElement env = this.getApplicationElementById(envAid);
            if (env != null) {
                return env.getName();
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
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setInstanceValue(long aid, long iid, String aaName, TS_Value value) throws AoException {
        this.instanceValueMap.get(aid).get(iid).put(aaName, value);
    }

    /**
     * Returns a value of an instance element.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param valName The application attribute name.
     * @return The value, null if not found.
     */
    public TS_Value getInstanceValue(long aid, long iid, String aaName) {
        return this.instanceValueMap.get(aid).get(iid).get(aaName);
    }

    /***********************************************************************************
     * instance attribute values
     ***********************************************************************************/

    /**
     * List all instance attribute names.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return Collection of attribute names.
     */
    public Collection<String> listInstanceAttributes(long aid, long iid) {
        return this.instanceAttrValueMap.get(aid).get(iid).keySet();
    }

    /**
     * Sets an instance attribute value.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param attrName The attribute name.
     * @param value The instance value.
     */
    public void setInstanceAttributeValue(long aid, long iid, String attrName, TS_Value value) {
        this.instanceAttrValueMap.get(aid).get(iid).put(attrName, value);
    }

    /**
     * Returns the value of an instance attribute.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param attrName The attribute name.
     * @return The value, null if instance attribute does not exist.
     */
    public TS_Value getInstanceAttributeValue(long aid, long iid, String attrName) {
        return instanceAttrValueMap.get(aid).get(iid).get(attrName);
    }

    /**
     * Returns all values of an instance attribute of a given list of instances.
     * 
     * @param aid the application element id
     * @param iids the array of instance ids
     * @param attrName the attribute name
     * @return TS_ValueSeq containing all values, or null, if one or more of the instances do not possess the instance
     *         attribute.
     * @throws AoException if the conversion from tsValue array to tsValueSeq fails
     */
    public TS_ValueSeq listInstanceValues(long aid, long[] iids, String attrName) throws AoException {
        TS_Value[] tsValues = new TS_Value[iids.length];
        for (int index = 0; index < iids.length; index++) {
            TS_Value value = getInstanceValue(aid, iids[index], attrName);
            if (value == null) {
                return null;
            }
            tsValues[index] = value;
        }
        return ODSHelper.tsValue2ts_valueSeq(tsValues);
    }

    public void removeInstanceAttribute(long aid, long iid, String attrName) {
        this.instanceAttrValueMap.get(aid).get(iid).remove(attrName);
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

        // add relation, if none multiple cardinality, overwrite
        Set<Long> relInstIds = this.instanceRelMap.get(aid).get(iid).get(applRel);
        if (applRel.getRelationRange().max != -1) {
            relInstIds.clear();
        }
        relInstIds.addAll(otherIids);

        // add inverse relation
        ApplicationRelation invApplRel = getInverseRelation(applRel);
        long otherAid = ODSHelper.asJLong(invApplRel.getElem1().getId());
        for (Long otherIid : otherIids) {
            Set<Long> invRelInstIds = this.instanceRelMap.get(otherAid).get(otherIid).get(invApplRel);
            if (invApplRel.getRelationRange().max != -1) {
                invRelInstIds.clear();
            }
            invRelInstIds.add(iid);
        }
    }

    /**
     * Removes an instance relation
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIid The target instance element id.
     * @throws AoException Error removing instance relation.
     */
    public void removeInstanceRelations(long aid, long iid, ApplicationRelation applRel, Collection<Long> otherIids)
            throws AoException {
        if (otherIids.isEmpty()) {
            return;
        }

        // remove relations
        this.instanceRelMap.get(aid).get(iid).get(applRel).removeAll(otherIids);

        // remove inverse relations
        ApplicationRelation invApplRel = getInverseRelation(applRel);
        long otherAid = ODSHelper.asJLong(invApplRel.getElem1().getId());
        for (Long otherIid : otherIids) {
            this.instanceRelMap.get(otherAid).get(otherIid).get(invApplRel).remove(iid);
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
    public Collection<Long> getRelatedInstanceIds(long aid, long iid, ApplicationRelation applRel) throws AoException {
        return new ArrayList<Long>(this.instanceRelMap.get(aid).get(iid).get(applRel));
    }

}
