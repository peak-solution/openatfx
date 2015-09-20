package de.rechner.openatfx_mdf4.xml;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Helper class for performant parsing of the XML content of an MDF4 file.
 * 
 * @author Christian Rechner
 */
public class MDF4XMLParser {

    private static final Log LOG = LogFactory.getLog(MDF4XMLParser.class);

    private final XMLInputFactory xmlInputFactory;
    private final DateFormat xmlDateTimeFormat;

    /**
     * Constructor.
     */
    public MDF4XMLParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // e.g. 2012-11-07T10:16:03
    }

    /**
     * Writes the content of the meta data block of a header block to the instance element attributes
     * 
     * @param ieMea The measurement instance element.
     * @param mdCommentXML The XML string to parse.
     * @throws IOException
     * @throws AoException
     */
    public void writeHDCommentToMea(InstanceElement ieMea, String mdCommentXML) throws IOException, AoException {
        XMLStreamReader reader = null;
        try {
            reader = this.xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
            while (reader.hasNext()) {
                reader.next();
                // TX
                if (reader.isStartElement() && reader.getLocalName().equals("TX")) {
                    ieMea.setValue(ODSHelper.createStringNVU("desc", reader.getElementText()));
                }
                // time_source
                else if (reader.isStartElement() && reader.getLocalName().equals("time_source")) {
                    ieMea.addInstanceAttribute(ODSHelper.createStringNVU("time_source", reader.getElementText()));
                }
                // constants
                else if (reader.isStartElement() && reader.getLocalName().equals("constants")) {
                    LOG.warn("'constants' in XML content 'HDcomment' is not yet supported!");
                }
                // UNITSPEC
                else if (reader.isStartElement() && reader.getLocalName().equals("UNITSPEC")) {
                    LOG.warn("UNITSPEC in XML content 'HDcomment' is not yet supported!");
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {
                    writeCommonProperties(ieMea, reader);
                }
            }
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Writes the content of the meta data block of a file history block to the instance element attributes
     * 
     * @param ieFh The file history instance element.
     * @param mdCommentXML The XML string to parse.
     * @throws IOException
     * @throws AoException
     */
    public void writeFHCommentToFh(InstanceElement ieFh, String mdCommentXML) throws IOException, AoException {
        XMLStreamReader reader = null;
        try {
            reader = this.xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
            List<NameValueUnit> list = new ArrayList<NameValueUnit>();
            while (reader.hasNext()) {
                reader.next();
                // TX
                if (reader.isStartElement() && reader.getLocalName().equals("TX")) {
                    list.add(ODSHelper.createStringNVU("desc", reader.getElementText()));
                }
                // tool_id
                else if (reader.isStartElement() && reader.getLocalName().equals("tool_id")) {
                    list.add(ODSHelper.createStringNVU("tool_id", reader.getElementText()));
                }
                // tool_vendor
                else if (reader.isStartElement() && reader.getLocalName().equals("tool_vendor")) {
                    list.add(ODSHelper.createStringNVU("tool_vendor", reader.getElementText()));
                }
                // tool_version
                else if (reader.isStartElement() && reader.getLocalName().equals("tool_version")) {
                    list.add(ODSHelper.createStringNVU("tool_version", reader.getElementText()));
                }
                // user_name
                else if (reader.isStartElement() && reader.getLocalName().equals("user_name")) {
                    list.add(ODSHelper.createStringNVU("user_name", reader.getElementText()));
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {
                    writeCommonProperties(ieFh, reader);
                }
            }
            if (list.size() > 0) {
                ieFh.setValueSeq(list.toArray(new NameValueUnit[0]));
            }
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Writes the content of the meta data block of a channel group block to the instance element attributes
     * 
     * @param ieCg The channel group instance element.
     * @param mdCommentXML The XML string to parse.
     * @throws IOException
     * @throws AoException
     */
    public void writeCGCommentToCg(InstanceElement ieCg, String mdCommentXML) throws IOException, AoException {
        XMLStreamReader reader = null;
        try {
            reader = this.xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
            List<NameValueUnit> list = new ArrayList<NameValueUnit>();
            while (reader.hasNext()) {
                reader.next();
                // TX
                if (reader.isStartElement() && reader.getLocalName().equals("TX")) {
                    list.add(ODSHelper.createStringNVU("desc", reader.getElementText()));
                }
                // names
                else if (reader.isStartElement() && reader.getLocalName().equals("names")) {
                    LOG.warn("'names' in XML content 'CGcomment' is not yet supported!");
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {
                    writeCommonProperties(ieCg, reader);
                }
            }
            if (list.size() > 0) {
                ieCg.setValueSeq(list.toArray(new NameValueUnit[0]));
            }
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Writes the content of the meta data block of a channel group block to the instance element attributes
     * 
     * @param ieCg The channel group instance element.
     * @param mdCommentXML The XML string to parse.
     * @throws IOException
     * @throws AoException
     */
    public void writeSICommentToCg(InstanceElement ieCg, String mdCommentXML) throws IOException, AoException {
        XMLStreamReader reader = null;
        try {
            reader = this.xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
            List<NameValueUnit> list = new ArrayList<NameValueUnit>();
            while (reader.hasNext()) {
                reader.next();
                // TX
                if (reader.isStartElement() && reader.getLocalName().equals("TX")) {
                    list.add(ODSHelper.createStringNVU("desc", reader.getElementText()));
                }
                // names
                else if (reader.isStartElement() && reader.getLocalName().equals("names")) {
                    LOG.warn("'names' in XML content 'SIcomment' is not yet supported!");
                }
                // path
                else if (reader.isStartElement() && reader.getLocalName().equals("path")) {
                    LOG.warn("'path' in XML content 'SIcomment' is not yet supported!");
                }
                // bus
                else if (reader.isStartElement() && reader.getLocalName().equals("bus")) {
                    LOG.warn("'bus' in XML content 'SIcomment' is not yet supported!");
                }
                // protocol
                else if (reader.isStartElement() && reader.getLocalName().equals("bus")) {
                    LOG.warn("'protocol' in XML content 'SIcomment' is not yet supported!");
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {
                    writeCommonProperties(ieCg, reader);
                }
            }
            if (list.size() > 0) {
                ieCg.setValueSeq(list.toArray(new NameValueUnit[0]));
            }
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.error(e.getMessage(), e);
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Writes the content of 'common_properties' from the XML stream reader as ASAM ODS instance attributes.
     * 
     * @param ie THe instance to write to.
     * @param reader The XML stream reader.
     * @return The content.
     * @throws XMLStreamException Error reading XML content.
     * @throws AoException
     * @throws NumberFormatException
     */
    private void writeCommonProperties(InstanceElement ie, XMLStreamReader reader) throws XMLStreamException,
            NumberFormatException, AoException {
        reader.nextTag();
        while (!(reader.isEndElement() && reader.getLocalName().equals("common_properties"))) {
            // e
            if (reader.isStartElement() && reader.getLocalName().equals("e")) {
                String name = reader.getAttributeValue(null, "name");
                String type = reader.getAttributeValue(null, "type");
                String value = reader.getElementText();
                if (type == null || type.length() < 1 || type.equalsIgnoreCase("string")) {
                    ie.addInstanceAttribute(ODSHelper.createStringNVU(name, value));
                } else if (type.equalsIgnoreCase("decimal")) {
                    ie.addInstanceAttribute(ODSHelper.createDoubleNVU(name, Double.valueOf(value)));
                } else if (type.equalsIgnoreCase("integer")) {
                    ie.addInstanceAttribute(ODSHelper.createLongNVU(name, Integer.valueOf(value)));
                } else if (type.equalsIgnoreCase("float")) {
                    ie.addInstanceAttribute(ODSHelper.createFloatNVU(name, Float.valueOf(value)));
                } else if (type.equalsIgnoreCase("boolean")) {
                    short s = Boolean.valueOf(value) ? (short) 1 : (short) 0;
                    ie.addInstanceAttribute(ODSHelper.createShortNVU(name, s));
                } else if (type.equalsIgnoreCase("datetime")) {
                    try {
                        Date date = this.xmlDateTimeFormat.parse(value);
                        ie.addInstanceAttribute(ODSHelper.createDateNVU(name, ODSHelper.asODSDate(date)));
                    } catch (ParseException e) {
                        LOG.warn(e.getMessage(), e);
                        ie.addInstanceAttribute(ODSHelper.createStringNVU(name, value));
                    }
                } else {
                    ie.addInstanceAttribute(ODSHelper.createStringNVU(name, value));
                }
            }
            // tree
            else if (reader.isStartElement() && reader.getLocalName().equals("tree")) {
                LOG.warn("'tree' in XML content 'common_properties' is not yet supported!");
            }
            // list
            else if (reader.isStartElement() && reader.getLocalName().equals("list")) {
                LOG.warn("'list' in XML content 'common_properties' is not yet supported!");
            }
            // elist
            else if (reader.isStartElement() && reader.getLocalName().equals("elist")) {
                LOG.warn("'elist' in XML content 'common_properties' is not yet supported!");
            }
            reader.next();
        }
    }

}
