package de.rechner.openatfx.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplAttr;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.AttrType;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Object for writing ATFX files.
 * 
 * @author Christian Rechner
 */
public class AtfxWriter {

    private static final Log LOG = LogFactory.getLog(AtfxWriter.class);

    /** singleton instance */
    private static volatile AtfxWriter instance;

    /** model information for writing local column values */
    private Long aidLocalColumn;
    private String aaNameLocalColumnValues;
    private String aaNameLocalColumnSequenceRepresentation;

    /**
     * Non visible constructor.
     */
    private AtfxWriter() {}

    /**
     * Writes the complete content of given aoSession to specified XML file.
     * 
     * @param xmlFile The XML file.
     * @param aoSession The session.
     * @throws AoException Error writing XML file.
     */
    public synchronized void writeXML(File xmlFile, AoSession aoSession) throws AoException {
        this.aidLocalColumn = null;
        this.aaNameLocalColumnValues = null;
        this.aaNameLocalColumnSequenceRepresentation = null;
        long start = System.currentTimeMillis();

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        OutputStream fos = null;
        XMLStreamWriter streamWriter = null;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(xmlFile));
            streamWriter = factory.createXMLStreamWriter(fos, "UTF-8");
            if (shouldIndentXML(aoSession)) {
                streamWriter = new IndentingXMLStreamWriter(streamWriter);
            }

            streamWriter.writeStartDocument("UTF-8", "1.0");
            streamWriter.writeStartElement(AtfxTagConstants.ATFX_FILE);
            streamWriter.writeAttribute("version", "atfx_file: V1.2.0");
            streamWriter.writeAttribute("xmlns", "http://www.asam.net/ODS/5.2.0/Schema");

