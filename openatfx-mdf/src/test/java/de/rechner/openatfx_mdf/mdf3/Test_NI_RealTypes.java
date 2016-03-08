package de.rechner.openatfx_mdf.mdf3;

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
 * Test case for reading the example MDF4-file <code>NI_RealTypes.mdf</code>.
 * 
 * @author Christian Rechner
 */
public class Test_NI_RealTypes {

    private static final String mdfFile = "de/rechner/openatfx_mdf/mdf3/NI_RealTypes.mdf";

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
            assertEquals("3.00    ", ODSHelper.getStringVal(ieTst.getValue("mdf_version_str")));
            assertEquals(300, ODSHelper.getLongVal(ieTst.getValue("mdf_version")));
            assertEquals("", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
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

            InstanceElement ieMea = as.getElementByName("mea").getInstances("NI_RealTypes.mdf").nextOne();
            assertEquals("NI_RealTypes.mdf", ODSHelper.getStringVal(ieMea.getValue("iname")));
            assertEquals("Datensatz mit numerischen X/Y-, Waveform- und Textkanälen in mehreren Kanalgruppen",
                         ODSHelper.getStringVal(ieMea.getValue("desc")));
            assertEquals("20150922182537", ODSHelper.getDateVal(ieMea.getValue("date_created")));
            assertEquals("20150922182537", ODSHelper.getDateVal(ieMea.getValue("mea_begin")));
            assertEquals("", ODSHelper.getDateVal(ieMea.getValue("mea_end")));
            assertEquals(0, ODSHelper.getLongLongVal(ieMea.getValue("start_time_ns")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("local_time")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("time_offsets_valid")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("tz_offset_min")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("dst_offset_min")));
            assertEquals(0, ODSHelper.getEnumVal(ieMea.getValue("time_quality_class")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_angle_valid")));
            assertEquals(0, ODSHelper.getShortVal(ieMea.getValue("start_distance_valid")));
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_angle_rad")), 0.0000001);
            assertEquals(0, ODSHelper.getDoubleVal(ieMea.getValue("start_distance_m")), 0.0000001);

            assertEquals(1, ieMea.listAttributes("*", AttrType.INSTATTR_ONLY).length);

            assertEquals("National Instruments", ODSHelper.getStringVal(ieMea.getValue("author")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReadFHBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElementIterator iter = as.getElementByName("fh").getInstances("*");
            assertEquals(0, iter.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReadCGBlock() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            InstanceElementIterator iter = as.getElementByName("sm").getInstances("*");
            assertEquals(1, iter.getCount());

            InstanceElement ieSm = as.getElementByName("sm").getInstances("sm_00001").nextOne();
            assertEquals("sm_00001", ODSHelper.getStringVal(ieSm.getValue("iname")));
            assertEquals("100ms", ODSHelper.getStringVal(ieSm.getValue("desc")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("acq_name")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_name")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_path")));
            assertEquals("", ODSHelper.getStringVal(ieSm.getValue("src_cmt")));
            assertEquals(0, ODSHelper.getEnumVal(ieSm.getValue("src_type")));
            assertEquals(0, ODSHelper.getEnumVal(ieSm.getValue("src_bus")));
            assertEquals(0, ODSHelper.getShortVal(ieSm.getValue("src_sim")));
            assertEquals(202, ODSHelper.getLongVal(ieSm.getValue("rows")));
            assertEquals(0, ieSm.listAttributes("*", AttrType.INSTATTR_ONLY).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_NI_RealTypes.class);
    }

}
