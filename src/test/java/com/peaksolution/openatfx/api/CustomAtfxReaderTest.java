package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;

@Disabled
public class CustomAtfxReaderTest {
    private static OpenAtfxAPIImplementation api;
    private static int baseModelVersionNr;
    
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        Path atfxFile = Paths.get("<filePath>");
        AtfxReader reader = new AtfxReader(new LocalFileHandler(), atfxFile, false, null);
        baseModelVersionNr = 35; // the current model version of the example.atfx file

        IFileHandler fileHandler = new LocalFileHandler();
        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = reader.readFile(xmlReader, Collections.emptyList());
        }
        assertThat(api).isNotNull();
        
        api.setContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT, DataType.DT_STRING, atfxFile.getParent().toString()));
    }
    
    @Test
    void test() {
        
        Element element = api.getElementByName("LocalColumn");
        Attribute attr = element.getAttributeByBaseName("name");
        assertThat(attr.isUnique()).isTrue();
    }
}
