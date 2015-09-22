package de.rechner.openatfx_mdf.mdf4.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx_mdf.MDFConverter;


/**
 * Test case for reading the example MDF4-file <code>Vector_CANape.MF4</code>.
 * 
 * @author Christian Rechner
 */
public class Test_Vector_CANape {

    private static final String mdfFile = "de/rechner/openatfx_mdf/mdf4/simple/Vector_CANape.MF4";

    private static ORB orb;
    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
        MDFConverter reader = new MDFConverter();
        aoSession = reader.getAoSessionForMDF(orb, path);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    public void testReadIDBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElement ieTst = as.getElementByName("tst").getInstances("*").nextOne();
            assertEquals("MDF     ", ODSHelper.getStringVal(ieTst.getValue("mdf_file_id")));
            assertEquals("4.10    ", ODSHelper.getStringVal(ieTst.getValue("mdf_version_str")));
            assertEquals(410, ODSHelper.getLongVal(ieTst.getValue("mdf_version")));
            assertEquals("MCD10.00", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReadHDBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElementIterator iter = as.getElementByName("mea").getInstances("*");
            assertEquals(1, iter.getCount());

            InstanceElement ieMea = as.getElementByName("mea").getInstances("Vector_CANape.MF4").nextOne();
            assertEquals("Vector_CANape.MF4", ODSHelper.getStringVal(ieMea.getValue("iname")));
            assertEquals("Simple MF4 file created by Vector CANape 10.0\n\nThis file shows 2 channel groups, each containing a time master channel and some value channels.",
                         ODSHelper.getStringVal(ieMea.getValue("desc")));
            assertEquals("20110824175319", ODSHelper.getDateVal(ieMea.getValue("date_created")));
            assertEquals("20110824175319", ODSHelper.getDateVal(ieMea.getValue("mea_begin")));
            assertEquals("", ODSHelper.getDateVal(ieMea.getValue("mea_end")));
            assertEquals(1314193999000000259l, ODSHelper.getLongLongVal(ieMea.getValue("start_time_ns")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("local_time")));
            assertEquals(1, ODSHelper.getShortVal(ieMea.getValue("time_offsets_valid")));
            assertEquals(60, ODSHelper.getShortVal(ieMea.getValue("tz_offset_min")));
            assertEquals(60, ODSHelper.getShortVal(ieMea.getValue("dst_offset_min")));
            assertEquals(0, ODSHelper.getEnumVal(ieMea.getValue("time_quality_class")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_angle_valid")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_distance_valid")));
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_angle_rad")), 0.0000001);
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_distance_m")), 0.0000001);

            assertEquals(4, ieMea.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            assertEquals("Otmar Schneider", ODSHelper.getStringVal(ieMea.getValue("author")));
            assertEquals("ASAM COMMON MDF 4.1 example file", ODSHelper.getStringVal(ieMea.getValue("project")));
            assertEquals("Vector Informatik GmbH", ODSHelper.getStringVal(ieMea.getValue("department")));
            assertEquals("XCPsim simulation", ODSHelper.getStringVal(ieMea.getValue("subject")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReadFHBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElementIterator iter = as.getElementByName("fh").getInstances("*");
            assertEquals(1, iter.getCount());

            InstanceElement ieFh = as.getElementByName("fh").getInstances("fh_00001").nextOne();
            assertEquals("fh_00001", ODSHelper.getStringVal(ieFh.getValue("iname")));
            assertEquals("created", ODSHelper.getStringVal(ieFh.getValue("desc")));
            assertEquals("20110824175319", ODSHelper.getDateVal(ieFh.getValue("date")));
            assertEquals(1314193999000000259l, ODSHelper.getLongLongVal(ieFh.getValue("start_time_ns")));
            assertEquals(0, ODSHelper.getShortVal(ieFh.getValue("local_time")));
            assertEquals(1, ODSHelper.getShortVal(ieFh.getValue("time_offsets_valid")));
            assertEquals(60, ODSHelper.getShortVal(ieFh.getValue("tz_offset_min")));
            assertEquals(60, ODSHelper.getShortVal(ieFh.getValue("dst_offset_min")));
            assertEquals("CANape", ODSHelper.getStringVal(ieFh.getValue("tool_id")));
            assertEquals("Vector Informatik GmbH", ODSHelper.getStringVal(ieFh.getValue("tool_vendor")));
            assertEquals("10.0.0.30836", ODSHelper.getStringVal(ieFh.getValue("tool_version")));
            assertEquals("", ODSHelper.getStringVal(ieFh.getValue("user_name")));

            assertEquals(0, ieFh.listAttributes("*", AttrType.INSTATTR_ONLY).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReadCGBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElementIterator iter = as.getElementByName("sm").getInstances("*");
            assertEquals(2, iter.getCount());

            InstanceElement ieSm = as.getElementByName("sm").getInstances("sm_00001").nextOne();
            assertEquals("sm_00001", ODSHelper.getStringVal(ieSm.getValue("iname")));
            assertEquals("100ms", ODSHelper.getStringVal(ieSm.getValue("desc")));
            assertEquals("100ms", ODSHelper.getStringVal(ieSm.getValue("acq_name")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_name")));
            assertEquals("XCPsim", ODSHelper.getStringVal(ieSm.getValue("src_path")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_cmt")));
            assertEquals(1, ODSHelper.getEnumVal(ieSm.getValue("src_type")));
            assertEquals(2, ODSHelper.getEnumVal(ieSm.getValue("src_bus")));
            assertEquals(0, ODSHelper.getShortVal(ieSm.getValue("src_sim")));
            assertEquals(102, ODSHelper.getLongVal(ieSm.getValue("rows")));
            assertEquals(0, ieSm.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            ieSm = as.getElementByName("sm").getInstances("sm_00002").nextOne();
            assertEquals("sm_00002", ODSHelper.getStringVal(ieSm.getValue("iname")));
            assertEquals("10 ms", ODSHelper.getStringVal(ieSm.getValue("desc")));
            assertEquals("10 ms", ODSHelper.getStringVal(ieSm.getValue("acq_name")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_name")));
            assertEquals("XCPsim", ODSHelper.getStringVal(ieSm.getValue("src_path")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_cmt")));
            assertEquals(1, ODSHelper.getEnumVal(ieSm.getValue("src_type")));
            assertEquals(2, ODSHelper.getEnumVal(ieSm.getValue("src_bus")));
            assertEquals(0, ODSHelper.getShortVal(ieSm.getValue("src_sim")));
            assertEquals(1019, ODSHelper.getLongVal(ieSm.getValue("rows")));
            assertEquals(0, ieSm.listAttributes("*", AttrType.INSTATTR_ONLY).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_Vector_CANape.class);
    }

}
