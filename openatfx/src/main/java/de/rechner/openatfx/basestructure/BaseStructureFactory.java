package de.rechner.openatfx.basestructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
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
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Factory object used to retrieve <code>org.asam.ods.BaseStructure</code> objects.
 * 
 * @author Christian Rechner
 */
public class BaseStructureFactory {

    private static final Log LOG = LogFactory.getLog(BaseStructureFactory.class);

    /** The singleton instance */
    private static BaseStructureFactory instance;

    /** The base structure cache. */
    private final Map<String, BaseStructure> baseStructureCache;

    /**
     * Non visible constructor.
     */
    private BaseStructureFactory() {
        this.baseStructureCache = new HashMap<String, BaseStructure>();
    }

    /**
     * Returns the ASAM ODS base structure object for given base model version.
     * <p>
     * The base structure objects will be cached for each base model version.
     * 
     * @param orb The ORB.
     * @param baseModelVersion The base model version.
     * @return The base structure.
     * @throws AoException Error getting base structure.
     */
    public BaseStructure getBaseStructure(ORB orb, String baseModelVersion) throws AoException {
        try {
            BaseStructure baseStructure = this.baseStructureCache.get(baseModelVersion);
            if (baseStructure != null) {
                return baseStructure;
            }

            // read base model XML from resources
            InputStream in = BaseStructureFactory.class.getResourceAsStream(baseModelVersion + ".xml");
            if (in == null) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                      "Unsupported base model version: " + baseModelVersion);
            }

