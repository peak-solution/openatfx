package de.rechner.openatfx.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationAttributeStructure;
import org.asam.ods.EnumerationItemStructure;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Object for writing ATFX files.
 * 
 * @author Christian Rechner
 */
public class AtfxWriter {

    private static final Log LOG = LogFactory.getLog(AtfxWriter.class);

    /** The singleton instance */
    private static AtfxWriter instance;

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
    public void writeXML(File xmlFile, AoSession aoSession) throws AoException {
        long start = System.currentTimeMillis();

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        OutputStream fos = null;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(xmlFile));
            XMLStreamWriter streamWriter = factory.createXMLStreamWriter(fos, "UTF-8");
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

            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        LOG.info("Wrote XML in " + (System.currentTimeMillis() - start) + "ms");
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
        writeElement(streamWriter, AtfxTagConstants.EXPORTER_VERSION, "1.0.0");
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
        streamWriter.writeStartElement(AtfxTagConstants.APPLICATION_MODEL);

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
        }

        // applicaton relations
        for (ApplRel applRel : applRelsMap.get(aid)) {
            writeApplRel(streamWriter, applRel, applElemMap);
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
        if (!applAttr.baName.equals("id") && applAttr.isUnique) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_UNIQUE, String.valueOf(applAttr.isUnique));
        }
        // length
        if (applAttr.dType == DataType.DT_STRING || applAttr.dType == DataType.DS_STRING
                || applAttr.dType == DataType.DT_EXTERNALREFERENCE || applAttr.dType == DataType.DS_EXTERNALREFERENCE
                || applAttr.dType == DataType.DT_DATE || applAttr.dType == DataType.DS_DATE) {
            writeElement(streamWriter, AtfxTagConstants.APPL_ATTR_LENGTH, String.valueOf(applAttr.length));
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
