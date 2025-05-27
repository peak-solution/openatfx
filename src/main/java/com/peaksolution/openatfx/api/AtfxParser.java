package com.peaksolution.openatfx.api;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.ErrorCode;
import org.asam.ods.SetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.io.AtfxParseUtil;
import com.peaksolution.openatfx.io.AtfxTagConstants;
import com.peaksolution.openatfx.util.BufferedRandomAccessFile;
import com.peaksolution.openatfx.util.FileUtil;
import com.peaksolution.openatfx.util.ODSHelper;

import generated.AttrTypesEnum;

class AtfxParser {
    private static final Logger LOG = LoggerFactory.getLogger(AtfxParser.class);

    private final IFileHandler fileHandler;
    private final Path atfxPath;
    private final boolean isExtendedCompatibilityMode;
    private final String configuredExtCompFilenameStartRemoveString;
    
    private Map<Long, Map<Long, Collection<NameValueUnit>>> instanceAttributesByIidByAid = new HashMap<>();
    private String lcValuesAttrName;
    private String lcFlagsAttrName;
    private boolean trimStringValues;

    public AtfxParser(IFileHandler fileHandler, Path atfxPath, boolean isExtendedCompatiblityMode,
            String configuredExtCompFilenameStartRemoveString) {
        this.fileHandler = fileHandler;
        this.atfxPath = atfxPath;
        this.isExtendedCompatibilityMode = isExtendedCompatiblityMode;
        this.configuredExtCompFilenameStartRemoveString = configuredExtCompFilenameStartRemoveString;
    }
    
