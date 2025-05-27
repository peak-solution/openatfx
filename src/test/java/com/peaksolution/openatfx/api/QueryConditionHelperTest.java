package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AoSession;
import org.asam.ods.DataType;
import org.asam.ods.JoinDef;
import org.asam.ods.JoinType;
import org.asam.ods.RelationType;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelValueExt;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.util.ODSHelper;


@ExtendWith(GlassfishCorbaExtension.class)
public class QueryConditionHelperTest {

    private static AoSession aoSession;
    private static AtfxElement measurementElement;
    private static AtfxElement subTestElementAtfx;
    private static AtfxRelation baseInfoRel;
    private static AtfxRelation baseFCRel;
    /**
     * actually impossible for ODS to have a father/child relation that is not defined in the base model
     */
    private static AtfxRelation noneBaseFCRel;
    private static AtfxRelation noneBaseInfoRel1;
    private static AtfxRelation noneBaseInfoRel2;
    private static AtfxRelation m2nRelation1;
    private static AtfxRelation m2nRelation2;
    private static OpenAtfxAPIImplementation api;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        URL url = QueryConditionHelperTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        Path atfxFile = Path.of(url.toURI());
        String fileRoot = Paths.get(url.toURI()).getParent().toString();
        AtfxReader reader = new AtfxReader(new LocalFileHandler(), atfxFile, false, null);

