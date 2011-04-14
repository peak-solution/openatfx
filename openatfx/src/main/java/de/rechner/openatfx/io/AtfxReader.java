package de.rechner.openatfx.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;

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

    /** cached model information for faster parsing */
    private final Map<String, String> documentation;
    private final Map<String, String> files;
    private final Map<String, Map<String, ApplicationAttribute>> applAttrs;
    private final Map<String, Map<String, ApplicationRelation>> applRels; // aeName, relName, rel
    private ApplicationAttribute applAttrLocalColumnValues;

    /**
     * Non visible constructor.
     */
    private AtfxReader() {
        this.documentation = new HashMap<String, String>();
        this.files = new HashMap<String, String>();
        this.applAttrs = new HashMap<String, Map<String, ApplicationAttribute>>();
        this.applRels = new HashMap<String, Map<String, ApplicationRelation>>();
    }

    /**
     * Returns the ASAM ODS aoSession object for a ATFX file.
     * 
     * @param orb The ORB.
     * @param atfxFile The ATFX file.
     * @return The aoSession object.
     * @throws AoException Error getting aoSession.
     */
    public synchronized AoSession createSessionForATFX(ORB orb, File atfxFile) throws AoException {
        long start = System.currentTimeMillis();
        this.documentation.clear();
        this.files.clear();
        this.applAttrs.clear();
        this.applRels.clear();
        this.applAttrLocalColumnValues = null;

        InputStream in = null;
        try {
            // open XML file
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            in = new BufferedInputStream(new FileInputStream(atfxFile));
            XMLStreamReader reader = inputFactory.createXMLStreamReader(in);

            String baseModelVersion = "";
            AoSession aoSession = null;
            while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.ATFX_FILE))) {

                // parse 'documentation'
                if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION)) {
                    documentation.putAll(parseDocumentation(reader));
                }
                // parse 'base_model_version'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BASE_MODEL_VERSION)) {
                    baseModelVersion = reader.getElementText();
                }
                // parse 'files'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.FILES)) {
                    files.putAll(parseFiles(reader));
                }
                // parse 'application_model'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_MODEL)) {
                    parseApplicationModel(aoSession.getApplicationStructure(), reader);
                }
                // parse 'instance_data'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA)) {
                    parseInstanceElements(aoSession, reader);
                }

                // create AoSession object and write documentation to context
                if ((baseModelVersion.length() > 0) && (aoSession == null)) {
                    aoSession = AoServiceFactory.getInstance().newEmptyAoSession(orb, atfxFile, baseModelVersion);
                }

                reader.nextTag();
            }

            // set context
            for (String docKey : documentation.keySet()) {
                aoSession.setContextString("documentation_" + docKey, documentation.get(docKey));
            }

            LOG.info("Read ATFX in " + (System.currentTimeMillis() - start) + "ms: " + atfxFile.getAbsolutePath());
            return aoSession;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Parse the 'documentation' part of the ATFX file.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the documentation.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseDocumentation(XMLStreamReader reader) throws XMLStreamException {
        reader.nextTag();
        Map<String, String> map = new HashMap<String, String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION))) {
            if (reader.isStartElement()) {
                map.put(reader.getLocalName(), reader.getElementText());
            }
            reader.nextTag();
        }
        return map;
    }

    /***************************************************************************************
     * methods for parsing the component files declaration
     ***************************************************************************************/

    /**
     * Parse the 'files' part of the ATFX file.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the component files.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseFiles(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> map = new HashMap<String, String>();
        // 'files'
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.FILES))) {
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT)) {
                map.putAll(parseComponent(reader));
            }
            reader.next();
        }
        return map;
    }

    /**
     * Parse one 'component' part.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the component files.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseComponent(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> map = new HashMap<String, String>();
        String identifier = "";
        String filename = "";
        // 'component'
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT))) {
            // 'identifier'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER)) {
                identifier = reader.getElementText();
            }
            // 'filename'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT_FILENAME)) {
                filename = reader.getElementText();
            }
            reader.next();
        }
        map.put(identifier, filename);
        return map;
    }

    /***************************************************************************************
     * methods for parsing the application model
     ***************************************************************************************/

    /**
     * Parse the application model.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing application structure.
     */
    private void parseApplicationModel(ApplicationStructure as, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        Map<ApplicationRelation, String> applRelElem2Map = new HashMap<ApplicationRelation, String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_MODEL))) {
            // 'application_enumeration'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM)) {
                parseEnumerationDefinition(as, reader);
            }
            // 'application_element'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM)) {
                applRelElem2Map.putAll(parseApplicationElement(as, reader));
            }
            reader.next();
        }

        // create missing inverse relations
        createMissingInverseRelations(as, applRelElem2Map);

        // set the elem2 of all application relations (this has to be done after parsing all elements)
        for (ApplicationRelation rel : applRelElem2Map.keySet()) {
            rel.setElem2(as.getElementByName(applRelElem2Map.get(rel)));
        }
    }

    /**
     * Creates missing inverse relations.
     * 
     * @param as The application structure.
     * @param applRelElem2Map Map containing the elem2 ae name for the relations.
     * @throws AoException Error creating inverse relations.
     */
    private void createMissingInverseRelations(ApplicationStructure as, Map<ApplicationRelation, String> applRelElem2Map)
            throws AoException {
        for (String elem1Name : this.applRels.keySet()) {
            for (String relName : this.applRels.get(elem1Name).keySet()) {
                ApplicationRelation rel = this.applRels.get(elem1Name).get(relName);
                String elem2Name = applRelElem2Map.get(rel);
                String invRelName = rel.getInverseRelationName();

                ApplicationRelation invRel = this.applRels.get(elem2Name).get(invRelName);
                if (invRel == null) {
                    LOG.warn("Inverse relation for aeName='" + elem1Name + "',relName='" + relName + "',invRelName='"
                            + invRelName + "' not found!");

                    // implicit create inverse relation
                    invRel = as.createRelation();
                    // empty inverse relation name
                    if (invRelName == null || invRelName.length() < 1) {
                        invRelName = elem1Name;
                        rel.setInverseRelationName(invRelName);
                    }
                    invRel.setRelationName(invRelName);
                    invRel.setInverseRelationName(relName);
                    invRel.setElem1(as.getElementByName(elem2Name));
                    invRel.setElem2(as.getElementByName(elem1Name));

                    BaseRelation baseRel = rel.getBaseRelation();
                    if (baseRel != null) {
                        BaseStructure bs = as.getSession().getBaseStructure();
                        BaseRelation invBaseRel = bs.getRelation(baseRel.getElem2(), baseRel.getElem1());
                        invRel.setBaseRelation(invBaseRel);
                    } else {
                        // default relation range (unknown information)
                        invRel.setRelationRange(new RelationRange((short) 0, (short) -1));
                    }

                    // put to maps
                    applRelElem2Map.put(invRel, elem1Name);
                    this.applRels.get(elem2Name).put(invRelName, invRel);
                }
            }
        }
    }

    /**
     * Parse an enumeration definition.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing application structure.
     */
    private void parseEnumerationDefinition(ApplicationStructure as, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, "Expected enumeration name");
        }
        EnumerationDefinition enumDef = as.createEnumerationDefinition(reader.getElementText());
        // items
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM))) {
            // 'item'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM)) {
                parseEnumerationItem(enumDef, reader);
            }
            reader.next();
        }
    }

    /**
     * Parse an enumeration item.
     * 
     * @param enumDef The enumeration definition.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to enumeration definition.
     */
    private void parseEnumerationItem(EnumerationDefinition enumDef, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM))) {
            // 'name'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
                enumDef.addItem(reader.getElementText());
            }
            reader.next();
        }
    }

    /**
     * Parse an application element.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @return Map containing the elem2 ae name for the relations.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private Map<ApplicationRelation, String> parseApplicationElement(ApplicationStructure as, XMLStreamReader reader)
            throws XMLStreamException, AoException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_NAME)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Expected application element 'name'");
        }
        String aeName = reader.getElementText();
        // 'basetype'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_BASETYPE)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Expected application element 'basetype'");
        }
        String basetype = reader.getElementText();

        // create application element
        BaseElement be = as.getSession().getBaseStructure().getElementByType(basetype);
        ApplicationElement applElem = as.createElement(be);
        applElem.setName(aeName);

        // cache base attributes and base relations
        Map<String, BaseAttribute> baseAttrMap = new HashMap<String, BaseAttribute>();
        Map<String, BaseRelation> baseRelMap = new HashMap<String, BaseRelation>();
        for (BaseAttribute baseAttr : applElem.getBaseElement().getAttributes("*")) {
            baseAttrMap.put(baseAttr.getName(), baseAttr);
        }
        for (BaseRelation baseRel : applElem.getBaseElement().getAllRelations()) {
            baseRelMap.put(baseRel.getRelationName(), baseRel);
        }

        // add to global map
        this.applAttrs.put(aeName, new HashMap<String, ApplicationAttribute>());
        this.applRels.put(aeName, new HashMap<String, ApplicationRelation>());

        // attributes and relations
        Map<ApplicationRelation, String> applRelElem2Map = new HashMap<ApplicationRelation, String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM))) {
            // 'application_attribute'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR)) {
                parseApplicationAttribute(applElem, reader, baseAttrMap);
            }
            // 'relation_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL)) {
                applRelElem2Map.putAll(parseApplicationRelation(applElem, reader, baseRelMap));
            }
            reader.next();
        }

        return applRelElem2Map;
    }

    /**
     * Parse an application attribute.
     * 
     * @param applElem The application element.
     * @param reader The XML stream reader.
     * @param baseAttrMap Map containing all base attributes of the application element.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private void parseApplicationAttribute(ApplicationElement applElem, XMLStreamReader reader,
            Map<String, BaseAttribute> baseAttrMap) throws XMLStreamException, AoException {
        String aaNameStr = "";
        String baseAttrStr = "";
        String dataTypeStr = "";
        String lengthStr = "";
        String obligatoryStr = "";
        String uniqueStr = "";
        String autogeneratedStr = "";
        String enumtypeStr = "";
        String unitStr = "";
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR))) {
            // 'name'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_NAME)) {
                aaNameStr = reader.getElementText();
            }
            // 'base_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_BASEATTR)) {
                baseAttrStr = reader.getElementText();
            }
            // 'datatype'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_DATATYPE)) {
                dataTypeStr = reader.getElementText();
            }
            // 'length'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_LENGTH)) {
                lengthStr = reader.getElementText();
            }
            // 'obligatory'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_OBLIGATORY)) {
                obligatoryStr = reader.getElementText();
            }
            // 'unique'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIQUE)) {
                uniqueStr = reader.getElementText();
            }
            // 'autogenerated'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_AUTOGENERATED)) {
                autogeneratedStr = reader.getElementText();
            }
            // 'enumeration_type'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_ENUMTYPE)) {
                enumtypeStr = reader.getElementText();
            }
            // 'unit'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIT)) {
                unitStr = reader.getElementText();
            }
            reader.next();
        }

        // check if base attribute already exists (obligatory base attributes are generated automatically)
        ApplicationAttribute aa = null;
        BaseAttribute baseAttr = null;
        if (baseAttrStr != null && baseAttrStr.length() > 0) {
            baseAttr = baseAttrMap.get(baseAttrStr);
            if (baseAttr == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Base attribute '" + baseAttrStr
                        + "' not found");
            }
            if (baseAttr.isObligatory()) {
                aa = applElem.getAttributeByBaseName(baseAttrStr);
            }
        }
        if (aa == null) {
            aa = applElem.createAttribute();
        }
        aa.setName(aaNameStr);

        // base attribute?
        if (baseAttrStr != null && baseAttrStr.length() > 0) {
            aa.setBaseAttribute(baseAttr);
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
        // unit
        if (unitStr != null && unitStr.length() > 0) {
            aa.setUnit(AtfxParseUtil.parseLongLong(unitStr));
        }

        // add to global map
        this.applAttrs.get(applElem.getName()).put(aaNameStr, aa);
        if (this.applAttrLocalColumnValues == null && baseAttrStr.equals("values")
                && applElem.getBaseElement().getType().equals("AoLocalColumn")) {
            this.applAttrLocalColumnValues = aa;
        }
    }

    /**
     * Parse an application relation.
     * 
     * @param applElem The application element
     * @param reader The XML stream reader.
     * @param baseRelMap Map containing all base relations. of the application element.
     * @return Map containing the elem2 ae name for the relations.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private Map<ApplicationRelation, String> parseApplicationRelation(ApplicationElement applElem,
            XMLStreamReader reader, Map<String, BaseRelation> baseRelMap) throws XMLStreamException, AoException {
        String elem2Name = "";
        String relName = "";
        String inverseRelName = "";
        String brName = "";
        String minStr = "";
        String maxStr = "";
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL))) {
            // 'ref_to'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_REFTO)) {
                elem2Name = reader.getElementText();
            }
            // 'name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_NAME)) {
                relName = reader.getElementText();
            }
            // 'inverse_name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_INVNAME)) {
                inverseRelName = reader.getElementText();
            }
            // 'base_relation'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_BASEREL)) {
                brName = reader.getElementText();
            }
            // 'min_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MIN)) {
                minStr = reader.getElementText();
            }
            // 'max_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MAX)) {
                maxStr = reader.getElementText();
            }
            reader.next();
        }

        ApplicationStructure as = applElem.getApplicationStructure();
        ApplicationRelation rel = as.createRelation();

        rel.setElem1(applElem);
        rel.setRelationName(relName);

        if (minStr.length() > 0 && maxStr.length() > 0) {
            RelationRange relRange = new RelationRange();
            relRange.min = ODSHelper.string2relRange(minStr);
            relRange.max = ODSHelper.string2relRange(maxStr);
            rel.setRelationRange(relRange);
        }

        rel.setInverseRelationName(inverseRelName);
        if (brName != null && brName.length() > 0) {
            BaseRelation baseRel = baseRelMap.get(brName);
            if (baseRel == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "BaseRelation '" + brName
                        + "' not found'");
            }
            rel.setBaseRelation(baseRel);
        }

        // add to global map
        this.applRels.get(applElem.getName()).put(relName, rel);

        // return the information of the ref to application element
        Map<ApplicationRelation, String> applRelElem2Map = new HashMap<ApplicationRelation, String>();
        applRelElem2Map.put(rel, elem2Name);
        return applRelElem2Map;
    }

    /***************************************************************************************
     * methods for parsing instance elements
     ***************************************************************************************/

    /**
     * Read the instance elements from the instance data XML element.
     * <p>
     * Also the relations are parsed and set.
     * 
     * @param aoSession The session.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private void parseInstanceElements(AoSession aoSession, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        ApplicationStructure as = aoSession.getApplicationStructure();
        Map<ElemId, Map<String, T_LONGLONG[]>> relMap = new HashMap<ElemId, Map<String, T_LONGLONG[]>>();

        // parse instances
        reader.next();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA))) {
            if (reader.isStartElement()) {
                relMap.putAll(parseInstanceElement(as, reader));
            }
            reader.next();
        }

        // create relations
        ApplElemAccess applElemAccess = aoSession.getApplElemAccess();
        for (ElemId elemId : relMap.keySet()) {
            for (String relName : relMap.get(elemId).keySet()) {
                T_LONGLONG[] relIids = relMap.get(elemId).get(relName);
                applElemAccess.setRelInst(elemId, relName, relIids, SetType.APPEND);
            }
        }
    }

    /**
     * Read the all attributes,relations and security information from the instance element XML element.
     * 
     * @param as The applications structure.
     * @param reader The XML stream reader.
     * @return Map containing the information about the instance relations (the relation has to be set after all
     *         instances have been created!).
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private Map<ElemId, Map<String, T_LONGLONG[]>> parseInstanceElement(ApplicationStructure as, XMLStreamReader reader)
            throws XMLStreamException, AoException {
        // 'name'
        String aeName = reader.getLocalName();
        ApplicationElement applElem = as.getElementByName(aeName);

        // read attributes
        List<AIDNameValueSeqUnitId> applAttrValues = new ArrayList<AIDNameValueSeqUnitId>();
        List<NameValueUnit> instAttrValues = new ArrayList<NameValueUnit>();
        Map<String, T_LONGLONG[]> instRelMap = new HashMap<String, T_LONGLONG[]>();
        InstanceElement ieExternalComponent = null;

        String currentTagName = null;
        while (!(reader.isEndElement() && reader.getLocalName().equals(aeName) && (currentTagName == null))) {

            // need this 'trick' to indicate whether to parse an instance or application element to know when to end
            if (reader.isEndElement() && currentTagName != null) {
                currentTagName = null;
            }
            reader.next();
            if (reader.isStartElement()) {
                currentTagName = reader.getLocalName();
            }

            // base attribute 'values' of 'LocalColumn'
            if (reader.isStartElement() && isLocalColumnValuesAttr(aeName, reader.getLocalName())) {
                reader.nextTag();
                // external component
                if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT)) {
                    ieExternalComponent = parseLocalColumnComponent(as, reader);
                }
                // explicit values inline XML
                else if (reader.isStartElement()) {
                    TS_Value value = parseLocalColumnValues(this.applAttrLocalColumnValues, reader);
                    AIDNameValueSeqUnitId valuesAttrValue = new AIDNameValueSeqUnitId();
                    valuesAttrValue.unitId = ODSHelper.asODSLongLong(0);
                    valuesAttrValue.attr = new AIDName();
                    valuesAttrValue.attr.aid = applElem.getId();
                    valuesAttrValue.attr.aaName = this.applAttrLocalColumnValues.getName();
                    valuesAttrValue.values = ODSHelper.tsValue2tsValueSeq(value);
                    applAttrValues.add(valuesAttrValue);
                }
            }

            // application attribute
            else if (reader.isStartElement() && (getApplAttr(aeName, reader.getLocalName()) != null)) {
                ApplicationAttribute aa = getApplAttr(aeName, reader.getLocalName());
                AIDNameValueSeqUnitId applAttrValue = new AIDNameValueSeqUnitId();
                applAttrValue.unitId = ODSHelper.asODSLongLong(0);
                applAttrValue.attr = new AIDName();
                applAttrValue.attr.aid = applElem.getId();
                applAttrValue.attr.aaName = reader.getLocalName();
                applAttrValue.values = ODSHelper.tsValue2tsValueSeq(parseAttributeContent(aa, reader));
                applAttrValues.add(applAttrValue);
            }

            // application relation
            else if (reader.isStartElement() && (getApplRel(aeName, reader.getLocalName()) != null)) {
                // only read the non inverse relations for performance reasons!
                ApplicationRelation applRel = getApplRel(aeName, reader.getLocalName());
                short relMax = applRel.getRelationRange().max;
                short invMax = applRel.getInverseRelationRange().max;
                if ((invMax == -1) || (relMax == 1 && invMax == 1)) {
                    String textContent = reader.getElementText();
                    if (textContent.length() > 0) {
                        T_LONGLONG[] relInstIids = AtfxParseUtil.parseLongLongSeq(textContent);
                        instRelMap.put(applRel.getRelationName(), relInstIids);
                    }
                }
            }

            // instance attribute
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR))) {
                instAttrValues = parseInstanceAttributes(reader);
            }

            // ACLA
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.SECURITY_ACLA))) {
            }

            // ACLI
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.SECURITY_ACLI))) {
            }

            // unknown
            else if (reader.isStartElement()) {
                LOG.warn("Unsupported XML tag name: " + reader.getLocalName());
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

        // add external component instance, and set sequence representation to external_component
        if (ieExternalComponent != null) {
            ApplicationElement aeLocalColumn = as.getElementById(elemId.aid);
            ApplicationRelation rel = aeLocalColumn.getRelationsByBaseName("external_component")[0];
            // create relation to external component
            InstanceElement ieLocalColumn = as.getElementById(elemId.aid).getInstanceById(elemId.iid);
            ieLocalColumn.createRelation(rel, ieExternalComponent);
            // alter sequence representation
            String attrSeqRep = aeLocalColumn.getAttributeByBaseName("sequence_representation").getName();
            int seqRep = ODSHelper.getEnumVal(ieLocalColumn.getValue(attrSeqRep));
            if (seqRep == 0) { // explicit
                seqRep = 7; // external_component
            } else if (seqRep == 4) { // raw_linear
                seqRep = 8; // raw_linear_external
            } else if (seqRep == 5) { // raw_polynomial
                seqRep = 9; // raw_polynomial_external
            } else if (seqRep == 10) { // raw_linear_calibrated
                seqRep = 11; // raw_linear_calibrated_external
            }
            ieLocalColumn.setValue(ODSHelper.createEnumNVU(attrSeqRep, seqRep));
        }

        // create relation map
        Map<ElemId, Map<String, T_LONGLONG[]>> retMap = new HashMap<ElemId, Map<String, T_LONGLONG[]>>();
        retMap.put(new ElemId(applElem.getId(), elemId.iid), instRelMap);

        return retMap;
    }

    /**
     * Returns the application attribute object for given application element and attribute name.
     * 
     * @param aeName The application element name.
     * @param name The application attribute name.
     * @return The application attribute, null if no attribute found.
     */
    private ApplicationAttribute getApplAttr(String aeName, String name) {
        Map<String, ApplicationAttribute> attrMap = this.applAttrs.get(aeName);
        if (attrMap != null) {
            return attrMap.get(name);
        }
        return null;
    }

    /**
     * Returns the application relation object for given application element and relation name.
     * 
     * @param aeName The application element name.
     * @param name The relation name.
     * @return The application relation, null if not attribute found.
     */
    private ApplicationRelation getApplRel(String aeName, String name) {
        Map<String, ApplicationRelation> relMap = this.applRels.get(aeName);
        if (relMap != null) {
            return relMap.get(name);
        }
        return null;
    }

    /**
     * Returns whether given attribute name if the attribute 'values' of the local column instance.
     * 
     * @param aeName The application element name.
     * @param name The instance name.
     * @return True, if 'values' attribute.
     * @throws AoException Error checking attribute.
     */
    private boolean isLocalColumnValuesAttr(String aeName, String name) throws AoException {
        if (this.applAttrLocalColumnValues != null) {
            if (this.applAttrLocalColumnValues.getName().equals(name)
                    && this.applAttrLocalColumnValues.getApplicationElement().getName().equals(aeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the 'component' XML element and create an external component instance.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @return The created instance element of base type 'external_component'
     * @throws XMLStreamException Error reading XML.
     * @throws AoException Error creating instance element.
     */
    private InstanceElement parseLocalColumnComponent(ApplicationStructure as, XMLStreamReader reader)
            throws XMLStreamException, AoException {
        EnumerationDefinition typeSpectEnum = as.getEnumerationDefinition("typespec_enum");
        String description = "";
        String fileName = "";
        int dataType = 0;
        int length = 0;
        long inioffset = 0;
        int blockSize = 0;
        int valPerBlock = 0;
        int valOffsets = 0;
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT))) {
            // 'description'
            if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DESCRIPTION))) {
                description = reader.getElementText();
            }
            // 'identifier'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER))) {
                String identifier = reader.getElementText();
                fileName = this.files.get(identifier);
                if (fileName == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "External component file not found for identifier '" + identifier + "'");
                }
            }
            // 'datatype'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DATATYPE))) {
                dataType = typeSpectEnum.getItem(reader.getElementText());
            }
            // 'length'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_LENGTH))) {
                length = AtfxParseUtil.parseLong(reader.getElementText());
            }
            // 'inioffset'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_INIOFFSET))) {
                inioffset = AtfxParseUtil.parseLong(reader.getElementText());
            }
            // 'blocksize'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_BLOCKSIZE))) {
                blockSize = AtfxParseUtil.parseLong(reader.getElementText());
            }
            // 'valperblock'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALPERBLOCK))) {
                valPerBlock = AtfxParseUtil.parseLong(reader.getElementText());
            }
            // 'valoffsets'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALOFFSETS))) {
                valOffsets = AtfxParseUtil.parseLong(reader.getElementText());
            }
            reader.next();
        }

        // create attribute values of external component
        ApplicationElement[] aes = as.getElementsByBaseType("AoExternalComponent");
        if (aes.length != 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "None or multiple application elements of type 'AoExternalComponent' found");
        }
        ApplicationElement aeExtComp = aes[0];

        // collect base attributes and map to application attributes
        Map<String, String> baseAttrMap = new HashMap<String, String>();
        for (ApplicationAttribute aa : aeExtComp.getAttributes("*")) {
            BaseAttribute ba = aa.getBaseAttribute();
            if (ba != null) {
                baseAttrMap.put(ba.getName(), aa.getName());
            }
        }

        // create external component instance
        InstanceElement ieExtComp = aeExtComp.createInstance("ExtComp");
        List<NameValueUnit> attrsList = new ArrayList<NameValueUnit>();
        // mandatory base attribute 'filename_url'
        String attrname = baseAttrMap.get("filename_url");
        attrsList.add(ODSHelper.createStringNVU(attrname, fileName));
        // mandatory base attribute 'value_type'
        attrname = baseAttrMap.get("value_type");
        attrsList.add(ODSHelper.createEnumNVU(attrname, dataType));
        // mandatory base attribute 'component_length'
        attrname = baseAttrMap.get("component_length");
        attrsList.add(ODSHelper.createLongNVU(attrname, length));
        // mandatory base attribute 'start_offset'
        attrname = baseAttrMap.get("start_offset");
        attrsList.add(ODSHelper.createLongLongNVU(attrname, inioffset));
        // mandatory base attribute 'block_size'
        attrname = baseAttrMap.get("block_size");
        attrsList.add(ODSHelper.createLongNVU(attrname, blockSize));
        // mandatory base attribute 'valuesperblock'
        attrname = baseAttrMap.get("valuesperblock");
        attrsList.add(ODSHelper.createLongNVU(attrname, valPerBlock));
        // mandatory base attribute 'value_offset'
        attrname = baseAttrMap.get("value_offset");
        attrsList.add(ODSHelper.createLongNVU(attrname, valOffsets));
        // optional base attribute 'description'
        attrname = baseAttrMap.get("description");
        if (attrname != null && attrname.length() > 0) {
            attrsList.add(ODSHelper.createStringNVU(attrname, description));
        }
        // optional base attribute 'ordinal_number'
        attrname = baseAttrMap.get("ordinal_number");
        if (attrname != null && attrname.length() > 0) {
            attrsList.add(ODSHelper.createLongNVU(attrname, 1));
        }
        ieExtComp.setValueSeq(attrsList.toArray(new NameValueUnit[0]));

        return ieExtComp;
    }

    /**
     * Parse the explicit inline XML mass data from the local column 'values' attribute.
     * 
     * @param aa The application attribute.
     * @param reader The XML stream reader.
     * @return The parsed value.
     * @throws XMLStreamException Error reading XML.
     * @throws AoException Error parsing values.
     */
    private TS_Value parseLocalColumnValues(ApplicationAttribute aa, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        String aaName = aa.getName();
        TS_Value value = new TS_Value();
        value.flag = (short) 15;
        value.u = new TS_Union();
        while (!(reader.isEndElement() && reader.getLocalName().equals(aaName))) {
            // DT_BLOB
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BLOB)) {
                AoSession aoSession = aa.getApplicationElement().getApplicationStructure().getSession();
                value.u.blobVal(parseBlob(aoSession, AtfxTagConstants.VALUES_ATTR_BLOB, reader));
            }
            // DT_BOOLEAN
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BOOLEAN)) {
                value.u.booleanSeq(AtfxParseUtil.parseBooleanSeq(reader.getElementText()));
            }
            // DT_COMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX32)) {
                value.u.complexSeq(AtfxParseUtil.parseComplexSeq(reader.getElementText()));
            }
            // DT_DCOMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX64)) {
                value.u.dcomplexSeq(AtfxParseUtil.parseDComplexSeq(reader.getElementText()));
            }
            // DT_EXTERNALREFERENCE
            else if (reader.isStartElement()
                    && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE)) {
                value.u.extRefSeq(parseExtRefs(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE, reader));
            }
            // DT_BYTE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BYTEFIELD)) {
                value.u.byteSeq(AtfxParseUtil.parseByteSeq(reader.getElementText()));
            }
            // DT_SHORT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT16)) {
                value.u.shortSeq(AtfxParseUtil.parseShortSeq(reader.getElementText()));
            }
            // DT_LONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT32)) {
                value.u.longSeq(AtfxParseUtil.parseLongSeq(reader.getElementText()));
            }
            // DT_LONGLONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT64)) {
                value.u.longlongSeq(AtfxParseUtil.parseLongLongSeq(reader.getElementText()));
            }
            // DT_FLOAT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT32)) {
                value.u.floatSeq(AtfxParseUtil.parseFloatSeq(reader.getElementText()));
            }
            // DT_DOUBLE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT64)) {
                value.u.doubleSeq(AtfxParseUtil.parseDoubleSeq(reader.getElementText()));
            }
            // DT_DATE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_TIMESTRING)) {
                String input = reader.getElementText().trim();
                String[] dateSeq = new String[0];
                if (input.length() > 0) {
                    dateSeq = input.split("\\s+");
                }
                value.u.dateSeq(dateSeq);
            }
            // DT_STRING
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_UTF8STRING)) {
                value.u.stringSeq(parseStringSeq(AtfxTagConstants.VALUES_ATTR_UTF8STRING, reader));
            }
            // not supported
            else if (reader.isStartElement()) {
                throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                      "Unsupported local column 'values' datatype: " + reader.getLocalName());
            }
            reader.nextTag();
        }
        return value;
    }

    /**
     * Parse the instance attributes from the XML stream reader.
     * 
     * @param reader The XML stream reader.
     * @return List of instance attributes.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private List<NameValueUnit> parseInstanceAttributes(XMLStreamReader reader) throws XMLStreamException, AoException {
        List<NameValueUnit> instAttrs = new ArrayList<NameValueUnit>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.INST_ATTR))) {
            reader.next();
            if (reader.isStartElement()) {
                NameValueUnit nvu = new NameValueUnit();
                nvu.unit = "";
                nvu.valName = reader.getAttributeValue(null, AtfxTagConstants.INST_ATTR_NAME);
                nvu.value = new TS_Value();
                nvu.value.u = new TS_Union();
                String textContent = reader.getElementText();
                // DT_STRING
                if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_ASCIISTRING)) {
                    nvu.value.u.stringVal(textContent);
                    nvu.value.flag = (textContent == null || textContent.length() < 1) ? (short) 0 : (short) 15;
                }
                // DT_FLOAT
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_FLOAT32)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.floatVal(AtfxParseUtil.parseFloat(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.floatVal(0);
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_DOUBLE
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_FLOAT64)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.doubleVal(AtfxParseUtil.parseDouble(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.doubleVal(0);
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_BYTE
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_INT8)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.byteVal(AtfxParseUtil.parseByte(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.byteVal((byte) 0);
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_SHORT
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_INT16)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.shortVal(AtfxParseUtil.parseShort(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.shortVal((short) 0);
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_LONG
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_INT32)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.longVal(AtfxParseUtil.parseLong(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.longVal(0);
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_LONGLONG
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_INT64)) {
                    if (textContent.trim().length() > 0) {
                        nvu.value.u.longlongVal(AtfxParseUtil.parseLongLong(textContent));
                        nvu.value.flag = (short) 15;
                    } else {
                        nvu.value.u.longlongVal(ODSHelper.asODSLongLong(0));
                        nvu.value.flag = (short) 0;
                    }
                }
                // DT_DATE
                else if (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR_TIME)) {
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
        }
        return instAttrs;
    }

    /***************************************************************************************
     * methods for parsing attribute values
     ***************************************************************************************/

    /**
     * Parse the content of an application attribute.
     * 
     * @param aa The application attribute.
     * @param reader The XML stream reader.
     * @return The parsed value.
     * @throws XMLStreamException Error reading XML.
     * @throws AoException Error parsing value.
     */
    private TS_Value parseAttributeContent(ApplicationAttribute aa, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        DataType dataType = aa.getDataType();
        TS_Value tsValue = ODSHelper.createEmptyTS_Value(dataType);
        // DT_BLOB
        if (dataType == DataType.DT_BLOB) {
            AoSession aoSession = aa.getApplicationElement().getApplicationStructure().getSession();
            Blob blob = parseBlob(aoSession, aa.getName(), reader);
            tsValue.u.blobVal(blob);
            tsValue.flag = blob.getHeader().length() < 1 && blob.getLength() < 1 ? (short) 0 : 15;
        }
        // DT_BOOLEAN
        else if (dataType == DataType.DT_BOOLEAN) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.booleanVal(AtfxParseUtil.parseBoolean(txt));
                tsValue.flag = 15;
            }
        }
        // DT_BYTE
        else if (dataType == DataType.DT_BYTE) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.byteVal(AtfxParseUtil.parseByte(txt));
                tsValue.flag = 15;
            }
        }
        // DT_BYTESTR
        else if (dataType == DataType.DT_BYTESTR) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.bytestrVal(AtfxParseUtil.parseByteSeq(txt));
                tsValue.flag = tsValue.u.bytestrVal().length > 0 ? 15 : (short) 0;
            }
        }
        // DT_COMPLEX
        else if (dataType == DataType.DT_COMPLEX) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.complexVal(AtfxParseUtil.parseComplex(txt));
                tsValue.flag = 15;
            }
        }
        // DT_DATE
        else if (dataType == DataType.DT_DATE) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.dateVal(txt);
                tsValue.flag = 15;
            }
        }
        // DT_COMPLEX
        else if (dataType == DataType.DT_DCOMPLEX) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.dcomplexVal(AtfxParseUtil.parseDComplex(txt));
                tsValue.flag = 15;
            }
        }
        // DT_DOUBLE
        else if (dataType == DataType.DT_DOUBLE) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.doubleVal(AtfxParseUtil.parseDouble(txt));
                tsValue.flag = 15;
            }
        }
        // DT_ENUM
        else if (dataType == DataType.DT_ENUM) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                EnumerationDefinition ed = aa.getEnumerationDefinition();
                tsValue.u.enumVal(ed.getItem(txt));
                tsValue.flag = 15;
            }
        }
        // DT_EXTERNALREFERENCE
        else if (dataType == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference[] extRefs = parseExtRefs(aa.getName(), reader);
            if (extRefs.length > 1) {
                throw new AoException(ErrorCode.AO_INVALID_LENGTH, SeverityFlag.ERROR, 0,
                                      "Multiple references for datatype DT_EXTERNALREFERENCE found");
            } else if (extRefs.length == 1) {
                tsValue.u.extRefVal(extRefs[0]);
                tsValue.flag = 15;
            }
        }
        // DT_FLOAT
        else if (dataType == DataType.DT_FLOAT) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.floatVal(AtfxParseUtil.parseFloat(txt));
                tsValue.flag = 15;
            }
        }
        // DT_ID
        else if (dataType == DataType.DT_ID) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "DataType 'DT_ID' not supported for application attribute");
        }
        // DT_LONG
        else if (dataType == DataType.DT_LONG) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.longVal(AtfxParseUtil.parseLong(txt));
                tsValue.flag = 15;
            }
        }
        // DT_LONGLONG
        else if (dataType == DataType.DT_LONGLONG) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.longlongVal(AtfxParseUtil.parseLongLong(txt));
                tsValue.flag = 15;
            }
        }
        // DT_SHORT
        else if (dataType == DataType.DT_SHORT) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                tsValue.u.shortVal(AtfxParseUtil.parseShort(txt));
                tsValue.flag = 15;
            }
        }
        // DT_STRING
        else if (dataType == DataType.DT_STRING) {
            String txt = reader.getElementText();
            if (txt.length() > 0) {
                tsValue.u.stringVal(txt);
                tsValue.flag = 15;
            }
        }
        // DS_BOOLEAN
        else if (dataType == DataType.DS_BOOLEAN) {
            boolean[] seq = AtfxParseUtil.parseBooleanSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.booleanSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_BYTE
        else if (dataType == DataType.DS_BYTE) {
            byte[] seq = AtfxParseUtil.parseByteSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.byteSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_BYTESTR
        else if (dataType == DataType.DS_BYTESTR) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "DataType 'DS_BYTESTR' not supported for application attribute");
        }
        // DS_COMPLEX
        else if (dataType == DataType.DS_COMPLEX) {
            T_COMPLEX[] seq = AtfxParseUtil.parseComplexSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.complexSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_DATE
        else if (dataType == DataType.DS_DATE) {
            String[] seq = parseStringSeq(aa.getName(), reader);
            if (seq.length > 0) {
                tsValue.u.dateSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_DCOMPLEX
        else if (dataType == DataType.DS_DCOMPLEX) {
            T_DCOMPLEX[] seq = AtfxParseUtil.parseDComplexSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.dcomplexSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_DOUBLE
        else if (dataType == DataType.DS_DOUBLE) {
            double[] seq = AtfxParseUtil.parseDoubleSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.doubleSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_ENUM
        else if (dataType == DataType.DS_ENUM) {
            String[] seq = parseStringSeq(aa.getName(), reader);
            if (seq.length > 0) {
                EnumerationDefinition ed = aa.getEnumerationDefinition();
                int[] enumItems = new int[seq.length];
                for (int i = 0; i < enumItems.length; i++) {
                    enumItems[i] = ed.getItem(seq[i]);
                }
                tsValue.u.enumSeq(enumItems);
                tsValue.flag = 15;
            }
        }
        // DS_EXTERNALREFERENCE
        else if (dataType == DataType.DS_EXTERNALREFERENCE) {
            T_ExternalReference[] seq = parseExtRefs(aa.getName(), reader);
            if (seq.length > 0) {
                tsValue.u.extRefSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_FLOAT
        else if (dataType == DataType.DS_FLOAT) {
            float[] seq = AtfxParseUtil.parseFloatSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.floatSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_ID
        else if (dataType == DataType.DS_ID) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "DataType 'DS_ID' not supported for application attribute");
        }
        // DS_LONG
        else if (dataType == DataType.DS_LONG) {
            int[] seq = AtfxParseUtil.parseLongSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.longSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_LONGLONG
        else if (dataType == DataType.DS_LONGLONG) {
            T_LONGLONG[] seq = AtfxParseUtil.parseLongLongSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.longlongSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_SHORT
        else if (dataType == DataType.DS_SHORT) {
            short[] seq = AtfxParseUtil.parseShortSeq(reader.getElementText());
            if (seq.length > 0) {
                tsValue.u.shortSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DS_STRING
        else if (dataType == DataType.DS_STRING) {
            String[] seq = parseStringSeq(aa.getName(), reader);
            if (seq.length > 0) {
                tsValue.u.stringSeq(seq);
                tsValue.flag = 15;
            }
        }
        // DT_UNKNOWN: only for the values of a LocalColumn
        else if (dataType == DataType.DT_UNKNOWN) {
        }
        // unsupported data type
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "DataType " + dataType.value()
                    + " not yet implemented");
        }
        return tsValue;
    }

    /**
     * Parse a BLOB object from given application attribute XML element.
     * 
     * @param aoSession The session to create the Blob object.
     * @param attrName The attribute name to know when to stop parsing.
     * @param reader The XML stream reader.
     * @return The Blob.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private Blob parseBlob(AoSession aoSession, String attrName, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        Blob blob = aoSession.createBlob();
        while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
            // 'text'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BLOB_TEXT)) {
                blob.setHeader(reader.getElementText());
            }
            // 'sequence'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BLOB_SEQUENCE)) {
                blob.append(AtfxParseUtil.parseByteSeq(reader.getElementText()));
            }
            reader.next();
        }
        return blob;
    }

    /**
     * Parse an array of T_ExternalReference objects from given XML element.
     * 
     * @param attrName The attribute name to know when to stop parsing.
     * @param reader The XML stream reader.
     * @return The array T_ExternalReference objects.
     * @throws XMLStreamException Error parsing XML.
     */
    private T_ExternalReference[] parseExtRefs(String attrName, XMLStreamReader reader) throws XMLStreamException {
        List<T_ExternalReference> list = new ArrayList<T_ExternalReference>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
            // 'external_reference'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF)) {
                list.add(parseExtRef(reader));
            }
            reader.next();
        }
        return list.toArray(new T_ExternalReference[0]);
    }

    /**
     * Parse an external reference from the external references node.
     * 
     * @param reader The XML stream reader.
     * @return The T_ExternalReference object.
     * @throws XMLStreamException Error parsing XML.
     */
    private T_ExternalReference parseExtRef(XMLStreamReader reader) throws XMLStreamException {
        T_ExternalReference extRef = new T_ExternalReference("", "", "");
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF))) {
            // 'description'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_DESCRIPTION)) {
                extRef.description = reader.getElementText();
            }
            // 'mimetype'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_MIMETYPE)) {
                extRef.mimeType = reader.getElementText();
            }
            // 'location'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_LOCATION)) {
                extRef.location = reader.getElementText();
            }
            reader.next();
        }
        return extRef;
    }

    /**
     * Parse an array of strings objects from given XML element.
     * 
     * @param attrName The attribute name.
     * @param reader The XML stream reader.
     * @return The string sequence.
     * @throws XMLStreamException Error parsing XML.
     */
    private String[] parseStringSeq(String attrName, XMLStreamReader reader) throws XMLStreamException {
        List<String> list = new ArrayList<String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
            // 's'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.STRING_SEQ)) {
                list.add(reader.getElementText());
            }
            reader.next();
        }
        return list.toArray(new String[0]);
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