    /**
     * Read the instance elements from the instance data XML element.
     * <p>
     * Also the relations are parsed and set.
     * 
     * @param aoSession The session.
     * @param api 
     * @param files Map containing component files.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    public void parseInstanceElements(OpenAtfxAPIImplementation api, Map<String, String> files, XMLStreamReader reader)
            throws XMLStreamException, OpenAtfxException {
        long start = System.currentTimeMillis();
        
        NameValueUnit nvu = api.getContext(OpenAtfxConstants.CONTEXT_TRIM_STRING_VALUES);
        if (nvu != null && nvu.hasValidValue()) {
            trimStringValues = Boolean.parseBoolean(nvu.getValue().stringVal());
        }

        // delete 'old' flags file if existing (in case flags are stored as component file)
        File flagsFile = getFlagsTmpFile();
        if (flagsFile.isFile() && flagsFile.exists() && flagsFile.length() > 0 && flagsFile.canWrite()) {
            try {
                Files.delete(flagsFile.toPath());
            } catch (IOException ex) {
                throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, "Unable to delete file '" + flagsFile + "': " + ex.getMessage());
            }
            LOG.info("Deleted existing flag file: {}", flagsFile.getName());
        }

        // parse instances
        reader.next();
        Map<Long, Map<Long, Map<String, Collection<Long>>>> relMap = parseInstElements(api, files, reader);

        LOG.info("Parsed instances in {} ms", System.currentTimeMillis() - start);
        
        // update unit names of instance attributes
        updateInstanceAttrUnits(api);
        
        // create relations
        start = System.currentTimeMillis();
        for (Entry<Long, Map<Long, Map<String, Collection<Long>>>> aidEntry : relMap.entrySet()) {
            for (Entry<Long, Map<String, Collection<Long>>> iidEntry : aidEntry.getValue().entrySet()) {
                for (Entry<String, Collection<Long>> relInstEntry : iidEntry.getValue().entrySet()) {
                    String relName = relInstEntry.getKey();
                    api.setRelatedInstances(aidEntry.getKey(), iidEntry.getKey(), relName, relInstEntry.getValue(), SetType.APPEND);
                }
            }
        }

        LOG.info("Set relations in {} ms", System.currentTimeMillis() - start);
    }
    
    /**
     * For instance attributes, if a unit is set, the value of its "unit" attribute has to be a String containing the
     * iid of the respective unit. Often atfx files incorrectly contain a unit name instead, though. To tolerate that
     * and also mainly to convert the unit iid from the instance attribute in the atfx file to the unit name required to
     * be used in the {@link NameValueUnit} class, this method cares about any adjustments regarding this topic. This
     * has to be done after all instances have been read from file, because it cannot be relied on that unit instances
     * will always be defined before any referencing instance attribute.
     * 
     * @param api 
     * @throws OpenAtfxException
     */
    private void updateInstanceAttrUnits(OpenAtfxAPIImplementation api) {
        Element unitElement = api.getUniqueElementByBaseType("aounit");

//        Map<Long, Instance> unitsByIid = new HashMap<>();
        Collection<String> knownUnitNames = new HashSet<>();
        if (unitElement != null) {
            Collection<Instance> instances = api.getInstances(unitElement.getId());
            for (Instance unit : instances) {
                api.addUnitMapping(unit.getIid(), unit.getName());
//                unitsByIid.put(unit.getIid(), unit);
                knownUnitNames.add(unit.getName());
            }
        }

        for (Element currentElement : api.getElements()) {
            Collection<Instance> instances = new ArrayList<>();
            Collection<Attribute> attrsWithUnit = new ArrayList<>();
            for (Attribute currentAttr : currentElement.getAttributes()) {
                long unitId = currentAttr.getUnitId();
                if (unitId > 0) {
                    attrsWithUnit.add(currentAttr);
                }
            }
            
            if (!attrsWithUnit.isEmpty()) {
                if (instances.isEmpty()) {
                    instances = api.getInstances(currentElement.getId());
                }
                
                for (Instance currentInstance : instances) {
                    for (Attribute currentAttr : attrsWithUnit) {
                        NameValueUnit nvu = currentInstance.getValue(currentAttr.getName());
                        if (nvu != null) {
                            nvu.setUnit(api.getUnitName(currentAttr.getUnitId()));
                        }
                    }
                }
            }
        }
        
        for (Entry<Long, Map<Long, Collection<NameValueUnit>>> elementEntry : instanceAttributesByIidByAid.entrySet()) {
            long aid = elementEntry.getKey();
            Collection<Long> iids = new HashSet<>();
            for (Entry<Long, Collection<NameValueUnit>> instanceEntry : elementEntry.getValue().entrySet()) {
                iids.add(instanceEntry.getKey());
            }
            Collection<Instance> instances = api.getInstances(aid, iids);
            for (Instance instance : instances) {
                Collection<NameValueUnit> instAttrs = elementEntry.getValue().get(instance.getIid());
                for (NameValueUnit nvu : instAttrs) {
                    String unitString = nvu.getUnit();
                    if (unitString == null || unitString.isEmpty()) {
                        continue;
                    }

                    try {
                        if (unitString != null && !unitString.isEmpty()) {
                            long unitId = Long.parseLong(unitString);
                            // the unit id is correctly contained in the source NVU from atfx, change it to the unit
                            // name in the NameValueUnit now
                            String unitName = null;
                            try {
                                unitName = api.getUnitName(unitId);
                            } catch (OpenAtfxException oae) {
                                if (ErrorCode._AO_NOT_FOUND == oae.getError().value()) {
                                    String elementName = api.getElementById(aid).getName();
                                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                          elementName + "instance " + instance.getName()
                                                                  + " references unknown unit with iid " + unitId
                                                                  + " in its instance attribute " + nvu.getValName());
                                }
                            }
                            nvu.setUnit(unitName);
                            api.setInstanceAttributeValue(aid, instance.getIid(), nvu);
                        }
                    } catch (NumberFormatException ex) {
                        // in this case the unit name is specified in the NVU (actually incorrect), just check the unit
                        // name
                        if (!knownUnitNames.contains(nvu.getUnit())) {
                            String elementName = api.getElementById(aid).getName();
                            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                        elementName + " instance " + instance.getName()
                                                                + " references unknown unit with name " + nvu.getUnit()
                                                                + " in its instance attribute " + nvu.getValName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the all attribute values,relations and security information from the instance element XML element.
     * 
     * @param reader The XML stream reader.
     * @param api 
     * @return Map containing the information about the instance relations (the relations have to be set AFTER all
     *         instances have been created!).
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    private Map<Long, Map<Long, Map<String, Collection<Long>>>> parseInstElements (OpenAtfxAPI api,
            Map<String, String> files, XMLStreamReader reader) throws XMLStreamException {
        String lcValsAttrName = getLcValuesAaName(api);
        String lcFlgsAttrName = getLcFlagsAaName(api);
        
        Map<Long, Map<Long, Map<String, Collection<Long>>>> relMap = new HashMap<>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA))) {
            if (reader.isStartElement()) {
                // application element name
                String aeName = reader.getLocalName();
                Element element = api.getElementByName(aeName);
                if (element == null) {
                    throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                                "ApplicationElement '" + aeName + "' not found");
                }
                Long aid = element.getId();
                
                // read attributes
                List<NameValueUnit> applAttrValues = new ArrayList<>();
                List<NameValueUnit> instAttrValues = new ArrayList<>();
                Map<Relation, Collection<Long>> instApplRelMap = new HashMap<>();
                Instance ieExternalComponent = null;

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
                    if (reader.isStartElement() && lcValsAttrName.equals(currentTagName)) {
                        reader.nextTag();
                        // external component
                        if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT)) {
                            if (ieExternalComponent == null) {
                                ieExternalComponent = createExtCompIe(api);
                            }
                            parseLocalColumnValuesComponent(api, ieExternalComponent, files, reader);
                        }
                        // explicit values inline XML
                        else if (reader.isStartElement()) {
                            applAttrValues.add(parseLocalColumnValues(reader, currentTagName));
                        }
                    }

                    // base attribute 'flags' of 'LocalColumn'
                    else if (lcFlgsAttrName != null && reader.isStartElement() && lcFlgsAttrName.equals(currentTagName)) {
                        // try to read flags from inline XML
                        // no other way than trying with exception could be found
                        try {
                            Attribute attribute = element.getAttributeByName(currentTagName);
                            applAttrValues.add(parseAttributeContent(api, aid, currentTagName, attribute.getDataType(),
                                                                     reader));
                        }
                        // flags in external component
                        catch (XMLStreamException e) {
                            if (ieExternalComponent == null) {
                                ieExternalComponent = createExtCompIe(api);
                            }
                            parseLocalColumnFlagsComponent(api,ieExternalComponent, files, reader);
                        }
                    }

                    // application attribute value
                    else if (reader.isStartElement() && (element.getAttributeByName(currentTagName) != null)) {
                        Attribute attribute = element.getAttributeByName(currentTagName);
                        applAttrValues.add(parseAttributeContent(api, aid, currentTagName, attribute.getDataType(),
                                                                 reader));
                    }

                    // application relation
                    else if (reader.isStartElement() && (element.getRelationByName(currentTagName) != null)) {
                        // to tolerate incorrect atfx files (missing inverse relations is very common) the previous
                        // reduction to reading inverse relations (for performance reasons) has been removed in favor of
                        // tolerance to this common atfx generation mistake
                        Relation applRel = element.getRelationByName(currentTagName);
                        String textContent = reader.getElementText();
                        if (textContent.length() > 0) {
                            long[] relInstIids = AtfxParseUtil.parseLongLongSeq(textContent);
                            instApplRelMap.put(applRel, Arrays.stream(relInstIids).boxed().toList());
                        }
                    }

                    // instance attribute
                    else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.INST_ATTR))) {
                        instAttrValues = parseInstanceAttributes(reader);
                    }

                    // ACLA
                    else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.SECURITY_ACLA))) {
                        // consume but ignore
                    }

                    // ACLI
                    else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.SECURITY_ACLI))) {
                        // consume but ignore
                    }

                    // unknown
                    else if (reader.isStartElement()) {
                        LOG.warn("Unsupported XML tag name: {}", reader.getLocalName());
                    }
                }
                
                // fix external component file url if configured
                if (configuredExtCompFilenameStartRemoveString != null && element.getType().equalsIgnoreCase("AoExternalComponent")) {
                    Element extCompAE = api.getElementById(element.getId());
                    fixExtCompFileUrls(configuredExtCompFilenameStartRemoveString, extCompAE, "filename_url", applAttrValues);
                    fixExtCompFileUrls(configuredExtCompFilenameStartRemoveString, extCompAE, "flags_filename_url", applAttrValues);
                }

                // create instance element
                if (applAttrValues.isEmpty()) { // no values
                    return Collections.emptyMap();
                }
                Instance newInstance = api.createInstance(element.getId(), applAttrValues);
                long insertedIid = newInstance.getIid();

                // set instance attributes
                if (!instAttrValues.isEmpty()) {
                    Instance ie = api.getInstanceById(aid, insertedIid);
                    for (NameValueUnit nvu : instAttrValues) {
                        ie.setInstanceValue(nvu);
                    }
                    instanceAttributesByIidByAid.computeIfAbsent(aid, v -> new HashMap<>()).put(insertedIid, instAttrValues);
                }

                // if an external component was created, connect it with local column and set sequence representation to external_component
                if (ieExternalComponent != null) {
                    Relation rel = api.getRelationByBaseName(aid, "external_component");
                    if (rel != null) {
                        api.setRelatedInstances(aid, insertedIid, rel.getRelationName(), Arrays.asList(ieExternalComponent.getIid()), SetType.INSERT);
                    }
                    // alter sequence representation
                    String attrSeqRep = element.getAttributeByBaseName("sequence_representation").getName();
                    int seqRepOrig = newInstance.getValue(attrSeqRep).getValue().enumVal();
                    int seqRep = ODSHelper.seqRepComp2seqRepExtComp(seqRepOrig);
                    newInstance.setAttributeValue(new NameValueUnit(attrSeqRep, DataType.DT_ENUM, seqRep));
                }

                // create relation map
                Map<String, Collection<Long>> instRelMap = relMap.computeIfAbsent(aid,
                                                                                   v -> new HashMap<>())
                                                                  .computeIfAbsent(insertedIid,
                                                                                   v -> new HashMap<>());
                for (Entry<Relation, Collection<Long>> entry : instApplRelMap.entrySet()) {
                    // set/update the relation from this side
                    if (!instRelMap.containsKey(entry.getKey().getRelationName())) {
                        instRelMap.put(entry.getKey().getRelationName(), null);
                    }
                    instRelMap.putAll(handleRelationMapEntry(instRelMap.get(entry.getKey().getRelationName()),
                                                             entry.getKey().getRelationName(), entry.getValue()));
                    // set/update the inverse relation
                    for (Long relIid : entry.getValue()) {
                        Map<String, Collection<Long>> inverseRelMap = relMap.computeIfAbsent(entry.getKey()
                                                                                                  .getElement2()
                                                                                                  .getId(),
                                                                                             v -> new HashMap<>())
                                                                            .computeIfAbsent(relIid,
                                                                                             v -> new HashMap<>());
                        
                        if (!inverseRelMap.containsKey(entry.getKey().getInverseRelationName())) {
                            inverseRelMap.put(entry.getKey().getInverseRelationName(), null);
                        }
                        inverseRelMap.putAll(handleRelationMapEntry(inverseRelMap.get(entry.getKey().getInverseRelationName()),
                                                                    entry.getKey().getInverseRelationName(),
                                                                    Arrays.asList(insertedIid)));
                    }
                }
            }
            reader.next();
        }
        return relMap;
    }

    private Map<String, Collection<Long>> handleRelationMapEntry(Collection<Long> existingIids, String relationName, Collection<Long> newIids) {
        Map<String, Collection<Long>> instRelMap = new HashMap<>();
        if (existingIids != null) {
            Collection<Long> mergedIids = new HashSet<>();
            // add already existing iids
            mergedIids.addAll(existingIids);
            // add additional new iids
            mergedIids.addAll(newIids);
            // set merged iids
            instRelMap.put(relationName, mergedIids);
        } else {
            // if no entry was yet created (by the inverse side) just add the iids for this relation
            instRelMap.put(relationName, newIids);
        }
        return instRelMap;
    }
    
    /**
     * @param api
     * @return
     */
    private String getLcValuesAaName(OpenAtfxAPI api) {
        if (lcValuesAttrName == null) {
            Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
            Attribute valuesAttr = lcElement.getAttributeByBaseName("values");
            if (valuesAttr == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Could not find the 'values' attribute at 'localcolumn' element!");
            }
            lcValuesAttrName = valuesAttr.getName();
        }
        return lcValuesAttrName;
    }
    
    /**
     * @param api
     * @return
     */
    private String getLcFlagsAaName(OpenAtfxAPI api) {
        if (lcFlagsAttrName == null) {
            Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
            Attribute flagsAttr = lcElement.getAttributeByBaseName("flags");
            if (flagsAttr != null) {
                lcFlagsAttrName = flagsAttr.getName();
            }
        }
        return lcFlagsAttrName;
    }
    
    /**
     * Removes a configured String from the beginning of the given external component file attribute if found and
     * updates the respective anvsui in given list. Primarily AVL atfx files come with invalid file urls specified,
     * still including their file symbol. If stripped, the url points to the actual path of that file relative to the
     * atfx file.
     * 
     * @param removeString
     * @param extCompAE
     * @param fileAttrBaseName
     * @param applAttrValues
     * @throws OpenAtfxException
     */
    private void fixExtCompFileUrls(String removeString, Element extCompAE, String fileAttrBaseName,
            List<NameValueUnit> applAttrValues) {
        Attribute filenameUrlAttr = extCompAE.getAttributeByBaseName(fileAttrBaseName);
        String filenameUrlAttrName = filenameUrlAttr.getName();
        for (NameValueUnit nvu : applAttrValues) {
            if (filenameUrlAttrName.equals(nvu.getValName())) {
                String orgUrl = nvu.getValue().stringVal();
                if (orgUrl.startsWith(removeString)) {
                    nvu.getValue().stringVal(orgUrl.substring(removeString.length()));
                }
                break;
            }
        }
    }

    /**
     * Creates an instance of external component.
     * 
     * @param api The api.
     * @return The created instance.
     * @throws OpenAtfxException Error creating instance.
     */
    private Instance createExtCompIe(OpenAtfxAPI api) {
        Element aeExtComp = api.getUniqueElementByBaseType("AoExternalComponent");
        Attribute nameAttr = aeExtComp.getAttributeByBaseName("name");
        return api.createInstance(aeExtComp.getId(), Arrays.asList(new NameValueUnit(nameAttr.getName(), DataType.DT_STRING, "ExtComp")));
    }

    /**
     * Parse the 'component' XML element and fill an external component instance.
     * 
     * @param api
     * @param ieExtComp The external component file instance.
     * @param files Map with component files.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error reading XML.
     * @throws OpenAtfxException Error creating instance element.
     */
    private void parseLocalColumnValuesComponent(OpenAtfxAPI api, Instance ieExtComp, Map<String, String> files,
            XMLStreamReader reader) throws XMLStreamException {
        Element aeExtComp = ieExtComp.getElement();
        String description = "";
        String fileName = "";
        long valueType = 0;
        int length = 0;
        long inioffset = 0;
        int blockSize = 0;
        int valPerBlock = 0;
        int valOffsets = 0;
        Short bitCount = null;
        Short bitOffset = null;

        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT))) {
            // 'description'
            if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DESCRIPTION))) {
                description = reader.getElementText();
            }
            // 'identifier'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER))) {
                String identifier = reader.getElementText();
                fileName = files.get(identifier);
                if (fileName == null) {
                    throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                                "External component file not found for identifier '" + identifier + "'");
                }
            }
            // 'datatype'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DATATYPE))) {
                EnumerationDefinition typespecEnumeration = api.getEnumerationDefinition("typespec_enum");
                valueType = typespecEnumeration.getItem(reader.getElementText());
            }
            // 'length'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_LENGTH))) {
                length = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_LENGTH);
            }
            // 'inioffset'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_INIOFFSET))) {
                inioffset = parseStartOffset(reader.getElementText(), AtfxTagConstants.COMPONENT_INIOFFSET);
            }
            // 'blocksize'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_BLOCKSIZE))) {
                blockSize = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_BLOCKSIZE);
            }
            // 'valperblock'
            else if (reader.isStartElement()
                    && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALPERBLOCK))) {
                valPerBlock = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_VALPERBLOCK);
            }
            // 'valoffsets'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALOFFSETS))) {
                valOffsets = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_VALOFFSETS);
            }
            // 'bitcount'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_BITCOUNT))) {
                bitCount = AtfxParseUtil.parseShort(reader.getElementText());
            }
            // 'bitoffset'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_BITOFFSET))) {
                bitOffset = AtfxParseUtil.parseShort(reader.getElementText());
            }

            reader.next();
        }

        // create external component instance
        List<NameValueUnit> attrsList = new ArrayList<>();

        // mandatory base attribute 'filename_url'
        Attribute applAttr = aeExtComp.getAttributeByBaseName("filename_url");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_STRING, fileName));

        // mandatory base attribute 'value_type'
        applAttr = aeExtComp.getAttributeByBaseName("value_type");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_ENUM, Math.toIntExact(valueType)));

        // mandatory base attribute 'component_length'
        applAttr = aeExtComp.getAttributeByBaseName("component_length");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, length));

        // mandatory base attribute 'start_offset', may be DT_LONG or DT_LONGLONG
        applAttr = aeExtComp.getAttributeByBaseName("start_offset");
        if (DataType.DT_LONG == applAttr.getDataType()) {
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, (int) inioffset));
        } else {
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONGLONG, inioffset));
        }

        // mandatory base attribute 'block_size'
        applAttr = aeExtComp.getAttributeByBaseName("block_size");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, blockSize));

        // mandatory base attribute 'valuesperblock'
        applAttr = aeExtComp.getAttributeByBaseName("valuesperblock");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, valPerBlock));

        // mandatory base attribute 'value_offset'
        applAttr = aeExtComp.getAttributeByBaseName("value_offset");
        attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, valOffsets));

        // optional base attribute 'description'
        applAttr = aeExtComp.getAttributeByBaseName("description");
        if (applAttr != null && !applAttr.getName().isBlank()) {
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_STRING, description));
        }

        // optional base attribute 'ordinal_number'
        applAttr = aeExtComp.getAttributeByBaseName("ordinal_number");
        if (applAttr != null && !applAttr.getName().isBlank()) {
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, 1));
        }

        // optional base attribute 'bitcount'
        applAttr = aeExtComp.getAttributeByBaseName("ao_bit_count");
        if (bitCount != null) {
            if (applAttr == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                            "No application attribute of type 'ao_bit_count' found!");
            }
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_SHORT, bitCount));
        }

        // optional base attribute 'bitoffset'
        applAttr = aeExtComp.getAttributeByBaseName("ao_bit_offset");
        if (bitOffset != null) {
            if (applAttr == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                            "No application attribute of type 'ao_bit_offset' found!");
            }
            attrsList.add(new NameValueUnit(applAttr.getName(), DataType.DT_SHORT, bitOffset));
        }

        ieExtComp.setAttributeValues(attrsList);
    }

    /**
     * Parse the 'component' XML element, read the flags from the component file and append to external flag file.
     * <p/>
     * This operation is necessary because a MixedMode Server may not store flags in component file with block wise
     * persistence (all flags have to be in one block).
     * 
     * @param api The OpenAtfxAPI to use.
     * @param ieExtComp The external component instance.
     * @param files Map with component files.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error reading XML.
     * @throws OpenAtfxException Error creating instance element.
     */
    private void parseLocalColumnFlagsComponent(OpenAtfxAPI api, Instance ieExtComp, Map<String, String> files, XMLStreamReader reader)
            throws XMLStreamException {
        Element aeExtComp = ieExtComp.getElement();
        String fileName = "";
        AttrTypesEnum dataType = AttrTypesEnum.DT_UNKNOWN;
        int length = 0;
        long inioffset = 0;
        int blockSize = 0;
        int valPerBlock = 0;
        int valOffsets = 0;

        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT))) {
            // 'identifier'
            if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER))) {
                String identifier = reader.getElementText();
                fileName = files.get(identifier);
                if (fileName == null) {
                    throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                                "External component file not found for identifier '" + identifier + "'");
                }
            }
            // 'datatype'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DATATYPE))) {
                dataType = AttrTypesEnum.fromValue(reader.getElementText().toUpperCase());
                // only 'dt_short' is currently supported
                if (dataType != AttrTypesEnum.DT_SHORT) {
                    throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                                "Unsupported 'dataType' for flags component file: " + dataType);
                }
            }
            // 'length'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_LENGTH))) {
                length = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_LENGTH);
            }
            // 'inioffset'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_INIOFFSET))) {
                inioffset = parseStartOffset(reader.getElementText(), AtfxTagConstants.COMPONENT_INIOFFSET);
            }
            // 'blocksize'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_BLOCKSIZE))) {
                blockSize = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_BLOCKSIZE);
            }
            // 'valperblock'
            else if (reader.isStartElement()
                    && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALPERBLOCK))) {
                valPerBlock = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_VALPERBLOCK);
            }
            // 'valoffsets'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_VALOFFSETS))) {
                valOffsets = parseFileLength(reader.getElementText(), AtfxTagConstants.COMPONENT_VALOFFSETS);
            }

            reader.next();
        }

        // case 'valperblock' and 'blockSize' has not been found
        if (valPerBlock == 0) {
            valPerBlock = length;
        }
        if (blockSize == 0) {
            blockSize = length * 2;
        }

        // read flags to memory because the MixedMode server may not handle flags in component structure :-(
        long start = System.currentTimeMillis();
        File fileRoot;
        try {
            fileRoot = new File(fileHandler.getFileRoot(atfxPath));
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Error getting file root from fileHandler for: " + atfxPath);
        }
        File componentFile = new File(fileRoot, fileName);
        File flagsFile = getFlagsTmpFile();

        // read values
        try (RandomAccessFile raf = new BufferedRandomAccessFile(componentFile, "r", 32768);
                OutputStream fos = new BufferedOutputStream(new FileOutputStream(flagsFile, true))) {
            raf.seek(inioffset);
            long startOffset = flagsFile.length();

            // initialize source buffer
            ByteBuffer sourceMbb = ByteBuffer.allocate(blockSize);
            sourceMbb.order(ByteOrder.LITTLE_ENDIAN);

            // loop over blocks
            for (int i = 0; i < length; i += valPerBlock) {
                byte[] buffer = new byte[blockSize];
                raf.read(buffer, 0, buffer.length);

                // make buildable with both java8 and java9
                Buffer.class.cast(sourceMbb).clear();
                sourceMbb.put(buffer);
                sourceMbb.position(valOffsets);

                // read flag values and append to flags file
                for (int j = 0; j < valPerBlock; j++) {
                    byte[] b = new byte[2]; // read 2 bytes
                    sourceMbb.get(b);
                    fos.write(b);
                }
            }

            // set external component instance values
            Attribute applAttr = aeExtComp.getAttributeByBaseName("flags_filename_url");
            ieExtComp.setAttributeValue(new NameValueUnit(applAttr.getName(), DataType.DT_STRING, flagsFile.getName()));
            applAttr = aeExtComp.getAttributeByBaseName("flags_start_offset");
            if (DataType.DT_LONG == applAttr.getDataType()) {
                ieExtComp.setAttributeValue(new NameValueUnit(applAttr.getName(), DataType.DT_LONG, (int) startOffset));
            } else {
                ieExtComp.setAttributeValue(new NameValueUnit(applAttr.getName(), DataType.DT_LONGLONG, startOffset));
            }

            LOG.info("Copied {} flags from component file '{}' to external component '{}' in {}ms", length, fileName,
                     flagsFile.getName(), System.currentTimeMillis() - start);
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, e.getMessage());
        }
    }

    private File getFlagsTmpFile() {
        try {
            File fileRoot = new File(fileHandler.getFileRoot(atfxPath));
            File atfxFile = new File(fileHandler.getFileName(atfxPath));
            return new File(fileRoot, FileUtil.stripExtension(atfxFile.getName()) + "_flags_extract.btf");
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Error getting file path from fileHandler: " + atfxPath);
        }
    }

    /**
     * Parse the explicit inline XML mass data from the local column 'values' attribute.
     * 
     * @param reader The XML stream reader.
     * @param lcValsAttrName The name of the 'values' attribute of element 'localcolumn'.
     * @return The parsed value.
     * @throws XMLStreamException Error reading XML.
     * @throws OpenAtfxException Error parsing values.
     */
    private NameValueUnit parseLocalColumnValues(XMLStreamReader reader, String lcValsAttrName) throws XMLStreamException {
        NameValueUnit nvu = new NameValueUnit();
        nvu.setValName(lcValsAttrName);
        SingleValue value = new SingleValue();
        while (!(reader.isEndElement() && reader.getLocalName().equals(lcValsAttrName))) {
            // DS_BLOB
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BLOB)) {
                // TODO implement BLOB handling
                // AoSession aoSession = aa.getApplicationElement().getApplicationStructure().getSession();
                // value.u.blobVal(parseBlob(aoSession, AtfxTagConstants.VALUES_ATTR_BLOB, reader));
            }
            // DS_BOOLEAN
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BOOLEAN)) {
                value.setDiscriminator(DataType.DS_BOOLEAN);
                value.setValue(AtfxParseUtil.parseBooleanSeq(reader.getElementText()));
            }
            // DS_COMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX32)) {
                value.setDiscriminator(DataType.DS_COMPLEX);
                value.setValue(AtfxParseUtil.parseComplexSeq(reader.getElementText()));
            }
            // DS_DCOMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX64)) {
                value.setDiscriminator(DataType.DS_DCOMPLEX);
                value.setValue(AtfxParseUtil.parseDComplexSeq(reader.getElementText()));
            }
            // DS_EXTERNALREFERENCE
            else if (reader.isStartElement()
                    && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE)) {
                value.setDiscriminator(DataType.DS_EXTERNALREFERENCE);
                value.setValue(parseExtRefs(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE, reader));
            }
            // DS_BYTESTR
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BYTEFIELD)) {
                value.setDiscriminator(DataType.DS_BYTESTR);
                value.setValue(parseBytestrSeq(AtfxTagConstants.VALUES_ATTR_BYTEFIELD, reader));
            }
            // support AVL's incorrect Bytestream tag
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BYTESTREAM)) {
                value.setValue(parseBytestrSeq(AtfxTagConstants.VALUES_ATTR_BYTESTREAM, reader));
            }
            // DS_BYTE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT8)) {
                value.setDiscriminator(DataType.DS_BYTE);
                value.setValue(AtfxParseUtil.parseByteSeq(reader.getElementText()));
            }
            // DS_SHORT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT16)) {
                value.setDiscriminator(DataType.DS_SHORT);
                value.setValue(AtfxParseUtil.parseShortSeq(reader.getElementText()));
            }
            // DS_LONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT32)) {
                value.setDiscriminator(DataType.DS_LONG);
                value.setValue(AtfxParseUtil.parseLongSeq(reader.getElementText()));
            }
            // DS_LONGLONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT64)) {
                value.setDiscriminator(DataType.DS_LONGLONG);
                value.setValue(AtfxParseUtil.parseLongLongSeq(reader.getElementText()));
            }
            // DS_FLOAT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT32)) {
                value.setDiscriminator(DataType.DS_FLOAT);
                value.setValue(AtfxParseUtil.parseFloatSeq(reader.getElementText()));
            }
            // DS_DOUBLE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT64)) {
                value.setDiscriminator(DataType.DS_DOUBLE);
                value.setValue(AtfxParseUtil.parseDoubleSeq(reader.getElementText()));
            }
            // DS_DATE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_TIMESTRING)) {
                String input = reader.getElementText().trim();
                String[] dateSeq = new String[0];
                if (input.length() > 0) {
                    dateSeq = input.split("\\s+");
                }
                value.setDiscriminator(DataType.DS_DATE);
                value.setValue(dateSeq);
            }
            // DS_STRING
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_UTF8STRING)) {
                value.setDiscriminator(DataType.DS_STRING);
                value.setValue(parseStringSeq(AtfxTagConstants.VALUES_ATTR_UTF8STRING, reader));
            }
            // not supported
            else if (reader.isStartElement() && !isExtendedCompatibilityMode) {
                throw new OpenAtfxException(ErrorCode.AO_INVALID_DATATYPE,
                                            "Unsupported local column 'values' datatype: " + reader.getLocalName());
            }
            reader.nextTag();
        }
        
        nvu.setValue(value);
        return nvu;
    }

    /**
     * Parse the instance attributes from the XML stream reader.
     * 
     * @param reader The XML stream reader.
     * @return List of instance attributes.
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    private List<NameValueUnit> parseInstanceAttributes(XMLStreamReader reader) throws XMLStreamException {
        List<NameValueUnit> instAttrs = new ArrayList<>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.INST_ATTR))) {
            reader.next();
            if (reader.isStartElement()) {
                NameValueUnit nvu = new NameValueUnit();
                String unit = reader.getAttributeValue(null, AtfxTagConstants.INST_ATTR_UNIT);
                nvu.setUnit(unit == null ? "" : unit);
                nvu.setValName(reader.getAttributeValue(null, AtfxTagConstants.INST_ATTR_NAME));
                
                String localName = reader.getLocalName();
                String textContent = reader.getElementText();
                
                // DT_STRING
                if (AtfxTagConstants.INST_ATTR_ASCIISTRING.equals(localName)) {
                    String textValue = "";
                    if (textContent != null) {
                        textValue = textContent.trim();
                    }
                    
                    nvu.setValue(new SingleValue(DataType.DT_STRING, textValue));
                    nvu.getValue().setFlag((short)15);
                }
                // DT_DATE
                else if (AtfxTagConstants.INST_ATTR_TIME.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_DATE, textContent == null ? "" : textContent.trim()));
                }
                // DT_FLOAT
                else if (AtfxTagConstants.INST_ATTR_FLOAT32.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_FLOAT, textContent.isBlank() ? null : AtfxParseUtil.parseFloat(textContent)));
                }
                // DT_DOUBLE
                else if (AtfxTagConstants.INST_ATTR_FLOAT64.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_DOUBLE, textContent.isBlank() ? null : AtfxParseUtil.parseDouble(textContent)));
                }
                // DT_BYTE
                else if (AtfxTagConstants.INST_ATTR_INT8.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_BYTE, textContent.isBlank() ? null : AtfxParseUtil.parseByte(textContent)));
                }
                // DT_BYTE (unsigned)
                // DT_SHORT
                else if (AtfxTagConstants.INST_ATTR_UINT8.equals(localName)
                        || AtfxTagConstants.INST_ATTR_INT16.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_SHORT, textContent.isBlank() ? null : AtfxParseUtil.parseShort(textContent)));
                }
                // DT_LONG
                else if (AtfxTagConstants.INST_ATTR_INT32.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_LONG, textContent.isBlank() ? null : AtfxParseUtil.parseLong(textContent)));
                }
                // DT_LONGLONG
                else if (AtfxTagConstants.INST_ATTR_INT64.equals(localName)) {
                    nvu.setValue(new SingleValue(DataType.DT_LONGLONG, textContent.isBlank() ? null : AtfxParseUtil.parseLongLong(textContent)));
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
     * @param api The OpenAtfxAPI.
     * @param aid The element id.
     * @param aaName The application attribute name.
     * @param dataType The attribute's DataType.
     * @param reader The XML stream reader.
     * @return The parsed value.
     * @throws XMLStreamException Error reading XML.
     * @throws OpenAtfxException Error parsing value.
     */
    private NameValueUnit parseAttributeContent(OpenAtfxAPI api, Long aid, String aaName, DataType dataType,
            XMLStreamReader reader) throws XMLStreamException {
        NameValueUnit nvu = new NameValueUnit(aaName, dataType, null);
        // DT_BLOB
        if (dataType == DataType.DT_BLOB) {
            Blob blob = parseBlob(aaName, reader);
            nvu.getValue().setValue(blob);
            nvu.getValue().setFlag(blob.getHeader().length() < 1 && blob.getLength() < 1 ? (short) 0 : 15);
        }
        // DT_BOOLEAN
        else if (dataType == DataType.DT_BOOLEAN) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseBoolean(txt));
        }
        // DT_BYTE
        else if (dataType == DataType.DT_BYTE) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseByte(txt));
        }
        // DT_BYTESTR
        else if (dataType == DataType.DT_BYTESTR) {
            byte[] bytes = parseBytestr(aaName, reader);
            nvu.getValue().setValue(bytes);
        }
        // DT_COMPLEX
        else if (dataType == DataType.DT_COMPLEX) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseComplex(txt));
        }
        // DT_STRING
        else if (dataType == DataType.DT_STRING) {
            String txt = reader.getElementText();
            if (trimStringValues) {
                nvu.getValue().setValue(txt.trim());
            } else {
                nvu.getValue().setValue(txt);
            }
        }
        // DT_DATE
        else if (dataType == DataType.DT_DATE) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(txt);
        }
        // DT_COMPLEX
        else if (dataType == DataType.DT_DCOMPLEX) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseDComplex(txt));
        }
        // DT_DOUBLE
        else if (dataType == DataType.DT_DOUBLE) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseDouble(txt));
        }
        // DT_ENUM
        else if (dataType == DataType.DT_ENUM) {
            String txt = reader.getElementText().trim();
            if (txt.length() > 0) {
                EnumerationDefinition enumeration = getEnumFromApi(api, aid, aaName);
                EnumerationDefinition baseEnum = api.getBaseModel().getEnumDef(enumeration.getName());
                long item = enumeration.getItem(txt, baseEnum == null);
                int enumItem = Math.toIntExact(item);
                nvu.getValue().setValue(enumItem);
            }
        }
        // DT_EXTERNALREFERENCE
        else if (dataType == DataType.DT_EXTERNALREFERENCE) {
            ExternalReference[] extRefs = parseExtRefs(aaName, reader);
            if (extRefs.length > 1) {
                throw new OpenAtfxException(ErrorCode.AO_INVALID_LENGTH,
                                            "Multiple references for datatype DT_EXTERNALREFERENCE found");
            } else if (extRefs.length == 1) {
                nvu.getValue().setValue(extRefs[0]);
            }
        }
        // DT_FLOAT
        else if (dataType == DataType.DT_FLOAT) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseFloat(txt));
        }
        // DT_ID
        else if (dataType == DataType.DT_ID) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "DataType 'DT_ID' not supported for application attribute");
        }
        // DT_LONG
        else if (dataType == DataType.DT_LONG) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseLong(txt));
        }
        // DT_LONGLONG
        else if (dataType == DataType.DT_LONGLONG) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseLongLong(txt));
        }
        // DT_SHORT
        else if (dataType == DataType.DT_SHORT) {
            String txt = reader.getElementText().trim();
            nvu.getValue().setValue(AtfxParseUtil.parseShort(txt));
        }
        // DS_BOOLEAN
        else if (dataType == DataType.DS_BOOLEAN) {
            boolean[] seq = AtfxParseUtil.parseBooleanSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_BYTE
        else if (dataType == DataType.DS_BYTE) {
            byte[] seq = AtfxParseUtil.parseByteSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_BYTESTR
        else if (dataType == DataType.DS_BYTESTR) {
            byte[][] seq = parseBytestrSeq(aaName, reader);
            nvu.getValue().setValue(seq);
        }
        // DS_COMPLEX
        else if (dataType == DataType.DS_COMPLEX) {
            Complex[] seq = AtfxParseUtil.parseComplexSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_DATE
        else if (dataType == DataType.DS_DATE) {
            String[] seq = AtfxParseUtil.parseDateSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_DCOMPLEX
        else if (dataType == DataType.DS_DCOMPLEX) {
            DoubleComplex[] seq = AtfxParseUtil.parseDComplexSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_DOUBLE
        else if (dataType == DataType.DS_DOUBLE) {
            double[] seq = AtfxParseUtil.parseDoubleSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_ENUM
        else if (dataType == DataType.DS_ENUM) {
            String[] seq = parseStringSeq(aaName, reader);
            if (seq.length > 0) {
                EnumerationDefinition foundEnum = getEnumFromApi(api, aid, aaName);
                int[] enumItems = new int[seq.length];
                for (int i = 0; i < enumItems.length; i++) {
                    String itemName = seq[i];
                    long item = foundEnum.getItem(itemName);
                    enumItems[i] = Math.toIntExact(item);
                }
                nvu.getValue().setValue(enumItems);
            }
        }
        // DS_EXTERNALREFERENCE
        else if (dataType == DataType.DS_EXTERNALREFERENCE) {
            nvu.getValue().setValue(parseExtRefs(aaName, reader));
        }
        // DS_FLOAT
        else if (dataType == DataType.DS_FLOAT) {
            float[] seq = AtfxParseUtil.parseFloatSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_ID
        else if (dataType == DataType.DS_ID) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "DataType 'DS_ID' not supported for application attribute");
        }
        // DS_LONG
        else if (dataType == DataType.DS_LONG) {
            int[] seq = AtfxParseUtil.parseLongSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_LONGLONG
        else if (dataType == DataType.DS_LONGLONG) {
            long[] seq = AtfxParseUtil.parseLongLongSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_SHORT
        else if (dataType == DataType.DS_SHORT) {
            short[] seq = AtfxParseUtil.parseShortSeq(reader.getElementText());
            nvu.getValue().setValue(seq);
        }
        // DS_STRING
        else if (dataType == DataType.DS_STRING) {
            String[] seq = parseStringSeq(aaName, reader);
            nvu.getValue().setValue(seq);
        }
        // DT_UNKNOWN: only for the values of a LocalColumn
        else if (dataType == DataType.DT_UNKNOWN) {
        }
        // unsupported data type
        else {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "DataType " + dataType + " not yet implemented");
        }
        
        return nvu;
    }
    
    /**
     * @param api
     * @param aid
     * @param aaName
     * @return
     */
    private EnumerationDefinition getEnumFromApi(OpenAtfxAPI api, long aid, String aaName) {
        Element element = api.getElementById(aid);
        Attribute attr = element.getAttributeByName(aaName);
        String enumName = attr.getEnumName();
        if (enumName == null || enumName.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "Did not find any enumName at attribute '" + aaName + "' of " + element
                                                + ", although attribute is of type DT_ENUM!");
        }
        return api.getEnumerationDefinition(enumName);
    }

    /**
     * Parse a BLOB object from given application attribute XML element.
     * 
     * @param attrName The attribute name to know when to stop parsing.
     * @param reader The XML stream reader.
     * @return The Blob.
     * @throws XMLStreamException Error parsing XML.
     * @throws OpenAtfxException Error writing to application model.
     */
    private Blob parseBlob(String attrName, XMLStreamReader reader) throws XMLStreamException {
        Blob blob = new Blob();
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

    private byte[][] parseBytestrSeq(String attrName, XMLStreamReader reader) throws XMLStreamException {
        List<byte[]> list = new ArrayList<>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
            // 'length'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BYTESTR_LENGTH)) {
                Integer.parseInt(reader.getElementText());
            }
            // 'sequence'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BYTESTR_SEQUENCE)) {
                list.add(AtfxParseUtil.parseByteSeq(reader.getElementText()));
            }
            reader.next();
        }
        return list.toArray(new byte[0][0]);
    }

    private byte[] parseBytestr(String attrName, XMLStreamReader reader) throws XMLStreamException {
        byte[] bytes = new byte[0];
        // support "old" method of byte stream
        try {
            bytes = AtfxParseUtil.parseByteSeq(reader.getElementText());
        } catch (Exception e) {
            // new method '<length>4</length><sequence>...'
            while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
                // 'length'
                if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BYTESTR_LENGTH)) {
                }
                // 'sequence'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BYTESTR_SEQUENCE)) {
                    bytes = AtfxParseUtil.parseByteSeq(reader.getElementText());
                }
                reader.next();
            }
        }
        return bytes;
    }

    /**
     * Parse an array of ExternalReference objects from given XML element.
     * 
     * @param attrName The attribute name to know when to stop parsing.
     * @param reader The XML stream reader.
     * @return The array T_ExternalReference objects.
     * @throws XMLStreamException Error parsing XML.
     */
    private ExternalReference[] parseExtRefs(String attrName, XMLStreamReader reader) throws XMLStreamException {
        List<ExternalReference> list = new ArrayList<>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(attrName))) {
            // 'external_reference'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF)) {
                list.add(parseExtRef(reader));
            }
            reader.next();
        }
        return list.toArray(new ExternalReference[0]);
    }

    /**
     * Parse an external reference from the external references node.
     * 
     * @param reader The XML stream reader.
     * @return The ExternalReference object.
     * @throws XMLStreamException Error parsing XML.
     */
    private ExternalReference parseExtRef(XMLStreamReader reader) throws XMLStreamException {
        ExternalReference extRef = new ExternalReference();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF))) {
            // 'description'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_DESCRIPTION)) {
                extRef.setDescription(reader.getElementText());
            }
            // 'mimetype'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_MIMETYPE)) {
                extRef.setMimeType(reader.getElementText());
            }
            // 'location'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.EXTREF_LOCATION)) {
                extRef.setLocation(reader.getElementText());
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
        List<String> list = new ArrayList<>();
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
     * returns the given string length value as long value (parses string to long)
     * 
     * @param lengthValue string length value read from the ATFX file
     * @param attributeName name of the external component attribute
     * @return the parsed long value
     * @throws OpenAtfxException if an error occurs during the parse operation (e.g. lengthValue to parse > Long.MAX_VALUE)
     */
    private long parseStartOffset(String offsetValue, String attributeName) {
        try {
            if (offsetValue == null || offsetValue.trim().length() <= 0) {
                String reason = "empty string not allowed (value of external component with " + "attribute name '"
                        + attributeName + "')";
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, reason);
            }
            return Long.parseLong(offsetValue.trim());

        } catch (NumberFormatException e) {
            double sizeGB = Long.MAX_VALUE / 1073741824.0;
            String reason = "The value '" + offsetValue + "' of the data file specific external "
                    + "component attribute '" + attributeName + "' is not parsable to an long (DT_LONGLONG) value or "
                    + "exceeds the maximal allowed range of '" + Long.MAX_VALUE + "' byte (" + sizeGB + " GB)!";
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, reason);
        }
    }

    /**
     * returns the given string length value as integer value (parses string to integer)
     * 
     * @param lengthValue string length value read from the ATFX file
     * @param attributeName name of the external component attribute
     * @return the parsed integer value
     * @throws OpenAtfxException if an error occurs during the parse operation (e.g. lengthValue to parse > Integer.MAX_VALUE)
     */
    private int parseFileLength(String lengthValue, String attributeName) {

        try {
            if (lengthValue == null || lengthValue.trim().length() <= 0) {
                String reason = "empty string not allowed (value of external component with " + "attribute name '"
                        + attributeName + "')";
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, reason);
            }
            return Integer.parseInt(lengthValue.trim());

        } catch (NumberFormatException e) {
            double sizeGB = Integer.MAX_VALUE / 1073741824.0;
            String reason = "The value '" + lengthValue + "' of the data file specific external "
                    + "component attribute '" + attributeName + "' is not parsable to an integer (DT_LONG) value or "
                    + "exceeds the maximal allowed range of '" + Integer.MAX_VALUE + "' byte (" + sizeGB + " GB)!";
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, reason);
        }
    }
}