            // parse XML
            long start = System.currentTimeMillis();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);

            // create POA
            POA poa = createBaseStructurePOA(orb);
            baseStructure = parseBaseStructure(poa, doc);
            this.baseStructureCache.put(baseModelVersion, baseStructure);

            LOG.info("Read base model '" + baseModelVersion + "' in " + (System.currentTimeMillis() - start) + "ms");
            return baseStructure;
        } catch (ParserConfigurationException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (SAXException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Parse the base structure from given XML document.
     * 
     * @param poa The POA.
     * @param doc The XML document.
     * @return The base structure.
     * @throws AoException Error creating base structure.
     */
    private BaseStructure parseBaseStructure(POA poa, Document doc) throws AoException {
        try {
            Element rootElement = doc.getDocumentElement();
            String version = rootElement.getAttribute("version");
            Map<String, EnumerationDefinition> enumDefMap = parseEnumerationDefinitions(poa, rootElement);
            Map<String, BaseElementImpl> baseElemMap = parseBaseElements(poa, enumDefMap, rootElement);

            BaseStructureImpl baseStructureImpl = new BaseStructureImpl(version);
            for (BaseElementImpl baseElementImpl : baseElemMap.values()) {
                BaseElement baseElement = BaseElementHelper.narrow(poa.servant_to_reference(baseElementImpl));
                baseStructureImpl.addBaseElement(baseElement);
            }
            parseBaseRelations(poa, baseElemMap, rootElement);

            BaseStructure baseStructure = BaseStructureHelper.narrow(poa.servant_to_reference(baseStructureImpl));
            return baseStructure;
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Parse the enumeration definitions from the XML document.
     * 
     * @param poa The POA.
     * @param rootElement The root XML element.
     * @return map of enumeration definitions.
     * @throws WrongPolicy Error creating CORBA objects.
     * @throws ServantAlreadyActive Error creating CORBA objects.
     * @throws ServantNotActive
     */
    private Map<String, EnumerationDefinition> parseEnumerationDefinitions(POA poa, Element rootElement)
            throws ServantAlreadyActive, WrongPolicy, ServantNotActive {
        Map<String, EnumerationDefinition> map = new HashMap<String, EnumerationDefinition>();

        NodeList nodeList = rootElement.getElementsByTagName("EnumerationDefinition");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element enumDefElem = (Element) nodeList.item(i);
            int index = Integer.valueOf(enumDefElem.getAttribute("index"));
            String name = enumDefElem.getAttribute("name");
            BaseEnumerationDefinitionImpl impl = new BaseEnumerationDefinitionImpl(index, name);

            // parse items
            NodeList itemNodeList = enumDefElem.getElementsByTagName("EnumerationItem");
            for (int x = 0; x < itemNodeList.getLength(); x++) {
                Element itemElem = (Element) itemNodeList.item(x);
                int item = Integer.valueOf(itemElem.getAttribute("item"));
                String itemName = itemElem.getAttribute("name");
                impl.addItem(item, itemName);
            }

            EnumerationDefinition enumDef = EnumerationDefinitionHelper.narrow(poa.servant_to_reference(impl));
            map.put(name, enumDef);
        }

        return map;
    }

    /**
     * Parse the enumeration definitions from the XML document.
     * 
     * @param poa The POA.
     * @param enumDefs Map of enumeration definitions.
     * @param rootElement The root XML element.
     * @return map of enumeration definitions.
     * @throws AoException Error parsing base elements.
     * @throws WrongPolicy Error creating CORBA objects.
     * @throws ServantAlreadyActive Error creating CORBA objects.
     * @throws ServantNotActive
     */
    private Map<String, BaseElementImpl> parseBaseElements(POA poa, Map<String, EnumerationDefinition> enumDefs,
            Element rootElement) throws AoException, ServantAlreadyActive, WrongPolicy, ServantNotActive {
        Map<String, BaseElementImpl> map = new HashMap<String, BaseElementImpl>();

        NodeList nodeList = rootElement.getElementsByTagName("BaseElement");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element baseElementElem = (Element) nodeList.item(i);
            String type = baseElementElem.getAttribute("type");
            boolean topLevel = Boolean.valueOf(baseElementElem.getAttribute("topLevel"));
            BaseElementImpl baseElementImpl = new BaseElementImpl(type, topLevel);
            BaseElement baseElement = BaseElementHelper.narrow(poa.servant_to_reference(baseElementImpl));
            for (BaseAttribute baseAttr : parseBaseAttributes(poa, enumDefs, baseElement, baseElementElem)) {
                baseElementImpl.addBaseAttribute(baseAttr);
            }
            map.put(type, baseElementImpl);
        }

        return map;
    }

    /**
     * Parse the base attributes of an base element from the XML document.
     * 
     * @param poa The POA.
     * @param enumDefs Map of enumeration definitions.
     * @param baseElementImpl The base element.
     * @param baseElementElem The XML element of the base element.
     * @return Array of base attributes.
     * @throws AoException Error parsing base elements.
     * @throws WrongPolicy Error creating CORBA objects.
     * @throws ServantAlreadyActive Error creating CORBA objects.
     * @throws ServantNotActive
     */
    private BaseAttribute[] parseBaseAttributes(POA poa, Map<String, EnumerationDefinition> enumDefs,
            BaseElement baseElement, Element baseElementElem) throws AoException, ServantAlreadyActive, WrongPolicy,
            ServantNotActive {
        List<BaseAttribute> list = new ArrayList<BaseAttribute>();

        NodeList nodeList = baseElementElem.getElementsByTagName("BaseAttribute");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element baseAttributeElem = (Element) nodeList.item(i);
            String name = baseAttributeElem.getAttribute("name");
            DataType dataType = ODSHelper.string2dataType(baseAttributeElem.getAttribute("dataType"));
            boolean obligatory = Boolean.parseBoolean(baseAttributeElem.getAttribute("obligatory"));
            boolean unique = Boolean.parseBoolean(baseAttributeElem.getAttribute("unique"));
            EnumerationDefinition enumDef = null;
            if (dataType == DataType.DT_ENUM || dataType == DataType.DS_ENUM) {
                String enumDefName = baseAttributeElem.getAttribute("enumerationDefinition");
                enumDef = enumDefs.get(enumDefName);
                if (enumDef == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Enumeration definition '"
                            + enumDefName + "' not found");
                }
            }
            BaseAttributeImpl baseAttrImpl = new BaseAttributeImpl(name, dataType, obligatory, unique, baseElement,
                                                                   enumDef);
            BaseAttribute baseAttribute = BaseAttributeHelper.narrow(poa.servant_to_reference(baseAttrImpl));
            list.add(baseAttribute);
        }

        return list.toArray(new BaseAttribute[0]);
    }

    /**
     * Parse all base relations from given root element.
     * 
     * @param poa The POA.
     * @param baseElems The base elements.
     * @param rootElement The root XML element.
     * @return Array of base relations.
     * @throws AoException Error parsing base relations.
     * @throws ServantAlreadyActive Error creating CORBA objects.
     * @throws WrongPolicy Error creating CORBA objects.
     * @throws ServantNotActive
     */
    private BaseRelationImpl[] parseBaseRelations(POA poa, Map<String, BaseElementImpl> baseElems, Element rootElement)
            throws AoException, ServantAlreadyActive, WrongPolicy, ServantNotActive {
        List<BaseAttributeImpl> list = new ArrayList<BaseAttributeImpl>();

        NodeList nodeList = rootElement.getElementsByTagName("BaseRelation");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element baseRelationElem = (Element) nodeList.item(i);

            BaseElementImpl elem1Impl = baseElems.get(baseRelationElem.getAttribute("elem1"));
            BaseElementImpl elem2Impl = baseElems.get(baseRelationElem.getAttribute("elem2"));
            BaseElement elem1 = BaseElementHelper.narrow(poa.servant_to_reference(elem1Impl));
            BaseElement elem2 = BaseElementHelper.narrow(poa.servant_to_reference(elem2Impl));

            String relationName = baseRelationElem.getAttribute("relationName");
            String inverseRelationName = baseRelationElem.getAttribute("inverseRelationName");

            RelationRange relationRange = new RelationRange();
            relationRange.min = ODSHelper.string2relRange(baseRelationElem.getAttribute("relationRangeMin"));
            relationRange.max = ODSHelper.string2relRange(baseRelationElem.getAttribute("relationRangeMax"));
            RelationRange inverseRelationRange = new RelationRange();
            inverseRelationRange.min = ODSHelper.string2relRange(baseRelationElem.getAttribute("inverseRelationRangeMin"));
            inverseRelationRange.max = ODSHelper.string2relRange(baseRelationElem.getAttribute("inverseRelationRangeMax"));

            Relationship relationship = ODSHelper.string2relationship(baseRelationElem.getAttribute("relationship"));
            Relationship inverseRelationship = ODSHelper.string2relationship(baseRelationElem.getAttribute("inverseRelationship"));

            RelationType relationType = ODSHelper.string2relationType(baseRelationElem.getAttribute("relationType"));

            BaseRelationImpl baseRelationImpl = new BaseRelationImpl(elem1, elem2, relationName, inverseRelationName,
                                                                     relationRange, inverseRelationRange, relationship,
                                                                     inverseRelationship, relationType);
            BaseRelation baseRelation = BaseRelationHelper.narrow(poa.servant_to_reference(baseRelationImpl));
            elem1Impl.addBaseRelation(baseRelation);
        }

        return list.toArray(new BaseRelationImpl[0]);
    }

    /**
     * Creates a new POA for all elements for the base structure.
     * 
     * @param orb The ORB object.
     * @return The POA.
     * @throws AoException Error creating POA.
     */
    private POA createBaseStructurePOA(ORB orb) throws AoException {
        try {
            String poaName = "BaseStructure.POA." + UUID.randomUUID().toString();
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POA poa = rootPOA.create_POA(poaName,
                                         null,
                                         new Policy[] {
                                                 rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID),
                                                 rootPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
                                                 rootPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                                                 rootPOA.create_implicit_activation_policy(ImplicitActivationPolicyValue.IMPLICIT_ACTIVATION),
                                                 rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
                                                 rootPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
                                                 rootPOA.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL) });
            poa.the_POAManager().activate();
            return poa;
        } catch (InvalidName e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterAlreadyExists e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (InvalidPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterInactive e) {
            LOG.error(e.getMessage(), e);
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
