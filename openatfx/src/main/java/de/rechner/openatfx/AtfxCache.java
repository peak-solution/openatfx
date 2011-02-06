package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Central cache holding all data for performance reasons.
 * 
 * @author Christian Rechner
 */
class AtfxCache {

    private static final Log LOG = LogFactory.getLog(AtfxCache.class);

    /** application elements */
    private final Map<String, ApplicationElement> nameToAeMap; // <aeName, ae>
    private final Map<String, Set<Long>> beToAidMap; // <beName, aid>
    private final Map<Long, String> aidToAeNameMap;
    private final Map<Long, ApplicationElement> aidToAeMap;

    /** application attributes */
    private final Map<Long, Map<String, ApplicationAttribute>> applicationAttributeMap; // <aid,<aaName,aaName>>
    private final Map<Long, Map<String, ApplicationAttribute>> baNameToApplAttrMap; // <aid,<baName,aaName>>

    /** application relations */
    private final List<ApplicationRelation> applicationRelations;
    private final Map<Long, List<ApplicationRelation>> applicationRelationMap; // <aid,<applRels>

    /** instance elements */
    private final Map<Long, Map<Long, InstanceElement>> instanceElementMap; // <aid,<iid,ie>>

    /** instance values */
    private final Map<Long, Map<Long, Map<String, TS_Value>>> instanceValueMap; // <aid,<iid,<attrName,value>>>

    /** instance relations */
    private final Map<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>> instanceRelMap; // <aid,<iid,<applRel,relInstIds>>>

    /** The counters for ids */
    private final Map<Long, Long> nextIids;
    private int nextAid;

