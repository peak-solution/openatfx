package com.peaksolution.openatfx.api;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.asam.ods.Blob;
import org.asam.ods.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.io.AtfxExportUtil;
import com.peaksolution.openatfx.io.AtfxTagConstants;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Object for writing ATFX files.
 * 
 * @author Christian Rechner, Markus Renner
 */
public class AtfxWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AtfxWriter.class);

    /** singleton instance */
    private static AtfxWriter instance;
    
    private boolean trimStringValues;

    /**
     * Non visible constructor.
     */
    private AtfxWriter() {}

    /**
     * Writes the complete content of given aoSession to specified XML file.
     * 
     * @param xmlFile The XML file.
     * @param api The OpenAtfxAPI.
     * @throws OpenAtfxException Error writing XML file.
     */
    public void writeXML(File xmlFile, OpenAtfxAPIImplementation api) {
        long start = System.currentTimeMillis();
        
        NameValueUnit nvu = api.getContext(OpenAtfxConstants.CONTEXT_TRIM_STRING_VALUES);
        if (nvu != null && nvu.hasValidValue()) {
            trimStringValues = nvu.getValue().booleanVal();
        }
        
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter streamWriter = null;
        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(xmlFile))) {
            streamWriter = factory.createXMLStreamWriter(fos, "UTF-8");
            streamWriter.writeStartDocument("UTF-8", "1.0");
            streamWriter.writeStartElement(AtfxTagConstants.ATFX_FILE);
            streamWriter.writeAttribute("version", "atfx_file: V1.3.1");
            streamWriter.writeAttribute("xmlns", getXmlns(api));

            // documentation
            writeDocumentation(streamWriter);

            // base model version
            writeBaseModelVersion(streamWriter, api);

            // files
            boolean writeExtComps = shouldWriteExtComps(api);
            Map<String, String> componentFiles = new HashMap<>();
            if (!writeExtComps) {
                componentFiles.putAll(writeFiles(streamWriter, api));
            }

            // application model
            writeApplicationModel(api, streamWriter);

            // instance data
            if (instancesExists(api)) {
                writeInstanceData(streamWriter, api, writeExtComps, componentFiles);
            }

            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
            
            LOG.info("Wrote XML in {}ms to '{}'", System.currentTimeMillis() - start, xmlFile.getAbsolutePath());
            
            if (shouldIndentXML(api)) {
                // TODO implement pretty printing xml
//                TransformerFactory transformerFactory = TransformerFactory.newInstance();
//                transformerFactory.setAttribute("indent-number", 2);
//                try (InputStream in = new BufferedInputStream(new FileInputStream(xmlFile))) {
//                    Transformer transformer = transformerFactory.newTransformer();
//                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//        //            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
//                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//                    
//                    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
//                
//                
//                    XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
//                    transformer.transform(new StAXSource(rawReader), new StAXResult(streamWriter));
//                } catch (FileNotFoundException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (TransformerConfigurationException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (XMLStreamException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (TransformerException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
            }
        } catch (FileNotFoundException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, e.getMessage());
        } catch (IOException | XMLStreamException e) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, e.getMessage());
        }
    }
    
    /**
     * Creates the XML namespace string based on the session's base model version.
     * 
     * @param api
     * @return
     */
    private String getXmlns(OpenAtfxAPI api) {
        String xmlns = "http://www.asam.net/ODS/";
        String baseModelVersion = api.getBaseModelVersion();
        String version = "5.1.1";
        if ("asam30".equalsIgnoreCase(baseModelVersion)) {
            version = "5.2.0";
        } else if ("asam31".equalsIgnoreCase(baseModelVersion)) {
            version = "5.3.0";
        } else if ("asam32".equalsIgnoreCase(baseModelVersion)) {
            version = "5.3.1";
        } else if ("asam33".equalsIgnoreCase(baseModelVersion)) {
            version = "6.0.0";
        } else if ("asam34".equalsIgnoreCase(baseModelVersion)) {
            version = "6.0.1";
        } else if ("asam35".equalsIgnoreCase(baseModelVersion)) {
            version = "6.1.0";
        } else if ("asam36".equalsIgnoreCase(baseModelVersion)) {
            version = "6.2.0";
        }
        return xmlns + version + "/Schema";
    }

    /**
     * Returns whether to indent the aoSession depending on the value of the context parameter 'INDENT_XML'.
     * 
     * @param api The OpenAtfxAPI.
     * @return Whether to indent the XML.
     */
    private boolean shouldIndentXML(OpenAtfxAPI api) {
        NameValueUnit value = api.getContext("INDENT_XML");
        boolean indent = false;
        if (value.getValue().stringVal().equalsIgnoreCase("TRUE")) {
            indent = true;
        }
        return indent;
    }

    /**
     * Returns whether external component instances instead of component files should be written (if possible).
     * 
     * @param api The OpenAtfxAPI.
     * @return Whether to write external component instances.
     */
    private boolean shouldWriteExtComps(OpenAtfxAPI api) {
        NameValueUnit value = api.getContext("WRITE_EXTERNALCOMPONENTS");
        boolean writeExtComps = false;
        if (value.getValue().stringVal().equalsIgnoreCase("TRUE")) {
            writeExtComps = true;
        }
        return writeExtComps;
    }

    /**
     * Returns whether a single instance exists in given session.
     * 
     * @param api The OpenAtfxAPI.
     * @return True if a single instance exists.
     */
    private boolean instancesExists(OpenAtfxAPI api) {
        for (Element ae : api.getElements()) {
            Collection<Instance> instances = api.getInstances(ae.getId());
            if (!instances.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the documentation part of the ATFX.
     * 
     * @param streamWriter The XML stream writer.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeDocumentation(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.DOCUMENTATION);
        writeElement(streamWriter, AtfxTagConstants.EXPORTED_BY, "openATFX");
        writeElement(streamWriter, AtfxTagConstants.EXPORTER, "openATFX");
        writeElement(streamWriter, AtfxTagConstants.EXPORT_DATETIME, ODSHelper.getCurrentODSDate());
        writeElement(streamWriter, AtfxTagConstants.EXPORTER_VERSION, getClass().getPackage().getImplementationVersion());
        streamWriter.writeEndElement();
    }

    /**
     * Writes the base model version to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param api The OpenAtfxAPI.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeBaseModelVersion(XMLStreamWriter streamWriter, OpenAtfxAPI api) throws XMLStreamException {
        writeElement(streamWriter, AtfxTagConstants.BASE_MODEL_VERSION, api.getBaseModelVersion());
    }

    /**
     * Writes the component files section to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param api The OpenAtfxAPI.
     * @return Map of component files.
     * @throws XMLStreamException Error writing XML file.
     */
    private Map<String, String> writeFiles(XMLStreamWriter streamWriter, OpenAtfxAPI api)
            throws XMLStreamException {
        Collection<Element> aes = api.getElementsByBaseType("AoExternalComponent");
        if (aes.isEmpty()) {
            return Collections.emptyMap();
        }
        Element aeExtComp = aes.iterator().next();

        // collect value of 'filename_url' and 'flags_filename_url'
        Map<String, String> map = new HashMap<>();
        Collection<Instance> instances = api.getInstances(aeExtComp.getId());
        int componentCount = 1;
        for (Instance ieExtComp : instances) {
            // filename_url
            String filenameUrl = ieExtComp.getValueByBaseName("filename_url").getValue().stringVal();
            if (!map.containsKey(filenameUrl)) {
                map.put(filenameUrl, "component_" + componentCount);
                componentCount++;
            }
            // flags_filename_url
            if (ieExtComp.hasValidValue(null, "flags_filename_url")) {
                NameValueUnit flagsFileNvu = ieExtComp.getValueByBaseName("flags_filename_url");
                if (flagsFileNvu != null) {
                    String flagsFilenameUrl = flagsFileNvu.getValue().stringVal();
                    if ((flagsFilenameUrl != null) && (flagsFilenameUrl.length() > 0) && !map.containsKey(flagsFilenameUrl)) {
                        map.put(flagsFilenameUrl, "component_" + componentCount);
                        componentCount++;
                    }
                }
            }
        }

        if (map.isEmpty()) {
            return Collections.emptyMap();
        }

        // write to XML
        streamWriter.writeStartElement(AtfxTagConstants.FILES);
        for (Entry<String, String> entry : map.entrySet()) {
            streamWriter.writeStartElement(AtfxTagConstants.COMPONENT);
            writeElement(streamWriter, AtfxTagConstants.COMPONENT_IDENTIFIER, entry.getValue());
            writeElement(streamWriter, AtfxTagConstants.COMPONENT_FILENAME, entry.getKey());
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();

        return map;
    }

    /***************************************************************************************
     * methods for writing the application model
     ***************************************************************************************/

    /**
     * Writes the application model to the XML stream.
     * 
     * @param api The OpenAtfxAPI.
     * @param streamWriter The XML stream writer.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading application model.
     */
    private void writeApplicationModel(OpenAtfxAPIImplementation api, XMLStreamWriter streamWriter)
            throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_MODEL);

        // enumerations
        for (String enumName : api.listEnumerationNames(false)) {
            writeEnumerationStructure(streamWriter, api.getEnumerationDefinition(enumName));
        }

        // application elements
        for (Element applElem : api.getElements()) {
            writeApplElem(api, streamWriter, applElem);
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes an application enumeration definition to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param es The enumeration definition.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeEnumerationStructure(XMLStreamWriter streamWriter, EnumerationDefinition es)
            throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ENUM);
        writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_NAME, es.getName());
        for (String itemName : es.listItemNames()) {
            streamWriter.writeStartElement(AtfxTagConstants.APPL_ENUM_ITEM);
            writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_ITEM_NAME, itemName);
            writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_ITEM_VALUE, String.valueOf(es.getItem(itemName)));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    /**
     * Writes an application element to the XML stream.
     * 
     * @param api The OpenAtfxAPI.
     * @param streamWriter The XML stream writer.
     * @param applElem The application element structure.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading application model.
     */
    private void writeApplElem(OpenAtfxAPIImplementation api, XMLStreamWriter streamWriter, Element applElem) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ELEM);
        writeElement(streamWriter, AtfxTagConstants.APPL_ELEM_NAME, applElem.getName());
        writeElement(streamWriter, AtfxTagConstants.APPL_ELEM_BASETYPE, applElem.getType());

        // application attributes
        for (Attribute applAttr : applElem.getAttributes()) {
            writeApplAttr(streamWriter, applAttr);
        }

        // applicaton relations
        for (Relation applRel : applElem.getRelations()) {
            writeApplRel(streamWriter, applRel);
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes an application attribute to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param applAttr The application attribute.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading application structure values.
     */
    private void writeApplAttr(XMLStreamWriter streamWriter, Attribute applAttr) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ATTR);
        // name
        writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_NAME, applAttr.getName());
        // base attr / datatype
        String baName = applAttr.getBaseName();
        if (baName == null) {
            baName = "";
        }
        DataType dt = applAttr.getDataType();
        if (baName.length() > 0) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_BASEATTR, baName);
            // write datatype for attributes of 'AoExternalComponent', because these can be DT_LONG or DT_LONGLONG
            if (baName.equals("start_offset") || baName.equals("flags_start_offset")) {
                writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_DATATYPE, dt.toString());
            }

        } else {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_DATATYPE, dt.toString());
        }
        // enumeration
        if (baName.isBlank() && (dt == DataType.DT_ENUM || dt == DataType.DS_ENUM)) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_ENUMTYPE, applAttr.getEnumName());
        }
        // autogenerated
        if (applAttr.isAutogenerated()) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_AUTOGENERATE, "true");
        }
        // obligatory
        if (applAttr.isObligatory()) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_OBLIGATORY, "true");
        }
        // unique
        if (!baName.equalsIgnoreCase("id") && applAttr.isUnique()) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_UNIQUE, String.valueOf(applAttr.isUnique()));
        }
        // length
        writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_LENGTH, String.valueOf(applAttr.getLength()));
        // unit
        long unitId = applAttr.getUnitId();
        if (unitId > 0) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_UNIT, String.valueOf(unitId));
        }
        streamWriter.writeEndElement();
    }

    /**
     * Writes an application relation to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param applRel The application relation.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeApplRel(XMLStreamWriter streamWriter, Relation applRel)
            throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_REL);
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_NAME, applRel.getRelationName());
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_REFTO, applRel.getElement2().getName());
        if (applRel.getBaseRelation() != null) {
            writeElement(streamWriter, AtfxTagConstants.APPL_REL_BASEREL, applRel.getBaseRelation().getName());
        }
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_MIN,
                     ODSHelper.relRange2string(applRel.getRelationRangeMin()));
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_MAX,
                     ODSHelper.relRange2string(applRel.getRelationRangeMax()));
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_INVNAME, applRel.getInverseRelationName());
        streamWriter.writeEndElement();
    }

    /***************************************************************************************
     * methods for writing the instance data
     ***************************************************************************************/

    /**
     * Writes the instance data to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param api The OpenAtfxAPI.
     * @param writeExtComps Whether to write external component instances.
     * @param componentFiles The component files.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeInstanceData(XMLStreamWriter streamWriter, OpenAtfxAPIImplementation api, boolean writeExtComps,
            Map<String, String> componentFiles) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.INSTANCE_DATA);

        // iterate over all application elements/instance elements
        for (Element ae : api.getElements()) {
            // skip instances of 'AoExternalComponent', they have to be handled specially
            if (ae.getType().equalsIgnoreCase("AoExternalComponent" )) {
                continue;
            }
            Collection<Instance> instances = api.getInstances(ae.getId());
            for (Instance ie : instances) {
                writeInstanceElement(streamWriter, api, ie, writeExtComps, componentFiles);
            }
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an instance element to XML.
     * 
     * @param streamWriter The XML stream writer.
     * @param api The OpenAtfxAPI.
     * @param ie The instance element.
     * @param writeExtComps Whether to write external component instances.
     * @param componentFiles Map of component files.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeInstanceElement(XMLStreamWriter streamWriter, OpenAtfxAPIImplementation api, Instance ie,
            boolean writeExtComps, Map<String, String> componentFiles) throws XMLStreamException {
        streamWriter.writeStartElement(ie.getElementName());

        // write application attribute data
        Collection<Attribute> attributes = ie.getElement().getAttributes();

        long aid = ie.getAid();
        
        // special handling: LocalColumn; do not write external component values
        Collection<Instance> externalComponentChildren = null; // is not null if ApplElem=AoLocalColumn
        if ("AoLocalColumn".equalsIgnoreCase(ie.getElement().getType())) {
            // remove attribute of base type 'values', 'flags' and 'sequence_representation' from attribute list
            Set<String> baseAttrNamesToRemove = new HashSet<>(Arrays.asList("values", "flags", "sequence_representation"));
            List<Attribute> attrsToRemove = new ArrayList<>();
            for (Attribute currentAttr : attributes) {
                if (baseAttrNamesToRemove.contains(currentAttr.getBaseName())) {
                    attrsToRemove.add(currentAttr);
                }
            }
            attributes.removeAll(attrsToRemove);
            
            // attrNames sequence representation
            Attribute seqRepAttr = ie.getElement().getAttributeByBaseName("sequence_representation");
            NameValueUnit seqRepNvu = ie.getValueByBaseName("sequence_representation");
            int seqRepEnum = seqRepNvu.getValue().enumVal();
            // check if the sequence representation is 7(external_component), 8(raw_linear_external),
            // 9(raw_polynomial_external), 11(raw_linear_calibrated_external) or 13(raw_rational_external)
            if (seqRepEnum == 7 || seqRepEnum == 8 || seqRepEnum == 9 || seqRepEnum == 11 || seqRepEnum == 13) {
                externalComponentChildren = api.getChildren(aid, ie.getIid());

                // write 'components'
                if (!writeExtComps && (externalComponentChildren.size() == 1)) {
                    Instance extCompInstance = externalComponentChildren.iterator().next();
                    writeLCValuesComponent(api, streamWriter, ie, extCompInstance, componentFiles); // values
                    writeLCFlagsComponent(api, streamWriter, ie, extCompInstance, componentFiles); // flags
                    seqRepEnum = ODSHelper.seqRepExtComp2seqRepComp(seqRepEnum);
                    writeApplAttrValue(api, streamWriter, seqRepAttr,
                                       new NameValueUnit(seqRepAttr.getName(), DataType.DT_ENUM, seqRepEnum)); // sequence_representation
                }
                // write 'AoExternalComponent' instances
                else {
                    writeApplAttrValue(api, streamWriter, seqRepAttr, seqRepNvu);
                }

            } else { // write values to XML (inline)
                writeApplAttrValue(api, streamWriter, seqRepAttr, seqRepNvu);
                
                Attribute flagsAttr = ie.getElement().getAttributeByBaseName("flags");
                if (flagsAttr != null && ie.hasValidValue(null, "flags")) {
                    writeApplAttrValue(api, streamWriter, flagsAttr, ie.getValueByBaseName("flags"));
                }
                
                writeLocalColumnValues(streamWriter, ie.getValueByBaseName("values"));
            }
        }

        // write attributes if not null
        for (Attribute currentAttr : attributes) {
            writeApplAttrValue(api, streamWriter, currentAttr, ie.getValue(currentAttr.getName()));
        }

        // write instance attribute data
        Collection<String> instAttrNames = ie.listInstanceAttributes();
        if (!instAttrNames.isEmpty()) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR);
            for (String instAttrName : instAttrNames) {
                NameValueUnit nvu = ie.getValue(instAttrName);
                writeInstAttrValue(api, streamWriter, nvu);
            }
            streamWriter.writeEndElement();
        }

        // write relations
        for (Relation applRel : ie.getElement().getRelations()) {
            // skip reference to 'AoExternalComponent'
            if (applRel.getBaseRelation() != null
                    && applRel.getBaseRelation().getName().equalsIgnoreCase("external_component") && !writeExtComps
                    && (externalComponentChildren != null) && (externalComponentChildren.size() == 1)) {
                continue;
            }
            Collection<Long> relatedIids = api.getRelatedInstanceIds(aid, ie.getIid(), applRel);
            if (!relatedIids.isEmpty()) {
                String iidsString = relatedIids.stream().map(String::valueOf).collect(Collectors.joining(" "));
                writeElement(streamWriter, applRel.getRelationName(), iidsString);
            }
        }

        streamWriter.writeEndElement();

        // write external component instances if necessary
        if ((externalComponentChildren != null) && (writeExtComps || externalComponentChildren.size() > 1)) {
            for (Instance ieExtComp : externalComponentChildren) {
                writeInstanceElement(streamWriter, api, ieExtComp, writeExtComps, componentFiles);
            }
        }
    }

    private void writeLCValuesComponent(OpenAtfxAPI api, XMLStreamWriter streamWriter, Instance ieLocalColumn, Instance ieExtComp,
            Map<String, String> componentFiles) throws XMLStreamException {
        Attribute valuesAttr = ieLocalColumn.getElement().getAttributeByBaseName("values");
        streamWriter.writeStartElement(valuesAttr.getName());
        streamWriter.writeStartElement(AtfxTagConstants.COMPONENT);

        // identifier
        String filenameUrl = ieExtComp.getValueByBaseName("filename_url").getValue().stringVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_IDENTIFIER, componentFiles.get(filenameUrl));

        // datatype
        int dt = ieExtComp.getValueByBaseName("value_type").getValue().enumVal();
        String itemName = api.getEnumerationItemName("typespec_enum", dt);
        if (itemName == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Not item " + dt + " found at enumeration typespec_enum!");
        }
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_DATATYPE, itemName);

        // length
        int componentLength = ieExtComp.getValueByBaseName("component_length").getValue().longVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_LENGTH, String.valueOf(componentLength));

        // inioffset, may be DT_LONG or DT_LONGLONG
        long startOffset = 0;
        NameValueUnit nvuStartOffset = ieExtComp.getValueByBaseName("start_offset");
        if (nvuStartOffset.getValue().discriminator() == DataType.DT_LONG) {
            startOffset = nvuStartOffset.getValue().longVal();
        } else if (nvuStartOffset.getValue().discriminator() == DataType.DT_LONGLONG) {
            startOffset = nvuStartOffset.getValue().longlongVal();
        }
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_INIOFFSET, String.valueOf(startOffset));

        // length
        int blockSize = ieExtComp.getValueByBaseName("block_size").getValue().longVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_BLOCKSIZE, String.valueOf(blockSize));

        // valuesperblock
        int valuesperblock = ieExtComp.getValueByBaseName("valuesperblock").getValue().longVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_VALPERBLOCK, String.valueOf(valuesperblock));

        // valuesperblock
        int valueOffset = ieExtComp.getValueByBaseName("value_offset").getValue().longVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_VALOFFSETS, String.valueOf(valueOffset));

        // bitcount
        NameValueUnit bitCountNvu = ieExtComp.getValueByBaseName("ao_bit_count");
        if (bitCountNvu != null && bitCountNvu.hasValidValue()) {
            writeElement(streamWriter, AtfxTagConstants.COMPONENT_BITCOUNT,
                         String.valueOf(bitCountNvu.getValue().shortVal()));
        }

        // bitoffset
        NameValueUnit bitOffsetNvu = ieExtComp.getValueByBaseName("ao_bit_offset");
        if (bitOffsetNvu != null && bitOffsetNvu.hasValidValue()) {
            writeElement(streamWriter, AtfxTagConstants.COMPONENT_BITOFFSET,
                         String.valueOf(bitOffsetNvu.getValue().shortVal()));
        }

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private void writeLCFlagsComponent(OpenAtfxAPI api, XMLStreamWriter streamWriter,
            Instance ieLocalColumn, Instance ieExtComp, Map<String, String> componentFiles) throws XMLStreamException {
        // check if flagsFileNameUrl is set
        String filenameUrl = ieExtComp.getValueByBaseName("flags_filename_url").getValue().stringVal();
        if (filenameUrl != null && filenameUrl.length() < 1) {
            return;
        }

        Attribute flagsAttr = ieLocalColumn.getElement().getAttributeByBaseName("flags");
        streamWriter.writeStartElement(flagsAttr.getName());
        streamWriter.writeStartElement(AtfxTagConstants.COMPONENT);

        // identifier
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_IDENTIFIER, componentFiles.get(filenameUrl));

        // datatype
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_DATATYPE, "dt_short");

        // length
        int componentLength = ieExtComp.getValueByBaseName("component_length").getValue().longVal();
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_LENGTH, String.valueOf(componentLength));

        // inioffset, may be DT_LONG or DT_LONGLONG
        long startOffset = 0;
        NameValueUnit nvuStartOffset = ieExtComp.getValueByBaseName("flags_start_offset");
        if (nvuStartOffset.getValue().discriminator() == DataType.DT_LONG) {
            startOffset = nvuStartOffset.getValue().longVal();
        } else if (nvuStartOffset.getValue().discriminator() == DataType.DT_LONGLONG) {
            startOffset = nvuStartOffset.getValue().longlongVal();
        }
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_INIOFFSET, String.valueOf(startOffset));

        // valueperblock = length
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_VALPERBLOCK, String.valueOf(componentLength));

        // blocksize=length * 2
        long blockSize = componentLength * 2;
        writeElement(streamWriter, AtfxTagConstants.COMPONENT_BLOCKSIZE, String.valueOf(blockSize));

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    /**
     * Writes the value of an instance attribute to the XML stream.
     * 
     * @param api The OpenAtfxAPIImplementation.
     * @param streamWriter The XML stream writer.
     * @param instAttrValue The instance attribute value.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Invalid attribute data type.
     */
    private void writeInstAttrValue(OpenAtfxAPIImplementation api, XMLStreamWriter streamWriter, NameValueUnit instAttrValue)
            throws XMLStreamException {
        DataType dataType = instAttrValue.getValue().discriminator();
        String attrName = instAttrValue.getValName();
        SingleValue value = instAttrValue.getValue();
        String unitIdString = "";
        String unitString = instAttrValue.getUnit();
        if (unitString != null && !unitString.isEmpty()) {
            long unitId = api.getUnitId(unitString);
            unitIdString = String.valueOf(unitId);
        }
        
        // DT_STRING
        if (dataType == DataType.DT_STRING) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_ASCIISTRING);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(value.stringVal());
            }
            streamWriter.writeEndElement();
        }
        // DT_FLOAT
        else if (dataType == DataType.DT_FLOAT) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_FLOAT32);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (unitIdString != null && !unitIdString.equals("")) {
                streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_UNIT, unitIdString);
            }
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createFloatString(value.floatVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_DOUBLE
        else if (dataType == DataType.DT_DOUBLE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_FLOAT64);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (unitIdString != null && !unitIdString.equals("")) {
                streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_UNIT, unitIdString);
            }
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createDoubleString(value.doubleVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_BYTE
        else if (dataType == DataType.DT_BYTE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT8);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createByteString(value.byteVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_SHORT
        else if (dataType == DataType.DT_SHORT) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT16);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (unitIdString != null && !unitIdString.equals("")) {
                streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_UNIT, unitIdString);
            }
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createShortString(value.shortVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_LONG
        else if (dataType == DataType.DT_LONG) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT32);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (unitIdString != null && !unitIdString.equals("")) {
                streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_UNIT, unitIdString);
            }
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createLongString(value.longVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_LONGLONG
        else if (dataType == DataType.DT_LONGLONG) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT64);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (unitIdString != null && !unitIdString.equals("")) {
                streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_UNIT, unitIdString);
            }
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(AtfxExportUtil.createLongLongString(value.longlongVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_DATE
        else if (dataType == DataType.DT_DATE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_TIME);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.hasValidValue()) {
                streamWriter.writeCharacters(value.dateVal());
            }
            streamWriter.writeEndElement();
        }
        // unsupported data type
        else {
            String msg = "DataType '" + dataType + "' is not allowed for instance attributes";
            LOG.error(msg);
            throw new OpenAtfxException(ErrorCode.AO_INVALID_DATATYPE, msg);
        }
    }

    /**
     * Writes the value of an application attribute to the XML stream.
     * 
     * @param api The OpenAtfxAPI.
     * @param streamWriter The XML stream writer.
     * @param attr The attribute to write.
     * @param nvu The value to write.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading instance data.
     */
    private void writeApplAttrValue(OpenAtfxAPI api, XMLStreamWriter streamWriter, Attribute attr,
            NameValueUnit nvu) throws XMLStreamException {
        if (!nvu.hasValidValue()) {
            return;
        }

        streamWriter.writeStartElement(nvu.getValName());

        SingleValue u = nvu.getValue();
        DataType dataType = u.discriminator();

        // DT_BLOB
        if (dataType == DataType.DT_BLOB) {
//            writeBlob(streamWriter, u.blobVal());
        }
        // DT_BOOLEAN
        else if (dataType == DataType.DT_BOOLEAN) {
            streamWriter.writeCharacters(AtfxExportUtil.createBooleanString(u.booleanVal()));
        }
        // DT_BYTE
        else if (dataType == DataType.DT_BYTE) {
            streamWriter.writeCharacters(AtfxExportUtil.createByteString(u.byteVal()));
        }
        // DT_BYTESTR
        else if (dataType == DataType.DT_BYTESTR) {
            streamWriter.writeCharacters(AtfxExportUtil.createByteSeqString(u.bytestrVal()));
        }
        // DT_COMPLEX
        else if (dataType == DataType.DT_COMPLEX) {
            streamWriter.writeCharacters(AtfxExportUtil.createComplexString(u.complexVal()));
        }
        // DT_DATE
        else if (dataType == DataType.DT_DATE) {
            streamWriter.writeCharacters(u.dateVal());
        }
        // DT_DCOMPLEX
        else if (dataType == DataType.DT_DCOMPLEX) {
            streamWriter.writeCharacters(AtfxExportUtil.createDComplexString(u.dcomplexVal()));
        }
        // DT_DOUBLE
        else if (dataType == DataType.DT_DOUBLE) {
            streamWriter.writeCharacters(AtfxExportUtil.createDoubleString(u.doubleVal()));
        }
        // DT_ENUM
        else if (dataType == DataType.DT_ENUM) {
            String enumValue = api.getEnumerationItemName(attr.getEnumName(), u.enumVal());
            if (enumValue == null) {
                enumValue = "";
            }
            streamWriter.writeCharacters(enumValue);
        }
        // DT_EXTERNALREFERENCE
        else if (dataType == DataType.DT_EXTERNALREFERENCE) {
            writeExtRef(streamWriter, u.extRefVal());
        }
        // DT_FLOAT
        else if (dataType == DataType.DT_FLOAT) {
            streamWriter.writeCharacters(AtfxExportUtil.createFloatString(u.floatVal()));
        }
        // DT_ID
        else if (dataType == DataType.DT_ID) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "DataType 'DT_ID' not supported for application attribute");
        }
        // DT_LONG
        else if (dataType == DataType.DT_LONG) {
            streamWriter.writeCharacters(AtfxExportUtil.createLongString(u.longVal()));
        }
        // DT_LONGLONG
        else if (dataType == DataType.DT_LONGLONG) {
            streamWriter.writeCharacters(AtfxExportUtil.createLongLongString(u.longlongVal()));
        }
        // DT_SHORT
        else if (dataType == DataType.DT_SHORT) {
            streamWriter.writeCharacters(AtfxExportUtil.createShortString(u.shortVal()));
        }
        // DT_STRING
        else if (dataType == DataType.DT_STRING) {
            if (trimStringValues) {
                streamWriter.writeCharacters(u.stringVal().trim());
            } else {
                streamWriter.writeCharacters(u.stringVal());
            }
        }
        // DS_BOOLEAN
        else if (dataType == DataType.DS_BOOLEAN) {
            streamWriter.writeCharacters(AtfxExportUtil.createBooleanSeqString(u.booleanSeq()));
        }
        // DS_BYTE
        else if (dataType == DataType.DS_BYTE) {
            streamWriter.writeCharacters(AtfxExportUtil.createByteSeqString(u.byteSeq()));
        }
        // DS_BYTESTR
        else if (dataType == DataType.DS_BYTESTR) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "DataType 'DS_BYTESTR' not supported for application attribute");
        }
        // DS_COMPLEX
        else if (dataType == DataType.DS_COMPLEX) {
            streamWriter.writeCharacters(AtfxExportUtil.createComplexSeqString(u.complexSeq()));
        }
        // DS_DATE
        else if (dataType == DataType.DS_DATE) {
            streamWriter.writeCharacters(AtfxExportUtil.createDateSeqString(u.dateSeq()));
        }
        // DS_DCOMPLEX
        else if (dataType == DataType.DS_DCOMPLEX) {
            streamWriter.writeCharacters(AtfxExportUtil.createDComplexSeqString(u.dcomplexSeq()));
        }
        // DS_DOUBLE
        else if (dataType == DataType.DS_DOUBLE) {
            streamWriter.writeCharacters(AtfxExportUtil.createDoubleSeqString(u.doubleSeq()));
        }
        // DS_ENUM
        else if (dataType == DataType.DS_ENUM) {
            List<String> list = new ArrayList<>();
            for (int i : u.enumSeq()) {
                String enumValue = api.getEnumerationItemName(attr.getEnumName(), i);
                list.add(enumValue);
            }
            writeStringSeq(streamWriter, list.toArray(new String[0]));
        }
        // DS_EXTERNALREFERENCE
        else if (dataType == DataType.DS_EXTERNALREFERENCE) {
            writeExtRefs(streamWriter, u.extRefSeq());
        }
        // DS_FLOAT
        else if (dataType == DataType.DS_FLOAT) {
            streamWriter.writeCharacters(AtfxExportUtil.createFloatSeqString(u.floatSeq()));
        }
        // DS_ID
        else if (dataType == DataType.DS_ID) {
            throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                        "DataType 'DS_ID' not supported for application attribute");
        }
        // DS_LONG
        else if (dataType == DataType.DS_LONG) {
            streamWriter.writeCharacters(AtfxExportUtil.createLongSeqString(u.longSeq()));
        }
        // DS_LONGLONG
        else if (dataType == DataType.DS_LONGLONG) {
            streamWriter.writeCharacters(AtfxExportUtil.createLongLongSeqString(u.longlongSeq()));
        }
        // DS_SHORT
        else if (dataType == DataType.DS_SHORT) {
            streamWriter.writeCharacters(AtfxExportUtil.createShortSeqString(u.shortSeq()));
        }
        // DS_STRING
        else if (dataType == DataType.DS_STRING) {
            writeStringSeq(streamWriter, u.stringSeq());
        }
        // DT_UNKNOWN: only for the values of a LocalColumn
        else if (dataType == DataType.DT_UNKNOWN) {
            // tsValue.u = parseMeasurementData(attrElem);
        }
        // unsupported data type
        else {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "DataType " + dataType + " not yet implemented");
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes LocalColumn values to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param nvu The values.
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading instance data.
     */
    private void writeLocalColumnValues(XMLStreamWriter streamWriter, NameValueUnit nvu)
            throws XMLStreamException {
        streamWriter.writeStartElement(nvu.getValName());

        SingleValue u = nvu.getValue();
        DataType dataType = u.discriminator();

        // DS_BOOLEAN
        if (dataType == DataType.DS_BOOLEAN) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_BOOLEAN);
            streamWriter.writeCharacters(AtfxExportUtil.createBooleanSeqString(u.booleanSeq()));
            streamWriter.writeEndElement();
        }
        // DS_COMPLEX
        else if (dataType == DataType.DS_COMPLEX) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_COMPLEX32);
            streamWriter.writeCharacters(AtfxExportUtil.createComplexSeqString(u.complexSeq()));
            streamWriter.writeEndElement();
        }
        // DS_DCOMPLEX
        else if (dataType == DataType.DS_DCOMPLEX) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_COMPLEX64);
            streamWriter.writeCharacters(AtfxExportUtil.createDComplexSeqString(u.dcomplexSeq()));
            streamWriter.writeEndElement();
        }
        // DS_EXTERNALREFERENCE
        else if (dataType == DataType.DS_EXTERNALREFERENCE) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE);
            writeExtRefs(streamWriter, u.extRefSeq());
            streamWriter.writeEndElement();
        }
        // DS_BYTE
        else if (dataType == DataType.DS_BYTE) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_BYTEFIELD);
            streamWriter.writeCharacters(AtfxExportUtil.createByteSeqString(u.byteSeq()));
            streamWriter.writeEndElement();
        }
        // DS_SHORT
        else if (dataType == DataType.DS_SHORT) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_INT16);
            streamWriter.writeCharacters(AtfxExportUtil.createShortSeqString(u.shortSeq()));
            streamWriter.writeEndElement();
        }
        // DS_LONG
        else if (dataType == DataType.DS_LONG) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_INT32);
            streamWriter.writeCharacters(AtfxExportUtil.createLongSeqString(u.longSeq()));
            streamWriter.writeEndElement();
        }
        // DS_LONGLONG
        else if (dataType == DataType.DS_LONGLONG) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_INT64);
            streamWriter.writeCharacters(AtfxExportUtil.createLongLongSeqString(u.longlongSeq()));
            streamWriter.writeEndElement();
        }
        // DS_FLOAT
        else if (dataType == DataType.DS_FLOAT) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_FLOAT32);
            streamWriter.writeCharacters(AtfxExportUtil.createFloatSeqString(u.floatSeq()));
            streamWriter.writeEndElement();
        }
        // DS_DOUBLE
        else if (dataType == DataType.DS_DOUBLE) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_FLOAT64);
            streamWriter.writeCharacters(AtfxExportUtil.createDoubleSeqString(u.doubleSeq()));
            streamWriter.writeEndElement();
        }
        // DS_DATE
        else if (dataType == DataType.DS_DATE) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_TIMESTRING);
            streamWriter.writeCharacters(AtfxExportUtil.createDateSeqString(u.dateSeq()));
            streamWriter.writeEndElement();
        }
        // DS_STRING
        else if (dataType == DataType.DS_STRING) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_UTF8STRING);
            writeStringSeq(streamWriter, u.stringSeq());
            streamWriter.writeEndElement();
        }
        // DS_BYTESTR
        else if (dataType == DataType.DS_BYTESTR) {
            streamWriter.writeStartElement(AtfxTagConstants.VALUES_ATTR_BYTEFIELD);
            writeBytestrSeq(streamWriter, u.bytestrSeq());
            streamWriter.writeEndElement();
        }
        // not supported
        else {
            throw new OpenAtfxException(ErrorCode.AO_INVALID_DATATYPE,
                                        "Unsupported local column 'values' datatype: " + dataType);
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes the Blob value to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param blob The Blob to write
     * @throws XMLStreamException Error writing XML file.
     * @throws OpenAtfxException Error reading instance data.
     */
    private void writeBlob(XMLStreamWriter streamWriter, Blob blob) throws XMLStreamException {
        // TODO implement Blob handling
//        writeElement(streamWriter, AtfxTagConstants.BLOB_TEXT, blob.getHeader());
//        streamWriter.writeStartElement(AtfxTagConstants.BLOB_BYTEFIELD);
//        writeElement(streamWriter, AtfxTagConstants.BLOB_LENGTH, AtfxExportUtil.createLongString(blob.getLength()));
//        writeElement(streamWriter, AtfxTagConstants.BLOB_SEQUENCE,
//                     AtfxExportUtil.createByteSeqString(blob.get(0, blob.getLength())));
//        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an external reference value to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param extRef The external reference.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeExtRef(XMLStreamWriter streamWriter, ExternalReference extRef) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.EXTREF);
        writeElement(streamWriter, AtfxTagConstants.EXTREF_DESCRIPTION, extRef.getDescription());
        writeElement(streamWriter, AtfxTagConstants.EXTREF_MIMETYPE, extRef.getMimeType());
        writeElement(streamWriter, AtfxTagConstants.EXTREF_LOCATION, extRef.getLocation());
        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an external reference sequence to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param extRefs The external references.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeExtRefs(XMLStreamWriter streamWriter, ExternalReference[] extRefs) throws XMLStreamException {
        for (ExternalReference extRef : extRefs) {
            writeExtRef(streamWriter, extRef);
        }
    }

    /**
     * Writes a string sequence to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param stringSeq The string sequence.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeStringSeq(XMLStreamWriter streamWriter, String[] stringSeq) throws XMLStreamException {
        for (String s : stringSeq) {
            writeElement(streamWriter, AtfxTagConstants.STRING_SEQ, s);
        }
    }

    /**
     * Writes a bytestr sequence to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param bytestrSeq The bytes sequence.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeBytestrSeq(XMLStreamWriter streamWriter, byte[][] bytestrSeq) throws XMLStreamException {
        for (byte[] bytes : bytestrSeq) {
            writeElement(streamWriter, AtfxTagConstants.BYTESTR_LENGTH, String.valueOf(bytes.length));
            writeElement(streamWriter, AtfxTagConstants.BYTESTR_SEQUENCE, AtfxExportUtil.createByteSeqString(bytes));
        }
    }

    private void writeElement(XMLStreamWriter streamWriter, String elemName, String textContent)
            throws XMLStreamException {
        streamWriter.writeStartElement(elemName);
        streamWriter.writeCharacters(textContent);
        streamWriter.writeEndElement();
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static AtfxWriter getInstance() {
        if (instance == null) {
            instance = new AtfxWriter();
        }
        return instance;
    }

}
