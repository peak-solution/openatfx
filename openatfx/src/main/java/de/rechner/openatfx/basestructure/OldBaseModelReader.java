package de.rechner.openatfx.basestructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.rechner.openatfx.util.ODSHelper;


public class OldBaseModelReader extends BasicModelReader {

    private static final Logger LOG = LoggerFactory.getLogger(OldBaseModelReader.class);

    @Override
    BaseStructure readBaseStructure(String baseModelVersion, POA poa) throws AoException {
        try {
            // parse XML
            InputStream in = OldBaseModelReader.class.getResourceAsStream(baseModelVersion + ".xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            return parseBaseStructure(poa, doc);
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
            BaseElement baseElement, Element baseElementElem)
            throws AoException, ServantAlreadyActive, WrongPolicy, ServantNotActive {
        List<BaseAttribute> list = new ArrayList<BaseAttribute>();

        NodeList nodeList = baseElementElem.getElementsByTagName("BaseAttribute");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element baseAttributeElem = (Element) nodeList.item(i);
            String name = baseAttributeElem.getAttribute("name");
            DataType dataType = ODSHelper.string2dataType(baseAttributeElem.getAttribute("dataType"));
            boolean obligatory = Boolean.parseBoolean(baseAttributeElem.getAttribute("obligatory"));
            boolean unique = Boolean.parseBoolean(baseAttributeElem.getAttribute("unique"));
            boolean autogenerated = Boolean.parseBoolean(baseAttributeElem.getAttribute("autogenerated"));
            EnumerationDefinition enumDef = null;
            if (dataType == DataType.DT_ENUM || dataType == DataType.DS_ENUM) {
                String enumDefName = baseAttributeElem.getAttribute("enumerationDefinition");
                enumDef = enumDefs.get(enumDefName);
                if (enumDef == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "Enumeration definition '" + enumDefName + "' not found");
                }
            }
            BaseAttributeImpl baseAttrImpl = new BaseAttributeImpl(name, dataType, obligatory, unique, baseElement,
                                                                   enumDef);
            BaseAttribute baseAttribute = BaseAttributeHelper.narrow(poa.servant_to_reference(baseAttrImpl));
            autogeneratedFlags.computeIfAbsent(baseElement.getType(), v -> new HashMap<>()).put(baseAttribute.getName(),
                                                                                                autogenerated);
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
}
