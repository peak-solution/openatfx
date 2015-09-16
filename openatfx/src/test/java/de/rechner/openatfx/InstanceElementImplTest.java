package de.rechner.openatfx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.Measurement;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.SubMatrix;
import org.asam.ods.T_ExternalReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.InstanceElement</code>.
 * 
 * @author Christian Rechner
 */
public class InstanceElementImplTest {

    private static AoSession aoSession;
    private static InstanceElement ieDts;
    private static InstanceElement ieSm;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeDts = applicationStructure.getElementByName("dts");
        ieDts = aeDts.getInstanceById(ODSHelper.asODSLongLong(32));
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        ieSm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetApplicationElement() {
        try {
            assertEquals("dts", ieDts.getApplicationElement().getName());
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
            assertEquals(-1, ODSHelper.asJLong(ieS2.compare(ieDts)));
            assertEquals(1, ODSHelper.asJLong(ieDts.compare(ieS2)));

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
            assertEquals(32, ODSHelper.asJLong(ieDts.getId()));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetName() {
        try {
            assertEquals("Detector;rms A fast - Zusammenfassung", ieDts.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetName() {
        try {
            assertEquals("Detector;rms A fast - Zusammenfassung", ieDts.getName());
            ieDts.setName("new name");
            assertEquals("new name", ieDts.getName());
            ieDts.setName("Detector;rms A fast - Zusammenfassung");
            assertEquals("Detector;rms A fast - Zusammenfassung", ieDts.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListAttributes() {
        try {
            assertEquals(6, ieDts.listAttributes("*", AttrType.ALL).length);
            assertEquals(1, ieDts.listAttributes("*iid", AttrType.ALL).length);
            assertEquals(6, ieDts.listAttributes("*", AttrType.APPLATTR_ONLY).length);
            assertEquals(1, ieDts.listAttributes("*iid", AttrType.APPLATTR_ONLY).length);
            assertEquals(0, ieDts.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            // test listing of instance attributes
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));

            assertEquals(7, ieDts.listAttributes("*", AttrType.ALL).length);
            assertEquals(6, ieDts.listAttributes("*", AttrType.APPLATTR_ONLY).length);
            assertEquals(1, ieDts.listAttributes("*", AttrType.INSTATTR_ONLY).length);
            assertEquals(1, ieDts.listAttributes("instattr*", AttrType.INSTATTR_ONLY).length);
            assertEquals(0, ieDts.listAttributes("xxx", AttrType.INSTATTR_ONLY).length);
            ieDts.removeInstanceAttribute("instattr");
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
            // assertEquals(5.1d,
            // ieTstSer.getValue("appl_attr_dt_complex").value.u.complexVal().r,
            // 0.000001);
            // assertEquals(10.2d,
            // ieTstSer.getValue("appl_attr_dt_complex").value.u.complexVal().i,
            // 0.000001);
            // DT_DOUBLE
            assertEquals(1726381.1234567, ieTstSer.getValue("appl_attr_dt_double").value.u.doubleVal(), 0.000001);
            assertEquals("V", ieTstSer.getValue("appl_attr_dt_double").unit);
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
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            assertEquals("test", ieDts.getValue("instattr").value.u.stringVal());
            ieDts.removeInstanceAttribute("instattr");

            // values 'implicit'
            InstanceElement ieLc = aoSession.getApplicationStructure().getElementByName("lc")
                                            .getInstanceById(ODSHelper.asODSLongLong(67));
            NameValueUnit nvu = ieLc.getValue("values");
            assertEquals(15, nvu.value.flag);
            assertEquals(31, nvu.value.u.doubleSeq().length);

            // values 'external_component'
            ieLc = aoSession.getApplicationStructure().getElementByName("lc")
                            .getInstanceById(ODSHelper.asODSLongLong(47));
            nvu = ieLc.getValue("values");
            assertEquals(15, nvu.value.flag);
            assertEquals(167, nvu.value.u.floatSeq().length);
            assertEquals(Float.valueOf((float) 0.020362169), Float.valueOf((float) nvu.value.u.floatSeq()[0])); // first
            assertEquals(Float.valueOf((float) 0.01960019), Float.valueOf((float) nvu.value.u.floatSeq()[166])); // last

            // flags 'external_component'
            nvu = ieLc.getValue("flags");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing value
        try {
            ieDts.getValue("non_existing_attr");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueSeq() {
        try {
            // application attribute
            NameValueUnit[] values = ieDts.getValueSeq(new String[] { "iname", "dts_iid", "version" });
            assertEquals("Detector;rms A fast - Zusammenfassung", ODSHelper.getStringVal(values[0]));
            assertEquals(32, ODSHelper.getLongLongVal(values[1]));
            assertEquals(0, values[2].value.flag);

            // instance attribute
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            values = ieDts.getValueSeq(new String[] { "iname", "dts_iid", "instattr" });
            assertEquals("test", ODSHelper.getStringVal(values[2]));
            ieDts.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing value
        try {
            ieDts.getValueSeq(new String[] { "iname", "non_existing_attr", "version" });
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueByBaseName() {
        try {
            assertEquals(32, ODSHelper.asJLong(ieDts.getValueByBaseName("id").value.u.longlongVal()));
            assertEquals("Detector;rms A fast - Zusammenfassung", ieDts.getValueByBaseName("name").value.u.stringVal());
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing base name
        try {
            ieDts.getValueByBaseName("non_existing_basename");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    @Test
    public void testGetValueInUnit() {
        try {
            ieDts.getValueInUnit(new NameUnit("aaa", "unit"));
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetValue() {
        try {
            // application attribute
            ieDts.setValue(ODSHelper.createStringNVU("version", "aaa"));
            assertEquals("aaa", ieDts.getValue("version").value.u.stringVal());

            // AoLocalColumn
            aoSession.startTransaction();
            aoSession.setContext(ODSHelper.createStringNV("write_mode", "file"));
            // aoSession.setContext(ODSHelper.createStringNV("WRITE_EXTERNALCOMPONENTS", "TRUE"));

            ApplicationElement aeLc = aoSession.getApplicationStructure().getElementByName("lc");
            InstanceElement ieLc = aeLc.getInstanceById(ODSHelper.asODSLongLong(47));
            // 'values' of 'AoLocalColumn'
            ieLc.setValue(ODSHelper.createFloatSeqNVU("values", new float[] { -1, 0, 1, 9999999 }));
            assertEquals(true,
                         Arrays.equals(new float[] { -1, 0, 1, 9999999 }, ieLc.getValue("values").value.u.floatSeq()));
            // 'flags' of 'AoLocalColumn
            // ieLc.setValue(ODSHelper.createShortSeqNVU("flags", new short[] { 15, 0, 10, 0 }));
            // ieLc.getValueByBaseName("flags");
            // aoSession.commitTransaction();

            // instance attribute
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));
            assertEquals("test", ieDts.getValue("instattr").value.u.stringVal());
            ieDts.setValue(ODSHelper.createStringNVU("instattr", "aaa"));
            assertEquals("aaa", ieDts.getValue("instattr").value.u.stringVal());
            ieDts.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            System.err.println(e.reason);
            e.printStackTrace();
            fail(e.reason);
        }
        // non existing value
        try {
            ieDts.setValue(ODSHelper.createStringNVU("non_existing_attr", "asd"));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetValueSeq() {
        try {
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("instattr", "test"));

            NameValueUnit[] nvu = new NameValueUnit[2];
            nvu[0] = ODSHelper.createStringNVU("version", "version1"); // applAttr
            nvu[1] = ODSHelper.createStringNVU("instattr", "aaa"); // instAttr
            ieDts.setValueSeq(nvu);

            assertEquals("version1", ieDts.getValue("version").value.u.stringVal());
            assertEquals("aaa", ieDts.getValue("instattr").value.u.stringVal());

            ieDts.setValue(ODSHelper.createStringNVU("version", ""));
            ieDts.removeInstanceAttribute("instattr");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testAddInstanceAttribute() {
        try {
            // add attribute of each supported data type
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr_dt_string", "test"));
            ieDts.addInstanceAttribute(ODSHelper.createFloatNVU("inst_attr_dt_float", 123.321f));
            ieDts.addInstanceAttribute(ODSHelper.createDoubleNVU("inst_attr_dt_double", -456.654));
            ieDts.addInstanceAttribute(ODSHelper.createByteNVU("inst_attr_dt_byte", (byte) 222));
            ieDts.addInstanceAttribute(ODSHelper.createShortNVU("inst_attr_dt_short", (short) 333));
            ieDts.addInstanceAttribute(ODSHelper.createLongNVU("inst_attr_dt_long", 999999));
            ieDts.addInstanceAttribute(ODSHelper.createLongLongNVU("inst_attr_dt_longlong", 2));
            ieDts.addInstanceAttribute(ODSHelper.createDateNVU("inst_attr_dt_date", "20100101"));
        } catch (AoException e) {
            fail(e.reason);
        }
        // existing application attribute
        try {
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("version", ""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        // not allowed data type
        try {
            ieDts.addInstanceAttribute(ODSHelper.createExtRefNVU("xxx", new T_ExternalReference()));
            fail("AoException expected");
        } catch (AoException e) {
        }

        // remove
        try {
            ieDts.removeInstanceAttribute("inst_attr_dt_string");
            ieDts.removeInstanceAttribute("inst_attr_dt_float");
            ieDts.removeInstanceAttribute("inst_attr_dt_double");
            ieDts.removeInstanceAttribute("inst_attr_dt_byte");
            ieDts.removeInstanceAttribute("inst_attr_dt_short");
            ieDts.removeInstanceAttribute("inst_attr_dt_long");
            ieDts.removeInstanceAttribute("inst_attr_dt_longlong");
            ieDts.removeInstanceAttribute("inst_attr_dt_date");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testRemoveInstanceAttribute() {
        try {
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr", "test"));
            ieDts.removeInstanceAttribute("inst_attr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing instance attribute
        try {
            ieDts.removeInstanceAttribute("xxx_yyy");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    @Test
    public void testRenameInstanceAttribute() {
        try {
            ieDts.addInstanceAttribute(ODSHelper.createStringNVU("inst_attr", "test"));
            assertEquals("test", ieDts.getValue("inst_attr").value.u.stringVal());
            ieDts.renameInstanceAttribute("inst_attr", "new_inst_attr");
            assertEquals("test", ieDts.getValue("new_inst_attr").value.u.stringVal());
            ieDts.removeInstanceAttribute("new_inst_attr");
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing instance attribute
        try {
            ieDts.renameInstanceAttribute("xxx_yyy", "aaa");
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
            ApplicationRelation rel = ieDts.getApplicationElement().getRelationsByBaseName("test")[0];
            assertEquals(1, ieDts.listRelatedInstances(rel, "*").getCount());

            // child 'measurement_quantities'
            rel = ieDts.getApplicationElement().getRelationsByBaseName("measurement_quantities")[0];
            assertEquals(3, ieDts.listRelatedInstances(rel, "*").getCount());

            // child 'submatrices'
            rel = ieDts.getApplicationElement().getRelationsByBaseName("submatrices")[0];
            assertEquals(1, ieDts.listRelatedInstances(rel, "*").getCount());
        } catch (AoException e) {
            e.printStackTrace();
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
            assertEquals(1, ieDts.getRelatedInstances(applRel, "bas*").getCount());
            // pas->dts
            InstanceElement iePas = aePas.getInstanceById(ODSHelper.asODSLongLong(48));
            applRel = as.getRelations(aePas, aeDts)[0];
            assertEquals(1, iePas.getRelatedInstances(applRel, "*").getCount());

            // mea->audifm_iid
            ApplicationElement aeMea = as.getElementByName("mea");
            ApplicationElement aeAudiFm = as.getElementByName("audifm");
            InstanceElement ieMea = aeMea.getInstanceById(ODSHelper.asODSLongLong(22));
            applRel = as.getRelations(aeMea, aeAudiFm)[0];
            assertEquals(1, ieMea.getRelatedInstances(applRel, "*").getCount());

            // sm->sm (self relation)
            ApplicationElement aeSm = as.getElementByName("sm");
            ApplicationRelation[] applRels = as.getRelations(aeSm, aeSm);
            assertEquals(4, applRels.length);

            // 'y-axis-for-x-axis'
            ApplicationRelation relYforX = getApplicationRelationByName(applRels, "y-axis-for-x-axis");
            InstanceElement ie = aeSm.getInstanceById(ODSHelper.asODSLongLong(62));
            assertEquals(1, ie.getRelatedInstances(relYforX, "*").getCount());
            // 'x-axis-for-y-axis'
            ApplicationRelation relXforY = getApplicationRelationByName(applRels, "x-axis-for-y-axis");
            ie = aeSm.getInstanceById(ODSHelper.asODSLongLong(59));
            assertEquals(1, ie.getRelatedInstances(relXforY, "*").getCount());

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    private ApplicationRelation getApplicationRelationByName(ApplicationRelation[] applRels, String relName)
            throws AoException {
        for (ApplicationRelation rel : applRels) {
            if (rel.getRelationName().equals(relName)) {
                return rel;
            }
        }
        return null;
    }

    @Test
    public void testListRelatedInstancesByRelationship() {
        try {
            assertEquals(8, ieDts.listRelatedInstancesByRelationship(Relationship.ALL_REL, "*").getCount());
            assertEquals(1, ieDts.listRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            assertEquals(4, ieDts.listRelatedInstancesByRelationship(Relationship.CHILD, "*").getCount());
            assertEquals(2, ieDts.listRelatedInstancesByRelationship(Relationship.INFO_TO, "*").getCount());
            assertEquals(1, ieDts.listRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelatedInstancesByRelationship() {
        try {
            assertEquals(8, ieDts.getRelatedInstancesByRelationship(Relationship.ALL_REL, "*").getCount());
            assertEquals(1, ieDts.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            assertEquals(4, ieDts.getRelatedInstancesByRelationship(Relationship.CHILD, "*").getCount());
            assertEquals(2, ieDts.getRelatedInstancesByRelationship(Relationship.INFO_TO, "*").getCount());
            assertEquals(1, ieDts.getRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testCreateRelation() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeDts = as.getElementByName("dts");
            ApplicationElement aeMeaQua = as.getElementByName("meq");
            ApplicationElement aePas = as.getElementByName("pas");
            ApplicationElement aeSM = as.getElementByName("sm");
            InstanceElement ieMeaQua = aeMeaQua.createInstance("NewMeaQua");

            // adding a child (1:n)
            ApplicationRelation arChild = aeDts.getRelationsByBaseName("measurement_quantities")[0];
            assertEquals(3, ieDts.getRelatedInstances(arChild, "*").getCount());
            assertEquals(0, ieMeaQua.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            ieDts.createRelation(arChild, ieMeaQua);
            assertEquals(4, ieDts.getRelatedInstances(arChild, "*").getCount());
            assertEquals(1, ieMeaQua.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());

            // changing the parent (1:1)
            ApplicationRelation arParent = aeMeaQua.getRelationsByBaseName("measurement")[0];
            assertEquals(1, ieMeaQua.getRelatedInstances(arParent, "*").getCount());
            assertEquals("Detector;rms A fast - Zusammenfassung", ieMeaQua.getRelatedInstances(arParent, "*").nextOne()
                                                                          .getName());
            InstanceElement otherMeasurement = aeDts.getInstanceByName("Slow quantity - Zusammenfassung");
            ieMeaQua.createRelation(arParent, otherMeasurement);
            assertEquals(1, ieMeaQua.getRelatedInstances(arParent, "*").getCount());
            assertEquals("Slow quantity - Zusammenfassung", ieMeaQua.getRelatedInstances(arParent, "*").nextOne()
                                                                    .getName());

            // adding a info relation (m:n)
            ApplicationRelation arPas = aoSession.getApplicationStructure().getRelations(aeDts, aePas)[0];
            assertEquals(1, ieDts.getRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());
            InstanceElement iePas = aePas.getInstanceById(ODSHelper.asODSLongLong(48));
            ieDts.createRelation(arPas, iePas);
            assertEquals(2, ieDts.getRelatedInstancesByRelationship(Relationship.INFO_REL, "*").getCount());

            // setting a self relation
            InstanceElement ieSMy = aeSM.getInstanceById(ODSHelper.asODSLongLong(59));
            InstanceElement ieSMx = aeSM.getInstanceById(ODSHelper.asODSLongLong(62));
            assertEquals(7, aeSM.getAllRelations().length);

            // re-set y->x
            ApplicationRelation arSMx = aoSession.getApplicationStructure().getRelations(aeSM, aeSM)[0]; // x-axis-for-y-axis
            assertEquals(1, ieSMy.getRelatedInstances(arSMx, "*").getCount());
            ieSMy.createRelation(arSMx, ieSMx);
            assertEquals(1, ieSMy.getRelatedInstances(arSMx, "*").getCount());
            // re-set x->y
            ApplicationRelation arSMy = aoSession.getApplicationStructure().getRelations(aeSM, aeSM)[1]; // y-axis-for-x-axis
            assertEquals(1, ieSMx.getRelatedInstances(arSMy, "*").getCount());
            ieSMx.createRelation(arSMy, ieSMy);
            assertEquals(1, ieSMx.getRelatedInstances(arSMy, "*").getCount());

            // cleanup
            ieDts.removeRelation(arChild, ieMeaQua);
            ieMeaQua.removeRelation(arParent, otherMeasurement);
            ieDts.removeRelation(arPas, iePas);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testRemoveRelation() {
        try {
            ApplicationElement aeDts = aoSession.getApplicationStructure().getElementByName("dts");
            ApplicationElement aeMeaQua = aoSession.getApplicationStructure().getElementByName("meq");
            InstanceElement ieMeaQua = aeMeaQua.getInstanceById(ODSHelper.asODSLongLong(46));

            // removing a child (1:n)
            ApplicationRelation arChild = aeDts.getRelationsByBaseName("measurement_quantities")[0];
            assertEquals(3, ieDts.getRelatedInstances(arChild, "*").getCount());
            assertEquals(1, ieMeaQua.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());
            ieDts.removeRelation(arChild, ieMeaQua);
            assertEquals(2, ieDts.getRelatedInstances(arChild, "*").getCount());
            assertEquals(0, ieMeaQua.getRelatedInstancesByRelationship(Relationship.FATHER, "*").getCount());

            // cleanup
            ieDts.createRelation(arChild, ieMeaQua);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testCreateRelatedInstances() {
        try {
            ieDts.createRelatedInstances(null, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetAsamPath() {
        try {
            // dts
            assertEquals("/[prj]no_project/[tstser]Test_Vorbeifahrt/[mea]Run_middEng_FINAL_RES/[dts]Detector\\;rms A fast - Zusammenfassung",
                         ieDts.getAsamPath());

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
            Measurement mea = ieDts.upcastMeasurement();
            assertEquals("Detector;rms A fast - Zusammenfassung", mea.getName());
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            ieSm.upcastMeasurement();
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_INVALID_BASETYPE.value(), e.errCode.value());
        }
    }

    @Test
    public void testUpcastSubMatrix() {
        try {
            SubMatrix sm = ieSm.upcastSubMatrix();
            assertEquals("Detector;rms A fast(Zusammenfassung)", sm.getName());
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            ieDts.upcastSubMatrix();
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_INVALID_BASETYPE.value(), e.errCode.value());
        }
    }

    @Test
    public void testShallowCopy() {
        try {
            InstanceElement result = ieDts.shallowCopy("SomeShallowCopiedIE", "");
            assertEquals("SomeShallowCopiedIE", result.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testDeepCopy() {
        // try {
        // InstanceElement result = ieDts.deepCopy("SomeCopiedIE", "");
        // assertEquals("SomeCopiedIE", result.getName());
        // } catch (AoException e) {
        // fail(e.reason);
        // }
    }

    @Test
    public void testGetRights() {
        try {
            ieDts.getRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetRights() {
        try {
            ieDts.setRights(null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInitialRights() {
        try {
            ieDts.getInitialRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInitialRights() {
        try {
            ieDts.setInitialRights(null, 0, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(InstanceElementImplTest.class);
    }

}
