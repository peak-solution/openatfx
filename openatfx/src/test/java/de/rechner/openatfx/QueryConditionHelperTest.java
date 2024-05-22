package de.rechner.openatfx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseRelation;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;

@ExtendWith(GlassfishCorbaExtension.class)
public class QueryConditionHelperTest {
    private static AoSession aoSession;
    private static ApplicationRelation baseInfoRel;
    private static ApplicationRelation baseFCRel;
    private static ApplicationRelation noneBaseFCRel;
    private static ApplicationRelation noneBaseInfoRel1;
    private static ApplicationRelation noneBaseInfoRel2;
    private static ApplicationRelation m2nRelation1;
    private static ApplicationRelation m2nRelation2;
    private static AtfxCache cacheMock;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = QueryConditionHelperTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        
        ApplicationElement elem1 = mock(ApplicationElement.class);
        when(elem1.getId()).thenReturn(new T_LONGLONG(0, 1));
        
        ApplicationElement elem2 = mock(ApplicationElement.class);
        when(elem2.getId()).thenReturn(new T_LONGLONG(0, 2));
        
        BaseRelation baseRel = mock(BaseRelation.class);
        baseInfoRel = mock(ApplicationRelation.class);
        when(baseInfoRel.getBaseRelation()).thenReturn(baseRel);
        when(baseInfoRel.getRelationType()).thenReturn(RelationType.INFO);
        when(baseInfoRel.getRelationName()).thenReturn("baseInfoRel");
        when(baseInfoRel.getElem1()).thenReturn(elem1);
        when(baseInfoRel.getElem2()).thenReturn(elem2);
        
        baseFCRel = mock(ApplicationRelation.class);
        when(baseFCRel.getBaseRelation()).thenReturn(baseRel);
        when(baseFCRel.getRelationType()).thenReturn(RelationType.FATHER_CHILD);
        when(baseFCRel.getRelationName()).thenReturn("baseFCRel");
        
        noneBaseFCRel = mock(ApplicationRelation.class);
        when(noneBaseFCRel.getBaseRelation()).thenReturn(null);
        when(noneBaseFCRel.getRelationType()).thenReturn(RelationType.FATHER_CHILD);
        when(noneBaseFCRel.getRelationName()).thenReturn("noneBaseFCRel");
        
        noneBaseInfoRel1 = mock(ApplicationRelation.class);
        when(noneBaseInfoRel1.getBaseRelation()).thenReturn(null);
        when(noneBaseInfoRel1.getRelationType()).thenReturn(RelationType.INFO);
        when(noneBaseInfoRel1.getRelationName()).thenReturn("noneBaseInfoRel1");
        when(noneBaseInfoRel1.getElem1()).thenReturn(elem1);
        when(noneBaseInfoRel1.getElem2()).thenReturn(elem2);
        
        noneBaseInfoRel2 = mock(ApplicationRelation.class);
        when(noneBaseInfoRel2.getBaseRelation()).thenReturn(null);
        when(noneBaseInfoRel2.getRelationType()).thenReturn(RelationType.INFO);
        when(noneBaseInfoRel2.getRelationName()).thenReturn("noneBaseInfoRel2");
        
        m2nRelation1 = mock(ApplicationRelation.class);
        when(m2nRelation1.getBaseRelation()).thenReturn(null);
        when(m2nRelation1.getRelationType()).thenReturn(RelationType.INFO);
        when(m2nRelation1.getRelationName()).thenReturn("m2nRel1");
        when(m2nRelation1.getInverseRelationName()).thenReturn("inverse1");
        when(m2nRelation1.getElem1()).thenReturn(elem1);
        when(m2nRelation1.getElem2()).thenReturn(elem2);
        
