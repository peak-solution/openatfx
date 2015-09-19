package de.rechner.openatfx_mdf4.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Helper class for performant parsing of the XML content of an MDF4 file.
 * 
 * @author Christian Rechner
 */
public class MDF4XMLParser {

    private static final Log LOG = LogFactory.getLog(MDF4XMLParser.class);

    private final XMLInputFactory xmlInputFactory;

    /**
     * Constructor.
     */
    public MDF4XMLParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
    }

    /**
     * @param ieMea
     * @param mdCommentXML
     * @throws IOException
     * @throws AoException
     */
    public void writeMDCommentToMea(InstanceElement ieMea, String mdCommentXML) throws IOException, AoException {
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
                    LOG.warn("'constants' in XML content 'MDComment' is not yet supported!");
                }
                // UNITSPEC
                else if (reader.isStartElement() && reader.getLocalName().equals("UNITSPEC")) {
                    LOG.warn("UNITSPEC in XML content 'MDComment' is not yet supported!");
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {
                    for (Entry<String, String> entry : readCommonProperties(reader).entrySet()) {
                        ieMea.addInstanceAttribute(ODSHelper.createStringNVU(entry.getKey(), entry.getValue()));
                    }
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
     * Reads the content of 'common_properties' from the XML stream reader.
     * 
     * @param reader The XML stream reader.
     * @return The content.
     * @throws XMLStreamException Error reading XML content.
     */
    private Map<String, String> readCommonProperties(XMLStreamReader reader) throws XMLStreamException {
        reader.nextTag();
        Map<String, String> map = new HashMap<String, String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals("common_properties"))) {
            // e
            if (reader.isStartElement() && reader.getLocalName().equals("e")) {
                map.put(reader.getAttributeValue(null, "name"), reader.getElementText());
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
        return map;
    }

}
