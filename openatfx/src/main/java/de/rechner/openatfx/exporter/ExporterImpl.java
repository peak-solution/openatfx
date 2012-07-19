package de.rechner.openatfx.exporter;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
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
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.RelationRange;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ModelCache;
import de.rechner.openatfx.util.ODSHelper;


public class ExporterImpl implements IExporter {

    private static final Log LOG = LogFactory.getLog(ExporterImpl.class);

    private Set<String> includeBeNames;

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

            targetSession.commitTransaction();
        } catch (AoException aoe) {
            LOG.error(aoe.reason, aoe);
            targetSession.abortTransaction();
        }
    }

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

    private void exportApplicationRelations(ModelCache smc, AoSession targetSession, Map<Long, Long> source2TargetAidMap)
            throws AoException {
        BaseStructure targetBs = targetSession.getBaseStructure();
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
            if (exportedRels.contains(elem2.aeName + "_" + elem1.aeName + "_" + applRel.invName)) {
                continue;
            }

            Long targetElem1Aid = source2TargetAidMap.get(ODSHelper.asJLong(applRel.elem1));
            Long targetElem2Aid = source2TargetAidMap.get(ODSHelper.asJLong(applRel.elem2));
            ApplicationElement targetElem1 = targetAs.getElementById(ODSHelper.asODSLongLong(targetElem1Aid));
            ApplicationElement targetElem2 = targetAs.getElementById(ODSHelper.asODSLongLong(targetElem2Aid));
            BaseRelation targetBr = null;
            if (applRel.brName.length() > 0) {
                targetBr = targetBs.getRelation(targetElem1.getBaseElement(), targetElem2.getBaseElement());
            }

            // create relation
            ApplicationRelation targetRel = targetAs.createRelation();
            targetRel.setBaseRelation(targetBr);
            targetRel.setElem1(targetElem1);
            targetRel.setElem2(targetElem2);
            targetRel.setRelationName(applRel.arName);
            targetRel.setInverseRelationName(applRel.invName);
            targetRel.setRelationRange(new RelationRange(applRel.arRelationRange.min, applRel.arRelationRange.max));
            targetRel.setInverseRelationRange(applRel.invRelationRange);

            exportedRels.add(elem1.aeName + "_" + elem2.aeName + "_" + applRel.arName);
        }
    }

    /**
     * Exports an enumeration definition.
     * 
     * @param sourceEs The source enumeration structure.
     * @param targetAs The target application structure.
     * @return The created target enumeration definition.
     * @throws AoException Error creating enumeration definition.
     */
    private EnumerationDefinition exportEnumerationDefinition(EnumerationStructure sourceEs,
            ApplicationStructure targetAs) throws AoException {
        EnumerationDefinition enumDef = targetAs.createEnumerationDefinition(sourceEs.enumName);
        for (EnumerationItemStructure eis : sourceEs.items) {
            enumDef.addItem(eis.itemName);
        }
        return enumDef;
    }

}