        IFileHandler fileHandler = new LocalFileHandler();
        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = reader.readFile(xmlReader, Collections.emptyList());
        }
        assertThat(api).isNotNull();

        api.setContext(new NameValueUnit("FILE_ROOT", com.peaksolution.openatfx.api.DataType.DT_STRING, fileRoot));

        Element measurementElem = api.getElementByName("dts");
        measurementElement = api.getAtfxElement(measurementElem.getId());
        baseInfoRel = measurementElement.getRelationByName("audifm_iid");
        baseFCRel = measurementElement.getRelationByName("mea_iid");
        noneBaseInfoRel1 = measurementElement.getRelationByName("pas_iid");
        noneBaseInfoRel2 = measurementElement.getRelationByName("geometry");
        m2nRelation1 = measurementElement.getRelationByName("audifahrzeug_iid");
        
        Element subTestElement = api.getElementByName("mea");
        subTestElementAtfx = api.getAtfxElement(subTestElement.getId());
        m2nRelation2 = subTestElementAtfx.getRelationByName("audifm.passbyresult_iid");

        noneBaseFCRel = Mockito.mock(AtfxRelation.class);
        Mockito.when(noneBaseFCRel.getBaseRelation()).thenReturn(null);
        Mockito.when(noneBaseFCRel.getRelationType()).thenReturn(RelationType.FATHER_CHILD);
        Mockito.when(noneBaseFCRel.getRelationName()).thenReturn("noneBaseFCRel");
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    void testIdentifyRelevantRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();

        AtfxRelation ar = qch.identifyRelevantRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testIdentifyRelevantRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);

        AtfxRelation ar = qch.identifyRelevantRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testIdentifyRelevantRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel1);

        AtfxRelation ar = qch.identifyRelevantRelation(relations);

        assertThat(ar).isNotNull().isEqualTo(baseInfoRel);
    }

    @Test
    void testIdentifyRelevantRelation_noBaseOneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);

        AtfxRelation ar = qch.identifyRelevantRelation(relations);

        assertThat(ar).isNotNull().isEqualTo(noneBaseFCRel);
    }

    @Test
    void testIdentifyRelevantRelation_twoBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(baseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(baseInfoRel);

        Throwable thrown = catchThrowable(() -> {
            qch.identifyRelevantRelation(relations);
        });

        assertThat(thrown).isNotNull().isInstanceOf(OpenAtfxException.class);
    }

    @Test
    void testIdentifyRelevantRelation_m2nRelationJoin() throws Exception {
        JoinDef join = new JoinDef(ODSHelper.asODSLongLong(m2nRelation2.getAtfxElement1().getId()),
                                   ODSHelper.asODSLongLong(m2nRelation2.getAtfxElement2().getId()),
                                   m2nRelation2.getRelationName(), JoinType.JTDEFAULT);
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), new JoinDef[] { join }, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel1);
        relations.add(m2nRelation1);
        relations.add(m2nRelation2);

        AtfxRelation ar = qch.identifyRelevantRelation(relations);

        assertThat(ar).isNotNull().isEqualTo(m2nRelation2);
    }

    @Test
    void testFindBaseRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();

        AtfxRelation ar = qch.findBaseRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testFindBaseRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);

        AtfxRelation ar = qch.findBaseRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testFindBaseRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);

        AtfxRelation ar = qch.findBaseRelation(relations);

        assertThat(ar).isNotNull().isEqualTo(baseInfoRel);
    }

    @Test
    void testFindBaseRelation_moreBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);
        relations.add(baseFCRel);

        Throwable thrown = catchThrowable(() -> {
            qch.findBaseRelation(relations);
        });

        assertThat(thrown).isNotNull().isInstanceOf(OpenAtfxException.class);
    }

    @Test
    void testFindFatherChildRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();

        AtfxRelation ar = qch.findFatherChildRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testFindFatherChildRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);

        AtfxRelation ar = qch.findFatherChildRelation(relations);

        assertThat(ar).isNull();
    }

    @Test
    void testFindFatherChildRelation_oneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);

        AtfxRelation ar = qch.findFatherChildRelation(relations);

        assertThat(ar).isNotNull().isEqualTo(noneBaseFCRel);
    }

    @Test
    void testFindFatherChildRelation_moreFCRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, api);

        List<AtfxRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);

        Throwable thrown = catchThrowable(() -> {
            qch.findFatherChildRelation(relations);
        });

        assertThat(thrown).isNotNull().isInstanceOf(OpenAtfxException.class);
    }

    @Test
    void testFindRelationPath_directRelation() throws Exception {
        long projId = api.getElementByName("prj").getId();
        long tstserId = api.getElementByName("tstser").getId();

        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, api);
        List<AtfxRelation> ar = qch.findRelationPath(projId, tstserId);

        assertThat(ar).isNotNull();
    }

    @Test
    void testFindRelationPath_twoSteps() throws Exception {
        long meaId = api.getElementByName("mea").getId();
        long projId = api.getElementByName("prj").getId();

        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, api);
        List<AtfxRelation> relationPath = qch.findRelationPath(projId, meaId);

        assertThat(relationPath).isNotNull().hasSize(2);
        for (int i = 0; i < relationPath.size(); i++) {
            AtfxRelation ar = relationPath.get(i);
            if (i == 0) {
                assertThat(ar.getRelationName()).isEqualTo("tstser_iid");
            } else if (i == 1) {
                assertThat(ar.getRelationName()).isEqualTo("mea_iid");
            }
        }
    }

    @Test
    void testFindRelationPath_exceedMaxRelJumps() throws Exception {
        long projId = api.getElementByName("prj").getId();
        long lcId = api.getElementByName("lc").getId();

        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, api);
        Throwable thrown = catchThrowable(() -> {
            qch.findRelationPath(projId, lcId);
        });

        assertThat(thrown).isNotNull().isInstanceOf(OpenAtfxException.class);
    }

    @Test
    void testCheckConditionOnRelatedInstances() throws Exception {
        long projAid = api.getElementByName("prj").getId();
        long projectIID = 1;

        // prepare attribute and value
        String attrName = "typ";
        String attrValue = "OCT,GES";
        TS_Value value = ODSHelper.createEmptyTS_Value(DataType.DT_STRING);
        value.flag = 15;
        value.u.stringVal(attrValue);

        // prepare condition
        SelValueExt condition = new SelValueExt();
        AIDName attr = new AIDName(ODSHelper.asODSLongLong(subTestElementAtfx.getId()), attrName);
        AIDNameUnitId anui = new AIDNameUnitId(attr, new T_LONGLONG(0, 0));
        condition.attr = anui;
        condition.value = value;
        condition.oper = SelOpcode.EQ;

        QueryConditionHelper qch = new QueryConditionHelper(projAid, Arrays.asList(1l), null, api);
        boolean checkResult = qch.checkConditionOnRelatedInstances(projectIID,
                                                                   ODSHelper.asJLong(condition.attr.attr.aid),
                                                                   condition);

        assertThat(checkResult).isTrue();
    }
}
