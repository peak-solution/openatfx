package de.rechner.openatfx.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AIDName;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.codehaus.stax2.typed.TypedXMLStreamException;

import de.rechner.openatfx.util.BufferedRandomAccessFile;
import de.rechner.openatfx.util.FileUtil;
import de.rechner.openatfx.util.ModelCache;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Object for reading the instance part of ATFX files.
 * 
 * @author Christian Rechner
 */
class AtfxInstanceReader {

    private static final Log LOG = LogFactory.getLog(AtfxInstanceReader.class);

    /** The singleton instance */
    private static volatile AtfxInstanceReader instance;

    /**
     * Non visible constructor.
     */
    private AtfxInstanceReader() {}

    /**
     * Read the instance elements from the instance data XML element.
     * <p>
     * Also the relations are parsed and set.
     * 
     * @param aoSession The session.
     * @param files Map containing component files.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    public void parseInstanceElements(AoSession aoSession, Map<String, String> files, XMLStreamReader reader)
            throws XMLStreamException, AoException {
        ModelCache modelCache = new ModelCache(aoSession.getApplicationStructureValue(),
                                               aoSession.getEnumerationAttributes(),
                                               aoSession.getEnumerationStructure());
        Map<ElemId, Map<String, T_LONGLONG[]>> relMap = new HashMap<ElemId, Map<String, T_LONGLONG[]>>();

        long start = System.currentTimeMillis();

        // delete 'old' flags file if existing (in case flags are stored as component file)
        File flagsFile = getFlagsTmpFile(aoSession);
        if (flagsFile.isFile() && flagsFile.exists() && flagsFile.length() > 0 && flagsFile.canWrite()) {
            if (!flagsFile.delete()) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, "Unable to delete file '"
                        + flagsFile + "'");
            }
            LOG.info("Deleted existing flag file: " + flagsFile.getName());
        }

        // parse instances
        reader.next();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA))) {
            if (reader.isStartElement()) {
                relMap.putAll(parseInstanceElement(aoSession, files, modelCache, reader));
            }
            reader.next();
        }

        LOG.info("Parsed instances in " + (System.currentTimeMillis() - start) + " ms");

        // create relations
        start = System.currentTimeMillis();
        ApplElemAccess applElemAccess = aoSession.getApplElemAccess();
        for (Entry<ElemId, Map<String, T_LONGLONG[]>> entry : relMap.entrySet()) {
            ElemId elemId = entry.getKey();
            for (Entry<String, T_LONGLONG[]> relInstEntry : entry.getValue().entrySet()) {
                String relName = relInstEntry.getKey();
                T_LONGLONG[] relIids = relInstEntry.getValue();
                applElemAccess.setRelInst(elemId, relName, relIids, SetType.APPEND);
            }
        }

        LOG.info("Set relations in " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Read the all attribute values,relations and security information from the instance element XML element.
     * 
     * @param as The applications structure.
     * @param reader The XML stream reader.
     * @param aoSession The session.
     * @param modelCache The application model cache.
     * @return Map containing the information about the instance relations (the relations have to be set AFTER all
     *         instances have been created!).
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private Map<ElemId, Map<String, T_LONGLONG[]>> parseInstanceElement(AoSession aoSession, Map<String, String> files,
            ModelCache modelCache, XMLStreamReader reader) throws XMLStreamException, AoException {
        // application element name
        String aeName = reader.getLocalName();
        ApplElem applElem = modelCache.getApplElem(aeName);
        if (applElem == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationElement '" + aeName
                    + "' not found");
        }
        Long aid = ODSHelper.asJLong(applElem.aid);

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
            if (reader.isStartElement() && modelCache.isLocalColumnValuesAttr(aeName, currentTagName)) {
                reader.nextTag();
                // external component
                if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT)) {
                    if (ieExternalComponent == null) {
                        ieExternalComponent = createExtCompIe(aoSession);
                    }
                    parseLocalColumnValuesComponent(ieExternalComponent, files, modelCache, reader);
                }
                // explicit values inline XML
                else if (reader.isStartElement()) {
                    TS_Value value = parseLocalColumnValues(modelCache, reader);
                    AIDNameValueSeqUnitId valuesAttrValue = new AIDNameValueSeqUnitId();
                    valuesAttrValue.unitId = ODSHelper.asODSLongLong(0);
                    valuesAttrValue.attr = new AIDName();
                    valuesAttrValue.attr.aid = applElem.aid;
                    valuesAttrValue.attr.aaName = modelCache.getLcValuesAaName();
                    valuesAttrValue.values = ODSHelper.tsValue2tsValueSeq(value);
                    applAttrValues.add(valuesAttrValue);
                }
            }

            // base attribute 'flags' of 'LocalColumn'
            else if (reader.isStartElement() && modelCache.isLocalColumnFlagsAttr(aeName, currentTagName)) {
                // try to read flags from inline XML
                // no other way than trying with exception could be found
                try {
                    ApplAttr applAttr = modelCache.getApplAttr(aid, currentTagName);
                    AIDNameValueSeqUnitId applAttrValue = new AIDNameValueSeqUnitId();
                    applAttrValue.unitId = ODSHelper.asODSLongLong(0);
                    applAttrValue.attr = new AIDName();
                    applAttrValue.attr.aid = applElem.aid;
                    applAttrValue.attr.aaName = currentTagName;
                    applAttrValue.values = new TS_ValueSeq();
                    applAttrValue.values = ODSHelper.tsValue2tsValueSeq(parseAttributeContent(aoSession, aid,
                                                                                              currentTagName,
                                                                                              applAttr.dType,
                                                                                              modelCache, reader));
                    applAttrValues.add(applAttrValue);
                }
                // flags in external component
                catch (TypedXMLStreamException e) {
                    if (ieExternalComponent == null) {
                        ieExternalComponent = createExtCompIe(aoSession);
                    }
                    parseLocalColumnFlagsComponent(ieExternalComponent, files, modelCache, reader);
                }
            }

            // application attribute value
            else if (reader.isStartElement() && (modelCache.getApplAttr(aid, currentTagName) != null)) {
                ApplAttr applAttr = modelCache.getApplAttr(aid, currentTagName);
                AIDNameValueSeqUnitId applAttrValue = new AIDNameValueSeqUnitId();
                applAttrValue.unitId = ODSHelper.asODSLongLong(0);
                applAttrValue.attr = new AIDName();
                applAttrValue.attr.aid = applElem.aid;
                applAttrValue.attr.aaName = currentTagName;
                applAttrValue.values = ODSHelper.tsValue2tsValueSeq(parseAttributeContent(aoSession, aid,
                                                                                          currentTagName,
                                                                                          applAttr.dType, modelCache,
                                                                                          reader));
                applAttrValues.add(applAttrValue);
            }

            // application relation
            else if (reader.isStartElement() && (modelCache.getApplRel(aid, currentTagName) != null)) {
                // only read the INVERSE relations for performance reasons!
                ApplRel applRel = modelCache.getApplRel(aid, currentTagName);
                short relMax = applRel.arRelationRange.max;
                short invMax = applRel.invRelationRange.max;
                if ((relMax == -1) || (relMax == 1 && invMax == 1) || (applRel.brName.equals("measurement_quantity"))) {
                    String textContent = reader.getElementText();
                    if (textContent.length() > 0) {
                        T_LONGLONG[] relInstIids = AtfxParseUtil.parseLongLongSeq(textContent);
                        instRelMap.put(applRel.arName, relInstIids);
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
        if (applAttrValues.isEmpty()) { // no values
            return Collections.emptyMap();
        }
        ApplElemAccess aea = aoSession.getApplElemAccess();
        ElemId elemId = aea.insertInstances(applAttrValues.toArray(new AIDNameValueSeqUnitId[0]))[0];

        // set instance attributes
        if (!instAttrValues.isEmpty()) {
            InstanceElement ie = aoSession.getApplicationStructure().getInstancesById(new ElemId[] { elemId })[0];
            for (NameValueUnit nvu : instAttrValues) {
                ie.addInstanceAttribute(nvu);
            }
        }

        // add external component instance, and set sequence representation to external_component
        if (ieExternalComponent != null) {
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeLocalColumn = as.getElementById(elemId.aid);
            ApplicationRelation rel = aeLocalColumn.getRelationsByBaseName("external_component")[0];
            // create relation to external component
            InstanceElement ieLocalColumn = as.getElementById(elemId.aid).getInstanceById(elemId.iid);
            ieLocalColumn.createRelation(rel, ieExternalComponent);
            // alter sequence representation
            String attrSeqRep = aeLocalColumn.getAttributeByBaseName("sequence_representation").getName();
            int seqRepOrig = ODSHelper.getEnumVal(ieLocalColumn.getValue(attrSeqRep));
            int seqRep = ODSHelper.seqRepComp2seqRepExtComp(seqRepOrig);
            ieLocalColumn.setValue(ODSHelper.createEnumNVU(attrSeqRep, seqRep));
        }

        // create relation map
        Map<ElemId, Map<String, T_LONGLONG[]>> retMap = new HashMap<ElemId, Map<String, T_LONGLONG[]>>();
        retMap.put(new ElemId(applElem.aid, elemId.iid), instRelMap);

        return retMap;
    }

    /**
     * Creates an instance of external component.
     * 
     * @param aoSession The session.
     * @return The created instance.
     * @throws AoException Error creating instance.
     */
    private InstanceElement createExtCompIe(AoSession aoSession) throws AoException {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement[] aes = as.getElementsByBaseType("AoExternalComponent");
        if (aes.length != 1) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "None or multiple application elements of type 'AoExternalComponent' found");
        }
        ApplicationElement aeExtComp = aes[0];
        return aeExtComp.createInstance("ExtComp");
    }

    /**
     * Parse the 'component' XML element and fill an external component instance.
     * 
     * @param ieExtComp The external component file instance.
     * @param files Map with component files.
     * @param modelCache The application model cache.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error reading XML.
     * @throws AoException Error creating instance element.
     */
    private void parseLocalColumnValuesComponent(InstanceElement ieExtComp, Map<String, String> files,
            ModelCache modelCache, XMLStreamReader reader) throws XMLStreamException, AoException {
        ApplicationElement aeExtComp = ieExtComp.getApplicationElement();
        ApplicationStructure as = aeExtComp.getApplicationStructure();
        EnumerationDefinition typeSpectEnum = as.getEnumerationDefinition("typespec_enum");
        long aidExtComp = ODSHelper.asJLong(aeExtComp.getId());

        String description = "";
        String fileName = "";
        int dataType = 0;
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
        List<NameValueUnit> attrsList = new ArrayList<NameValueUnit>();

        // mandatory base attribute 'filename_url'
        ApplAttr applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "filename_url");
        attrsList.add(ODSHelper.createStringNVU(applAttr.aaName, fileName));

        // mandatory base attribute 'value_type'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "value_type");
        attrsList.add(ODSHelper.createEnumNVU(applAttr.aaName, dataType));

        // mandatory base attribute 'component_length'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "component_length");
        attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, length));

        // mandatory base attribute 'start_offset', may be DT_LONG or DT_LONGLONG
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "start_offset");
        if (applAttr.dType == DataType.DT_LONG) {
            attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, (int) inioffset));
        } else {
            attrsList.add(ODSHelper.createLongLongNVU(applAttr.aaName, inioffset));
        }

        // mandatory base attribute 'block_size'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "block_size");
        attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, blockSize));

        // mandatory base attribute 'valuesperblock'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "valuesperblock");
        attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, valPerBlock));

        // mandatory base attribute 'value_offset'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "value_offset");
        attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, valOffsets));

        // optional base attribute 'description'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "description");
        if (applAttr != null && applAttr.aaName.length() > 0) {
            attrsList.add(ODSHelper.createStringNVU(applAttr.aaName, description));
        }

        // optional base attribute 'ordinal_number'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "ordinal_number");
        if (applAttr != null && applAttr.aaName.length() > 0) {
            attrsList.add(ODSHelper.createLongNVU(applAttr.aaName, 1));
        }

        // optional base attribute 'bitcount'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "ao_bit_count");
        if (bitCount != null) {
            if (applAttr == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'ao_bit_count' found!");
            }
            attrsList.add(ODSHelper.createShortNVU(applAttr.aaName, bitCount));
        }

        // optional base attribute 'bitoffset'
        applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "ao_bit_offset");
        if (bitOffset != null) {
            if (applAttr == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'ao_bit_offset' found!");
            }
            attrsList.add(ODSHelper.createShortNVU(applAttr.aaName, bitOffset));
        }

        ieExtComp.setValueSeq(attrsList.toArray(new NameValueUnit[0]));
    }

    /**
     * Parse the 'component' XML element, read the flags from the component file and append to external flag file.
     * <p/>
     * This operation is necessary because a MixedMode Server may not store flags in component file with block wise
     * persistence (all flags have to be in one block).
     * 
     * @param ieExtComp The external component instance.
     * @param files Map with component files.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error reading XML.
     * @throws AoException Error creating instance element.
     */
    private void parseLocalColumnFlagsComponent(InstanceElement ieExtComp, Map<String, String> files,
            ModelCache modelCache, XMLStreamReader reader) throws XMLStreamException, AoException {
        ApplicationElement aeExtComp = ieExtComp.getApplicationElement();
        ApplicationStructure as = aeExtComp.getApplicationStructure();
        EnumerationDefinition typeSpectEnum = as.getEnumerationDefinition("typespec_enum");
        long aidExtComp = ODSHelper.asJLong(aeExtComp.getId());

        String fileName = "";
        int dataType = 0;
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
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "External component file not found for identifier '" + identifier + "'");
                }
            }
            // 'datatype'
            else if (reader.isStartElement() && (reader.getLocalName().equals(AtfxTagConstants.COMPONENT_DATATYPE))) {
                dataType = typeSpectEnum.getItem(reader.getElementText());
                // only 'dt_short' is currently supported
                if (dataType != 2) {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "Unsupported 'dataType' for flags component file: " + dataType);
                }
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

        // read flags to memory because the MixedMode server may not handle flags in component structure :-(
        long start = System.currentTimeMillis();
        AoSession aoSession = ieExtComp.getApplicationElement().getApplicationStructure().getSession();
        File fileRoot = new File(aoSession.getContextByName("FILE_ROOT").value.u.stringVal());
        File componentFile = new File(fileRoot, fileName);
        File flagsFile = getFlagsTmpFile(aoSession);

        // read values
        RandomAccessFile raf = null;
        OutputStream fos = null;
        try {
            // open source stream
            raf = new BufferedRandomAccessFile(componentFile, "r", 32768);
            raf.seek(inioffset);

            // open target stream
            fos = new BufferedOutputStream(new FileOutputStream(flagsFile, true));
            long startOffset = flagsFile.length();

            // initialize source buffer
            ByteBuffer sourceMbb = ByteBuffer.allocate(blockSize);
            sourceMbb.order(ByteOrder.LITTLE_ENDIAN);

            // loop over blocks
            for (int i = 0; i < length; i += valPerBlock) {
                byte[] buffer = new byte[blockSize];
                raf.read(buffer, 0, buffer.length);

                sourceMbb.clear();
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
            ApplAttr applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "flags_filename_url");
            ieExtComp.setValue(ODSHelper.createStringNVU(applAttr.aaName, flagsFile.getName()));
            applAttr = modelCache.getApplAttrByBaseName(aidExtComp, "flags_start_offset");
            if (applAttr.dType == DataType.DT_LONG) {
                ieExtComp.setValue(ODSHelper.createLongNVU(applAttr.aaName, (int) startOffset));
            } else {
                ieExtComp.setValue(ODSHelper.createLongLongNVU(applAttr.aaName, startOffset));
            }

            LOG.info("Copied " + length + " flags from component file '" + fileName + "' to external component '"
                    + flagsFile.getName() + "' in " + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                }
                raf = null;
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                }
                fos = null;
            }
        }
    }

    private File getFlagsTmpFile(AoSession aoSession) throws AoException {
        File fileRoot = new File(aoSession.getContextByName("FILE_ROOT").value.u.stringVal());
        File atfxFile = new File(aoSession.getContextByName("FILENAME").value.u.stringVal());
        File flagsFile = new File(fileRoot, FileUtil.stripExtension(atfxFile.getName()) + "_flags_extract.btf");
        return flagsFile;
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
    private TS_Value parseLocalColumnValues(ModelCache modelCache, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        TS_Value value = new TS_Value();
        value.flag = (short) 15;
        value.u = new TS_Union();
        while (!(reader.isEndElement() && reader.getLocalName().equals(modelCache.getLcValuesAaName()))) {
            // DS_BLOB
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BLOB)) {
                // AoSession aoSession = aa.getApplicationElement().getApplicationStructure().getSession();
                // value.u.blobVal(parseBlob(aoSession, AtfxTagConstants.VALUES_ATTR_BLOB, reader));
            }
            // DS_BOOLEAN
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BOOLEAN)) {
                value.u.booleanSeq(AtfxParseUtil.parseBooleanSeq(reader.getElementText()));
            }
            // DS_COMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX32)) {
                value.u.complexSeq(AtfxParseUtil.parseComplexSeq(reader.getElementText()));
            }
            // DS_DCOMPLEX
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_COMPLEX64)) {
                value.u.dcomplexSeq(AtfxParseUtil.parseDComplexSeq(reader.getElementText()));
            }
            // DS_EXTERNALREFERENCE
            else if (reader.isStartElement()
                    && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE)) {
                value.u.extRefSeq(parseExtRefs(AtfxTagConstants.VALUES_ATTR_EXTERNALREFERENCE, reader));
            }
            // DS_BYTE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_BYTEFIELD)) {
                value.u.byteSeq(AtfxParseUtil.parseByteSeq(reader.getElementText()));
            }
            // DS_SHORT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT16)) {
                value.u.shortSeq(AtfxParseUtil.parseShortSeq(reader.getElementText()));
            }
            // DS_LONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT32)) {
                value.u.longSeq(AtfxParseUtil.parseLongSeq(reader.getElementText()));
            }
            // DS_LONGLONG
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_INT64)) {
                value.u.longlongSeq(AtfxParseUtil.parseLongLongSeq(reader.getElementText()));
            }
            // DS_FLOAT
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT32)) {
                value.u.floatSeq(AtfxParseUtil.parseFloatSeq(reader.getElementText()));
            }
            // DS_DOUBLE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_FLOAT64)) {
                value.u.doubleSeq(AtfxParseUtil.parseDoubleSeq(reader.getElementText()));
            }
            // DS_DATE
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.VALUES_ATTR_TIMESTRING)) {
                String input = reader.getElementText().trim();
                String[] dateSeq = new String[0];
                if (input.length() > 0) {
                    dateSeq = input.split("\\s+");
                }
                value.u.dateSeq(dateSeq);
            }
            // DS_STRING
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
    private TS_Value parseAttributeContent(AoSession aoSession, Long aid, String aaName, DataType dataType,
            ModelCache modelCache, XMLStreamReader reader) throws XMLStreamException, AoException {
        TS_Value tsValue = ODSHelper.createEmptyTS_Value(dataType);
        // DT_BLOB
        if (dataType == DataType.DT_BLOB) {
            Blob blob = parseBlob(aoSession, aaName, reader);
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
                String enumName = modelCache.getEnumName(aid, aaName);
                int enumItem = modelCache.getEnumIndex(enumName, txt);
                tsValue.u.enumVal(enumItem);
                tsValue.flag = 15;
            }
        }
        // DT_EXTERNALREFERENCE
        else if (dataType == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference[] extRefs = parseExtRefs(aaName, reader);
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
            String[] seq = AtfxParseUtil.parseDateSeq(reader.getElementText());
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
            String[] seq = parseStringSeq(aaName, reader);
            if (seq.length > 0) {
                String enumName = modelCache.getEnumName(aid, aaName);
                int[] enumItems = new int[seq.length];
                for (int i = 0; i < enumItems.length; i++) {
                    enumItems[i] = modelCache.getEnumIndex(enumName, seq[i]);
                }
                tsValue.u.enumSeq(enumItems);
                tsValue.flag = 15;
            }
        }
        // DS_EXTERNALREFERENCE
        else if (dataType == DataType.DS_EXTERNALREFERENCE) {
            T_ExternalReference[] seq = parseExtRefs(aaName, reader);
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
            String[] seq = parseStringSeq(aaName, reader);
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
    public static AtfxInstanceReader getInstance() {
        if (instance == null) {
            instance = new AtfxInstanceReader();
        }
        return instance;
    }

}