        m2nRelation2 = mock(ApplicationRelation.class);
        when(m2nRelation2.getBaseRelation()).thenReturn(null);
        when(m2nRelation2.getRelationType()).thenReturn(RelationType.INFO);
        when(m2nRelation2.getRelationName()).thenReturn("m2nRel2");
        when(m2nRelation2.getInverseRelationName()).thenReturn("inverse2");
        when(m2nRelation2.getElem1()).thenReturn(elem1);
        when(m2nRelation2.getElem2()).thenReturn(elem2);
    }
    
    @BeforeEach
    public void prepareMock()
    {
        cacheMock = Mockito.mock(AtfxCache.class);
        Mockito.when(cacheMock.getContext()).thenReturn(Collections.emptyMap());
    }
    
    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }
    
    @Test
    public void testIdentifyRelevantRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testIdentifyRelevantRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testIdentifyRelevantRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel1);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNotNull().isEqualTo(baseInfoRel);
    }
    
    @Test
    public void testIdentifyRelevantRelation_noBaseOneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNotNull().isEqualTo(noneBaseFCRel);
    }
    
    @Test
    public void testIdentifyRelevantRelation_twoBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(baseInfoRel);
        
        Throwable thrown = catchThrowable(() -> { qch.identifyRelevantRelation(relations); });
        
        assertThat(thrown).isNotNull().isInstanceOf(AoException.class);
    }
    
    @Test
    public void testIdentifyRelevantRelation_m2nRelationJoin() throws Exception {
        JoinDef join = new JoinDef(new T_LONGLONG(0, 1), new T_LONGLONG(0, 2), "m2nRel2", JoinType.JTDEFAULT);
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), new JoinDef[] {join}, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel1);
        relations.add(m2nRelation1);
        relations.add(m2nRelation2);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNotNull().isEqualTo(m2nRelation2);
    }
    
    @Test
    public void testFindBaseRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testFindBaseRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNull();
    }

    @Test
    public void testFindBaseRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNotNull().isEqualTo(baseInfoRel);
    }
    
    @Test
    public void testFindBaseRelation_moreBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);
        relations.add(baseFCRel);
        
        Throwable thrown = catchThrowable(() -> { qch.findBaseRelation(relations); });
        
        assertThat(thrown).isNotNull().isInstanceOf(AoException.class);
    }
    
    @Test
    public void testFindFatherChildRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testFindFatherChildRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNull();
    }

    @Test
    public void testFindFatherChildRelation_oneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNotNull().isEqualTo(noneBaseFCRel);
    }
    
    @Test
    public void testFindFatherChildRelation_moreFCRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);
        
        Throwable thrown = catchThrowable(() -> { qch.findFatherChildRelation(relations); });
        
        assertThat(thrown).isNotNull().isInstanceOf(AoException.class);
    }

    @Test
    public void testFindRelationPath_directRelation() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        long projId = ODSHelper.asJLong(projElem.getId());
        
        when(cacheMock.getApplicationRelations(projId)).thenReturn(Arrays.asList(projElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> ar = qch.findRelationPath(projId, ODSHelper.asJLong(tstserElem.getId()));
        
        assertThat(ar).isNotNull();
    }
    
    @Test
    public void testFindRelationPath_twoSteps() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        ApplicationElement meaElem = as.getElementByName("mea");
        long projId = ODSHelper.asJLong(projElem.getId());
        
        when(cacheMock.getApplicationRelations(projId)).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(cacheMock.getApplicationRelations(ODSHelper.asJLong(tstserElem.getId()))).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, cacheMock);
        
        List<ApplicationRelation> relationPath = qch.findRelationPath(projId, ODSHelper.asJLong(meaElem.getId()));
        
        assertThat(relationPath).isNotNull().hasSize(2);
        for (int i = 0; i < relationPath.size(); i++) {
            ApplicationRelation ar = relationPath.get(i);
            if (i == 0) {
                assertThat(ar.getRelationName()).isEqualTo("tstser_iid");
            } else if (i == 1) {
                assertThat(ar.getRelationName()).isEqualTo("mea_iid");
            }
        }
    }
    
    @Test
    public void testFindRelationPath_exceedMaxRelJumps() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        ApplicationElement meaElem = as.getElementByName("mea");
        ApplicationElement matrixElem = as.getElementByName("sm");
        ApplicationElement lcElem = as.getElementByName("lc");
        long projId = ODSHelper.asJLong(projElem.getId());
        
        when(cacheMock.getApplicationRelations(projId)).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(cacheMock.getApplicationRelations(ODSHelper.asJLong(tstserElem.getId()))).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        when(cacheMock.getApplicationRelations(ODSHelper.asJLong(meaElem.getId()))).thenReturn(Arrays.asList(meaElem.getAllRelations()));
        when(cacheMock.getApplicationRelations(ODSHelper.asJLong(matrixElem.getId()))).thenReturn(Arrays.asList(matrixElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), null, cacheMock);
        
        Throwable thrown = catchThrowable(() -> { qch.findRelationPath(projId, ODSHelper.asJLong(lcElem.getId())); });
        
        assertThat(thrown).isNotNull().isInstanceOf(AoException.class);
    }

    @Test
    public void testCheckConditionOnRelatedInstances() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        ApplicationElement meaElem = as.getElementByName("mea");
        long projAid = ODSHelper.asJLong(projElem.getId());
        long projectIID = 1;
        long tstSerAid = ODSHelper.asJLong(tstserElem.getId());
        long tstSerIID1 = 1;
        long tstSerIID2 = 2;
        long meaAid = ODSHelper.asJLong(meaElem.getId());
        long meaIID = 22;
        
        // prepare attribute and value
        String attrName = "typ";
        int attrNo = 5;
        String attrValue = "OCT,GES";
        TS_Value value = ODSHelper.createEmptyTS_Value(DataType.DT_STRING);
        value.flag = 15;
        value.u.stringVal(attrValue);
        
        // prepare condition
        SelValueExt condition = new SelValueExt();
        AIDName attr = new AIDName(meaElem.getId(), attrName);
        AIDNameUnitId anui = new AIDNameUnitId(attr, new T_LONGLONG(0, 0));
        condition.attr = anui;
        condition.value = value;
        condition.oper = SelOpcode.EQ;
        
        // mock the atfxCache for this test case
        when(cacheMock.getApplicationRelations(projAid)).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(cacheMock.getApplicationRelations(tstSerAid)).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        when(cacheMock.getRelatedInstanceIds(eq(projAid), eq(projectIID), any(ApplicationRelation.class))).thenReturn(Arrays.asList(tstSerIID1, tstSerIID2));
        when(cacheMock.getRelatedInstanceIds(eq(tstSerAid), eq(tstSerIID1), any(ApplicationRelation.class))).thenReturn(Collections.emptyList());
        when(cacheMock.getRelatedInstanceIds(eq(tstSerAid), eq(tstSerIID2), any(ApplicationRelation.class))).thenReturn(Arrays.asList(22l));
        when(cacheMock.getAttrNoByName(meaAid, attrName)).thenReturn(attrNo);
        when(cacheMock.getInstanceValue(meaAid, attrNo, meaIID)).thenReturn(value);
        QueryConditionHelper qch = new QueryConditionHelper(projAid, Arrays.asList(1l), null, cacheMock);
        
        boolean checkResult = qch.checkConditionOnRelatedInstances(projectIID, ODSHelper.asJLong(condition.attr.attr.aid), condition);
        
        assertThat(checkResult).isTrue();
    }
}
