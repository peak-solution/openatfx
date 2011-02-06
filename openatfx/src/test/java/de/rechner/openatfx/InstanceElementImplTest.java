package de.rechner.openatfx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import junit.framework.JUnit4TestAdapter;

import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.T_ExternalReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.InstanceElement</code>.
 * 
 * @author Christian Rechner
 */
public class InstanceElementImplTest {

    private static AoSession aoSession;
    private static InstanceElement ieMeasurement;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        BasicConfigurator.configure();
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement applicationElement = applicationStructure.getElementByName("dts");
        ieMeasurement = applicationElement.getInstanceById(ODSHelper.asODSLongLong(32));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetApplicationElement() {
        try {
            assertEquals("dts", ieMeasurement.getApplicationElement().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testCompare() {
        try {
            ApplicationElement aeUnit = aoSession.getApplicationStructure().getElementByName("unt");
            InstanceElement ieS1 = aeUnit.getInstanceByName("s");
            InstanceElement ieS2 = aeUnit.getInstanceByName("m");
            InstanceElement ieS3 = aeUnit.getInstanceById(ODSHelper.asODSLongLong(42));

            // different application elements
            assertEquals(-1, ODSHelper.asJLong(ieS2.compare(ieMeasurement)));
            assertEquals(1, ODSHelper.asJLong(ieMeasurement.compare(ieS2)));

            // different instances
            assertEquals(-1, ODSHelper.asJLong(ieS1.compare(ieS2)));
            assertEquals(1, ODSHelper.asJLong(ieS2.compare(ieS1)));

            // same instance
            assertEquals(0, ODSHelper.asJLong(ieS1.compare(ieS3)));

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetId() {
        try {
            assertEquals(32, ODSHelper.asJLong(ieMeasurement.getId()));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetName() {
        try {
            assertEquals("Detector;rms A fast - Zusammenfassung", ieMeasurement.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetName() {
        try {
            assertEquals("Detector;rms A fast - Zusammenfassung", ieMeasurement.getName());
            ieMeasurement.setName("new name");
            assertEquals("new name", ieMeasurement.getName());
            ieMeasurement.setName("Detector;rms A fast - Zusammenfassung");
            assertEquals("Detector;rms A fast - Zusammenfassung", ieMeasurement.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListAttributes() {
        try {
            assertEquals(6, ieMeasurement.listAttributes("*", AttrType.ALL).length);
            assertEquals(1, ieMeasurement.listAttributes("*iid", AttrType.ALL).length);
            assertEquals(6, ieMeasurement.listAttributes("*", AttrType.APPLATTR_ONLY).length);
            assertEquals(1, ieMeasurement.listAttributes("*iid", AttrType.APPLATTR_ONLY).length);
            assertEquals(0, ieMeasurement.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            // test listing of instance attributes
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            assertEquals(7, ieMeasurement.listAttributes("*", AttrType.ALL).length);
            assertEquals(6, ieMeasurement.listAttributes("*", AttrType.APPLATTR_ONLY).length);
            assertEquals(1, ieMeasurement.listAttributes("*", AttrType.INSTATTR_ONLY).length);
            ieMeasurement.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValue() {
        try {
            InstanceElement ieTstSer = aoSession.getApplicationStructure().getElementByName("tstser")
                                                .getInstanceByName("Test_Vorbeifahrt");
            // application attributes
            assertEquals(2, ODSHelper.getLongLongVal(ieTstSer.getValue("tstser_iid")));
            assertEquals("Test_Vorbeifahrt", ODSHelper.getStringVal(ieTstSer.getValue("iname")));
            assertEquals("", ODSHelper.getStringVal(ieTstSer.getValue("mime_type")));

            // DT_BLOB
            assertEquals("MyBlob", ieTstSer.getValue("appl_attr_dt_blob").value.u.blobVal().getHeader());
            assertArrayEquals(new byte[] { (byte) 18, (byte) 42, (byte) 52, (byte) 222 },
                              ieTstSer.getValue("appl_attr_dt_blob").value.u.blobVal().get(0, 4));
            // DT_BOOLEAN
            assertEquals(true, ieTstSer.getValue("appl_attr_dt_boolean").value.u.booleanVal());
            // DT_BYTE
            assertEquals((byte) 222, ieTstSer.getValue("appl_attr_dt_byte").value.u.byteVal());
            // DT_BYTESTR
            assertArrayEquals(new byte[] { (byte) 18, (byte) 42, (byte) 52, (byte) 222 },
                              ieTstSer.getValue("appl_attr_dt_bytestr").value.u.bytestrVal());
            // DT_COMPLEX
            assertEquals(5.1d, ieTstSer.getValue("appl_attr_dt_complex").value.u.complexVal().r, 0.000001);
            assertEquals(10.2d, ieTstSer.getValue("appl_attr_dt_complex").value.u.complexVal().i, 0.000001);
            // DT_DOUBLE
            assertEquals(1726381.1234567, ieTstSer.getValue("appl_attr_dt_double").value.u.doubleVal(), 0.000001);
            // DT_ENUM
            assertEquals(28, ieTstSer.getValue("appl_attr_dt_enum").value.u.enumVal());
            // DT_EXTERNALREFERENCE
            assertEquals("extref_desc",
                         ieTstSer.getValue("appl_attr_dt_externalreference").value.u.extRefVal().description);
            assertEquals("mime_type", ieTstSer.getValue("appl_attr_dt_externalreference").value.u.extRefVal().mimeType);
            assertEquals("http://www.test.de",
                         ieTstSer.getValue("appl_attr_dt_externalreference").value.u.extRefVal().location);
            // DT_FLOAT
            assertEquals(123.456f, ieTstSer.getValue("appl_attr_dt_float").value.u.floatVal(), 0.000001);
            // DT_LONG
            assertEquals(123456, ieTstSer.getValue("appl_attr_dt_long").value.u.longVal());
            // DT_LONGLONG
            assertEquals(654321, ODSHelper.asJLong(ieTstSer.getValue("appl_attr_dt_longlong").value.u.longlongVal()));
            // DT_SHORT
            assertEquals(666, ieTstSer.getValue("appl_attr_dt_short").value.u.shortVal());
            // DT_STRING
            assertEquals("test string", ieTstSer.getValue("appl_attr_dt_string").value.u.stringVal());
            // DS_BOOLEAN
            assertEquals(true,
                         Arrays.equals(new boolean[] { true, true, false, false },
                                       ieTstSer.getValue("appl_attr_ds_boolean").value.u.booleanSeq()));
            // DS_BYTE
            assertArrayEquals(new byte[] { (byte) 18, (byte) 42, (byte) 52, (byte) 222 },
                              ieTstSer.getValue("appl_attr_ds_byte").value.u.byteSeq());
            // DS_COMPLEX
            assertEquals(1.2f, ieTstSer.getValue("appl_attr_ds_complex").value.u.complexSeq()[0].r, 0.000001);
            assertEquals(3.4f, ieTstSer.getValue("appl_attr_ds_complex").value.u.complexSeq()[0].i, 0.000001);
            assertEquals(2.1f, ieTstSer.getValue("appl_attr_ds_complex").value.u.complexSeq()[1].r, 0.000001);
            assertEquals(4.3f, ieTstSer.getValue("appl_attr_ds_complex").value.u.complexSeq()[1].i, 0.000001);
            // DS_DATE
            assertArrayEquals(new String[] { "201101011300", "201101011400" },
                              ieTstSer.getValue("appl_attr_ds_date").value.u.dateSeq());
            // DS_DCOMPLEX
            assertEquals(1.2f, ieTstSer.getValue("appl_attr_ds_dcomplex").value.u.dcomplexSeq()[0].r, 0.000001);
            assertEquals(3.4f, ieTstSer.getValue("appl_attr_ds_dcomplex").value.u.dcomplexSeq()[0].i, 0.000001);
            assertEquals(2.1f, ieTstSer.getValue("appl_attr_ds_dcomplex").value.u.dcomplexSeq()[1].r, 0.000001);
            assertEquals(4.3f, ieTstSer.getValue("appl_attr_ds_dcomplex").value.u.dcomplexSeq()[1].i, 0.000001);
            // DS_DOUBLE
            assertArrayEquals(new double[] { 1726381.1234567, 123.0, 321.45 },
                              ieTstSer.getValue("appl_attr_ds_double").value.u.doubleSeq(), 0.0000000001);
            // DS_EXTERNALREFERENCE
            assertEquals("extref_desc1",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[0].description);
            assertEquals("mime_type1",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[0].mimeType);
            assertEquals("http://www.test.de1",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[0].location);
            assertEquals("extref_desc2",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[1].description);
            assertEquals("mime_type2",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[1].mimeType);
            assertEquals("http://www.test.de2",
                         ieTstSer.getValue("appl_attr_ds_externalreference").value.u.extRefSeq()[1].location);
            // DS_ENUM
            assertArrayEquals(new int[] { 1, 7, 8 }, ieTstSer.getValue("appl_attr_ds_enum").value.u.enumSeq());
            // DS_FLOAT
            assertArrayEquals(new float[] { 1.2f, 3.4f, 5.6f, 7.89f },
                              ieTstSer.getValue("appl_attr_ds_float").value.u.floatSeq(), 0.0000000001f);
            // DS_LONG
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, ieTstSer.getValue("appl_attr_ds_long").value.u.longSeq());
            // DS_LONGLONG
            assertArrayEquals(new long[] { 100, 200, 300, 400 },
                              ODSHelper.asJLong(ieTstSer.getValue("appl_attr_ds_longlong").value.u.longlongSeq()));
            // DS_SHORT

            // instance attribute
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            assertEquals("test", ieMeasurement.getValue("instattr").value.u.stringVal());
            ieMeasurement.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing value
        try {
            ieMeasurement.getValue("non_existing_attr");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueSeq() {
        try {
            // application attribute
            NameValueUnit[] values = ieMeasurement.getValueSeq(new String[] { "iname", "dts_iid", "version" });
            assertEquals("Detector;rms A fast - Zusammenfassung", ODSHelper.getStringVal(values[0]));
            assertEquals(32, ODSHelper.getLongLongVal(values[1]));
            assertEquals(0, values[2].value.flag);

            // instance attribute
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            values = ieMeasurement.getValueSeq(new String[] { "iname", "dts_iid", "instattr" });
            assertEquals("test", ODSHelper.getStringVal(values[2]));
            ieMeasurement.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing value
        try {
            ieMeasurement.getValueSeq(new String[] { "iname", "non_existing_attr", "version" });
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueByBaseName() {
        try {
            assertEquals(32, ODSHelper.asJLong(ieMeasurement.getValueByBaseName("id").value.u.longlongVal()));
            assertEquals("Detector;rms A fast - Zusammenfassung",
                         ieMeasurement.getValueByBaseName("name").value.u.stringVal());
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing base name
        try {
            ieMeasurement.getValueByBaseName("non_existing_basename");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    @Test
    public void testGetValueInUnit() {
        try {
            ieMeasurement.getValueInUnit(new NameUnit("aaa", "unit"));
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetValue() {
        try {
            // application attribute
            ieMeasurement.setValue(ODSHelper.createStringNVU("version", "aaa"));
            assertEquals("aaa", ieMeasurement.getValue("version").value.u.stringVal());

            // instance attribute
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            assertEquals("test", ieMeasurement.getValue("instattr").value.u.stringVal());
            ieMeasurement.setValue(ODSHelper.createStringNVU("instattr", "aaa"));
            assertEquals("aaa", ieMeasurement.getValue("instattr").value.u.stringVal());
            ieMeasurement.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing value
        try {
            ieMeasurement.getValue("non_existing_attr");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetValueSeq() {
        try {
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));

            NameValueUnit[] nvu = new NameValueUnit[2];
            nvu[0] = ODSHelper.createStringNVU("version", "version1"); // applAttr
            nvu[1] = ODSHelper.createStringNVU("instattr", "aaa"); // instAttr
            ieMeasurement.setValueSeq(nvu);

            assertEquals("version1", ieMeasurement.getValue("version").value.u.stringVal());
            assertEquals("aaa", ieMeasurement.getValue("instattr").value.u.stringVal());

            ieMeasurement.setValue(ODSHelper.createStringNVU("version", ""));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testAddInstanceAttribute() {
        try {
            // add attribute of each supported data type
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr_dt_string", "test"));
            ieMeasurement.addInstanceAttribute(ODSHelper.createFloatNVU("inst_attr_dt_float", 123.321f));
            ieMeasurement.addInstanceAttribute(ODSHelper.createDoubleNVU("inst_attr_dt_double", -456.654));
            ieMeasurement.addInstanceAttribute(ODSHelper.createByteNVU("inst_attr_dt_byte", (byte) 222));
            ieMeasurement.addInstanceAttribute(ODSHelper.createShortNVU("inst_attr_dt_short", (short) 333));
            ieMeasurement.addInstanceAttribute(ODSHelper.createLongNVU("inst_attr_dt_long", 999999));
            ieMeasurement.addInstanceAttribute(ODSHelper.createLongLongNVU("inst_attr_dt_longlong", 2));
            ieMeasurement.addInstanceAttribute(ODSHelper.createDateNVU("inst_attr_dt_date", "20100101"));
        } catch (AoException e) {
            fail(e.reason);
        }
        // existing application attribute
        try {
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("version", ""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        // not allowed data type
        try {
            ieMeasurement.addInstanceAttribute(ODSHelper.createExtRefNVU("xxx", new T_ExternalReference()));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testRemoveInstanceAttribute() {
        try {
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr", "test"));
            ieMeasurement.removeInstanceAttribute("inst_attr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing instance attribute
        try {
            ieMeasurement.removeInstanceAttribute("xxx_yyy");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    @Test
    public void testRenameInstanceAttribute() {
        try {
            ieMeasurement.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr", "test"));
            assertEquals("test", ieMeasurement.getValue("inst_attr").value.u.stringVal());
            ieMeasurement.renameInstanceAttribute("inst_attr", "new_inst_attr");
            assertEquals("test", ieMeasurement.getValue("new_inst_attr").value.u.stringVal());
            ieMeasurement.removeInstanceAttribute("new_inst_attr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing instance attribute
        try {
            ieMeasurement.renameInstanceAttribute("xxx_yyy", "aaa");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    /***********************************************************************************
     * relations
     ***********************************************************************************/

    @Test
    public void testListRelatedInstances() {
        try {
            // father 'test'
            ApplicationRelation rel = ieMeasurement.getApplicationElement().getRelationsByBaseName("test")[0];
            assertEquals(1, ieMeasurement.listRelatedInstances(rel, "*").getCount());

            // child 'measurement_quantities'
            rel = ieMeasurement.getApplicationElement().getRelationsByBaseName("measurement_quantities")[0];
            assertEquals(3, ieMeasurement.listRelatedInstances(rel, "*").getCount());

            // child 'submatrices'
            rel = ieMeasurement.getApplicationElement().getRelationsByBaseName("submatrices")[0];
            assertEquals(1, ieMeasurement.listRelatedInstances(rel, "*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelatedInstances() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();

            // dts->pas
            ApplicationElement aeDts = as.getElementByName("dts");
            ApplicationElement aePas = as.getElementByName("pas");
            ApplicationRelation applRel = as.getRelations(aeDts, aePas)[0];
            assertEquals(1, ieMeasurement.getRelatedInstances(applRel, "*").getCount());
            // pas->dts
            InstanceElement iePas = aePas.getInstanceById(ODSHelper.asODSLongLong(48));
            assertEquals(1, iePas.getRelatedInstances(applRel, "*").getCount());

            // mea->audifm_iid
            ApplicationElement aeMea = as.getElementByName("mea");
            ApplicationElement aeAudiFm = as.getElementByName("audifm");
            InstanceElement ieMea = aeMea.getInstanceById(ODSHelper.asODSLongLong(22));
            applRel = as.getRelations(aeMea, aeAudiFm)[0];
            assertEquals(1, ieMea.getRelatedInstances(applRel, "*").getCount());

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListRelatedInstancesByRelationship() {
        try {
            assertEquals(8, ieMeasurement.listRelatedInstancesByRelationship(Relationship.ALL_REL, "*").getCount());
            assertEquals(1, ieMeasurement.listRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            assertEquals(4, ieMeasurement.listRelatedInstancesByRelationship(Relationship.CHILD, "*").getCount());
            assertEquals(2, ieMeasurement.listRelatedInstancesByRelationship(Relationship.INFO_TO, "*").getCount());
            assertEquals(1, ieMeasurement.listRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelatedInstancesByRelationship() {
        try {
            assertEquals(8, ieMeasurement.getRelatedInstancesByRelationship(Relationship.ALL_REL, "*").getCount());
            assertEquals(1, ieMeasurement.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            assertEquals(4, ieMeasurement.getRelatedInstancesByRelationship(Relationship.CHILD, "*").getCount());
            assertEquals(2, ieMeasurement.getRelatedInstancesByRelationship(Relationship.INFO_TO, "*").getCount());
            assertEquals(1, ieMeasurement.getRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testCreateRelation() {
        fail("Not yet implemented");
    }

    @Test
    public void testRemoveRelation() {
        fail("Not yet implemented");
    }

    @Test
    public void testCreateRelatedInstances() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAsamPath() {
        try {
            // dts
            assertEquals("/[prj]no_project/[tstser]Test_Vorbeifahrt/[mea]Run_middEng_FINAL_RES/[dts]Detector\\;rms A fast - Zusammenfassung",
                         ieMeasurement.getAsamPath());

            // pas
            InstanceElement iePas = aoSession.getApplicationStructure().getElementByName("par")
                                             .getInstanceById(ODSHelper.asODSLongLong(50));
            assertEquals("/[pas]basic;5.1\\; PAK 5.6 Pre SR 4/[par]acquisition\\/calculation method",
                         iePas.getAsamPath());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testDestroy() {
        // no test necessary
    }

    @Test
    public void testUpcastMeasurement() {
        try {
            ieMeasurement.upcastMeasurement();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testUpcastSubMatrix() {
        try {
            ieMeasurement.upcastSubMatrix();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testShallowCopy() {
        try {
            ieMeasurement.shallowCopy("", "");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testDeepCopy() {
        try {
            ieMeasurement.deepCopy("", "");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetRights() {
        try {
            ieMeasurement.getRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetRights() {
        try {
            ieMeasurement.setRights(null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInitialRights() {
        try {
            ieMeasurement.getInitialRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInitialRights() {
        try {
            ieMeasurement.setInitialRights(null, 0, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(InstanceElementImplTest.class);
    }

}
