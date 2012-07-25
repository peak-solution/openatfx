package de.rechner.openatfx.exporter;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AggrFunc;
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
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelOrder;
import org.asam.ods.SelValueExt;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ModelCache;
import de.rechner.openatfx.util.ODSHelper;


public class ExporterImpl implements IExporter {

    private static final Log LOG = LogFactory.getLog(ExporterImpl.class);

    private Set<String>      includeBeNames;

    public ExporterImpl() {
        this.includeBeNames = new HashSet<String>();
        this.includeBeNames.add("aoenvironment");
        this.includeBeNames.add("aotest");
        this.includeBeNames.add("aosubtest");
        this.includeBeNames.add("aomeasurement");
        this.includeBeNames.add("aomeasurementquantity");
        this.includeBeNames.add("aosubmatrix");
        this.includeBeNames.add("aolocalcolumn");
        this.includeBeNames.add("aoexternalcomponent");
        this.includeBeNames.add("aophysicaldimension");
        this.includeBeNames.add("aounit");
        this.includeBeNames.add("aoquantity");
        this.includeBeNames.add("aoparameterset");
        this.includeBeNames.add("aoparameter");
        this.includeBeNames.add("aounitundertest");
        this.includeBeNames.add("aounitundertestpart");
        this.includeBeNames.add("aotestsequence");
        this.includeBeNames.add("aotestsequencepart");
        this.includeBeNames.add("aotestequipment");
        this.includeBeNames.add("aotestequipmentpart");
    }

    /**
     * 
     */
    public void export(AoSession sourceSession, ElemId[] sourceElemIds, File targetFile, Properties props)
            throws AoException {
        long start = System.currentTimeMillis();

        // configure ORB
        ORB orb = ORB.init(new String[0], System.getProperties());

        // create target session
        String bsVersion = sourceSession.getBaseStructure().getVersion();
        AoSession targetSession = AoServiceFactory.getInstance().newEmptyAoSession(orb, targetFile, bsVersion);

        // create source model cache
        ModelCache smc = new ModelCache(sourceSession.getApplicationStructureValue(),
                                        sourceSession.getEnumerationAttributes(),
                                        sourceSession.getEnumerationStructure());

        try {
            targetSession.startTransaction();

            // export application model
            Map<Long, Long> source2TargetAidMap = exportApplicationElements(smc, targetSession);
            exportApplicationRelations(smc, targetSession, source2TargetAidMap);

            // export instances
            ApplElemAccess sourceAea = sourceSession.getApplElemAccess();
            ApplElemAccess targetAea = targetSession.getApplElemAccess();

            Map<ElemIdMap, ElemIdMap> source2TargetElemIdMap = new HashMap<ElemIdMap, ElemIdMap>();
            for (ElemId sourceElemId : sourceElemIds) {
                exportInstances(sourceAea, smc, targetAea, sourceElemId.aid, new T_LONGLONG[] { sourceElemId.iid },
                                source2TargetAidMap, source2TargetElemIdMap, true);
            }

            targetSession.commitTransaction();

            LOG.info("Exported " + source2TargetElemIdMap.size() + " instances in "
                    + (System.currentTimeMillis() - start) + " ms");
        } catch (AoException aoe) {
            LOG.error(aoe.reason, aoe);
            targetSession.abortTransaction();
        }
    }

