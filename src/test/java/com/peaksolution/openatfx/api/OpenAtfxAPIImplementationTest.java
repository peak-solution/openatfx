package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;

public class OpenAtfxAPIImplementationTest {
    private static OpenAtfxAPIImplementation api;
    
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        URL url = OpenAtfxAPIImplementationTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        Path atfxFile = Path.of(url.toURI());
        String fileRoot = Paths.get(url.toURI()).getParent().toString();
        IFileHandler fileHandler = new LocalFileHandler();
        AtfxReader reader = new AtfxReader(fileHandler, atfxFile, false, null);
        
        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = reader.readFile(xmlReader, Collections.emptyList());
        }
        assertThat(api).isNotNull();
        
        api.setContext(new NameValueUnit("FILE_ROOT", DataType.DT_STRING, fileRoot));
    }
    
    @Test
    void testGetBaseModel_enums() {
        BaseModel baseModel = api.getBaseModel();
        EnumerationDefinition typespecEnum = baseModel.getEnumDef("typespec_enum");
        assertThat(typespecEnum).isNotNull();
        long item = typespecEnum.getItem("IEEEFLOAT4", false);
        assertThat(item).isEqualTo(5);
        OpenAtfxException thrown = null;
        try {
            typespecEnum.getItem("IEEEFLOAT4", true);
        } catch (OpenAtfxException ex) {
            thrown = ex;
        }
        assertThat(thrown).isNotNull();
    }
}
