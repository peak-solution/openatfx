package com.peaksolution.openatfx.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.io.AtfxTagConstants;
import com.peaksolution.openatfx.util.ODSHelper;

class AtfxReader {
    public static final String UNINITIALIZED = "%UNINITIALIZED%";
    private static final Logger LOG = LoggerFactory.getLogger(AtfxReader.class);
    
    /** cached model information for faster parsing */
    private final Map<String, String> documentation;
    private final Map<String, String> files;
    
    private OpenAtfxAPIImplementation api;
    private AtfxParser atfxInstanceReader;
    
    /**
     * aeName -> arName -> TempRelation
     */
    private Map<String, Map<String, TempRelation>> relationCache;
    
    public AtfxReader(IFileHandler fileHandler, Path atfxPath, boolean isExtendedCompatiblityMode, String configuredExtCompFilenameStartRemoveString) {
        this.documentation = new HashMap<>();
        this.files = new HashMap<>();
        this.relationCache = new HashMap<>();
        this.atfxInstanceReader = new AtfxParser(fileHandler, atfxPath, isExtendedCompatiblityMode, configuredExtCompFilenameStartRemoveString);
    }
    
    /**
     * @param reader
     * @param context
     * @return
     */
    public OpenAtfxAPIImplementation readFile(XMLStreamReader reader, Collection<NameValueUnit> context) {
        long start = System.currentTimeMillis();
        try {
            String baseModelVersion = null;
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
                    parseApplicationModel(reader);
                }
                // parse 'instance_data'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA)) {
                    atfxInstanceReader.parseInstanceElements(api, files, reader);
                }

                // create openATFX API
                if (baseModelVersion != null && (api == null)) {
                    AtfxBaseModel baseModel = BaseModelFactory.getInstance().getBaseModel(baseModelVersion);
                    api = new OpenAtfxAPIImplementation(baseModel);
                    api.init(context);
                }