    /**
     * @param sourceAea
     * @param smc
     * @param targetAea
     * @param sourceAid
     * @param sourceIids
     * @param source2TargetAidMap
     * @param source2TargetElemIdMap
     * @param exportChildren
     * @throws AoException
     */
    private void exportInstances(ApplElemAccess sourceAea, ModelCache smc, ApplElemAccess targetAea,
            T_LONGLONG sourceAid, T_LONGLONG[] sourceIids, Map<Long, Long> source2TargetAidMap,
            Map<ElemIdMap, ElemIdMap> source2TargetElemIdMap, boolean exportChildren) throws AoException {
        // filter already exported instances
        List<T_LONGLONG> sourceIidList = new ArrayList<T_LONGLONG>();
        for (T_LONGLONG sourceIid : sourceIids) {
            if (!source2TargetElemIdMap.containsKey(new ElemIdMap(sourceAid, sourceIid))) {
                sourceIidList.add(sourceIid);
            }
        }
        sourceIids = sourceIidList.toArray(new T_LONGLONG[0]);

        // nothing to do?
        if (sourceIids.length < 1) {
            return;
        }

        // export instance value
        Map<ElemIdMap, ElemIdMap> exportedElemIds = exportInstanceValues(smc, sourceAea, sourceAid, sourceIids,
                                                                         targetAea, source2TargetAidMap);
        source2TargetElemIdMap.putAll(exportedElemIds);

        // export relations
        for (ElemIdMap sourceElemIdMap : exportedElemIds.keySet()) {

            // follow relations
            for (ApplRel sourceApplRel : smc.getApplRels(sourceElemIdMap.getAid())) {

                // do not export excluded target application element
                if (!shouldExportApplRel(smc, sourceApplRel)) {
                    continue;
                }

                // do not export external component instances
                if (sourceApplRel.brName.equals("external_component")) {
                    continue;
                }

                // do not follow the relation from 'AoSubMatrix' to 'AoLocalColumn' to avoid exporting ALL LocalColumns
                if (sourceApplRel.brName.equals("local_columns")
                        && smc.getApplElem(ODSHelper.asJLong(sourceApplRel.elem2)).aeName.equalsIgnoreCase("AoLocalColumn")) {
                    continue;
                }

                // do not export children, if not configured
                boolean isFatherRelation = (sourceApplRel.arRelationType == RelationType.FATHER_CHILD && sourceApplRel.arRelationRange.max == 1);
                boolean isChildRelation = (sourceApplRel.arRelationType == RelationType.FATHER_CHILD)
                        && (sourceApplRel.arRelationRange.max == -1);
                if (!exportChildren && isChildRelation) {
                    continue;
                }
                // do not follow 1..n relations, except 'children' and '
                if ((sourceApplRel.arRelationRange.max == -1) && (!isChildRelation)
                        && !sourceApplRel.brName.equalsIgnoreCase("local_columns")) {
                    continue;
                }

                // copy relations
                T_LONGLONG[] relSourceIids = sourceAea.getRelInst(sourceElemIdMap.getElemId(), sourceApplRel.arName);
                if (relSourceIids.length < 1) { // no related instances found
                    continue;
                }

                exportInstances(sourceAea, smc, targetAea, sourceApplRel.elem2, relSourceIids, source2TargetAidMap,
                                source2TargetElemIdMap, !isFatherRelation);

                // T_LONGLONG relTargetAid =
                // ODSHelper.asODSLongLong(source2TargetAidMap.get(ODSHelper.asJLong(sourceApplRel.elem2)));
                T_LONGLONG[] relTargetIids = new T_LONGLONG[relSourceIids.length];
                for (int i = 0; i < relTargetIids.length; i++) {
                    ElemIdMap targetElemIdMap = source2TargetElemIdMap.get(new ElemIdMap(sourceApplRel.elem2,
                                                                                         relSourceIids[i]));
                    relTargetIids[i] = ODSHelper.asODSLongLong(targetElemIdMap.getIid());
                }

                ElemIdMap targetElemIdMap = source2TargetElemIdMap.get(sourceElemIdMap);
                targetAea.setRelInst(targetElemIdMap.getElemId(), sourceApplRel.arName, relTargetIids, SetType.INSERT);
            }

        }
    }

