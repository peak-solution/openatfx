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
import org.asam.ods.RelationType;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelValueExt;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;

public class QueryConditionHelperTest {
    private static AoSession aoSession;
    private static ApplicationRelation baseInfoRel;
    private static ApplicationRelation baseFCRel;
    private static ApplicationRelation noneBaseFCRel;
    private static ApplicationRelation noneBaseInfoRel1;
    private static ApplicationRelation noneBaseInfoRel2;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = QueryConditionHelperTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        
        BaseRelation baseRel = mock(BaseRelation.class);
        baseInfoRel = mock(ApplicationRelation.class);
        when(baseInfoRel.getBaseRelation()).thenReturn(baseRel);
        when(baseInfoRel.getRelationType()).thenReturn(RelationType.INFO);
        when(baseInfoRel.getRelationName()).thenReturn("baseInfoRel");
        
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
        
        noneBaseInfoRel2 = mock(ApplicationRelation.class);
        when(noneBaseInfoRel2.getBaseRelation()).thenReturn(null);
        when(noneBaseInfoRel2.getRelationType()).thenReturn(RelationType.INFO);
        when(noneBaseInfoRel2.getRelationName()).thenReturn("noneBaseInfoRel2");
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }
    
    @Test
    public void testIdentifyRelevantRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testIdentifyRelevantRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testIdentifyRelevantRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel1);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNotNull();
        assertThat(ar).isEqualTo(baseInfoRel);
    }
    
    @Test
    public void testIdentifyRelevantRelation_noBaseOneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);
        
        ApplicationRelation ar = qch.identifyRelevantRelation(relations);
        
        assertThat(ar).isNotNull();
        assertThat(ar).isEqualTo(noneBaseFCRel);
    }
    
    @Test
    public void testIdentifyRelevantRelation_twoBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(baseInfoRel);
        
        Throwable thrown = catchThrowable(() -> { qch.identifyRelevantRelation(relations); });
        
        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(AoException.class);
    }
    
    @Test
    public void testFindBaseRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testFindBaseRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNull();
    }

    @Test
    public void testFindBaseRelation_oneBaseRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findBaseRelation(relations);
        
        assertThat(ar).isNotNull();
        assertThat(ar).isEqualTo(baseInfoRel);
    }
    
    @Test
    public void testFindBaseRelation_moreBaseRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(baseInfoRel);
        relations.add(noneBaseInfoRel2);
        relations.add(baseFCRel);
        
        Throwable thrown = catchThrowable(() -> { qch.findBaseRelation(relations); });
        
        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(AoException.class);
    }
    
    @Test
    public void testFindFatherChildRelation_noRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNull();
    }
    
    @Test
    public void testFindFatherChildRelation_otherRelations_isNull() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseInfoRel2);
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNull();
    }

    @Test
    public void testFindFatherChildRelation_oneFCRelation_filterCorrectly() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);
        
        ApplicationRelation ar = qch.findFatherChildRelation(relations);
        
        assertThat(ar).isNotNull();
        assertThat(ar).isEqualTo(noneBaseFCRel);
    }
    
    @Test
    public void testFindFatherChildRelation_moreFCRelations_throw() throws Exception {
        QueryConditionHelper qch = new QueryConditionHelper(1, Arrays.asList(1l), null);
        
        List<ApplicationRelation> relations = new ArrayList<>();
        relations.add(noneBaseFCRel);
        relations.add(noneBaseInfoRel1);
        relations.add(noneBaseFCRel);
        
        Throwable thrown = catchThrowable(() -> { qch.findFatherChildRelation(relations); });
        
        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(AoException.class);
    }

    @Test
    public void testFindRelationPath_directRelation() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        long projId = ODSHelper.asJLong(projElem.getId());
        
        AtfxCache atfxCache = mock(AtfxCache.class);
        when(atfxCache.getApplicationRelations(eq(projId))).thenReturn(Arrays.asList(projElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), atfxCache);
        
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
        
        AtfxCache atfxCache = mock(AtfxCache.class);
        when(atfxCache.getApplicationRelations(eq(projId))).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(atfxCache.getApplicationRelations(eq(ODSHelper.asJLong(tstserElem.getId())))).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), atfxCache);
        
        List<ApplicationRelation> relationPath = qch.findRelationPath(projId, ODSHelper.asJLong(meaElem.getId()));
        
        assertThat(relationPath).isNotNull();
        assertThat(relationPath.size()).isEqualTo(2);
        for (int i = 0; i < relationPath.size(); i++) {
            ApplicationRelation ar = relationPath.get(i);
            if (i == 0) {
                assertThat("tstser_iid").isEqualTo(ar.getRelationName());
            } else if (i == 1) {
                assertThat("mea_iid").isEqualTo(ar.getRelationName());
            }
        }
    }
    
    @Test
    public void testFindRelationPath_threeSteps() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement projElem = as.getElementByName("prj");
        ApplicationElement tstserElem = as.getElementByName("tstser");
        ApplicationElement meaElem = as.getElementByName("mea");
        ApplicationElement rbElem = as.getElementByName("randbedingung");
        long projId = ODSHelper.asJLong(projElem.getId());
        
        AtfxCache atfxCache = mock(AtfxCache.class);
        when(atfxCache.getApplicationRelations(eq(projId))).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(atfxCache.getApplicationRelations(eq(ODSHelper.asJLong(tstserElem.getId())))).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        when(atfxCache.getApplicationRelations(eq(ODSHelper.asJLong(meaElem.getId())))).thenReturn(Arrays.asList(meaElem.getAllRelations()));
        QueryConditionHelper qch = new QueryConditionHelper(projId, Arrays.asList(1l), atfxCache);
        
        Throwable thrown = catchThrowable(() -> { qch.findRelationPath(projId, ODSHelper.asJLong(rbElem.getId())); });
        
        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(AoException.class);
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
        AtfxCache atfxCache = mock(AtfxCache.class);
        when(atfxCache.getApplicationRelations(eq(projAid))).thenReturn(Arrays.asList(projElem.getAllRelations()));
        when(atfxCache.getApplicationRelations(eq(tstSerAid))).thenReturn(Arrays.asList(tstserElem.getAllRelations()));
        when(atfxCache.getRelatedInstanceIds(eq(projAid), eq(projectIID), any(ApplicationRelation.class))).thenReturn(Arrays.asList(tstSerIID1, tstSerIID2));
        when(atfxCache.getRelatedInstanceIds(eq(tstSerAid), eq(tstSerIID1), any(ApplicationRelation.class))).thenReturn(Collections.emptyList());
        when(atfxCache.getRelatedInstanceIds(eq(tstSerAid), eq(tstSerIID2), any(ApplicationRelation.class))).thenReturn(Arrays.asList(22l));
        when(atfxCache.getAttrNoByName(eq(meaAid), eq(attrName))).thenReturn(attrNo);
        when(atfxCache.getInstanceValue(eq(meaAid), eq(attrNo), eq(meaIID))).thenReturn(value);
        QueryConditionHelper qch = new QueryConditionHelper(projAid, Arrays.asList(1l), atfxCache);
        
        boolean checkResult = qch.checkConditionOnRelatedInstances(projectIID, ODSHelper.asJLong(condition.attr.attr.aid), condition);
        
        assertThat(checkResult).isTrue();
    }
}
