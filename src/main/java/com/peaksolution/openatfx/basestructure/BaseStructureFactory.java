package com.peaksolution.openatfx.basestructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseAttributeHelper;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseElementHelper;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseRelationHelper;
import org.asam.ods.BaseStructure;
import org.asam.ods.BaseStructureHelper;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationDefinitionHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.api.BaseModel;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Factory object used to retrieve <code>org.asam.ods.BaseStructure</code> objects.
 * 
 * @author Markus Renner
 */
public class BaseStructureFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BaseStructureFactory.class);

    /** The singleton instance */
    private static BaseStructureFactory instance;

    /** The base structure cache. */
    private final Map<String, BaseStructure> baseStructureCache;

    /**
     * Non visible constructor.
     */
    private BaseStructureFactory() {
        this.baseStructureCache = new HashMap<>();
    }

    /**
     * Returns the ASAM ODS base structure object for given base model version.
     * <p>
     * The base structure objects will be cached for each base model version.
     * 
     * @param orb The ORB.
     * @param api The OpenAtfxAPI.
     * @return The base structure.
     * @throws AoException Error getting base structure.
     */
    public BaseStructure getBaseStructure(ORB orb, OpenAtfxAPI api) throws AoException {
        BaseModel baseModel = api.getBaseModel();
        String baseModelVersion = baseModel.getVersion();
        BaseStructure baseStructure = this.baseStructureCache.get(baseModelVersion);
        if (baseStructure != null) {
            return baseStructure;
        }

        long start = System.currentTimeMillis();
        LOG.debug("Prepare base model for CORBA...");
        // create POA
        POA poa = createBaseStructurePOA(orb);
        baseStructure = readBaseStructure(baseModel, api, poa);
        this.baseStructureCache.put(baseModelVersion, baseStructure);
        LOG.info("Prepared CORBA BaseStructure in {}ms", System.currentTimeMillis() - start);

        return baseStructure;
    }

    private BaseStructure readBaseStructure(BaseModel baseModel, OpenAtfxAPI api, POA poa) throws AoException {
        try {
            // base enums
            Map<String, EnumerationDefinition> enumDefs = new HashMap<>();
            for (com.peaksolution.openatfx.api.EnumerationDefinition currentEnum : baseModel.getEnumerations()) {
                int index = currentEnum.getIndex();
                String name = currentEnum.getName();
                BaseEnumerationDefinitionImpl impl = new BaseEnumerationDefinitionImpl(index, name);

                for (String itemName : currentEnum.listItemNames()) {
                    int item = Math.toIntExact(currentEnum.getItem(itemName));
                    impl.addItem(item, itemName);
                }
                EnumerationDefinition enumDef = EnumerationDefinitionHelper.narrow(poa.servant_to_reference(impl));
                enumDefs.put(name, enumDef);
            }

            // base elements
            Map<String, BaseElementImpl> baseElements = new HashMap<>();
            BaseStructureImpl baseStructureImpl = new BaseStructureImpl(baseModel.getVersion());
            for (com.peaksolution.openatfx.api.BaseElement currentElement : baseModel.getElements("*")) {
                BaseElementImpl baseElementImpl = new BaseElementImpl(api, currentElement);
                BaseElement baseElement = BaseElementHelper.narrow(poa.servant_to_reference(baseElementImpl));

                // base attributes
                for (BaseAttribute baseAttr : parseBaseAttributes(poa, enumDefs, baseElement, currentElement)) {
                    baseElementImpl.addBaseAttribute(baseAttr);
                }
                baseStructureImpl.addBaseElement(baseElement);
                baseElements.put(currentElement.getType(), baseElementImpl);
            }

            // base relations
            parseBaseRelations(poa, baseElements, baseModel.getElements("*"));

            // base structure
            return BaseStructureHelper.narrow(poa.servant_to_reference(baseStructureImpl));
        } catch (WrongPolicy | ServantNotActive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Parse the base attributes of an base element from the XML document.
     * 
     * @param poa The POA.
     * @param enumDefs Map of enumeration definitions.
     * @param baseElementImpl The base element.
     * @param atfxBaseElement The XML element of the base element.
     * @return Array of base attributes.
     * @throws AoException Error parsing base elements.
     * @throws WrongPolicy Error creating CORBA objects
     * @throws ServantNotActive
     */
    private BaseAttribute[] parseBaseAttributes(POA poa, Map<String, EnumerationDefinition> enumDefs,
            BaseElement baseElement, com.peaksolution.openatfx.api.BaseElement atfxBaseElement)
            throws AoException, WrongPolicy, ServantNotActive {
        List<BaseAttribute> list = new ArrayList<>();

        for (com.peaksolution.openatfx.api.BaseAttribute currentAttr : atfxBaseElement.getAttributes("*")) {
            com.peaksolution.openatfx.api.DataType dataType = currentAttr.getDataType();
            DataType odsDataType = ODSHelper.string2dataType(dataType.name());
            EnumerationDefinition enumDef = null;

            if (odsDataType == DataType.DT_ENUM || odsDataType == DataType.DS_ENUM) {
                String enumDefName = currentAttr.getEnumName();
                enumDef = enumDefs.get(enumDefName);
                if (enumDef == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "Enumeration definition '" + enumDefName + "' not found");
                }
            }
            BaseAttributeImpl baseAttrImpl = new BaseAttributeImpl(currentAttr.getName(), odsDataType,
                                                                   currentAttr.isObligatory(), currentAttr.isUnique(),
                                                                   baseElement, enumDef);
            BaseAttribute baseAttribute = BaseAttributeHelper.narrow(poa.servant_to_reference(baseAttrImpl));
            list.add(baseAttribute);
        }

        return list.toArray(new BaseAttribute[0]);
    }

    /**
     * Parse all base relations from given root element.
     * 
     * @param poa The POA.
     * @param baseElements The base elements.
     * @param atfxBaseElements The atfx base elements.
     * @throws WrongPolicy Error creating CORBA objects.
     * @throws ServantNotActive
     */
    private void parseBaseRelations(POA poa, Map<String, BaseElementImpl> baseElements,
            com.peaksolution.openatfx.api.BaseElement[] atfxBaseElements) throws WrongPolicy, ServantNotActive {
        for (com.peaksolution.openatfx.api.BaseElement atfxBaseElement : atfxBaseElements) {
            for (com.peaksolution.openatfx.api.BaseRelation currentRelation : atfxBaseElement.getRelations()) {
                BaseElementImpl elemImpl1 = baseElements.get(currentRelation.getElem1().getType());
                BaseElement elem1 = BaseElementHelper.narrow(poa.servant_to_reference(elemImpl1));
                for (com.peaksolution.openatfx.api.BaseElement atfxBaseElement2 : currentRelation.getElem2()) {
                    BaseElementImpl elemImpl2 = baseElements.get(atfxBaseElement2.getType());
                    BaseElement elem2 = BaseElementHelper.narrow(poa.servant_to_reference(elemImpl2));

                    Relationship relationship = ODSHelper.string2relationship(currentRelation.getRelationship()
                                                                                             .toString());
                    Relationship inverseRelationship = ODSHelper.string2relationship(currentRelation.getInverseRelationship()
                                                                                                    .toString());

                    BaseRelationImpl baseRelationImpl = new BaseRelationImpl(elem1, elem2, currentRelation.getName(),
                                                                             currentRelation.getInverseName(atfxBaseElement2.getType()),
                                                                             currentRelation.getRelationRange(),
                                                                             currentRelation.getInverseRelationRange(),
                                                                             relationship, inverseRelationship,
                                                                             currentRelation.getRelationType());
                    BaseRelation baseRelation = BaseRelationHelper.narrow(poa.servant_to_reference(baseRelationImpl));
                    elemImpl1.addBaseRelation(baseRelation);
                }
            }
        }
    }

    /**
     * Creates a new POA for all elements for the base structure.
     * 
     * @param orb The ORB object.
     * @return The POA.
     * @throws AoException Error creating POA.
     */
    protected POA createBaseStructurePOA(ORB orb) throws AoException {
        try {
            String poaName = "BaseStructure.POA." + UUID.randomUUID().toString();
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POA poa = rootPOA.create_POA(poaName, null, new Policy[] {
                    rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID),
                    rootPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
                    rootPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                    rootPOA.create_implicit_activation_policy(ImplicitActivationPolicyValue.IMPLICIT_ACTIVATION),
                    rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
                    rootPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
                    rootPOA.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL) });
            poa.the_POAManager().activate();
            return poa;
        } catch (InvalidName | AdapterAlreadyExists | InvalidPolicy | AdapterInactive e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static BaseStructureFactory getInstance() {
        if (instance == null) {
            instance = new BaseStructureFactory();
        }
        return instance;
    }
}
