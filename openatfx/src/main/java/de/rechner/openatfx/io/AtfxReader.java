package de.rechner.openatfx.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AIDName;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.asam.ods.RelationRange;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Object for reading ATFX files.
 * 
 * @author Christian Rechner
 */
public class AtfxReader {

    private static final Log LOG = LogFactory.getLog(AtfxReader.class);

    /** The singleton instance */
    private static AtfxReader instance;

    /**
     * Non visible constructor.
     */
    private AtfxReader() {}

    /**
     * Returns the ASAM ODS aoSession object for a ATFX file.
     * 
     * @param orb The ORB.
     * @param atfxFile The ATFX file.
     * @return The aoSession object.
     * @throws AoException Error getting aoSession.
     */
    public AoSession createSessionForATFX(ORB orb, File atfxFile) throws AoException {
        try {
            // parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            long start = System.currentTimeMillis();
            Document doc = builder.parse(atfxFile);
            Element rootElement = doc.getDocumentElement();
            System.out.println("Read XML in " + (System.currentTimeMillis() - start) + "ms");

            // read base model version
            String baseModelVersion = "";
            NodeList nodeList = rootElement.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && node.getNodeName().equals(AtfxTagConstants.BASE_MODEL_VERSION)) {
                    baseModelVersion = node.getTextContent();
                    break;
                }
            }

            // create AoSession object
            AoSession aoSession = AoServiceFactory.getInstance().newEmptyAoSession(orb, atfxFile, baseModelVersion);

            // parse the ATFX file
            parseATFX(aoSession, rootElement);

            // clear memory
            System.gc();