    /**
     * Exports all instance values from the source to the target.
     * 
     * @param smc The source model cache.
     * @param sourceAea The source ApplElemAccess.
     * @param sourceAid The source application element id.
     * @param sourceIids The source instance id´s.
     * @param targetAea The target ApplElemAccess.
     * @param source2TargetAidMap The map from source to target.
     * @return Map of exported instances (key=source,value=target).
     * @throws AoException Error exporting instances.
     */
    private Map<ElemIdMap, ElemIdMap> exportInstanceValues(ModelCache smc, ApplElemAccess sourceAea,
            T_LONGLONG sourceAid, T_LONGLONG[] sourceIids, ApplElemAccess targetAea, Map<Long, Long> source2TargetAidMap)
            throws AoException {
        if (sourceIids.length < 1) {
            return Collections.emptyMap();
        }

        String[] copyableAttrNames = getCopyableAttrNames(smc, ODSHelper.asJLong(sourceAid));

        // ********************************************************************
        // query source
        // ********************************************************************

        QueryStructureExt qse = new QueryStructureExt();

        // anuSeq (select all attributes)
        qse.anuSeq = new SelAIDNameUnitId[copyableAttrNames.length];
        for (int i = 0; i < copyableAttrNames.length; i++) {
            qse.anuSeq[i] = new SelAIDNameUnitId();
            qse.anuSeq[i].attr = new AIDName();
            qse.anuSeq[i].attr.aid = sourceAid;
            qse.anuSeq[i].attr.aaName = copyableAttrNames[i];
            qse.anuSeq[i].unitId = new T_LONGLONG(0, 0);
            qse.anuSeq[i].aggregate = AggrFunc.NONE;
        }

        // condSeqs (filter by instance iids)
        SelValueExt sve = new SelValueExt();
        sve.attr = new AIDNameUnitId();
        sve.attr.attr = new AIDName();
        sve.attr.attr.aid = sourceAid;
        sve.attr.attr.aaName = smc.getApplAttrByBaseName(ODSHelper.asJLong(sourceAid), "id").aaName;
        sve.attr.unitId = new T_LONGLONG(0, 0);
        sve.oper = SelOpcode.INSET;
        sve.value = new TS_Value();
        sve.value.flag = (short) 15;
        sve.value.u = new TS_Union();
        sve.value.u.longlongSeq(sourceIids);
        qse.condSeq = new SelItem[1];
        qse.condSeq[0] = new SelItem();
        qse.condSeq[0].value(sve);

        qse.joinSeq = new JoinDef[0];
        qse.groupBy = new AIDName[0];
        qse.orderBy = new SelOrder[0];

        // execute query in source
        ResultSetExt[] rse = sourceAea.getInstancesExt(qse, 0);

        // ********************************************************************
        // insert to target
        // ********************************************************************

        List<AIDNameValueSeqUnitId> anvsuiList = new ArrayList<AIDNameValueSeqUnitId>();
        for (ElemResultSetExt erse : rse[0].firstElems) {
            for (NameValueSeqUnitId nvsu : erse.values) {
                AIDNameValueSeqUnitId anvsui = new AIDNameValueSeqUnitId();
                anvsui.attr = new AIDName();
                anvsui.attr.aid = ODSHelper.asODSLongLong(source2TargetAidMap.get(ODSHelper.asJLong(sourceAid)));
                anvsui.attr.aaName = nvsu.valName;
                anvsui.unitId = new T_LONGLONG(0, 0);
                anvsui.values = nvsu.value;
                anvsuiList.add(anvsui);
            }
        }

        ElemId[] targetElemIds = targetAea.insertInstances(anvsuiList.toArray(new AIDNameValueSeqUnitId[0]));
        Map<ElemIdMap, ElemIdMap> map = new LinkedHashMap<ExporterImpl.ElemIdMap, ExporterImpl.ElemIdMap>();
        for (int i = 0; i < targetElemIds.length; i++) {
            ElemIdMap sourceElemIdMap = new ElemIdMap(sourceAid, sourceIids[i]);
            ElemIdMap targetElemIdMap = new ElemIdMap(targetElemIds[i]);
            map.put(sourceElemIdMap, targetElemIdMap);
        }

        return map;
    }

    /*********************************************************************************************************
     * Methods for exporting the application model
     *********************************************************************************************************/