    /**
     * Constructor.
     */
    public AtfxCache() {
        this.nameToAeMap = new HashMap<String, ApplicationElement>();
        this.beToAidMap = new HashMap<String, Set<Long>>();
        this.aidToAeMap = new HashMap<Long, ApplicationElement>();
        this.aidToAeNameMap = new HashMap<Long, String>();
        this.applicationAttributeMap = new HashMap<Long, Map<String, ApplicationAttribute>>();
        this.applicationRelationMap = new HashMap<Long, List<ApplicationRelation>>();
        this.baNameToApplAttrMap = new HashMap<Long, Map<String, ApplicationAttribute>>();
        this.applicationRelations = new ArrayList<ApplicationRelation>();
        this.instanceElementMap = new HashMap<Long, Map<Long, InstanceElement>>();
        this.instanceValueMap = new HashMap<Long, Map<Long, Map<String, TS_Value>>>();
        this.instanceRelMap = new HashMap<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>>();
        this.nextIids = new HashMap<Long, Long>();
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
        Long l = this.nextIids.get(aid);
        l++;
        this.nextIids.put(aid, l);
        return l;
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
        this.baNameToApplAttrMap.put(aid, new HashMap<String, ApplicationAttribute>());
        this.applicationAttributeMap.put(aid, new LinkedHashMap<String, ApplicationAttribute>());
        this.applicationRelationMap.put(aid, new ArrayList<ApplicationRelation>());
        this.instanceElementMap.put(aid, new HashMap<Long, InstanceElement>());
        this.instanceValueMap.put(aid, new HashMap<Long, Map<String, TS_Value>>());
        this.instanceRelMap.put(aid, new HashMap<Long, Map<ApplicationRelation, Set<Long>>>());
        this.nextIids.put(aid, (long) 0);

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
        this.baNameToApplAttrMap.remove(aid);
        this.applicationAttributeMap.remove(aid);
        this.applicationRelationMap.remove(aid);
        this.instanceElementMap.remove(aid);
        this.instanceValueMap.remove(aid);
        this.instanceRelMap.remove(aid);
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
    public void addApplicationAttribute(long aid, ApplicationAttribute aa) throws AoException {
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
        for (ApplicationAttribute aa : this.baNameToApplAttrMap.get(aid).values()) {
            if (aa.getName().equals(aaName)) {
                baName = aa.getBaseAttribute().getName();
            }
        }
        this.baNameToApplAttrMap.get(aid).remove(baName);

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
    public void setAaNameForBaName(long aid, String baName, ApplicationAttribute aa) {
        if (baName == null || baName.length() < 1) {
            this.baNameToApplAttrMap.get(aid).remove(baName);
        } else {
            this.baNameToApplAttrMap.get(aid).put(baName, aa);
        }
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
        return this.baNameToApplAttrMap.get(aid).get(baName.toLowerCase());
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
    public Collection<ApplicationRelation> getApplicationRelations(long aid) {
        return this.applicationRelationMap.get(aid);
    }

    public ApplicationRelation getApplicationRelationByName(long aid, String relName) throws AoException {
        for (ApplicationRelation ar : this.applicationRelationMap.get(aid)) {
            if (ar.getRelationName().equals(relName)) {
                return ar;
            }
        }
        return null;
    }

    public void setApplicationRelationElem1(long aid, ApplicationRelation applRel) {
        this.applicationRelationMap.get(aid).add(applRel);
    }

    public void removeApplicationRelationElem1(long aid, ApplicationRelation applRel) {
        this.applicationRelationMap.get(aid).remove(applRel);
    }

    public void addApplicationRelation(ApplicationRelation applRel) {
        this.applicationRelations.add(applRel);
    }

    public void removeApplicationRelation(ApplicationRelation applRel) {
        this.applicationRelations.remove(applRel);
        for (List<ApplicationRelation> relList : this.applicationRelationMap.values()) {
            if (relList.contains(applRel)) {
                relList.remove(applRel);
            }
        }

        // TODO: remove from instance relation cache

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
     * @param ie The instance element.
     */
    public void addInstance(long aid, long iid, InstanceElement ie) {
        this.instanceElementMap.get(aid).put(iid, ie);
        this.instanceValueMap.get(aid).put(iid, new HashMap<String, TS_Value>());
        this.instanceRelMap.get(aid).put(iid, new HashMap<ApplicationRelation, Set<Long>>());
    }

    /**
     * Removes an instance element from the instance cache.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public void removeInstance(long aid, long iid) {
        this.instanceElementMap.get(aid).remove(iid);
        this.instanceValueMap.get(aid).remove(iid);
        this.instanceRelMap.get(aid).remove(iid);
    }

    /**
     * Returns an instance element by given instance id.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return The instance element, null if not found.
     */
    public InstanceElement getInstanceById(long aid, long iid) {
        return this.instanceElementMap.get(aid).get(iid);
    }

    /**
     * Returns all instance ids for given application element id.
     * 
     * @param aid The application element id.
     * @return The instance ids.
     */
    public Set<Long> getInstanceIds(long aid) {
        return this.instanceElementMap.get(aid).keySet();
    }

    /**
     * Returns all instance elements for an application element.
     * 
     * @param aid The application element id.
     * @return Collection if instance elements.
     */
    public Collection<InstanceElement> getInstances(long aid) {
        return this.instanceElementMap.get(aid).values();
    }

    /**
     * Returns whether an instance element exists.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @return True, if instance exists, otherwise false.
     */
    public boolean instanceExists(long aid, long iid) {
        Map<Long, InstanceElement> iMap = this.instanceElementMap.get(aid);
        if (iMap != null) {
            return iMap.containsKey(iid);
        }
        return false;
    }

    /**
     * Returns the environment instance.
     * 
     * @return The environment instance, null if not application element derived from 'AoEnviroment' exists or no
     *         instance available.
     */
    public InstanceElement getEnvironmentInstance() {
        Set<Long> envAidSet = this.beToAidMap.get("aoenvironment");
        if (envAidSet != null && !envAidSet.isEmpty()) {
            long envAid = envAidSet.iterator().next();
            Map<Long, InstanceElement> map = this.instanceElementMap.get(envAid);
            if (map != null && !map.isEmpty()) {
                return map.values().iterator().next();
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
     * instance relations
     ***********************************************************************************/

    /**
     * Creates an instance relation.
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIid The target instance element id.
     * @throws AoException Error creating instance relation.
     */
    public void createInstanceRelation(long aid, long iid, ApplicationRelation applRel, long otherIid)
            throws AoException {
        // add relation
        Map<ApplicationRelation, Set<Long>> relsMap = this.instanceRelMap.get(aid).get(iid);
        Set<Long> relMap = relsMap.get(applRel);
        if (relMap == null) {
            relMap = new HashSet<Long>();
            relsMap.put(applRel, relMap);
        } else if (applRel.getRelationRange().max != -1) {
            relMap.clear();
        }
        relMap.add(otherIid);

        // add inverse relation
        ApplicationRelation invApplRel = getInverseRelation(applRel);
        long otherAid = ODSHelper.asJLong(invApplRel.getElem1().getId());
        Map<ApplicationRelation, Set<Long>> invRelsMap = this.instanceRelMap.get(otherAid).get(otherIid);
        Set<Long> invRelMap = invRelsMap.get(invApplRel);
        if (invRelMap == null) {
            invRelMap = new HashSet<Long>();
            invRelsMap.put(invApplRel, invRelMap);
        } else if (invApplRel.getRelationRange().max != -1) {
            invRelMap.clear();
        }
        invRelMap.add(iid);
    }

    /**
     * Removes an instance relation
     * 
     * @param aid The source application element id.
     * @param iid The source instance id.
     * @param applRel The application relation.
     * @param otherIid The target instance element id.
     */
    public void removeInstanceRelation(long aid, long iid, ApplicationRelation applRel, long otherIid) {
        Map<ApplicationRelation, Set<Long>> relsMap = this.instanceRelMap.get(aid).get(iid);
        Set<Long> relMap = relsMap.get(applRel);
        if (relMap != null) {
            relMap.remove(otherIid);
        }

        // TODO: remove inverse relation
    }

    public Set<Long> getRelatedInstanceIds(long aid, long iid, ApplicationRelation applRel) throws AoException {
        Set<Long> relInstIds = new HashSet<Long>();
        Set<Long> set = this.instanceRelMap.get(aid).get(iid).get(applRel);
        if (set != null) {
            long otherAid = ODSHelper.asJLong(applRel.getElem2().getId());
            for (Long otherIid : set) {
                if (instanceExists(otherAid, otherIid)) {
                    relInstIds.add(otherIid);
                } else {
                    String aeName = applRel.getElem1().getName();
                    String otherAeName = applRel.getElem2().getName();
                    LOG.warn("Related instance not found [fromAid=" + aeName + ",fromId=" + iid + " - toAe="
                            + otherAeName + ",toId=" + otherIid + "],relName=" + applRel.getRelationName());
                }
            }
            return relInstIds;
        }
        return Collections.emptySet();
    }

}
