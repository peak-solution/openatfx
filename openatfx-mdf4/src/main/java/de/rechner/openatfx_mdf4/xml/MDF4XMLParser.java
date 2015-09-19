package de.rechner.openatfx_mdf4.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.InstanceElement;


/**
 * @author fgaaw8t
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
     */
    public void writeMDCommentToMea(InstanceElement ieMea, String mdCommentXML) throws IOException {
        XMLStreamReader reader = null;
        try {
            reader = this.xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));

            while (reader.hasNext()) {
                reader.next();

                // TX
                if (reader.isStartElement() && reader.getLocalName().equals("TX")) {
                    String comment = reader.getElementText();

                }
                // time_source
                else if (reader.isStartElement() && reader.getLocalName().equals("time_source")) {
                    String timeSoure = reader.getElementText();

                }
                // constants
                else if (reader.isStartElement() && reader.getLocalName().equals("constants")) {

                }
                // UNITSPEC
                else if (reader.isStartElement() && reader.getLocalName().equals("UNITSPEC")) {
                    LOG.warn("UNITSPEC in XML content is not yet supported!");
                }
                // common_properties
                else if (reader.isStartElement() && reader.getLocalName().equals("common_properties")) {

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

    private Map<String, String> readCommonProperties(XMLStreamReader reader) {
        // reader.nextTag();
        // Map<String, String> map = new HashMap<String, String>();
        // while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION))) {
        // if (reader.isStartElement()) {
        // map.put(reader.getLocalName(), reader.getElementText());
        // }
        // reader.nextTag();
        // }
        return Collections.EMPTY_MAP;
    }

}