    /**
     * Writes the application elements to the target session.
     * 
     * @param smc
     * @param targetSession
     * @throws AoException
     */
    private Map<Long, Long> exportApplicationElements(ModelCache smc, AoSession targetSession) throws AoException {
        BaseStructure targetBs = targetSession.getBaseStructure();
        ApplicationStructure targetAs = targetSession.getApplicationStructure();
        Map<Long, Long> source2TargetAidMap = new HashMap<Long, Long>();

        // build map with base enums
        Map<String, EnumerationDefinition> exportedEnums = new HashMap<String, EnumerationDefinition>();
        for (String enumName : targetAs.listEnumerations()) {
            exportedEnums.put(enumName, targetAs.getEnumerationDefinition(enumName));
        }

        // iterate over application model
        for (ApplElem applElem : smc.getApplElems()) {

            // is base element included?
            if (!this.includeBeNames.contains(applElem.beName.toLowerCase())) {
                continue;
            }

            // cache base attributes and base relations
            BaseElement targetBe = targetBs.getElementByType(applElem.beName);
            Map<String, BaseAttribute> baseAttrMap = new HashMap<String, BaseAttribute>();
            Map<String, BaseRelation> baseRelMap = new HashMap<String, BaseRelation>();
            for (BaseAttribute baseAttr : targetBe.getAttributes("*")) {
                baseAttrMap.put(baseAttr.getName().toLowerCase(), baseAttr);
            }
            for (BaseRelation baseRel : targetBe.getAllRelations()) {
                baseRelMap.put(baseRel.getRelationName().toLowerCase(), baseRel);
            }

            // create application element
            ApplicationElement targetAe = targetAs.createElement(targetBe);
            targetAe.setName(applElem.aeName);
            source2TargetAidMap.put(ODSHelper.asJLong(applElem.aid), ODSHelper.asJLong(targetAe.getId()));

            // create application attributes
            long sourceAid = ODSHelper.asJLong(applElem.aid);
            for (ApplAttr applAttr : smc.getApplAttrs(sourceAid)) {
                BaseAttribute targetBa = baseAttrMap.get(applAttr.baName);
                ApplicationAttribute targetAa;
                if (targetBa != null && targetBa.isObligatory()) { // base is obligatory, aa is existing
                    targetAa = targetAe.getAttributeByBaseName(applAttr.baName);
                } else {// base not obligatory, aa not existing
                    targetAa = targetAe.createAttribute();
                }

                // name, base attribute, datatype
                targetAa.setName(applAttr.aaName);
                targetAa.setBaseAttribute(targetBa);
                targetAa.setDataType(applAttr.dType);
                targetAa.setIsObligatory(applAttr.isObligatory);
                targetAa.setLength(applAttr.length);

                // unique
                if (targetBa == null) {
                    targetAa.setIsUnique(applAttr.isUnique);
                }

                // enumeration
                if (((applAttr.dType == DataType.DT_ENUM) || (applAttr.dType == DataType.DS_ENUM))
                        && (targetBa == null)) {
                    String enumName = smc.getEnumName(sourceAid, applAttr.aaName);
                    EnumerationDefinition targetEnumDef = exportedEnums.get(enumName);
                    if (targetEnumDef == null) {
                        targetEnumDef = exportEnumerationDefinition(smc.getEnumerationStructure(enumName), targetAs);
                        exportedEnums.put(enumName, targetEnumDef);
                    }
                    targetAa.setEnumerationDefinition(targetEnumDef);
                }
            }
        }

        return source2TargetAidMap;
    }

    /**
     * Exports all application relations
     * 
     * @param smc
     * @param targetSession
     * @param source2TargetAidMap
     * @throws AoException
     */
    private void exportApplicationRelations(ModelCache smc, AoSession targetSession, Map<Long, Long> source2TargetAidMap)
            throws AoException {
        ApplicationStructure targetAs = targetSession.getApplicationStructure();

        // remember already exported relations because inverse relations are implicit created
        Set<String> exportedRels = new HashSet<String>();
        for (ApplRel applRel : smc.getApplicationStructureValue().applRels) {

            // is base element included?
            ApplElem elem1 = smc.getApplElem(ODSHelper.asJLong(applRel.elem1));
            ApplElem elem2 = smc.getApplElem(ODSHelper.asJLong(applRel.elem2));
            if (!this.includeBeNames.contains(elem1.beName.toLowerCase())
                    || !this.includeBeNames.contains(elem2.beName.toLowerCase())) {
                continue;
            }

            // is inverse relation?
            String relKey = elem1.aeName + "_" + elem2.aeName + "_" + applRel.arName;
            String invRelKey = elem2.aeName + "_" + elem1.aeName + "_" + applRel.invName;
            if (exportedRels.contains(invRelKey)) {
                continue;
            }

            Long targetElem1Aid = source2TargetAidMap.get(ODSHelper.asJLong(applRel.elem1));
            Long targetElem2Aid = source2TargetAidMap.get(ODSHelper.asJLong(applRel.elem2));
            ApplicationElement targetElem1 = targetAs.getElementById(ODSHelper.asODSLongLong(targetElem1Aid));
            ApplicationElement targetElem2 = targetAs.getElementById(ODSHelper.asODSLongLong(targetElem2Aid));
            BaseRelation targetBr = null;
            if (applRel.brName.length() > 0) {
                targetBr = lookupBaseRelation(targetElem1, targetElem1, applRel.brName, targetElem2.getBaseElement()
                                                                                                   .getType());
            }

            // create relation
            ApplicationRelation targetRel = targetAs.createRelation();
            targetRel.setBaseRelation(targetBr);
            targetRel.setElem1(targetElem1);
            targetRel.setElem2(targetElem2);
            targetRel.setRelationName(applRel.arName);
            targetRel.setInverseRelationName(applRel.invName);
            targetRel.setRelationRange(new RelationRange(applRel.arRelationRange.min, applRel.arRelationRange.max));
            targetRel.setInverseRelationRange(new RelationRange(applRel.invRelationRange.min,
                                                                applRel.invRelationRange.max));

            exportedRels.add(relKey);
        }
    }

