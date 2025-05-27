package com.peaksolution.openatfx.api;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;

/**
 * Custom Stax filter for only collect start and end elements.
 */
class StartEndElementFilter implements StreamFilter {

    @Override
    public boolean accept(XMLStreamReader myReader) {
        return myReader.isStartElement() || myReader.isEndElement();
    }

}
