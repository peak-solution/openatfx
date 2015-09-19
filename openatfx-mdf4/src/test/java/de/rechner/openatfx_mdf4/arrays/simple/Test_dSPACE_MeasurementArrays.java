package de.rechner.openatfx_mdf4.arrays.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx_mdf4.MDF4Converter;


/**
 * Test case for reading the example MDF4-file <code>dSPACE_MeasurementArrays.mf4</code>.
 * 
 * @author Christian Rechner
 */
public class Test_dSPACE_MeasurementArrays {

    private static final String mdfFile = "de/rechner/openatfx_mdf4/arrays/simple/dSPACE_MeasurementArrays.mf4";

    private static ORB orb;
    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
        MDF4Converter reader = new MDF4Converter();
        aoSession = reader.getAoSessionForMDF4(orb, path);
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
            assertEquals("CtrlDesk", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
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
            InstanceElement ieMea = as.getElementByName("mea").getInstances("*").nextOne();
            assertEquals("dSPACE_MeasurementArrays.mf4", ODSHelper.getStringVal(ieMea.getValue("iname")));
            assertEquals("ASAM COMMON MDF 4.1 sample file created by dSPACE. Contents: signals with measurement array types",
                         ODSHelper.getStringVal(ieMea.getValue("desc")));
            assertEquals("20121107121603", ODSHelper.getDateVal(ieMea.getValue("date_created")));
            assertEquals("20121107121603", ODSHelper.getDateVal(ieMea.getValue("mea_begin")));
            assertEquals("", ODSHelper.getDateVal(ieMea.getValue("mea_end")));
            assertEquals(1352283363000000000l, ODSHelper.getLongLongVal(ieMea.getValue("start_time_ns")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("local_time")));
            assertEquals(1, ODSHelper.getShortVal(ieMea.getValue("time_offsets_valid")));
            assertEquals(60, ODSHelper.getShortVal(ieMea.getValue("tz_offset_min")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("dst_offset_min")));
            assertEquals(0, ODSHelper.getEnumVal(ieMea.getValue("time_quality_class")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_angle_valid")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_distance_valid")));
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_angle_rad")), 0.0000001);
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_distance_m")), 0.0000001);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_dSPACE_MeasurementArrays.class);
    }

}