            // documentation
            writeDocumentation(streamWriter);
            // base model version
            writeBaseModelVersion(streamWriter, aoSession);
            // TODO: write files
            // application model
            writeApplicationModel(streamWriter, aoSession);
            // instance data
            if (instancesExists(aoSession)) {
                writeInstanceData(streamWriter, aoSession);
            }

            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (streamWriter != null) {
                try {
                    streamWriter.close();
                } catch (XMLStreamException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        LOG.info("Wrote XML in " + (System.currentTimeMillis() - start) + "ms to '" + xmlFile.getAbsolutePath() + "'");
    }

    /**
     * Returns whether to indent the aoSession depending on the value of the context parameter 'INDENT_XML'.
     * 
     * @param aoSession The session.
     * @return Whether to indent the XML.
     * @throws AoException Error querying context parameters.
     */
    private boolean shouldIndentXML(AoSession aoSession) throws AoException {
        NameValueIterator ni = aoSession.getContext("INDENT_XML");
        boolean indent = false;
        for (NameValue nv : ni.nextN(ni.getCount())) {
            if (nv.value.u.stringVal().equalsIgnoreCase("TRUE")) {
                indent = true;
            }
        }
        ni.destroy();
        return indent;
    }

    /**
     * Returns whether a single instance exists in given session.
     * 
     * @param aoSession The session.
     * @return True if a single instance exists.
     * @throws AoException Error querying instances.
     */
    private boolean instancesExists(AoSession aoSession) throws AoException {
        for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
            InstanceElementIterator iter = ae.getInstances("*");
            if (iter.getCount() > 0) {
                iter.destroy();
                return true;
            }
            iter.destroy();
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
        writeElement(streamWriter, AtfxTagConstants.EXPORTER_VERSION, "0.2.2");
        streamWriter.writeEndElement();
    }

    /**
     * Writes the base model version to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param aoSession The session.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading base model version.
     */
    private void writeBaseModelVersion(XMLStreamWriter streamWriter, AoSession aoSession) throws XMLStreamException,
            AoException {
        writeElement(streamWriter, AtfxTagConstants.BASE_MODEL_VERSION, aoSession.getBaseStructure().getVersion());
    }

    /***************************************************************************************
     * methods for writing the application model
     ***************************************************************************************/

    /**
     * Writes the application model to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param aoSession The session.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading application model.
     */
    private void writeApplicationModel(XMLStreamWriter streamWriter, AoSession aoSession) throws XMLStreamException,
            AoException {
        ApplicationStructureValue av = aoSession.getApplicationStructureValue();
        EnumerationAttributeStructure[] eas = aoSession.getEnumerationAttributes();
        streamWriter.writeStartElement(AtfxTagConstants.APPL_MODEL);

        // enumerations
        Set<String> baseEnums = new HashSet<String>();
        for (BaseElement be : aoSession.getBaseStructure().getElements("*")) {
            for (BaseAttribute ba : be.getAttributes("*")) {
                if (ba.getDataType() == DataType.DT_ENUM || ba.getDataType() == DataType.DS_ENUM) {
                    baseEnums.add(ba.getEnumerationDefinition().getName());
                }
            }
        }
        for (EnumerationStructure es : aoSession.getEnumerationStructure()) {
            if (!baseEnums.contains(es.enumName)) {
                writeEnumerationStructure(streamWriter, es);
            }
        }

        // build maps of application elements and application relations
        Map<Long, ApplElem> applElemMap = new HashMap<Long, ApplElem>();
        Map<Long, List<ApplRel>> applRelsMap = new HashMap<Long, List<ApplRel>>();
        for (ApplElem applElem : av.applElems) {
            applElemMap.put(ODSHelper.asJLong(applElem.aid), applElem);
        }
        for (ApplRel applRel : av.applRels) {
            long aid = ODSHelper.asJLong(applRel.elem1);
            List<ApplRel> applRelsList = applRelsMap.get(aid);
            if (applRelsList == null) {
                applRelsList = new ArrayList<ApplRel>();
                applRelsMap.put(aid, applRelsList);
            }
            applRelsList.add(applRel);
        }

        // application elements
        for (ApplElem applElem : av.applElems) {
            writeApplElem(streamWriter, applElem, applElemMap, applRelsMap, eas);
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes an application enumeration definition to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param es The enumeration structure.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeEnumerationStructure(XMLStreamWriter streamWriter, EnumerationStructure es)
            throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ENUM);
        writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_NAME, es.enumName);
        for (EnumerationItemStructure eis : es.items) {
            streamWriter.writeStartElement(AtfxTagConstants.APPL_ENUM_ITEM);
            writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_ITEM_NAME, eis.itemName);
            writeElement(streamWriter, AtfxTagConstants.APPL_ENUM_ITEM_VALUE, String.valueOf(eis.index));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    /**
     * Writes an application element to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param applElem The application element.
     * @param applElemMap Map containing all application elements.
     * @param applRelsMap Map containing all application relations.
     * @param eas The enumeration attributes.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeApplElem(XMLStreamWriter streamWriter, ApplElem applElem, Map<Long, ApplElem> applElemMap,
            Map<Long, List<ApplRel>> applRelsMap, EnumerationAttributeStructure[] eas) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ELEM);
        writeElement(streamWriter, AtfxTagConstants.APPL_ELEM_NAME, applElem.aeName);
        writeElement(streamWriter, AtfxTagConstants.APPL_ELEM_BASETYPE, applElem.beName);
        long aid = ODSHelper.asJLong(applElem.aid);

        // application attributes
        for (ApplAttr applAttr : applElem.attributes) {
            writeApplAttr(streamWriter, aid, applAttr, eas);
            // set global attributes
            if (applElem.beName.equalsIgnoreCase("AoLocalColumn")) {
                this.aidLocalColumn = ODSHelper.asJLong(applElem.aid);
                if (applAttr.baName.equalsIgnoreCase("values")) {
                    this.aaNameLocalColumnValues = applAttr.aaName;
                } else if (applAttr.baName.equalsIgnoreCase("sequence_representation")) {
                    this.aaNameLocalColumnSequenceRepresentation = applAttr.aaName;
                }
            }
        }

        // applicaton relations
        List<ApplRel> applRels = applRelsMap.get(aid);
        if (applRels != null) {
            for (ApplRel applRel : applRels) {
                writeApplRel(streamWriter, applRel, applElemMap);
            }
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes an application attribute to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param aid The application element id.
     * @param applAttr The application attribute.
     * @param eas All enumeration attributes to find out the enumeration name.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeApplAttr(XMLStreamWriter streamWriter, long aid, ApplAttr applAttr,
            EnumerationAttributeStructure[] eas) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_ATTR);
        // name
        writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_NAME, applAttr.aaName);
        // base attr / datatype
        if (applAttr.baName.length() > 0) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_BASEATTR, applAttr.baName);
        } else {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_DATATYPE, ODSHelper.dataType2String(applAttr.dType));
        }
        // enumeration
        if ((applAttr.baName.length() < 1)
                && (applAttr.dType == DataType.DT_ENUM || applAttr.dType == DataType.DS_ENUM)) {
            for (EnumerationAttributeStructure e : eas) {
                if (aid == ODSHelper.asJLong(e.aid) && applAttr.aaName.equals(e.aaName)) {
                    writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_ENUMTYPE, e.enumName);
                    break;
                }
            }
        }
        // obligatory
        if (applAttr.baName.length() < 1 && applAttr.isObligatory) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_OBLIGATORY, String.valueOf(applAttr.isObligatory));
        }
        // unique
        if (!applAttr.baName.equalsIgnoreCase("id") && applAttr.isUnique) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_UNIQUE, String.valueOf(applAttr.isUnique));
        }
        // length
        if (applAttr.dType == DataType.DT_STRING || applAttr.dType == DataType.DS_STRING
                || applAttr.dType == DataType.DT_EXTERNALREFERENCE || applAttr.dType == DataType.DS_EXTERNALREFERENCE
                || applAttr.dType == DataType.DT_DATE || applAttr.dType == DataType.DS_DATE) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_LENGTH, String.valueOf(applAttr.length));
        }
        // unit
        long unitId = ODSHelper.asJLong(applAttr.unitId);
        if (unitId != 0) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_UNIT, String.valueOf(unitId));
        }
        streamWriter.writeEndElement();
    }

    /**
     * Writes an application relation to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param applRel The application relation.
     * @param applElemMap Map containing all application elements by aid.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeApplRel(XMLStreamWriter streamWriter, ApplRel applRel, Map<Long, ApplElem> applElemMap)
            throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.APPL_REL);
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_NAME, applRel.arName);
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_REFTO,
                     applElemMap.get(ODSHelper.asJLong(applRel.elem2)).aeName);
        if (applRel.brName.length() > 0) {
            writeElement(streamWriter, AtfxTagConstants.APPL_REL_BASEREL, applRel.brName);
        }
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_MIN,
                     ODSHelper.relRange2string(applRel.arRelationRange.min));
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_MAX,
                     ODSHelper.relRange2string(applRel.arRelationRange.max));
        writeElement(streamWriter, AtfxTagConstants.APPL_REL_INVNAME, applRel.invName);
        streamWriter.writeEndElement();
    }

    /***************************************************************************************
     * methods for writing the instance data
     ***************************************************************************************/

    /**
     * Writes the instance data to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param aoSession The session.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading instances.
     */
    private void writeInstanceData(XMLStreamWriter streamWriter, AoSession aoSession) throws XMLStreamException,
            AoException {
        streamWriter.writeStartElement(AtfxTagConstants.INSTANCE_DATA);

        // iterate over all application elements/instance elements
        ApplElemAccess aea = aoSession.getApplElemAccess();
        for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
            InstanceElementIterator iter = ae.getInstances("*");
            for (InstanceElement ie : iter.nextN(iter.getCount())) {
                writeInstanceElement(streamWriter, aea, ie);
            }
            iter.destroy();
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an instance element to
     * 
     * @param streamWriter The XML stream writer.
     * @param aea The ApplElemAccess interface.
     * @param ie The instance element.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading instance data.
     */
    private void writeInstanceElement(XMLStreamWriter streamWriter, ApplElemAccess aea, InstanceElement ie)
            throws XMLStreamException, AoException {
        ApplicationElement applElem = ie.getApplicationElement();
        long aid = ODSHelper.asJLong(applElem.getId());
        streamWriter.writeStartElement(applElem.getName());

        // write application attribute data
        String[] attrNames = ie.listAttributes("*", AttrType.APPLATTR_ONLY);
        List<String> attrNameList = new ArrayList<String>(Arrays.asList(attrNames));

        // special handling: LocalColumn 'values'; do not write external component values
        if ((this.aidLocalColumn != null) && (this.aidLocalColumn == aid) && (this.aaNameLocalColumnValues != null)
                && (this.aaNameLocalColumnSequenceRepresentation != null)) {
            // remove values from attribute list
            attrNameList.remove(this.aaNameLocalColumnValues);
            // query sequence representation
            int seqRepEnum = ODSHelper.getEnumVal(ie.getValueByBaseName("sequence_representation"));
            // check if the sequence representation is 7(external_component), 8(raw_linear_external),
            // 9(raw_polynomial_external) or 11(raw_linear_calibrated_external)
            if (seqRepEnum == 7 || seqRepEnum == 8 || seqRepEnum == 9 || seqRepEnum == 11) {
            } else {
                writeLocalColumnValues(streamWriter, ie.getValue(this.aaNameLocalColumnValues));
            }
        }

        // write attributes if not null
        for (NameValueUnit nvu : ie.getValueSeq(attrNameList.toArray(new String[0]))) {
            if (nvu.value.flag == 15) {
                writeApplAttrValue(streamWriter, applElem, nvu);
            }
        }

        // write instance attribute data
        String[] instAttrNames = ie.listAttributes("*", AttrType.INSTATTR_ONLY);
        if (instAttrNames.length > 0) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR);
            for (NameValueUnit nvu : ie.getValueSeq(instAttrNames)) {
                writeInstAttrValue(streamWriter, nvu);
            }
            streamWriter.writeEndElement();
        }

        // write relations
        ElemId elemId = new ElemId(ie.getApplicationElement().getId(), ie.getId());
        for (ApplicationRelation rel : ie.getApplicationElement().getAllRelations()) {
            String relName = rel.getRelationName();
            T_LONGLONG[] relInsts = aea.getRelInst(elemId, rel.getRelationName());
            if (relInsts.length > 0) {
                writeElement(streamWriter, relName, AtfxExportUtil.createLongLongSeqString(relInsts));
            }
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes the value of an instance attribute to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param instAttrValue The instance attribute value.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Invalid attribute data type.
     */
    private void writeInstAttrValue(XMLStreamWriter streamWriter, NameValueUnit instAttrValue)
            throws XMLStreamException, AoException {
        DataType dataType = instAttrValue.value.u.discriminator();
        String attrName = instAttrValue.valName;
        TS_Value value = instAttrValue.value;

        // DT_STRING
        if (dataType == DataType.DT_STRING) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_ASCIISTRING);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(value.u.stringVal());
            }
            streamWriter.writeEndElement();
        }
        // DT_FLOAT
        else if (dataType == DataType.DT_FLOAT) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_FLOAT32);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createFloatString(value.u.floatVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_DOUBLE
        else if (dataType == DataType.DT_DOUBLE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_FLOAT64);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createDoubleString(value.u.doubleVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_BYTE
        else if (dataType == DataType.DT_BYTE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT8);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createByteString(value.u.byteVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_SHORT
        else if (dataType == DataType.DT_SHORT) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT16);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createShortString(value.u.shortVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_LONG
        else if (dataType == DataType.DT_LONG) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT32);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createLongString(value.u.longVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_LONGLONG
        else if (dataType == DataType.DT_LONGLONG) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_INT64);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(AtfxExportUtil.createLongLongString(value.u.longlongVal()));
            }
            streamWriter.writeEndElement();
        }
        // DT_DATE
        else if (dataType == DataType.DT_DATE) {
            streamWriter.writeStartElement(AtfxTagConstants.INST_ATTR_TIME);
            streamWriter.writeAttribute(AtfxTagConstants.INST_ATTR_NAME, attrName);
            if (instAttrValue.value.flag == 15) {
                streamWriter.writeCharacters(value.u.dateVal());
            }
            streamWriter.writeEndElement();
        }
        // unsupported data type
        else {
            String msg = "DataType '" + ODSHelper.dataType2String(dataType)
                    + "' is not allowed for instance attributes";
            LOG.error(msg);
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0, msg);
        }
    }

    /**
     * Writes the value of an application attribute to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param applElem The application element.
     * @param nvu The value.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading instance data.
     */
    private void writeApplAttrValue(XMLStreamWriter streamWriter, ApplicationElement applElem, NameValueUnit nvu)
            throws XMLStreamException, AoException {
        streamWriter.writeStartElement(nvu.valName);

        TS_Union u = nvu.value.u;
        DataType dataType = u.discriminator();

        // DT_BLOB
        if (dataType == DataType.DT_BLOB) {
            writeBlob(streamWriter, u.blobVal());
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
            ApplicationAttribute applAttr = applElem.getAttributeByName(nvu.valName);
            EnumerationDefinition ed = applAttr.getEnumerationDefinition();
            streamWriter.writeCharacters(ed.getItemName(u.enumVal()));
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
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
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
            streamWriter.writeCharacters(u.stringVal());
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
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "DataType 'DS_BYTESTR' not supported for application attribute");
        }
        // DS_COMPLEX
        else if (dataType == DataType.DS_COMPLEX) {
            streamWriter.writeCharacters(AtfxExportUtil.createComplexSeqString(u.complexSeq()));
        }
        // DS_DATE
        else if (dataType == DataType.DS_DATE) {
            writeStringSeq(streamWriter, u.dateSeq());
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
            ApplicationAttribute applAttr = applElem.getAttributeByName(nvu.valName);
            EnumerationDefinition ed = applAttr.getEnumerationDefinition();
            List<String> list = new ArrayList<String>();
            for (int i : u.enumSeq()) {
                list.add(ed.getItemName(i));
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
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
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
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "DataType " + dataType.value()
                    + " not yet implemented");
        }

        streamWriter.writeEndElement();
    }

    /**
     * Writes LocalColumn values to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param nvu The values.
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading instance data.
     */
    private void writeLocalColumnValues(XMLStreamWriter streamWriter, NameValueUnit nvu) throws XMLStreamException,
            AoException {
        streamWriter.writeStartElement(nvu.valName);

        TS_Union u = nvu.value.u;
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
        // not supported
        else {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "Unsupported local column 'values' datatype: " + dataType);
        }

        streamWriter.writeEndElement();
    }

    // /**
    // * Returns whether given attribute name if the attribute 'values' of the local column instance.
    // *
    // * @param aeName The application element name.
    // * @param aaName The application attribute name.
    // * @return True, if 'values' attribute.
    // * @throws AoException Error checking attribute.
    // */
    // private boolean isLocalColumnValuesAttr(String aeName, String aaName) throws AoException {
    // if (this.aeNameLocalColumn != null && this.aaNameLocalColumnValues != null) {
    // if (this.aeNameLocalColumn.equalsIgnoreCase(aeName)
    // && this.aaNameLocalColumnValues.equalsIgnoreCase(aaName)) {
    // return true;
    // }
    // }
    // return false;
    // }

    /**
     * Writes the Blob value to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param blob The Blob to write
     * @throws XMLStreamException Error writing XML file.
     * @throws AoException Error reading instance data.
     */
    private void writeBlob(XMLStreamWriter streamWriter, Blob blob) throws XMLStreamException, AoException {
        writeElement(streamWriter, AtfxTagConstants.BLOB_TEXT, blob.getHeader());
        streamWriter.writeStartElement(AtfxTagConstants.BLOB_BYTEFIELD);
        writeElement(streamWriter, AtfxTagConstants.BLOB_LENGTH, AtfxExportUtil.createLongString(blob.getLength()));
        writeElement(streamWriter, AtfxTagConstants.BLOB_SEQUENCE,
                     AtfxExportUtil.createByteSeqString(blob.get(0, blob.getLength())));
        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an external reference value to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param extRef The external reference.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeExtRef(XMLStreamWriter streamWriter, T_ExternalReference extRef) throws XMLStreamException {
        streamWriter.writeStartElement(AtfxTagConstants.EXTREF);
        writeElement(streamWriter, AtfxTagConstants.EXTREF_DESCRIPTION, extRef.description);
        writeElement(streamWriter, AtfxTagConstants.EXTREF_MIMETYPE, extRef.mimeType);
        writeElement(streamWriter, AtfxTagConstants.EXTREF_LOCATION, extRef.location);
        streamWriter.writeEndElement();
    }

    /**
     * Writes the data of an external reference sequence to the XML stream.
     * 
     * @param streamWriter The XML stream writer.
     * @param extRefs The external references.
     * @throws XMLStreamException Error writing XML file.
     */
    private void writeExtRefs(XMLStreamWriter streamWriter, T_ExternalReference[] extRefs) throws XMLStreamException {
        for (T_ExternalReference extRef : extRefs) {
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
