package de.rechner.openatfx.util;

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

    /** application structure */
    private final Map<String, ApplElem> applElems;
    private final Map<Long, Map<String, ApplAttr>> applAttrs; // aid, aaName, aa
    private final Map<Long, Map<String, ApplRel>> applRels; // aid, relName, rel

    /** enumerations */
    private final Map<Long, Map<String, String>> applAttrEnums; // aid, aaName, aa
    private final Map<String, Map<String, Integer>> enumItems; // enumName, enumItem, idx

    /** information where the mass data is stored in the model */
    private String lcAeName;
    private String lcValuesAaName;

    public ModelCache(ApplicationStructureValue asv, EnumerationAttributeStructure[] easAr, EnumerationStructure[] esAr) {
        this.applElems = new HashMap<String, ApplElem>();
        this.applAttrs = new HashMap<Long, Map<String, ApplAttr>>();
        this.applRels = new HashMap<Long, Map<String, ApplRel>>();
        this.applAttrEnums = new HashMap<Long, Map<String, String>>();
        this.enumItems = new HashMap<String, Map<String, Integer>>();

        Map<Long, String> aid2AeNameMap = new HashMap<Long, String>();
        for (ApplElem applElem : asv.applElems) {
            this.applElems.put(applElem.aeName, applElem);

            // lcAeName
            if (applElem.beName.equalsIgnoreCase("AoLocalColumn")) {
                this.lcAeName = applElem.aeName;
            }

            aid2AeNameMap.put(ODSHelper.asJLong(applElem.aid), applElem.aeName);

            // attributes
            Map<String, ApplAttr> attrMap = new HashMap<String, ApplAttr>();
            for (ApplAttr applAttr : applElem.attributes) {
                attrMap.put(applAttr.aaName, applAttr);

                // lcAeName
                if (applElem.beName.equalsIgnoreCase("AoLocalColumn") && applAttr.baName.equals("values")) {
                    this.lcValuesAaName = applAttr.aaName;
                }

            }
            this.applAttrs.put(ODSHelper.asJLong(applElem.aid), attrMap);
        }

        // relations
        for (ApplRel applRel : asv.applRels) {
            Long elem1 = ODSHelper.asJLong(applRel.elem1);
            Map<String, ApplRel> map = this.applRels.get(elem1);
            if (map == null) {
                map = new HashMap<String, ApplRel>();
                this.applRels.put(elem1, map);
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
            Map<String, Integer> m = this.enumItems.get(es.enumName);
            if (m == null) {
                m = new HashMap<String, Integer>();
                this.enumItems.put(es.enumName, m);
            }
            for (EnumerationItemStructure eis : es.items) {
                m.put(eis.itemName, eis.index);
            }
        }

    }

    public ApplElem getApplElem(String aeName) {
        return this.applElems.get(aeName);
    }

    public ApplAttr getApplAttr(Long aid, String aaName) {
        return this.applAttrs.get(aid).get(aaName);
    }

    public ApplRel getApplRel(Long aid, String relName) {
        return this.applRels.get(aid).get(relName);
    }

    public String getEnumName(Long aid, String aaName) {
        return this.applAttrEnums.get(aid).get(aaName);
    }

    public int getEnumItem(String enumName, String enumItem) {
        return this.enumItems.get(enumName).get(enumItem);
    }

    public String getLcAeName() {
        return this.lcAeName;
    }

    public String getLcValuesAaName() {
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
