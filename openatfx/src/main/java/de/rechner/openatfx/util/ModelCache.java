package de.rechner.openatfx.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;


public class ModelCache {

    private final ApplicationStructureValue asv;
    private final EnumerationStructure[] esAr;

    /** application structure */
    private final Map<Long, ApplElem> aid2applElemMap; // <aid, ApplElem>
    private final Map<String, ApplElem> aeName2ApplElemMap; // <aeName, ApplElem>
    private final Map<Long, Map<String, ApplAttr>> applAttrs; // <aid, <aaName, aa>>
    private final Map<Long, String[]> applAttrNames; // <aid, attrNames>
    private final Map<Long, Map<String, ApplAttr>> applAttrsByBaseName; // <aid, <baName, aa>>
    private final Map<Long, Map<String, ApplRel>> applRels; // <aid, <relName, rel>>

    /** enumerations */
    private final Map<Long, Map<String, String>> applAttrEnums; // <aid, <aaName, aa>>
    private final Map<String, Map<String, Integer>> enumIndices; // <enumName, <enumItem, enumIndex>>
    private final Map<String, Map<Integer, String>> enumItems; // <enumName, <enumIndex, enumItem>>

    /** information where the mass data is stored in the model */
    private String lcAeName;
    private String lcValuesAaName;

    /**
     * @param asv
     * @param easAr
     * @param esAr
     */
    public ModelCache(ApplicationStructureValue asv, EnumerationAttributeStructure[] easAr, EnumerationStructure[] esAr) {
        this.asv = asv;
        this.esAr = esAr;
        this.aid2applElemMap = new HashMap<Long, ApplElem>();
        this.aeName2ApplElemMap = new HashMap<String, ApplElem>();
        this.applAttrs = new HashMap<Long, Map<String, ApplAttr>>();
        this.applAttrNames = new HashMap<Long, String[]>();
        this.applAttrsByBaseName = new HashMap<Long, Map<String, ApplAttr>>();
        this.applRels = new HashMap<Long, Map<String, ApplRel>>();
        this.applAttrEnums = new HashMap<Long, Map<String, String>>();
        this.enumIndices = new HashMap<String, Map<String, Integer>>();
        this.enumItems = new HashMap<String, Map<Integer, String>>();

        Map<Long, String> aid2AeNameMap = new HashMap<Long, String>();
        for (ApplElem applElem : asv.applElems) {
            this.aid2applElemMap.put(ODSHelper.asJLong(applElem.aid), applElem);
            this.aeName2ApplElemMap.put(applElem.aeName, applElem);

            // lcAeName
            if (applElem.beName.equalsIgnoreCase("AoLocalColumn")) {
                this.lcAeName = applElem.aeName;
            }

            aid2AeNameMap.put(ODSHelper.asJLong(applElem.aid), applElem.aeName);

            // attributes
            Map<String, ApplAttr> attrMap = new HashMap<String, ApplAttr>();
            Map<String, ApplAttr> attrByBaseNameMap = new HashMap<String, ApplAttr>();
            Collection<String> attrNames = new ArrayList<String>(applElem.attributes.length);
            for (ApplAttr applAttr : applElem.attributes) {
                attrMap.put(applAttr.aaName, applAttr);
                attrByBaseNameMap.put(applAttr.baName, applAttr);
                attrNames.add(applAttr.aaName);

                // lcAeName
                if (applElem.beName.equalsIgnoreCase("AoLocalColumn") && applAttr.baName.equals("values")) {
                    this.lcValuesAaName = applAttr.aaName;
                }

            }
            this.applAttrs.put(ODSHelper.asJLong(applElem.aid), attrMap);
            this.applAttrsByBaseName.put(ODSHelper.asJLong(applElem.aid), attrByBaseNameMap);
            this.applAttrNames.put(ODSHelper.asJLong(applElem.aid), attrNames.toArray(new String[0]));
        }

        // relations
        for (ApplRel applRel : asv.applRels) {
            Long aidElem1 = ODSHelper.asJLong(applRel.elem1);
            Map<String, ApplRel> map = this.applRels.get(aidElem1);
            if (map == null) {
                map = new HashMap<String, ApplRel>();
                this.applRels.put(aidElem1, map);
            }
            map.put(applRel.arName, applRel);
        }

        // enumerations
        for (EnumerationAttributeStructure eas : easAr) {
            Long aid = ODSHelper.asJLong(eas.aid);
            Map<String, String> m = this.applAttrEnums.get(aid);
            if (m == null) {
                m = new HashMap<String, String>();
                this.applAttrEnums.put(aid, m);
            }
            m.put(eas.aaName, eas.enumName);
        }

        for (EnumerationStructure es : esAr) {
            Map<String, Integer> mIndices = this.enumIndices.get(es.enumName);
            if (mIndices == null) {
                mIndices = new HashMap<String, Integer>();
                this.enumIndices.put(es.enumName, mIndices);
            }
            Map<Integer, String> mItems = this.enumItems.get(es.enumName);
            if (mItems == null) {
                mItems = new HashMap<Integer, String>();
                this.enumItems.put(es.enumName, mItems);
            }
            for (EnumerationItemStructure eis : es.items) {
                mIndices.put(eis.itemName, eis.index);
                mItems.put(eis.index, eis.itemName);
            }
        }
    }