    private static BaseRelation lookupBaseRelation(ApplicationElement elem1, ApplicationElement elem2, String bRelName,
            String elem2BaseType) throws AoException {
        for (BaseRelation baseRel : elem1.getBaseElement().getAllRelations()) {
            if (baseRel.getRelationName().equals(bRelName) && baseRel.getElem2().getType().equals(elem2BaseType)) {
                return baseRel;
            }
        }
        throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "BaseRelation not found for name='"
                + bRelName + "',targetBaseType='" + elem2BaseType + "'");
    }

    /**
     * Exports an enumeration definition.
     * 
     * @param sourceEs The source enumeration structure.
     * @param targetAs The target application structure.
     * @return The created target enumeration definition.
     * @throws AoException Error creating enumeration definition.
     */
    private static EnumerationDefinition exportEnumerationDefinition(EnumerationStructure sourceEs,
            ApplicationStructure targetAs) throws AoException {
        EnumerationDefinition enumDef = targetAs.createEnumerationDefinition(sourceEs.enumName);
        for (EnumerationItemStructure eis : sourceEs.items) {
            enumDef.addItem(eis.itemName);
        }
        return enumDef;
    }

    /************************************************************************************
     * utility methods
     ************************************************************************************/

    private static String[] getCopyableAttrNames(ModelCache smc, long aid) throws AoException {
        List<String> attrs = new ArrayList<String>();
        for (ApplAttr applAttr : smc.getApplAttrs(aid)) {
            if (!applAttr.baName.equals("id")) {
                attrs.add(applAttr.aaName);
            }
        }
        return attrs.toArray(new String[0]);
    }

    private boolean shouldExportApplRel(ModelCache smc, ApplRel applRel) throws AoException {
        // is base element included?
        ApplElem elem1 = smc.getApplElem(ODSHelper.asJLong(applRel.elem1));
        ApplElem elem2 = smc.getApplElem(ODSHelper.asJLong(applRel.elem2));
        if (!this.includeBeNames.contains(elem1.beName.toLowerCase())
                || !this.includeBeNames.contains(elem2.beName.toLowerCase())) {
            return false;
        }
        return true;
    }

    private static class ElemIdMap implements Serializable {

        private static final long serialVersionUID = 7737552775800892476L;

        private final long        aid;
        private final long        iid;

        public ElemIdMap(T_LONGLONG aid, T_LONGLONG iid) {
            this.aid = ODSHelper.asJLong(aid);
            this.iid = ODSHelper.asJLong(iid);
        }

        public ElemIdMap(ElemId elemId) {
            this.aid = ODSHelper.asJLong(elemId.aid);
            this.iid = ODSHelper.asJLong(elemId.iid);
        }

        public long getAid() {
            return aid;
        }

        public long getIid() {
            return iid;
        }

        public ElemId getElemId() {
            return new ElemId(ODSHelper.asODSLongLong(this.aid), ODSHelper.asODSLongLong(this.iid));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (aid ^ (aid >>> 32));
            result = prime * result + (int) (iid ^ (iid >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ElemIdMap other = (ElemIdMap) obj;
            if (aid != other.aid)
                return false;
            if (iid != other.iid)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ElemIdMap [aid=" + aid + ", iid=" + iid + "]";
        }

    }

}
