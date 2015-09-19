package de.rechner.openatfx_mdf4.simple;

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
import de.rechner.openatfx_mdf4.MDF4Converter;


/**
 * Test case for reading the example MDF4-file <code>ETAS_SimpleSorted.mf4</code>.
 * 
 * @author Christian Rechner
 */
public class Test_ETAS_SimpleSorted {

    private static final String mdfFile = "de/rechner/openatfx_mdf4/simple/ETAS_SimpleSorted.mf4";

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
            assertEquals("MDF_IP", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
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

            InstanceElement ieMea = as.getElementByName("mea").getInstances("ETAS_SimpleSorted.mf4").nextOne();
            assertEquals("ETAS_SimpleSorted.mf4", ODSHelper.getStringVal(ieMea.getValue("iname")));
            assertEquals("ASAM MDF 4.0 Example file created by ETAS. Contents: 2 simple channel groups containing ints and floats in little endian format.",
                         ODSHelper.getStringVal(ieMea.getValue("desc")));
            assertEquals("20110909102921", ODSHelper.getDateVal(ieMea.getValue("date_created")));
            assertEquals("20110909102921", ODSHelper.getDateVal(ieMea.getValue("mea_begin")));
            assertEquals("", ODSHelper.getDateVal(ieMea.getValue("mea_end")));
            assertEquals(1315549761850000000l, ODSHelper.getLongLongVal(ieMea.getValue("start_time_ns")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("local_time")));
            assertEquals(1, ODSHelper.getShortVal(ieMea.getValue("time_offsets_valid")));
            assertEquals(60, ODSHelper.getShortVal(ieMea.getValue("tz_offset_min")));
            assertEquals(60, ODSHelper.getShortVal(ieMea.getValue("dst_offset_min")));
            assertEquals(0, ODSHelper.getEnumVal(ieMea.getValue("time_quality_class")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_angle_valid")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_distance_valid")));
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_angle_rad")), 0.0000001);
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_distance_m")), 0.0000001);

            assertEquals(5, ieMea.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            assertEquals("PC timer", ODSHelper.getStringVal(ieMea.getValue("time_source")));
            assertEquals("Tobias Langner", ODSHelper.getStringVal(ieMea.getValue("author")));
            assertEquals("ASAM Example Files", ODSHelper.getStringVal(ieMea.getValue("project")));
            assertEquals("ETAS GmbH", ODSHelper.getStringVal(ieMea.getValue("department")));
            assertEquals("Example", ODSHelper.getStringVal(ieMea.getValue("subject")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_ETAS_SimpleSorted.class);
    }

}