                reader.nextTag();
            }

            // set context
            for (Entry<String, String> entry : documentation.entrySet()) {
                String docKey = entry.getKey();
                NameValueUnit nvu = new NameValueUnit("documentation_" + docKey, DataType.DT_STRING, documentation.get(docKey));
                api.setContext(nvu);
            }

            LOG.info("Read ATFX in {}ms", System.currentTimeMillis() - start);
            return api;
        } catch (XMLStreamException e) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, e.getMessage());
        } finally {
            this.documentation.clear();
            this.files.clear();
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
        Map<String, String> map = new HashMap<>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION))) {
            if (reader.isStartElement()) {
                try {
                    String tagName = reader.getLocalName();
                    String text = reader.getElementText();
                    map.put(tagName, text);
                    // catch exception to be able to read
                    // CAETEC dataLog having non standard documentation section
                } catch (XMLStreamException e) {
                    LOG.debug("Found corrupt XML tag in documentation section: {}", e.getMessage());
                    continue;
                }
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
        Map<String, String> map = new HashMap<>();
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
        Map<String, String> map = new HashMap<>();
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
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing application structure.
     */
    private void parseApplicationModel(XMLStreamReader reader) throws XMLStreamException {
        long start = System.currentTimeMillis();

        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_MODEL))) {
            // 'application_enumeration'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM)) {
                parseEnumerationDefinition(reader, api);
            }
            // 'application_element'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM)) {
                parseApplicationElement(reader);
            }
            reader.next();
        }

        // implicit create application element of "AoExternalComponent" if missing
        Collection<Element> aes = api.getElementsByBaseType("AoExternalComponent");
        Element aeExtComp = null;
        if (aes.size() == 1) {
            aeExtComp = aes.iterator().next();
        } else if (aes.isEmpty()) {
            aeExtComp = implicitCreateAoExtComp(api);
        } else {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR,
                                        "Multiple application elements of type 'AoExternalComponent' found");
        }
        // implicit create flag file attributes if missing
        if (aeExtComp != null) {
            implicitCreateAoExtCompOptAttrs(api, aeExtComp);
        }

        // create application relations from cached data, because now elements on both sides are definitely available
        for (Entry<String, Map<String, TempRelation>> elementEntry : relationCache.entrySet()) {
            Element element1 = api.getElementByName(elementEntry.getKey());
            if (element1 == null) {
                LOG.warn("Relation(s) '{}' reference(s) element '{}', which could not be found in file!", elementEntry.getValue().keySet(), elementEntry.getKey());
                continue;
            }
            BaseElement baseElement1 = api.getBaseElement(element1.getType());
            
            for (Entry<String, TempRelation> relationEntry : elementEntry.getValue().entrySet()) {
                String relationName = relationEntry.getKey();
                TempRelation tempRelation = relationEntry.getValue();
                
                Element element2 = api.getElementByName(tempRelation.toName);
                if (element2 == null) {
                    LOG.warn("Relation '{}' reference(s) element '{}', which could not be found in file!", relationName, tempRelation.toName);
                    continue;
                }
                BaseRelation baseRelation = baseElement1.getRelationByName(tempRelation.baseRelationName, element2.getType());
                
                short min = checkAndPrepareRelationRange(tempRelation.min, true, relationName, baseRelation == null ? null : baseRelation.getRelationRange());
                short max = checkAndPrepareRelationRange(tempRelation.max, false, relationName, baseRelation == null ? null : baseRelation.getRelationRange());
                
                api.createRelation(element1, element2, baseRelation, relationName, tempRelation.inverseRelationName,
                                   min, max);
            }
        }
        LOG.info("Parsed application model in {}ms", System.currentTimeMillis() - start);
    }
    
    /**
     * Parse an enumeration definition.
     * 
     * @param reader The XML stream reader.
     * @param api
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing application structure.
     */
    private void parseEnumerationDefinition(XMLStreamReader reader, OpenAtfxAPI api) throws XMLStreamException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, "Expected enumeration name");
        }
        String enumName = reader.getElementText();
        api.createEnumeration(enumName);
        // items
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM))) {
            // 'item'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM)) {
                String[] itemPair = parseEnumerationItem(reader);
                api.addEnumerationItem(enumName, Long.parseLong(itemPair[0]), itemPair[1]);
            }
            reader.next();
        }
    }

    /**
     * Parse an enumeration item.
     * 
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @return the parsed enumeration item pair, [0]=value, [1]=name.
     */
    private String[] parseEnumerationItem(XMLStreamReader reader)
            throws XMLStreamException {
        String[] item = new String[2];
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM))) {
            if (reader.isStartElement()) {
                if (reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
                    // 'name'
                    item[1] = reader.getElementText();
                } else if (reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM_VALUE)) {
                    // 'value'
                    item[0] = reader.getElementText();
                }
            }
            reader.next();
        }
        return item;
    }
    
    /**
     * Parse an application element.
     * 
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    private void parseApplicationElement(XMLStreamReader reader) throws XMLStreamException, OpenAtfxException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_NAME)) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, "Expected application element 'name'");
        }
        String aeName = reader.getElementText();
        // 'basetype'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_BASETYPE)) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, "Expected application element 'basetype'");
        }
        String basetype = reader.getElementText();

        // create application element
        Element applElem = api.createElement(basetype, aeName);

        // attributes and relations
        parseApplicationAttributesAndRelations(reader, applElem);
    }
    
    /**
     * @param reader
     * @param applElem
     * @throws XMLStreamException
     */
    private void parseApplicationAttributesAndRelations(XMLStreamReader reader, Element applElem) throws XMLStreamException {
        List<TempAttribute> attributesToAddAfterBaseAttributes = new ArrayList<>();

        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM))) {
            // 'application_attribute'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR)) {
                TempAttribute newAttr = parseApplicationAttribute(reader, applElem);

                // check if base attribute already exists (obligatory base attributes were generated automatically)
                if (newAttr.baseName != null && !newAttr.baseName.isBlank()) {
                    Attribute existingAttribute = applElem.getAttributeByBaseName(newAttr.baseName);
                    if (existingAttribute == null) {
                        // if base attribute does not yet exist, create it first
                        existingAttribute = api.createAttributeFromBaseAttribute(applElem.getId(), newAttr.name, newAttr.baseName);
                    }
                    // then check the changes to standard base attribute made in file and update when allowed
                    updateBaseAttribute(api, applElem.getId(), existingAttribute, newAttr);
                } else {
                    // if current attribute is no base attribute, add it to the list for creation after all base
                    // attributes were created
                    attributesToAddAfterBaseAttributes.add(newAttr);
                }
            }
            // 'relation_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL)) {
                parseApplicationRelation(reader, applElem);
            }

            reader.next();
        }

        // now create all non-base attributes
        for (TempAttribute currentAttr : attributesToAddAfterBaseAttributes) {
            api.createAttribute(applElem.getId(), currentAttr.name, currentAttr.baseName, currentAttr.dataType,
                                currentAttr.length, currentAttr.unitId, currentAttr.enumName, currentAttr.obligatory, currentAttr.unique, false);
        }
    }
    
    /**
     * Parses the next application attribute from the reader and extracts it into a temporary attribute element.
     * 
     * @param reader The XML stream reader.
     * @param applElem The parent application element.
     * @return the parsed attribute.
     * @throws XMLStreamException Error parsing XML.
     */
    private TempAttribute parseApplicationAttribute(XMLStreamReader reader, Element applElem) throws XMLStreamException {
        TempAttribute tempAttr = new TempAttribute();
        DataType dt = null;
        int length = 0;
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR))) {
            // 'name'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_NAME)) {
                tempAttr.name = reader.getElementText();
            }
            // 'base_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_BASEATTR)) {
                tempAttr.baseName = reader.getElementText();
            }
            // 'datatype'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_DATATYPE)) {
                String dataTypeStr = reader.getElementText();
                if (dataTypeStr != null) {
                    dt = DataType.valueOf(dataTypeStr);
                    tempAttr.dataType = dt;
                }
            }
            // 'length'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_LENGTH)) {
                String lengthStr = reader.getElementText();
                if (lengthStr != null && !lengthStr.isBlank()) {
                    length = Integer.parseInt(lengthStr);
                }
            }
            // 'obligatory'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_OBLIGATORY)) {
                String obligatoryStr = reader.getElementText();
                if (obligatoryStr != null && !obligatoryStr.isBlank()) {
                    tempAttr.obligatory = Boolean.parseBoolean(obligatoryStr);
                }
            }
            // 'unique'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIQUE)) {
                String uniqueStr = reader.getElementText();
                if (uniqueStr != null && !uniqueStr.isBlank()) {
                    tempAttr.unique = Boolean.parseBoolean(uniqueStr);
                }
            }
            // 'autogenerated'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_AUTOGENERATE)) {
                String autogeneratedStr = reader.getElementText();
                if (autogeneratedStr != null && !autogeneratedStr.isBlank()) {
                    tempAttr.autogenerated = Boolean.parseBoolean(autogeneratedStr);
                }
            }
            // 'enumeration_type'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_ENUMTYPE)) {
                tempAttr.enumName = reader.getElementText();
            }
            // 'unit'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIT)) {
                String text = reader.getElementText();
                if (text != null) {
                    tempAttr.unitId = Long.parseLong(text);
                }
            }
            reader.next();
        }
        
        if ((DataType.DT_STRING == dt || DataType.DS_STRING == dt) && length == 1) {
            length = 100;
        }
        tempAttr.length = length;
        
        return tempAttr;
    }
    
    private void updateBaseAttribute(OpenAtfxAPI api, long aid, Attribute existingAttribute, TempAttribute tempAttributeFromFile) {
        if (!existingAttribute.getName().equals(tempAttributeFromFile.name)) {
            api.renameAttribute(aid, existingAttribute.getName(), tempAttributeFromFile.name);
        }

        api.updateAttribute(aid, tempAttributeFromFile.name, tempAttributeFromFile.dataType,
                            tempAttributeFromFile.length, tempAttributeFromFile.enumName, tempAttributeFromFile.unitId, tempAttributeFromFile.obligatory,
                            tempAttributeFromFile.unique);
    }
    
    /**
     * Parse an application relation.
     * 
     * @param reader The XML stream reader.
     * @param applElem The application element
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    private void parseApplicationRelation(XMLStreamReader reader, Element applElem) throws XMLStreamException {
        String elem2Name = "";
        String relName = "";
        String inverseRelName = "";
        String brName = "";
        String minStr = "";
        String maxStr = "";
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL))) {
            // 'ref_to'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_REFTO)) {
                elem2Name = reader.getElementText().trim();
            }
            // 'name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_NAME)) {
                relName = reader.getElementText().trim();
            }
            // 'inverse_name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_INVNAME)) {
                inverseRelName = reader.getElementText().trim();
            }
            // 'base_relation'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_BASEREL)) {
                brName = reader.getElementText().trim();
            }
            // 'min_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MIN)) {
                minStr = reader.getElementText().trim();
            }
            // 'max_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MAX)) {
                maxStr = reader.getElementText().trim();
            }
            reader.next();
        }
        
        // handle the mistake in atfx, that the x-axis-for-y-axis or z-axis-for-y-axis are defined as m:n relations
        if (OpenAtfxConstants.BE_SUBMATRIX.equalsIgnoreCase(applElem.getType()) && inverseRelName.startsWith("y-axis-for-")
                && OpenAtfxConstants.REL_MAX_MANY.equalsIgnoreCase(maxStr)) {
            String message = "The relation '" + relName
                    + "' at element '" + applElem
                    + "', defined by the NVH associate standard for ODS, has configured the value '" + maxStr
                    + "' for property 'max_occurs', which is not allowed. It has to be '1'!";
            if (api.isExtendedCompatibilityMode()) {
                maxStr = "1";
                LOG.warn("{} This error was automatically corrected.", message);
            } else {
                throw new OpenAtfxException(ErrorCode.AO_INVALID_BASE_ELEMENT, message);
            }
        }
        
        // prepare relation; may already exist, if previously created by inverse relation creation, in which case the
        // values are now overwritten, assuming that if defined the definition should be more correct than the
        // artificially created relation
        Map<String, TempRelation> cachedRelsForElement = relationCache.get(applElem.getName());
        TempRelation rel = null;
        if (cachedRelsForElement != null) {
            rel = cachedRelsForElement.get(relName);
        }
        if (rel == null) {
            rel = new TempRelation();
        }
        rel.fromName = applElem.getName();
        if (!elem2Name.isBlank()) {
            rel.toName = elem2Name;
        }
        if (!brName.isBlank()) {
            rel.baseRelationName = brName;
        } else if (UNINITIALIZED.equals(rel.baseRelationName)) {
            // make sure any pre-created (inverse) relation looses its placeholder if not a base relation
            rel.baseRelationName = null;
        }
        if (!relName.isBlank()) {
            rel.relationName = relName;
        }
        if (!inverseRelName.isBlank()) {
            rel.inverseRelationName = inverseRelName;
        }
        rel.min = minStr;
        rel.max = maxStr;
        relationCache.computeIfAbsent(applElem.getName(), v -> new HashMap<>()).put(relName, rel);
        
        // prepare inverse relation if not existing, yet
        Map<String, TempRelation> cachedRelsForInverseElement = relationCache.get(elem2Name);
        TempRelation inverseRel = null;
        if (cachedRelsForInverseElement != null) {
            inverseRel = cachedRelsForInverseElement.get(inverseRelName);
        }
        if (inverseRel == null) {
            if (!"".equals(inverseRelName)) {
                // pre-create inverse relation to tolerate common mistake of missing inverse relations in model,
                // note that this is only possible if the inverse relation name is provided at least on this
                // side of the relation!
                inverseRel = new TempRelation();
                inverseRel.fromName = elem2Name;
                inverseRel.toName = applElem.getName();
                inverseRel.baseRelationName = UNINITIALIZED;
                inverseRel.relationName = inverseRelName;
                inverseRel.inverseRelationName = relName;
                inverseRel.min = "-2";
                inverseRel.max = "-2";
                relationCache.computeIfAbsent(elem2Name, v -> new HashMap<>()).put(inverseRelName, inverseRel);
            } else {
                LOG.warn("The inverse_name at relation '{}' of element '{}' is missing!", relName, applElem.getName());
            }
        } else if (inverseRel.inverseRelationName == null || inverseRel.inverseRelationName.isBlank()) {
            inverseRel.inverseRelationName = relName;
        }
    }
    
    /**
     * @param stringValue
     * @param isMinimum
     * @param relationName
     * @param relationRange
     * @return
     */
    private short checkAndPrepareRelationRange(String stringValue, boolean isMinimum, String relationName, RelationRange relationRange) {
        short rangeValue = -2;
        if (!stringValue.isEmpty()) {
            // take from atfx string if available
            rangeValue = ODSHelper.string2relRange(stringValue);
        }
        
        // initialize if defined uninitialized, yet
        if (rangeValue == -2) {
            if (relationRange != null) {
                // initialize range from base relation
                if (isMinimum) {
                    rangeValue = relationRange.min;
                } else {
                    rangeValue = relationRange.max;
                }
            }
            // otherwise initialize with default values
            else if (isMinimum) {
                LOG.warn("Setting default relation range minimum [0] to {}", relationName);
                rangeValue = 0;
            } else {
                LOG.warn("Setting default relation range maximum [-1] to {}", relationName);
                rangeValue = -1;
            }
        }
        // otherwise check the defined relation range against the base relation range
        else {
            // TODO check if that has to lead to an exception or warning in some cases, note that if a change is
            // allowed, the handling in the caller method has to be adjusted as well!
        }
        return rangeValue;
    }
    
    /**
     * Creates implicitly an application element of type "AoExternalComponent" including all relations.
     * 
     * @param api The OpenAtfxAPI.
     * @return The created application element.
     * @throws OpenAtfxException Error creating application element.
     */
    private Element implicitCreateAoExtComp(OpenAtfxAPI api) {
        Collection<Element> aeLCs = api.getElementsByBaseType("AoLocalColumn");
        if (aeLCs.isEmpty()) {
            return null;
        }
        Element aeLC = aeLCs.iterator().next();

        LOG.info("No application element of type 'AoExternalComponent' found, creating artificial element...");

        // create application element, this includes all mandatory base attributes
        Element aeExtComp = api.createElement("AoExternalComponent", "ec");

        // create relation to LocalColumn
        BaseRelation baseRel = api.getBaseRelation(aeExtComp.getType(), aeLC.getType());
        api.createRelationFromBaseRelation(aeExtComp.getId(), aeLC.getId(), baseRel.getName(), "rel_lc", "rel_ec");
        return aeExtComp;
    }
    
    /**
     * Creates implicitly all non mandatory application attributes of type "AoExternalComponent".
     * 
     * @param api The OpenAtfxAPI.
     * @param aeExtComp The ExternalComponent element.
     * @return The created application element.
     * @throws OpenAtfxException Error creating application element.
     */
    private void implicitCreateAoExtCompOptAttrs(OpenAtfxAPI api, Element aeExtComp) {

        // fetch existing attributes
        Set<String> existingBaNames = new HashSet<>();
        for (Attribute aa : aeExtComp.getAttributes()) {
            String baName = aa.getBaseName();
            if (baName != null && !baName.isBlank()) {
                existingBaNames.add(baName);
            }
        }

        String baseModelVersion = api.getBaseModelVersion();
        int baseModelVersionNo = Integer.parseInt(baseModelVersion.replace("asam", ""));
        long aid = aeExtComp.getId();
        
        Map<String, String> baseAttrNames = new HashMap<>();
        baseAttrNames.put("filename_url", "filename_url");
        baseAttrNames.put("flags_filename_url", "flags_filename_url");
        baseAttrNames.put("flags_start_offset", "flags_start_offset");
        baseAttrNames.put("ordinal_number", "ordinal_number");
        baseAttrNames.put("start_offset", "start_offset");
        baseAttrNames.put("block_size", "block_size");
        baseAttrNames.put("valuesperblock", "valuesperblock");
        baseAttrNames.put("value_offset", "value_offset");
        if (baseModelVersionNo > 30) {
            baseAttrNames.put("ao_bit_count", "bitcount");
            baseAttrNames.put("ao_bit_offset", "bitoffset");
        }
        
        for (Entry<String, String> currentBaseAttrNameEntry : baseAttrNames.entrySet()) {
            if (!existingBaNames.contains(currentBaseAttrNameEntry.getKey())) {
                api.createAttributeFromBaseAttribute(aid, currentBaseAttrNameEntry.getValue(), currentBaseAttrNameEntry.getKey());
            }
        }
    }
}