    public ApplicationStructureValue getApplicationStructureValue() throws AoException {
        return this.asv;
    }

    public EnumerationStructure getEnumerationStructure(String enumName) throws AoException {
        for (EnumerationStructure es : this.esAr) {
            if (es.enumName.equals(enumName)) {
                return es;
            }
        }
        return null;
    }

    public EnumerationStructure[] getEnumerationStructure() throws AoException {
        return this.esAr;
    }

    public Collection<ApplElem> getApplElems() throws AoException {
        return Arrays.asList(this.asv.applElems);
    }

    public ApplElem getApplElem(long aid) throws AoException {
        return this.aid2applElemMap.get(aid);
    }

    public ApplElem getApplElem(String aeName) throws AoException {
        return this.aeName2ApplElemMap.get(aeName);
    }

    public Collection<ApplAttr> getApplAttrs(Long aid) throws AoException {
        return this.applAttrs.get(aid).values();
    }

    public String[] getApplAttrNames(Long aid) throws AoException {
        return this.applAttrNames.get(aid);
    }

    public ApplAttr getApplAttr(Long aid, String aaName) throws AoException {
        return this.applAttrs.get(aid).get(aaName);
    }

    public ApplAttr getApplAttrByBaseName(Long aid, String baName) {
        return this.applAttrsByBaseName.get(aid).get(baName);
    }

    public Collection<ApplRel> getApplRels(Long aid) throws AoException {
        Map<String, ApplRel> map = this.applRels.get(aid);
        if (map == null) {
            map = Collections.emptyMap();
        }
        return map.values();
    }

    public ApplRel getApplRel(Long aid, String relName) throws AoException {
        return this.applRels.get(aid).get(relName);
    }

    public String getEnumName(Long aid, String aaName) throws AoException {
        return this.applAttrEnums.get(aid).get(aaName);
    }

    public int getEnumIndex(String enumName, String enumItem) throws AoException {
        return this.enumIndices.get(enumName).get(enumItem);
    }

    public String getEnumItem(String enumName, int enumIndex) throws AoException {
        return this.enumItems.get(enumName).get(enumIndex);
    }

    public String getLcAeName() throws AoException {
        return this.lcAeName;
    }

    public String getLcValuesAaName() throws AoException {
        return this.lcValuesAaName;
    }

    /**
     * Returns whether given attribute name if the attribute 'values' of the local column instance.
     * 
     * @param aeName The application element name.
     * @param aaName The application attribute name.
     * @return True, if 'values' attribute.
     * @throws AoException Error checking attribute.
     */
    public boolean isLocalColumnValuesAttr(String aeName, String aaName) throws AoException {
        if (this.lcAeName != null && this.lcValuesAaName != null && this.lcAeName.equals(aeName)
                && this.lcValuesAaName.equals(aaName)) {
            return true;
        }
        return false;
    }

}
