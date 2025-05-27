package com.peaksolution.openatfx.api.corba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ElemResultSetExt;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.JoinDef;
import org.asam.ods.NameValueSeqUnitId;
import org.asam.ods.NameValueUnit;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.RelationRange;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelOrder;
import org.asam.ods.SelValueExt;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.UnitTestFileHandler;
import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.io.AtfxReaderTest</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class CorbaAtfxReaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(CorbaAtfxReaderTest.class);

    private static ORB orb;

    @BeforeAll
    public static void setUpBeforeClass() {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    void testGetSession() throws URISyntaxException {
        try {
            URL url = CorbaAtfxReaderTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
            CorbaAtfxReader reader = new CorbaAtfxReader();
            IFileHandler fileHandler = new LocalFileHandler();
            Path path = Paths.get(url.toURI()).toAbsolutePath();
            reader.init(orb, fileHandler, path);
            AoSession aoSession = reader.getSession();
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }

    @Test
    void testReadInstanceAttributesWithUnits() throws URISyntaxException {
        try {
            URL url = CorbaAtfxReaderTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
            CorbaAtfxReader reader = new CorbaAtfxReader();
            IFileHandler fileHandler = new LocalFileHandler();
            Path path = Paths.get(url.toURI()).toAbsolutePath();
            reader.init(orb, fileHandler, path);
            AoSession aoSession = reader.getSession();
            
            ApplicationElement projectElement = aoSession.getApplicationStructure().getElementByName("prj");
            InstanceElement projectInstance = projectElement.getInstanceByName("no_project");
            NameValueUnit[] nvus = projectInstance.getValueSeq(new String[] {"inst_attr_dt_float", "inst_attr_dt_double", "inst_attr_dt_long"});
            for (int i = 0; i < nvus.length; i++) {
                NameValueUnit value = nvus[i];
                switch(i) {
                    case 0:
                        assertEquals("inst_attr_dt_float", value.valName);
                        // checks the correct case, when the unit iid is defined at the instance attribute in atfx
                        assertEquals("Pa", value.unit);
                        break;
                    case 1:
                        assertEquals("inst_attr_dt_double", value.valName);
                        assertThat(value.unit).isEmpty();
                        break;
                    case 2:
                        assertEquals("inst_attr_dt_long", value.valName);
                        // checks the tolerated but actually incorrect definition of the unit's name for the instance
                        // attribute in atfx
                        assertEquals("s", value.unit);
                        break;
                }
            }
            
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }
    
    /**
     * Tests the tolerance of following incorrectnesses in atfx:
     * <ul>
     * <li>missing inverse relations, both sides</li>
     * <li>missing inverse relations (self-relation of element)</li>
     * <li>extended relation range</li>
     * </ul>
     * @throws URISyntaxException 
     */
    @Test
    void testReadInstance_toleratedIncorrectnessesInAtfx() throws URISyntaxException {
        try {
            URL url = CorbaAtfxReaderTest.class.getResource("/com/peaksolution/openatfx/example_toleratedIncorrect.atfx");
            CorbaAtfxReader reader = new CorbaAtfxReader();
            reader.init(orb, new UnitTestFileHandler(), Paths.get(url.toURI()).toAbsolutePath(),
                        ODSHelper.createBooleanNV(OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE, true));
            AoSessionImpl aoSession = reader.getSessionImpl();
            
            // check a simple base relation when inverse relation is missing from the one or the other side
            // prj to tstser, missing inverse relation on n-side of relation
            ApplicationElement prjElement = aoSession.getApplicationStructure().getElementByName("prj");
            ApplicationRelation[] prjChildrenRelations = prjElement.getRelationsByBaseName("children");
            ApplicationRelation prjToTstserRelation = prjChildrenRelations[0];
            InstanceElement prjInstance = prjElement.getInstanceById(ODSHelper.asODSLongLong(1));
            InstanceElementIterator tstserIeIterator = prjInstance.getRelatedInstances(prjToTstserRelation, "*");
            assertThat(tstserIeIterator.getCount()).isEqualTo(2);
            for (int i = 0; i < tstserIeIterator.getCount(); i++) {
                assertThat(ODSHelper.asJLong(tstserIeIterator.nextOne().getId())).isIn(Arrays.asList(new Long[] {1L, 2L}));
            }
            // tstser to child mea, missing inverse relation on 1-side of relation
            ApplicationElement tstserElement = aoSession.getApplicationStructure().getElementByName("tstser");
            ApplicationRelation[] tstserChildrenRelations = tstserElement.getRelationsByBaseName("children");
            ApplicationRelation tstserToMeaRelation = tstserChildrenRelations[0];
            InstanceElement tstserInstance = tstserElement.getInstanceById(ODSHelper.asODSLongLong(2));
            InstanceElementIterator meaIeIterator = tstserInstance.getRelatedInstances(tstserToMeaRelation, "*");
            assertThat(meaIeIterator.getCount()).isEqualTo(1);
            assertThat(ODSHelper.asJLong(meaIeIterator.nextOne().getId())).isEqualTo(22);
            
            // check several ways to request the AoSubmatrix.x-axis-for-y-axis relation to make sure relation is correctly handled in openatfx
            String matrixElementName = "sm";
            String matrixIdAttributeName = "sm_iid";
            long matrixIid = 59;
            long xForY = 62;
            long zForY = 68;
            
            ApplicationElement matrixElement = aoSession.getApplicationStructure().getElementByName(matrixElementName);
            ApplicationRelation xaxisforyaxis = null;
            ApplicationRelation zaxisforyaxis = null;
            for (ApplicationRelation ar : matrixElement.getAllRelations()) {
                if (ar.getRelationName().equals("x-axis-for-y-axis")) {
                    xaxisforyaxis = ar;
                } else if (ar.getRelationName().equals("z-axis-for-y-axis")) {
                    zaxisforyaxis = ar;
                }
            }
            
            // openatfx is expected to fix the incorrect relation range max from -1 to 1, since extended compatibility
            // mode is on (otherwise exception would be thrown)
            RelationRange relRange = xaxisforyaxis.getRelationRange();
            assertThat(relRange.max).isEqualTo((short)1);
            
            InstanceElement matrixInstance = matrixElement.getInstanceById(ODSHelper.asODSLongLong(matrixIid));
            InstanceElementIterator ieIterator = matrixInstance.getRelatedInstances(xaxisforyaxis, "*");
            assertThat(ieIterator.getCount()).isEqualTo(1);
            assertThat(ODSHelper.asJLong(ieIterator.nextOne().getId())).isEqualTo(xForY);
            
            ieIterator = matrixInstance.getRelatedInstances(zaxisforyaxis, "*");
            assertThat(ieIterator.getCount()).isEqualTo(1);
            assertThat(ODSHelper.asJLong(ieIterator.nextOne().getId())).isEqualTo(zForY);
            
            QueryStructureExt qse = new QueryStructureExt();
            qse.anuSeq = new SelAIDNameUnitId[] {
                    new SelAIDNameUnitId(new AIDName(matrixElement.getId(), "*"),
                                         new T_LONGLONG(), AggrFunc.NONE) };
            SelItem sel = new SelItem();
            TS_Union u = new TS_Union();
            u.longlongVal(ODSHelper.asODSLongLong(xForY));
            TS_Value val = new TS_Value(u, (short)15);
            sel.value(new SelValueExt(new AIDNameUnitId(new AIDName(matrixElement.getId(), xaxisforyaxis.getRelationName()), new T_LONGLONG()), SelOpcode.EQ, val));
            qse.condSeq = new SelItem[] {sel};
            qse.joinSeq = new JoinDef[0];
            qse.groupBy = new AIDName[0];
            qse.orderBy = new SelOrder[0];
            ResultSetExt[] rses = aoSession.getApplElemAccess().getInstancesExt(qse, 0);
            boolean instanceFound = false;
            for (ResultSetExt rse : rses) {
                for (ElemResultSetExt erse : rse.firstElems) {
                    for (NameValueSeqUnitId nvsui : erse.values) {
                        if (matrixIdAttributeName.equals(nvsui.valName)) {
                            assertThat(ODSHelper.asJLong(nvsui.value.u.longlongVal()[0])).isEqualTo(matrixIid);
                            instanceFound = true;
                        }
                    }
                }
            }
            assertThat(instanceFound).isTrue();
            
            sel = new SelItem();
            u = new TS_Union();
            u.longlongSeq(new T_LONGLONG[] { ODSHelper.asODSLongLong(xForY), ODSHelper.asODSLongLong(101) });
            val = new TS_Value(u, (short)15);
            sel.value(new SelValueExt(new AIDNameUnitId(new AIDName(matrixElement.getId(), xaxisforyaxis.getRelationName()), new T_LONGLONG()), SelOpcode.INSET, val));
            qse.condSeq = new SelItem[] {sel};
            rses = aoSession.getApplElemAccess().getInstancesExt(qse, 0);
            instanceFound = false;
            int relationsFound = 0;
            for (ResultSetExt rse : rses) {
                for (ElemResultSetExt erse : rse.firstElems) {
                    for (NameValueSeqUnitId nvsui : erse.values) {
                        if (matrixIdAttributeName.equals(nvsui.valName)) {
                            assertThat(ODSHelper.asJLong(nvsui.value.u.longlongVal())).contains(matrixIid, 83L);
                            instanceFound = true;
                        } else if (xaxisforyaxis.getRelationName().equals(nvsui.valName)) {
                            relationsFound++;
                        } else if (zaxisforyaxis.getRelationName().equals(nvsui.valName)) {
                            relationsFound++;
                        }
                    }
                }
            }
            assertThat(instanceFound).isTrue();
            assertThat(relationsFound).isEqualTo(2);
            
            InstanceElementIterator iterator = matrixElement.getInstances("*");
            List<Long> allMatrixIids = new ArrayList<>();
            for (int i = 0; i < iterator.getCount(); i++) {
                allMatrixIids.add(ODSHelper.asJLong(iterator.nextOne().getId()));
            }
            List<Long> relatedIidsForSpecificInstance = aoSession.getAtfxCache().getRelatedInstanceIds(ODSHelper.asJLong(matrixElement.getId()), matrixIid, xaxisforyaxis);
            assertThat(relatedIidsForSpecificInstance).containsExactly(xForY);
            
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }
    
    @Test
    void testGetAutogeneratedValueForBaseAttributes() throws URISyntaxException, AoException {
        URL url = CorbaAtfxReaderTest.class.getResource("/com/peaksolution/openatfx/example_asam36.atfx");
        CorbaAtfxReader reader = new CorbaAtfxReader();
        IFileHandler fileHandler = new LocalFileHandler();
        Path path = Paths.get(url.toURI()).toAbsolutePath();
        reader.init(orb, fileHandler, path);
        AoSession aoSession = reader.getSession();
        
        ApplicationElement measurementElement = aoSession.getApplicationStructure().getElementByName("dts");
        ApplicationAttribute sizeAttr = measurementElement.getAttributeByBaseName("ao_mea_size");
        assertThat(sizeAttr.isAutogenerated()).isTrue();
        
        aoSession.close();
    }
}
