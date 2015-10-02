package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.asam.ods.BaseRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementHelper;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementIteratorHelper;
import org.asam.ods.NameValue;
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

    /** the file handler */
    private final IFileHandler fileHandler;

    /** session context variables */
    private final Map<String, NameValue> context;

    /** application elements */
    private final Map<String, ApplicationElement> nameToAeMap; // <aeName,
                                                               // ApplicationElement>
    private final Map<String, Set<Long>> beToAidMap; // <beName, aid>
    private final Map<Long, String> aidToAeNameMap;
    private final Map<Long, ApplicationElement> aidToAeMap;

    /** application attributes */
    private final Map<Long, Map<Integer, ApplicationAttribute>> attrNoToAttrMap; // <aid,<attrNo,Attribute>>
    private final Map<Long, Map<String, Integer>> aaNameToAttrNoMap; // <aid,<aaName,attrNo>>
    private final Map<Long, Map<String, Integer>> baNameToAttrNoMap; // <aid,<baName,attrNo>>

    /** application relations */
    private final Map<Long, Set<ApplicationRelation>> applicationRelationMap; // <aid,<applRels>
    private final Map<ApplicationRelation, ApplicationRelation> inverseRelationMap; // <rel, invRel>

    /** instance relations */
    private final Map<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>> instanceRelMap; // <aid,<iid,<applRel,relInstIds>>>

    /** instance values */
    private final Map<Long, Map<Long, Map<Integer, Object>>> instanceValueMap; // <aid,<iid,<attrNo,value>>>

    /** instance attribute values */
    private final Map<Long, Map<Long, Map<String, TS_Value>>> instanceAttrValueMap; // <aid,<iid,<attrName,value>>>

    /** instance element CORBA object references */
    private final Map<Long, Map<Long, InstanceElement>> instanceElementCache; // <aid,<iid,<InstanceElement>>>

    /** the instance iterator references */
    private final Map<Long, InstanceElement[]> instanceIteratorElementCache;
    private final Map<Long, Integer> instanceIteratorPointerCache;
    private long nextInstanceIteratorId = 0;

    /** The counters for ids */
    private int nextAid;
    private final Map<Long, Integer> nextAttrNoMap;

    /**
     * Constructor.
     * 
     * @param fileHandler The file handler.
     */
    public AtfxCache(IFileHandler fileHandler) {
        this.fileHandler = fileHandler;

        /** session context */
        this.context = new LinkedHashMap<String, NameValue>();

        /** application element */
        this.nameToAeMap = new HashMap<String, ApplicationElement>();
        this.beToAidMap = new HashMap<String, Set<Long>>();
        this.aidToAeMap = new TreeMap<Long, ApplicationElement>();
        this.aidToAeNameMap = new HashMap<Long, String>();

        /** application attributes */
        this.attrNoToAttrMap = new HashMap<Long, Map<Integer, ApplicationAttribute>>();
        this.aaNameToAttrNoMap = new HashMap<Long, Map<String, Integer>>();
        this.baNameToAttrNoMap = new HashMap<Long, Map<String, Integer>>();

        this.applicationRelationMap = new HashMap<Long, Set<ApplicationRelation>>();
        this.inverseRelationMap = new HashMap<ApplicationRelation, ApplicationRelation>();

        this.instanceRelMap = new HashMap<Long, Map<Long, Map<ApplicationRelation, Set<Long>>>>();
        this.instanceValueMap = new HashMap<Long, Map<Long, Map<Integer, Object>>>();
        this.instanceAttrValueMap = new HashMap<Long, Map<Long, Map<String, TS_Value>>>();
        this.instanceElementCache = new HashMap<Long, Map<Long, InstanceElement>>();
        this.instanceIteratorElementCache = new HashMap<Long, InstanceElement[]>();
        this.instanceIteratorPointerCache = new HashMap<Long, Integer>();

        this.nextAid = 1;
        this.nextAttrNoMap = new HashMap<Long, Integer>();
    }

    /**
     * Returns the file handler object.
     * 
     * @return The file handler.
     */
    public IFileHandler getFileHandler() {
        return fileHandler;
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
     * Returns the next free attribute number for an application element.
     * 
     * @param aid The application element id.
     * @return The attribute number.
     */
    public int nextAttrNo(long aid) {
        Integer nextAttrNo = this.nextAttrNoMap.get(aid);
        if (nextAttrNo == null) {
            nextAttrNo = 0;
        }
        nextAttrNo++;
        this.nextAttrNoMap.put(aid, nextAttrNo);
        return nextAttrNo;
    }

    /**
     * Returns the next free instance element id for an application element.
     * 
     * @param aid The application element id.
     * @return The instance element id.
     */
    public long nextIid(long aid) {
        Long[] instIids = getInstanceIds(aid).toArray(new Long[0]);
        if (instIids.length > 0) {
            return instIids[instIids.length - 1] + 1;
        }
        return 1;
    }

    /***********************************************************************************
     * session context
     ***********************************************************************************/

    public Map<String, NameValue> getContext() {
        return context;
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
        this.attrNoToAttrMap.put(aid, new LinkedHashMap<Integer, ApplicationAttribute>());
        this.aaNameToAttrNoMap.put(aid, new LinkedHashMap<String, Integer>());
        this.baNameToAttrNoMap.put(aid, new HashMap<String, Integer>());
        this.applicationRelationMap.put(aid, new LinkedHashSet<ApplicationRelation>());
        this.instanceRelMap.put(aid, new HashMap<Long, Map<ApplicationRelation, Set<Long>>>());
        this.instanceValueMap.put(aid, new TreeMap<Long, Map<Integer, Object>>());
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
        this.aaNameToAttrNoMap.remove(aid);
        this.baNameToAttrNoMap.remove(aid);
        this.aaNameToAttrNoMap.remove(aid);
        this.applicationRelationMap.remove(aid);
        this.instanceRelMap.remove(aid);
        this.instanceValueMap.remove(aid);
        this.instanceAttrValueMap.remove(aid);
        this.instanceElementCache.remove(aid);
        this.nextAttrNoMap.remove(aid);
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

    public void addApplicationAttribute(long aid, int attrNo, ApplicationAttribute aa) {
        this.attrNoToAttrMap.get(aid).put(attrNo, aa);
    }

    /**
     * Returns the ordered list of all available application attribute names.
     * 
     * @param aid The application element id.
     * @return Collection of attribute names.
     */
    public Collection<String> listApplicationAttributes(long aid) {
        return this.aaNameToAttrNoMap.get(aid).keySet();
    }

    public ApplicationAttribute getApplicationAttribute(long aid, int attrNo) {
        return this.attrNoToAttrMap.get(aid).get(attrNo);
    }

    public Integer getAttrNoByName(long aid, String aaName) {
        return this.aaNameToAttrNoMap.get(aid).get(aaName);
    }

    /**
     * Returns the attribute index number for given base attribute name.
     * 
     * @param aid The application element id.
     * @param baName The base attribute name.
     * @return The attribute index number, null if no application attribute found for base attribute.
     */
    public Integer getAttrNoByBaName(long aid, String baName) {
        return this.baNameToAttrNoMap.get(aid).get(baName);
    }

    public void setBaNameForAttrNo(long aid, int attrNo, String baName) {
        if (baName == null) {
            this.baNameToAttrNoMap.get(aid).remove(null);
        } else {
            this.baNameToAttrNoMap.get(aid).put(baName, attrNo);
        }
    }

    /**
     * Returns the ordered list of all available application attributes.
     * 
     * @param aid The application element id.
     * @return Collection of application attributes.
     */
    public Collection<ApplicationAttribute> getApplicationAttributes(long aid) {
        return this.attrNoToAttrMap.get(aid).values();
    }

    /**
     * If application attribute name changes, all value keys has to be altered.
     * 
     * @param aid The application element id.
     * @param attrNo The attribute number.
     * @param oldAaName The old application attribute name.
     * @param newAaName The new application attribute name.
     */
    public void renameApplicationAttribute(long aid, int attrNo, String oldAaName, String newAaName) {
        Map<String, Integer> attrNoMap = this.aaNameToAttrNoMap.get(aid);
        attrNoMap.remove(oldAaName);
        attrNoMap.put(newAaName, attrNo);
    }

    /**
     * Removes an application attribute from the cache.
     * 
     * @param aid The application element id.
     * @param aaName The application attribute name.
     * @throws AoException
     */
    public void removeApplicationAttribute(long aid, String aaName) throws AoException {
        Integer attrNo = getAttrNoByName(aid, aaName);
        this.attrNoToAttrMap.get(aid).remove(attrNo);
        this.aaNameToAttrNoMap.get(aid).remove(aaName);

        // remove from base attribute map
        String baName = null;
        for (Entry<String, Integer> entry : this.baNameToAttrNoMap.get(aid).entrySet()) {
            if (entry.getValue().equals(attrNo)) {
                baName = entry.getKey();
            }
        }
        this.baNameToAttrNoMap.get(aid).remove(baName);

        // remove from instance value map
        Map<Long, Map<Integer, Object>> ieValueMap = this.instanceValueMap.get(aid);
        for (Map<Integer, Object> v : ieValueMap.values()) {
            if (v != null) {
                v.remove(attrNo);
            }
        }
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
        return this.inverseRelationMap.keySet();
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

    public ApplicationRelation getApplicationRelationByBaseName(long aid, String bRelName) throws AoException {
        for (ApplicationRelation ar : this.applicationRelationMap.get(aid)) {
            BaseRelation br = ar.getBaseRelation();
            if (br != null && br.getRelationName().equals(bRelName)) {
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

    /**
     * Adds an application relation.
     * 
     * @param applRel The relation to add, may not be null.
     * @param invApplRel The inverse relation, may not be null.
     */
    public void addApplicationRelation(ApplicationRelation applRel, ApplicationRelation invApplRel) {
        this.inverseRelationMap.put(applRel, invApplRel);
        this.inverseRelationMap.put(invApplRel, applRel);

        // prepare application relation maps for all aids
        for (Map<Long, Map<ApplicationRelation, Set<Long>>> map : this.instanceRelMap.values()) {
            for (Map<ApplicationRelation, Set<Long>> iMap : map.values()) {
                iMap.put(applRel, new TreeSet<Long>());
            }
        }

        // prepare application relation maps for all aids
        for (Map<Long, Map<ApplicationRelation, Set<Long>>> map : this.instanceRelMap.values()) {
            for (Map<ApplicationRelation, Set<Long>> iMap : map.values()) {
                iMap.put(invApplRel, new TreeSet<Long>());
            }
        }
    }

    /**
     * Removes an application relation from the cache.
     * 
     * @param applRel The application relation.
     * @param invApplRel The inverse application relation.
     */
    public void removeApplicationRelation(ApplicationRelation applRel, ApplicationRelation invApplRel) {
        this.inverseRelationMap.remove(applRel);
        this.inverseRelationMap.remove(invApplRel);

        // remove application relation maps for all aids
        for (Set<ApplicationRelation> relList : this.applicationRelationMap.values()) {
            relList.remove(applRel);
            relList.remove(invApplRel);
        }
        // remove application relation maps for all instances
        for (Map<Long, Map<ApplicationRelation, Set<Long>>> map : this.instanceRelMap.values()) {
            for (Map<ApplicationRelation, Set<Long>> iMap : map.values()) {
                iMap.remove(applRel);
                iMap.remove(invApplRel);
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
        ApplicationRelation invRel = this.inverseRelationMap.get(applRel);
        if (invRel == null) {
            throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                  "Unable to find inverse relation for '" + applRel.getRelationName() + "'");
        }
        return invRel;
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

        this.instanceValueMap.get(aid).put(iid, new HashMap<Integer, Object>());
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
                byte[] oid = toByta(new long[] { 0, aid, iid }); // 0=InstanceElement
                org.omg.CORBA.Object obj = instancePOA.create_reference_with_id(oid, InstanceElementHelper.id());
                ie = InstanceElementHelper.unchecked_narrow(obj);
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
     * @param attrNo The application attribute number.
     * @param value The value.
     */
    public void setInstanceValue(long aid, long iid, int attrNo, TS_Value value) throws AoException {
        // check if attribute is 'values' of 'AoLocalColumn', then special handlong
        if (isLocalColumnValuesAttribute(aid, attrNo)) {

            // read sequence representation and write_mode
            int seqRepAttrNo = getAttrNoByBaName(aid, "sequence_representation");
            int seqRep = getInstanceValue(aid, seqRepAttrNo, iid).u.enumVal();
            String writeMode = this.context.get("write_mode").value.u.stringVal();

            // ***************************************************
            // seqRep implicit_constant=1,implicit_linear=2,implicit_saw=3,formula=6
            if ((seqRep == 1) || (seqRep == 2) || (seqRep == 3) || (seqRep == 6)) {
                // return;
            }

            // ***************************************************
            // write mode 'file', then write to external component
            else if (writeMode.equals("file")) {
                seqRep = ODSHelper.seqRepComp2seqRepExtComp(seqRep);
                ExtCompWriter.getInstance().writeValues(this, iid, value);
                setInstanceValue(aid, iid, seqRepAttrNo, ODSHelper.createEnumNV("", seqRep).value);
                return;
            }

            // ***************************************************
            // write mode 'database', then write to XML (memory)
            else if (writeMode.equals("database")) {
                seqRep = ODSHelper.seqRepExtComp2seqRepComp(seqRep);
                setInstanceValue(aid, iid, seqRepAttrNo, ODSHelper.createEnumNV("", seqRep).value);
            }
        }

        // check if attribute is 'flags' of 'AoLocalColumn', then special handling
        else if (isLocalColumnFlagsAttribute(aid, attrNo)) {
            // check if values are referenced from external component
            ApplicationRelation relLcExtComp = getApplicationRelationByBaseName(aid, "external_component");
            Collection<Long> extCompIids = getRelatedInstanceIds(aid, iid, relLcExtComp);
            if (extCompIids.size() == 1) {
                long extCompIid = extCompIids.iterator().next();
                ExtCompWriter.getInstance().writeFlags(this, extCompIid, value.u.shortSeq());
                return;
            }
        }

        // put value to memory
        java.lang.Object jValue = ODSHelper.tsValue2jObject(value);
        this.instanceValueMap.get(aid).get(iid).put(attrNo, jValue);
    }

    /**
     * Returns a value of an instance element.
     * 
     * @param aid The application element id.
     * @param iid The instance id.
     * @param valName The application attribute number.
     * @return The value, null if not found.
     * @throws AoException Error getting value.
     */
    public TS_Value getInstanceValue(long aid, int attrNo, long iid) throws AoException {
        ApplicationAttribute aa = getApplicationAttribute(aid, attrNo);

        // datatype
        boolean lcValuesAttr = isLocalColumnValuesAttribute(aid, attrNo);
        boolean lcFlagsAttr = isLocalColumnFlagsAttribute(aid, attrNo);
        DataType dt = lcValuesAttr ? getDataTypeForLocalColumnValues(iid) : aa.getDataType();

        // read generation parameters from values if null
        if (isLocalColumnGenParamsAttribute(aid, attrNo)) {
            int seqRepAttrNo = getAttrNoByBaName(aid, "sequence_representation");
            int seqRep = getInstanceValue(aid, seqRepAttrNo, iid).u.enumVal();
            // implicit_constant=1,implicit_linear=2,implicit_saw=3,formula=4
            if (seqRep == 1 || seqRep == 2 || seqRep == 3 || seqRep == 4) {
                int genParamsAttrNo = getAttrNoByBaName(aid, "generation_parameters");
                if (this.instanceValueMap.get(aid).get(iid).get(genParamsAttrNo) == null) {
                    int valuesAttrNo = getAttrNoByBaName(aid, "values");
                    TS_Value val = getInstanceValue(aid, valuesAttrNo, iid);
                    // generation parameters for 'implicit_constant' may be datatype DT_STRING
                    if (val.u.discriminator() == DataType.DS_STRING) {
                        return ODSHelper.createEmptyTS_Value(DataType.DS_DOUBLE);
                    }
                    return ODSHelper.convertTsValue(getInstanceValue(aid, valuesAttrNo, iid), DataType.DS_DOUBLE);
                }
            }
        }

        // read values from external component file
        if (lcValuesAttr) {
            int seqRepAttrNo = getAttrNoByBaName(aid, "sequence_representation");
            int seqRep = getInstanceValue(aid, seqRepAttrNo, iid).u.enumVal();
            // external_component=7,raw_linear_external=8,raw_polynomial_external=9,raw_linear_calibrated_external=11
            if (seqRep == 7 || seqRep == 8 || seqRep == 9 || seqRep == 11) {
                return ExtCompReader.getInstance().readValues(this, iid, dt);
            }
        }
        // read flags from external component file
        if (lcFlagsAttr) {
            TS_Value flags = ExtCompReader.getInstance().readFlags(this, iid);
            if (flags != null) {
                return flags;
            }
        }

        // read values from memory
        java.lang.Object jValue = this.instanceValueMap.get(aid).get(iid).get(attrNo);
        return (jValue == null) ? ODSHelper.createEmptyTS_Value(dt) : ODSHelper.jObject2tsValue(dt, jValue);
    }

    /**
     * Returns all values of an instance attribute of a given list of instances.
     * 
     * @param aid the application element id
     * @param attrNo the attribute number
     * @param iids the array of instance ids
     * @return TS_ValueSeq containing all values, or null, if one or more of the instances do not possess the instance
     *         attribute.
     * @throws AoException if the conversion from tsValue array to tsValueSeq fails
     */
    public TS_ValueSeq getInstanceValues(long aid, int attrNo, Collection<Long> iids) throws AoException {
        ApplicationAttribute aa = getApplicationAttribute(aid, attrNo);

        // datatype
        boolean lcValuesAttr = isLocalColumnValuesAttribute(aid, attrNo);
        DataType dt = aa.getDataType();

        List<TS_Value> list = new ArrayList<TS_Value>();
        for (long iid : iids) {
            dt = lcValuesAttr ? getDataTypeForLocalColumnValues(iid) : aa.getDataType();
            list.add(getInstanceValue(aid, attrNo, iid));
        }

        return ODSHelper.tsValue2tsValueSeq(list.toArray(new TS_Value[0]), dt);
    }

    /**
     * Returns the unit name for an application attribute.
     * 
     * @param aid The application element id.
     * @param attrNo The attribute number
     * @return The unit name.
     * @throws AoException
     */
    public String getUnitNameForAttr(long aid, int attrNo) throws AoException {
        ApplicationAttribute aa = getApplicationAttribute(aid, attrNo);

        // unit instance id
        long unitIid = ODSHelper.asJLong(aa.getUnit());
        if (unitIid < 1) {
            return "";
        }

        // unit aid
        Set<Long> untAids = getAidsByBaseType("aounit");
        if (untAids == null || untAids.isEmpty()) {
            return "";
        }
        long unitAid = untAids.iterator().next();

        // unit attribute index number
        Integer unitNameAttrNo = getAttrNoByBaName(unitAid, "name");
        if (unitNameAttrNo == null) {
            return "";
        }

        String unitNameObj = (String) this.instanceValueMap.get(unitAid).get(unitIid).get(unitNameAttrNo);
        return (unitNameObj == null) ? "" : unitNameObj;
    }

    /**
     * Checks whether given attribute name is from base attribute 'values' of and this instance is from base element
     * 'AoLocalColumn'.
     * 
     * @param aid The application element id.
     * @param attrNo The application attribute number.
     * @return True, if attribute is 'values'.
     */
    private boolean isLocalColumnValuesAttribute(long aid, long attrNo) {
        Set<Long> localColumnAids = getAidsByBaseType("aolocalcolumn");
        if (localColumnAids != null && localColumnAids.contains(aid)) {
            Integer valuesAttrNo = getAttrNoByBaName(aid, "values");
            return (valuesAttrNo != null) && (attrNo == valuesAttrNo);
        }
        return false;
    }

    /**
     * Checks whether given attribute name is from base attribute 'flags' of and this instance is from base element
     * 'AoLocalColumn'.
     * 
     * @param aid The application element id.
     * @param attrNo The application attribute number.
     * @return True, if attribute is 'flags'.
     */
    private boolean isLocalColumnFlagsAttribute(long aid, long attrNo) {
        Set<Long> localColumnAids = getAidsByBaseType("aolocalcolumn");
        if (localColumnAids != null && localColumnAids.contains(aid)) {
            Integer valuesAttrNo = getAttrNoByBaName(aid, "flags");
            return (valuesAttrNo != null) && (attrNo == valuesAttrNo);
        }
        return false;
    }

    /**
     * Checks whether given attribute name is from base attribute 'sequence_representation' of and this instance is from
     * base element 'AoLocalColumn'.
     * 
     * @param aid The application element id.
     * @param attrNo The application attribute number.
     * @return True, if attribute is 'sequence_representation'.
     */
    private boolean isLocalColumnGenParamsAttribute(long aid, long attrNo) {
        Set<Long> localColumnAids = getAidsByBaseType("aolocalcolumn");
        if (localColumnAids != null && localColumnAids.contains(aid)) {
            Integer genParamsAttrNo = getAttrNoByBaName(aid, "generation_parameters");
            return (genParamsAttrNo != null) && (attrNo == genParamsAttrNo);
        }
        return false;
    }

    /**
     * Returns the instance of the related measurement quantity for a local column instance.
     * 
     * @param lcIid The local column instance id.
     * @return The measurement quantity instance id.
     * @throws AoException Error getting instance.
     */
    private long getMeaQuantityInstance(long lcIid) throws AoException {
        long lcAid = getAidsByBaseType("aolocalcolumn").iterator().next();
        ApplicationRelation relMeaQua = getApplicationRelationByBaseName(lcAid, "measurement_quantity");
        Collection<Long> set = getRelatedInstanceIds(lcAid, lcIid, relMeaQua);
        if (set.size() != 1) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "None or multiple related instances found for base relation 'measurement_quantity' for local column with iid="
                                          + lcIid);
        }
        return set.iterator().next();
    }

    /**
     * In case this InstanceElement is from the ApplicationElement derived from "AoLocalColumn", the datatype of the
     * related "AoMeasurementQuantity" instance is returned.
     * 
     * @throws AoException Error getting datatype.
     */
    private DataType getDataTypeForLocalColumnValues(long lcIid) throws AoException {
        long meaQuaAid = getAidsByBaseType("aomeasurementquantity").iterator().next();
        long meaQuaIid = getMeaQuantityInstance(lcIid);
        int attrNo = getAttrNoByBaName(meaQuaAid, "datatype");
        TS_Value dtValue = getInstanceValue(meaQuaAid, attrNo, meaQuaIid);
        if (dtValue != null && dtValue.flag == 15 && dtValue.u.discriminator() == DataType.DT_ENUM) {
            int val = dtValue.u.enumVal();
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
        throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                              "Implementation problem at method 'getDataTypeForLocalColumnValues()' for iid=" + lcIid);
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

        // add relation, if none or multiple cardinality, overwrite
        Set<Long> relInstIds = this.instanceRelMap.get(aid).get(iid).get(applRel);
        if ((relInstIds.size() > 0) && (applRel.getRelationRange().max != -1)) {
            relInstIds.clear();
        }
        relInstIds.addAll(otherIids);

        // add inverse relation
        ApplicationRelation invApplRel = getInverseRelation(applRel);
        ApplicationElement elem1 = invApplRel.getElem1();
        if (elem1 == null) {
            throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "Elem1 not set for relation: "
                    + invApplRel.getRelationName());
        }

        long otherAid = ODSHelper.asJLong(invApplRel.getElem1().getId());
        for (Long otherIid : otherIids) {
            Set<Long> invRelInstIds = this.instanceRelMap.get(otherAid).get(otherIid).get(invApplRel);
            if ((invRelInstIds.size() > 0) && (invApplRel.getRelationRange().max != -1)) {
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
    public List<Long> getRelatedInstanceIds(long aid, long iid, ApplicationRelation applRel) throws AoException {
        return new ArrayList<Long>(this.instanceRelMap.get(aid).get(iid).get(applRel));
    }

    public synchronized InstanceElementIterator newInstanceElementIterator(POA instancePOA, InstanceElement[] instances)
            throws AoException {
        nextInstanceIteratorId++;
        this.instanceIteratorElementCache.put(nextInstanceIteratorId, instances);
        this.instanceIteratorPointerCache.put(nextInstanceIteratorId, 0);

        byte[] oid = toByta(new long[] { 3, nextInstanceIteratorId, 0 }); // 3=InstanceElementIterator
        org.omg.CORBA.Object obj = instancePOA.create_reference_with_id(oid, InstanceElementIteratorHelper.id());
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

}