            return aoSession;
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
     * Parse the content of the ATFX file.
     * 
     * @param aoSession The session.
     * @param rootElement The root XML element.
     * @throws AoException Error parsing ATFX file.
     */
    private void parseATFX(AoSession aoSession, Element rootElement) throws AoException {
        ApplicationStructure as = aoSession.getApplicationStructure();
        Map<String, String> componentMap = new HashMap<String, String>();

        NodeList nodeList = rootElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                if (nodeName.equals(AtfxTagConstants.FILES)) {
                    parseFiles(componentMap, (Element) node);
                } else if (nodeName.equals(AtfxTagConstants.APPLICATION_MODEL)) {
                    long start = System.currentTimeMillis();
                    parseApplicationModel(as, (Element) node);
                    System.out.println("Parsed ApplicationModel in " + (System.currentTimeMillis() - start) + "ms");
                } else if (nodeName.equals(AtfxTagConstants.INSTANCE_DATA)) {
                    long start = System.currentTimeMillis();
                    parseInstanceElements(as, componentMap, (Element) node);
                    System.out.println("Parsed InstanceElements in " + (System.currentTimeMillis() - start) + "ms");
                }
            }
        }
    }

    /**
     * Parse the file components and return as map.
     * 
     * @param componentMap The component map to fill.
     * @param fileElem The file XML element.
     * @return The mapping between component identifier and component name.
     */
    private void parseFiles(Map<String, String> componentMap, Element fileElem) {
        NodeList componentNodeList = fileElem.getChildNodes();
        for (int i = 0; i < componentNodeList.getLength(); i++) {
            Node componentNode = componentNodeList.item(i);
            if ((componentNode.getNodeType() == Node.ELEMENT_NODE)
                    || componentNode.getNodeName().equals(AtfxTagConstants.COMPONENT)) {
                parseComponent(componentMap, (Element) componentNode);
            }
        }
    }

    /**
     * Parse the file component from given XML element.
     * 
     * @param componentMap The component map to fill.
     * @param componentElem The component XML element.
     */
    private void parseComponent(Map<String, String> componentMap, Element componentElem) {
        String identifier = "";
        String filename = "";
        NodeList nodeList = componentElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER)) {
                    identifier = node.getTextContent();
                } else if (node.getNodeName().equals(AtfxTagConstants.COMPONENT_FILENAME)) {
                    filename = node.getTextContent();
                }
            }
        }
        componentMap.put(identifier, filename);
    }

    /***************************************************************************************
     * methods for parsing the application model
     ***************************************************************************************/

    /**
     * Parse the application model and build it directory on given application structure.
     * 
     * @param as The application structure.
     * @param applicationModelElem The application model XML element.
     * @throws AoException Error parsing application structure.
     */
    private void parseApplicationModel(ApplicationStructure as, Element applicationModelElem) throws AoException {
        // first parse application enumerations and application elements
        NodeList nodeList = applicationModelElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals(AtfxTagConstants.APPL_ENUM)) {
                    parseEnumerationDefinition(as, (Element) node);
                } else if (node.getNodeName().equals(AtfxTagConstants.APPL_ELEM)) {
                    parseApplicationElement(as, (Element) node);
                }
            }
        }

        // then parse application relations
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals(AtfxTagConstants.APPL_ELEM)) {
                    parseApplicationRelations(as, (Element) node);
                }
            }
        }
    }

    /**
     * Read the enumeration definitions from the enumeration definition XML element.
     * 
     * @param as The application structure.
     * @param enumDefElem The enumeration definition XML element.
     * @throws AoException Error parsing enumeration definitions.
     */
    private void parseEnumerationDefinition(ApplicationStructure as, Element enumDefElem) throws AoException {
        String enumName = getSingleChildContent(enumDefElem, AtfxTagConstants.APPL_ENUM_NAME);
        EnumerationDefinition enumDef = as.createEnumerationDefinition(enumName);
        NodeList itemNodeList = enumDefElem.getElementsByTagName(AtfxTagConstants.APPL_ENUM_ITEM);
        for (int x = 0; x < itemNodeList.getLength(); x++) {
            Element itemElem = (Element) itemNodeList.item(x);
            String itemName = getSingleChildContent(itemElem, AtfxTagConstants.APPL_ENUM_ITEM_NAME);
            enumDef.addItem(itemName);
        }
    }

    /**
     * Read the application elements from the application element XML element.
     * 
     * @param as The application structure.
     * @param aeElem The application element XML element.
     * @throws AoException Error parsing application elements.
     */
    private void parseApplicationElement(ApplicationStructure as, Element aeElem) throws AoException {
        BaseStructure bs = as.getSession().getBaseStructure();
        BaseElement baseElem = bs.getElementByType(getSingleChildContent(aeElem, AtfxTagConstants.APPL_ELEM_BASETYPE));
        ApplicationElement ae = as.createElement(baseElem);
        ae.setName(getSingleChildContent(aeElem, AtfxTagConstants.APPL_ELEM_NAME));
        parseApplicationAttributes(ae, aeElem);
    }

    /**
     * Read the application attributes from the application element node.
     * 
     * @param as The application structure.
     * @param rootElement The root XML element.
     * @throws AoException Error parsing application elements.
     */
    private void parseApplicationAttributes(ApplicationElement applElem, Element aeElem) throws AoException {
        NodeList nodeList = aeElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE) && node.getNodeName().equals(AtfxTagConstants.APPL_ATTR)) {
                parseApplicationAttribute(applElem, (Element) node);
            }
        }
    }

    /**
     * Read the application attribute from the application attribute node.
     * 
     * @param as The application structure.
     * @param aaElem The root application attribute XML element.
     * @throws AoException Error parsing application attribute.
     */
    private void parseApplicationAttribute(ApplicationElement applElem, Element aaElem) throws AoException {
        BaseElement baseElement = applElem.getBaseElement();

        // cache existing base attributes
        Map<String, ApplicationAttribute> baToAaMap = new HashMap<String, ApplicationAttribute>();
        for (ApplicationAttribute existingAa : applElem.getAttributes("*")) {
            BaseAttribute ba = existingAa.getBaseAttribute();
            if (ba != null) {
                baToAaMap.put(ba.getName(), existingAa);
            }
        }

        String aaNameStr = "";
        String baseAttrStr = "";
        String dataTypeStr = "";
        String lengthStr = "";
        String obligatoryStr = "";
        String uniqueStr = "";
        String autogeneratedStr = "";
        String enumtypeStr = "";
        NodeList nodeList = aaElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String elemName = node.getNodeName();
            if (elemName.equals(AtfxTagConstants.APPL_ATTR_NAME)) {
                aaNameStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_BASEATTR)) {
                baseAttrStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_DATATYPE)) {
                dataTypeStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_LENGTH)) {
                lengthStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_OBLIGATORY)) {
                obligatoryStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_UNIQUE)) {
                uniqueStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_AUTOGENERATED)) {
                autogeneratedStr = node.getTextContent();
            } else if (elemName.equals(AtfxTagConstants.APPL_ATTR_ENUMTYPE)) {
                enumtypeStr = node.getTextContent();
            }
        }

        // check if base attribute already exists
        ApplicationAttribute aa = null;
        if (baseAttrStr != null && baseAttrStr.length() > 0) {
            aa = baToAaMap.get(baseAttrStr);
        }
        if (aa == null) {
            aa = applElem.createAttribute();
        }
        aa.setName(aaNameStr);

        // base attribute?
        if (baseAttrStr != null && baseAttrStr.length() > 0) {
            BaseAttribute[] baseAttrs = baseElement.getAttributes(baseAttrStr);
            if (baseAttrs.length < 1) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Base attribute '" + baseAttrStr
                        + "' not found");
            }
            aa.setBaseAttribute(baseAttrs[0]);
        }
        // datatype & obligatory
        else {
            DataType datatype = ODSHelper.string2dataType(dataTypeStr);
            aa.setDataType(datatype);
            aa.setIsObligatory(Boolean.valueOf(obligatoryStr));
        }
        // length
        if (lengthStr != null && lengthStr.length() > 0) {
            aa.setLength(AtfxParseUtil.parseLong(lengthStr));
        }
        // unique
        if (uniqueStr != null && uniqueStr.length() > 0) {
            aa.setIsUnique(AtfxParseUtil.parseBoolean(uniqueStr));
        }
        // autogenerated
        if (autogeneratedStr != null && autogeneratedStr.length() > 0) {
            aa.setIsAutogenerated(AtfxParseUtil.parseBoolean(autogeneratedStr));
        }
        // enumeration
        if (enumtypeStr != null && enumtypeStr.length() > 0) {
            EnumerationDefinition enumDef = applElem.getApplicationStructure().getEnumerationDefinition(enumtypeStr);
            aa.setEnumerationDefinition(enumDef);
        }
    }

    /**
     * Read the application relations from the application element node.
     * 
     * @param as The application structure.
     * @param aeElem The application element node.
     * @throws AoException Error parsing application elements.
     */
    private void parseApplicationRelations(ApplicationStructure as, Element aeElem) throws AoException {
        NodeList nodeList = aeElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE) && node.getNodeName().equals(AtfxTagConstants.APPL_REL)) {
                parseApplicationRelation(as, (Element) node);
            }
        }
    }

    /**
     * Read a application relations from the application relation XML element.
     * 
     * @param as The application structure.
     * @param arElem The application relation XML element.
     * @throws AoException Error parsing application relations.
     */
    private void parseApplicationRelation(ApplicationStructure as, Element arElem) throws AoException {
        String elem1Name = getSingleChildContent((Element) arElem.getParentNode(), AtfxTagConstants.APPL_ELEM_NAME);
        String elem2Name = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_REFTO);
        String relName = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_NAME);
        String inverseRelname = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_INVNAME);
        String brName = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_BASEREL);
        String minStr = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_MIN);
        String maxStr = getSingleChildContent(arElem, AtfxTagConstants.APPL_REL_MAX);

        ApplicationRelation rel = as.createRelation();
        ApplicationElement elem1 = as.getElementByName(elem1Name);
        ApplicationElement elem2 = as.getElementByName(elem2Name);
        RelationRange relRange = new RelationRange();
        relRange.min = ODSHelper.string2relRange(minStr);
        relRange.max = ODSHelper.string2relRange(maxStr);

        rel.setElem1(elem1);
        rel.setElem2(elem2);
        rel.setRelationName(relName);
        rel.setInverseRelationName(inverseRelname);
        rel.setRelationRange(relRange);

        if (brName != null && brName.length() > 0) {
            BaseRelation baseRel = null;
            for (BaseRelation br : elem1.getBaseElement().getAllRelations()) {
                if (br.getRelationName().equals(brName)) {
                    baseRel = br;
                    break;
                }
            }
            if (baseRel == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "BaseRelation '" + brName
                        + "' not found'");
            }
            rel.setBaseRelation(baseRel);
        }
    }

    /***************************************************************************************
     * methods for parsing instance elements
     ***************************************************************************************/

    /**
     * Read the instance elements from the instance data XML element.
     * <p>
     * Also the relations are parsed and set.
     * 
     * @param as The application structure.
     * @param componentMap The mapping between external file identifier and file name.
     * @param instanceDataElement The instance data XML element.
     * @throws AoException Error parsing instance elements.
     */
    private void parseInstanceElements(ApplicationStructure as, Map<String, String> componentMap,
            Element instanceDataElement) throws AoException {
        Map<ElemId, Map<ApplicationRelation, T_LONGLONG[]>> relMap = new HashMap<ElemId, Map<ApplicationRelation, T_LONGLONG[]>>();

        // parse instances
        NodeList nodeList = instanceDataElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                relMap.putAll(parseInstanceElement(as, (Element) node));
            }
        }

        // create relations
        ApplElemAccess applElemAccess = as.getSession().getApplElemAccess();
        for (ElemId elemId : relMap.keySet()) {
            for (ApplicationRelation applRel : relMap.get(elemId).keySet()) {
                T_LONGLONG[] relIids = relMap.get(elemId).get(applRel);
                applElemAccess.setRelInst(elemId, applRel.getRelationName(), relIids, SetType.APPEND);
            }
        }
    }

    /**
     * Read the instance attributes from the instance element XML element.
     * 
     * @param as The application structure.
     * @param instanceNode The instance element XML element.
     * @return
     * @throws AoException Error parsing instance element.
     */
    private Map<ElemId, Map<ApplicationRelation, T_LONGLONG[]>> parseInstanceElement(ApplicationStructure as,
            Element instanceNode) throws AoException {
        ApplicationElement applElem = as.getElementByName(instanceNode.getNodeName());

        // collect all possible attributes and relations
        Map<String, ApplicationAttribute> applAttrs = new HashMap<String, ApplicationAttribute>();
        Map<String, ApplicationRelation> applRels = new HashMap<String, ApplicationRelation>();
        String valuesAttr = null;
        for (ApplicationAttribute applAttr : applElem.getAttributes("*")) {
            if (applAttr.getBaseAttribute() != null && applAttr.getBaseAttribute().getName().equals("values")) {
                valuesAttr = applAttr.getName();
            } else {
                applAttrs.put(applAttr.getName(), applAttr);
            }
        }
        for (ApplicationRelation applRel : applElem.getAllRelations()) {
            applRels.put(applRel.getRelationName(), applRel);
        }

        // parse application attribute and instance attribute values
        List<AIDNameValueSeqUnitId> applAttrValues = new ArrayList<AIDNameValueSeqUnitId>();
        List<NameValueUnit> instAttrValues = new ArrayList<NameValueUnit>();
        Map<ApplicationRelation, T_LONGLONG[]> relMap = new HashMap<ApplicationRelation, T_LONGLONG[]>();
        NodeList nodeList = instanceNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                // application attribute
                if (applAttrs.containsKey(nodeName)) {
                    ApplicationAttribute aa = applAttrs.get(node.getNodeName());
                    AIDNameValueSeqUnitId applAttrValue = new AIDNameValueSeqUnitId();
                    applAttrValue.unitId = ODSHelper.asODSLongLong(0);
                    applAttrValue.attr = new AIDName();
                    applAttrValue.attr.aid = applElem.getId();
                    applAttrValue.attr.aaName = nodeName;
                    applAttrValue.values = ODSHelper.tsValue2tsValueSeq(parseTextContent(aa, (Element) node));
                    applAttrValues.add(applAttrValue);
                }
                // instance attribute
                else if (node.getNodeName().equals(AtfxTagConstants.INST_ATTR)) {
                    instAttrValues.addAll(parseInstanceAttributes((Element) node));
                }
                // relation
                else if (applRels.containsKey(nodeName)) {
                    // only read the non inverse relations for performance reasons!
                    ApplicationRelation applRel = applRels.get(nodeName);
                    short invMax = applRel.getInverseRelationRange().max;
                    if (invMax == -1) {
                        String textContent = node.getTextContent();
                        if (textContent.length() > 0) {
                            T_LONGLONG[] relInstIids = AtfxParseUtil.parseLongLongSeq(node.getTextContent());
                            relMap.put(applRel, relInstIids);
                        }
                    }
                }
                // values of 'LocalColumn'
                else if (valuesAttr != null && valuesAttr.equals(nodeName)) {
                    // TODO: implement me
                }
            }
        }

        // create instance element
        ApplElemAccess aea = as.getSession().getApplElemAccess();
        ElemId elemId = aea.insertInstances(applAttrValues.toArray(new AIDNameValueSeqUnitId[0]))[0];

        // set instance attributes
        if (!instAttrValues.isEmpty()) {
            InstanceElement ie = applElem.getInstanceById(elemId.iid);
            for (NameValueUnit nvu : instAttrValues) {
                ie.addInstanceAttribute(nvu);
            }
        }

        // create relation map
        Map<ElemId, Map<ApplicationRelation, T_LONGLONG[]>> retMap = new HashMap<ElemId, Map<ApplicationRelation, T_LONGLONG[]>>();
        retMap.put(new ElemId(applElem.getId(), elemId.iid), relMap);

        return retMap;
    }

    /**
     * Read the instance attributes from the instance attributes XML element.
     * 
     * @param instElemsNode The instance attributes XML element.
     * @return List of values.
     */
    private List<NameValueUnit> parseInstanceAttributes(Element instElemsNode) throws AoException {
        List<NameValueUnit> instAttrs = new ArrayList<NameValueUnit>();

        NodeList nodeList = instElemsNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nodeName = node.getNodeName();
            NameValueUnit nvu = new NameValueUnit();
            nvu.unit = "";
            nvu.valName = ((Element) node).getAttribute(AtfxTagConstants.INST_ATTR_NAME);
            nvu.value = new TS_Value();
            nvu.value.u = new TS_Union();

            String textContent = node.getTextContent();
            // DT_STRING
            if (nodeName.equals(AtfxTagConstants.INST_ATTR_ASCIISTRING)) {
                nvu.value.u.stringVal(textContent);
                nvu.value.flag = (textContent == null || textContent.length() < 1) ? (short) 0 : (short) 15;
            }
            // DT_FLOAT
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_FLOAT32)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.floatVal(AtfxParseUtil.parseFloat(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.floatVal(0);
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_DOUBLE
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_FLOAT64)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.doubleVal(AtfxParseUtil.parseDouble(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.doubleVal(0);
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_BYTE
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_INT8)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.byteVal(AtfxParseUtil.parseByte(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.byteVal((byte) 0);
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_BYTE
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_INT16)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.shortVal(AtfxParseUtil.parseShort(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.shortVal((short) 0);
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_LONG
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_INT32)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.longVal(AtfxParseUtil.parseLong(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.longVal(0);
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_LONGLONG
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_INT64)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.longlongVal(AtfxParseUtil.parseLongLong(textContent));
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.longlongVal(ODSHelper.asODSLongLong(0));
                    nvu.value.flag = (short) 0;
                }
            }
            // DT_DATE
            else if (nodeName.equals(AtfxTagConstants.INST_ATTR_TIME)) {
                if (textContent.trim().length() > 0) {
                    nvu.value.u.dateVal(textContent.trim());
                    nvu.value.flag = (short) 15;
                } else {
                    nvu.value.u.dateVal("");
                    nvu.value.flag = (short) 0;
                }
            }

            instAttrs.add(nvu);
        }

        return instAttrs;
    }

    /**
     * Parse the inline measurement data found in the XML element of the 'values' application attribute of an instance
     * of LocalColumn.
     * 
     * @param attrElem The XML element.
     * @return The value.
     * @throws AoException
     */
    private TS_Union parseMeasurementData(Element attrElem) throws AoException {
        NodeList nodeList = attrElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
        }

        TS_Union u = new TS_Union();

        u.floatSeq(new float[] { 0, 2, 3, 3, 3, });

        return u;
    }

    /***************************************************************************************
     * methods for parsing attribute values
     ***************************************************************************************/

    /**
     * @param aa
     * @param attrElem
     * @return
     * @throws AoException
     */
    private TS_Value parseTextContent(ApplicationAttribute aa, Element attrElem) throws AoException {
        DataType dataType = aa.getDataType();
        TS_Value tsValue = ODSHelper.createEmptyTS_Value(dataType);
        String str = attrElem.getTextContent();
        if (str != null && str.length() > 0) {
            tsValue.flag = 15;
            tsValue.u = new TS_Union();
            // DT_BLOB
            if (dataType == DataType.DT_BLOB) {
                tsValue.u.blobVal(parseBlob(aa, attrElem));
            }
            // DT_BOOLEAN
            else if (dataType == DataType.DT_BOOLEAN) {
                tsValue.u.booleanVal(AtfxParseUtil.parseBoolean(str));
            }
            // DT_BYTE
            else if (dataType == DataType.DT_BYTE) {
                tsValue.u.byteVal(AtfxParseUtil.parseByte(str));
            }
            // DT_BYTESTR
            else if (dataType == DataType.DT_BYTESTR) {
                tsValue.u.bytestrVal(AtfxParseUtil.parseByteSeq(str));
            }
            // DT_COMPLEX
            else if (dataType == DataType.DT_COMPLEX) {
                tsValue.u.complexVal(AtfxParseUtil.parseComplex(str));
            }
            // DT_DATE
            else if (dataType == DataType.DT_DATE) {
                tsValue.u.dateVal(str);
            }
            // DT_COMPLEX
            else if (dataType == DataType.DT_DCOMPLEX) {
                tsValue.u.dcomplexVal(AtfxParseUtil.parseDComplex(str));
            }
            // DT_DOUBLE
            else if (dataType == DataType.DT_DOUBLE) {
                tsValue.u.doubleVal(AtfxParseUtil.parseDouble(str));
            }
            // DT_ENUM
            else if (dataType == DataType.DT_ENUM) {
                EnumerationDefinition ed = aa.getEnumerationDefinition();
                tsValue.u.enumVal(ed.getItem(str));
            }
            // DT_EXTERNALREFERENCE
            else if (dataType == DataType.DT_EXTERNALREFERENCE) {
                T_ExternalReference[] extRefs = parseExtRefs(attrElem);
                if (extRefs.length > 1) {
                    throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                          "Multiple references for datatype DT_EXTERNALREFERENCE FOUND");
                }
                tsValue.u.extRefVal(extRefs[0]);
            }
            // DT_FLOAT
            else if (dataType == DataType.DT_FLOAT) {
                tsValue.u.floatVal(AtfxParseUtil.parseFloat(str));
            }
            // DT_ID
            else if (dataType == DataType.DT_ID) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "DataType 'DT_ID' not supported for application attribute");
            }
            // DT_LONG
            else if (dataType == DataType.DT_LONG) {
                tsValue.u.longVal(AtfxParseUtil.parseLong(str));
            }
            // DT_LONGLONG
            else if (dataType == DataType.DT_LONGLONG) {
                tsValue.u.longlongVal(AtfxParseUtil.parseLongLong(str));
            }
            // DT_SHORT
            else if (dataType == DataType.DT_SHORT) {
                tsValue.u.shortVal(AtfxParseUtil.parseShort(str));
            }
            // DT_STRING
            else if (dataType == DataType.DT_STRING) {
                tsValue.u.stringVal(str);
            }
            // DS_BOOLEAN
            else if (dataType == DataType.DS_BOOLEAN) {
                tsValue.u.booleanSeq(AtfxParseUtil.parseBooleanSeq(str));
            }
            // DS_BYTE
            else if (dataType == DataType.DS_BYTE) {
                tsValue.u.byteSeq(AtfxParseUtil.parseByteSeq(str));
            }
            // DS_BYTESTR
            else if (dataType == DataType.DS_BYTESTR) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "DataType 'DS_BYTESTR' not supported for application attribute");
            }
            // DS_COMPLEX
            else if (dataType == DataType.DS_COMPLEX) {
                tsValue.u.complexSeq(AtfxParseUtil.parseComplexSeq(str));
            }
            // DS_DATE
            else if (dataType == DataType.DS_DATE) {
                tsValue.u.dateSeq(parseStringSeq(attrElem));
            }
            // DS_DCOMPLEX
            else if (dataType == DataType.DS_DCOMPLEX) {
                tsValue.u.dcomplexSeq(AtfxParseUtil.parseDComplexSeq(str));
            }
            // DS_DOUBLE
            else if (dataType == DataType.DS_DOUBLE) {
                tsValue.u.doubleSeq(AtfxParseUtil.parseDoubleSeq(str));
            }
            // DS_ENUM
            else if (dataType == DataType.DS_ENUM) {
                String[] enumValues = parseStringSeq(attrElem);
                EnumerationDefinition ed = aa.getEnumerationDefinition();
                int[] enumItems = new int[enumValues.length];
                for (int i = 0; i < enumItems.length; i++) {
                    enumItems[i] = ed.getItem(enumValues[i]);
                }
                tsValue.u.enumSeq(enumItems);
            }
            // DS_EXTERNALREFERENCE
            else if (dataType == DataType.DS_EXTERNALREFERENCE) {
                tsValue.u.extRefSeq(parseExtRefs(attrElem));
            }
            // DS_FLOAT
            else if (dataType == DataType.DS_FLOAT) {
                tsValue.u.floatSeq(AtfxParseUtil.parseFloatSeq(str));
            }
            // DS_ID
            else if (dataType == DataType.DS_ID) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "DataType 'DS_ID' not supported for application attribute");
            }
            // DS_LONG
            else if (dataType == DataType.DS_LONG) {
                tsValue.u.longSeq(AtfxParseUtil.parseLongSeq(str));
            }
            // DS_LONGLONG
            else if (dataType == DataType.DS_LONGLONG) {
                tsValue.u.longlongSeq(AtfxParseUtil.parseLongLongSeq(str));
            }
            // DS_SHORT
            else if (dataType == DataType.DS_SHORT) {
                tsValue.u.shortSeq(AtfxParseUtil.parseShortSeq(str));
            }
            // DS_STRING
            else if (dataType == DataType.DS_STRING) {
                tsValue.u.stringSeq(parseStringSeq(attrElem));
            }
            // DT_UNKNOWN: only for the values of a LocalColumn
            else if (dataType == DataType.DT_UNKNOWN) {
                tsValue.u = parseMeasurementData(attrElem);
            }
            // unsupported data type
            else {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "DataType "
                        + dataType.value() + " not yet implemented");
            }
        }
        return tsValue;
    }

    /**
     * Parse a BLOB object from given application attribute XML element.
     * 
     * @param aa The application attribute.
     * @param attrElem The XML element.
     * @return The Blob.
     * @throws AoException The Blob.
     */
    private Blob parseBlob(ApplicationAttribute aa, Element attrElem) throws AoException {
        Blob blob = aa.getApplicationElement().getApplicationStructure().getSession().createBlob();
        String header = getSingleChildContent(attrElem, AtfxTagConstants.BLOB_TEXT);
        blob.setHeader(header == null ? "" : header);
        Element byteFieldElem = getSingleChildElement(attrElem, AtfxTagConstants.BLOB_BYTEFIELD);
        if (byteFieldElem != null) {
            String content = getSingleChildContent(byteFieldElem, AtfxTagConstants.BLOB_SEQUENCE);
            blob.append(AtfxParseUtil.parseByteSeq(content));
        }
        return blob;
    }

    /**
     * Parse an array of strings objects from given XML element.
     * 
     * @param attrElem The XML element.
     * @return Array of string objects.
     */
    private String[] parseStringSeq(Element attrElem) {
        List<String> list = new ArrayList<String>();
        NodeList nodeList = attrElem.getElementsByTagName("s");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element extRefElem = (Element) nodeList.item(i);
            list.add(extRefElem.getTextContent());
        }
        return list.toArray(new String[0]);
    }

    /**
     * Parse an array of T_ExternalReference objects from given XML element.
     * 
     * @param attrElem The XML element.
     * @return Array of T_ExternalReference objects.
     */
    private T_ExternalReference[] parseExtRefs(Element attrElem) {
        List<T_ExternalReference> list = new ArrayList<T_ExternalReference>();
        NodeList nodeList = attrElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE) && (node.getNodeName().equals(AtfxTagConstants.EXTREF))) {
                list.add(parseExtRef((Element) node));
            }
        }
        return list.toArray(new T_ExternalReference[0]);
    }

    /**
     * Parse an external reference from the external references node.
     * 
     * @param extRefElem The external references XML element.
     * @return The T_ExternalReference object.
     */
    private T_ExternalReference parseExtRef(Element extRefElem) {
        T_ExternalReference extRef = new T_ExternalReference("", "", "");
        NodeList nodeList = extRefElem.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                if (nodeName.equals(AtfxTagConstants.EXTREF_DESCRIPTION)) {
                    extRef.description = node.getTextContent();
                } else if (nodeName.equals(AtfxTagConstants.EXTREF_MIMETYPE)) {
                    extRef.mimeType = node.getTextContent();
                } else if (nodeName.equals(AtfxTagConstants.EXTREF_LOCATION)) {
                    extRef.location = node.getTextContent();
                }
            }
        }
        return extRef;
    }

    /**
     * Returns the text content of a single direct child element.
     * 
     * @param element The parent element.
     * @param elemName The child element name.
     * @return Child element not found.
     */
    private static String getSingleChildContent(Element element, String elemName) {
        Element childElement = getSingleChildElement(element, elemName);
        return childElement == null ? null : childElement.getTextContent();
    }

    /**
     * Returns the single direct child XML element.
     * 
     * @param element The parent element.
     * @param elemName The child element.
     * @return Child element not found.
     */
    private static Element getSingleChildElement(Element element, String elemName) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(elemName)) {
                return (Element) node;
            }
        }
        return null;
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static AtfxReader getInstance() {
        if (instance == null) {
            instance = new AtfxReader();
        }
        return instance;
    }

}
