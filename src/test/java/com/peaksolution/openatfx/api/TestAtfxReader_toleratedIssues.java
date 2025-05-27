package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.AoSessionImplTest;
import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;

public class TestAtfxReader_toleratedIssues {
    private static OpenAtfxAPIImplementation api;
    
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        URL url = AoSessionImplTest.class.getResource("/com/peaksolution/openatfx/example_toleratedIncorrect.atfx");
        Path atfxFile = Path.of(url.toURI());
        AtfxReader reader = new AtfxReader(new LocalFileHandler(), atfxFile, false, null);
        
        IFileHandler fileHandler = new LocalFileHandler();
        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = reader.readFile(xmlReader,
                                  Arrays.asList(new NameValueUnit(OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE,
                                                                  DataType.DT_STRING, "true")));
        }
        assertThat(api).isNotNull();
    }
    
    @Test
    void test_missingInverseRelations() {
        // Model relations
        
        // base relation, missing on 1-side
        Element envElement = api.getElementByName("env");
        Element setupElement = api.getElementByName("setup");
        Relation missingRelation = envElement.getRelationByName("setup_iid");
        assertThat(missingRelation).isNotNull();
        assertThat(missingRelation.getRelationRangeMin()).isEqualTo((short) 0);
        assertThat(missingRelation.getRelationRangeMax()).isEqualTo((short) -1);
        assertThat(setupElement.getRelationByName("env_iid")).isNotNull();
        
        // base relation, missing on n-side, default relation range [0, -1] is set
        Element audifahrzeugElement = api.getElementByName("audifahrzeug");
        missingRelation = audifahrzeugElement.getRelationByName("env_iid");
        assertThat(missingRelation).isNotNull();
        assertThat(missingRelation.getRelationRangeMin()).isEqualTo((short) 0);
        assertThat(missingRelation.getRelationRangeMax()).isEqualTo((short) -1);
        assertThat(envElement.getRelationByName("audifahrzeug_iid")).isNotNull();
        
        // non-base relation, missing on 1-side
        Element randbedinungElement = api.getElementByName("randbedingung");
        missingRelation = envElement.getRelationByName("randbedingung_iid");
        assertThat(missingRelation).isNotNull();
        assertThat(missingRelation.getRelationRangeMin()).isEqualTo((short) 0);
        assertThat(missingRelation.getRelationRangeMax()).isEqualTo((short) -1);
        assertThat(randbedinungElement.getRelationByName("env_iid")).isNotNull();
        
        // non-base relation, missing on n-side
        Element messzyklusElement = api.getElementByName("messzyklus");
        missingRelation = messzyklusElement.getRelationByName("env_iid");
        assertThat(missingRelation).isNotNull();
        assertThat(missingRelation.getRelationRangeMin()).isEqualTo((short) 0);
        assertThat(missingRelation.getRelationRangeMax()).isEqualTo((short) -1); // note that actual maximum 1 is not defined and therefore the default relation range is taken as fallback!
        assertThat(envElement.getRelationByName("messzyklus_iid")).isNotNull();
        
        // Instance relations
        
        // check a simple base relation when inverse relation is missing from the one or the other side
        // prj to tstser, missing inverse relation on n-side of relation
        Element projectElement = api.getElementByName("prj");
        Relation prjToTstserRelation = api.getRelationByBaseName(projectElement.getId(), "children");
        List<Long> relatedTstSerIids = api.getRelatedInstanceIds(projectElement.getId(), 1, prjToTstserRelation);
        assertThat(relatedTstSerIids).containsExactlyInAnyOrder(1L, 2L);
        
        // tstser to child mea, missing inverse relation on 1-side of relation
        Element tstSerElement = api.getElementByName("tstser");
        Relation tstserToMeaRelation = api.getRelationByBaseName(tstSerElement.getId(), "children");
        List<Long> relatedChildrenIids = api.getRelatedInstanceIds(tstSerElement.getId(), 2, tstserToMeaRelation);
        assertThat(relatedChildrenIids).containsExactly(22L);
    }
    
    /**
     * Check the AoSubmatrix.x-axis-for-y-axis relation to make sure relation is correctly handled in openatfx.
     */
    @Test
    void test_incorrectRelRangeForNVHRels() {
        String matrixElementName = "sm";
        long matrixIid = 59;
        long xForY = 62;
        long zForY = 68;
        
        Element matrixElement = api.getElementByName(matrixElementName);
        Relation xaxisforyaxis = api.getRelationByName(matrixElement.getId(), "x-axis-for-y-axis");
        Relation zaxisforyaxis = api.getRelationByName(matrixElement.getId(), "z-axis-for-y-axis");
        
        // openatfx corrects the incorrect max value -1, if extended compatibility mode is on, otherwise it throws an
        // exception
        assertThat(xaxisforyaxis.getRelationRangeMax()).isEqualTo((short) 1);
        
        List<Long> relatedIids = api.getRelatedInstanceIds(matrixElement.getId(), matrixIid, xaxisforyaxis);
        assertThat(relatedIids).containsExactly(xForY);
        relatedIids = api.getRelatedInstanceIds(matrixElement.getId(), matrixIid, zaxisforyaxis);
        assertThat(relatedIids).containsExactly(zForY);
    }
}
