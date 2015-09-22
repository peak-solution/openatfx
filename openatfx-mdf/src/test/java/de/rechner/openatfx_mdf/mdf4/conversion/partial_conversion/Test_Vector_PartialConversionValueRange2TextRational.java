package de.rechner.openatfx_mdf.mdf4.conversion.partial_conversion;

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
import de.rechner.openatfx_mdf.MDFConverter;


/**
 * Test case for reading the example MDF4-file <code>Vector_PartialConversionValueRange2TextRational.mf4</code>.
 * 
 * @author Christian Rechner
 */
public class Test_Vector_PartialConversionValueRange2TextRational {

    private static final String mdfFile = "de/rechner/openatfx_mdf/mdf4/conversion/partial_conversion/Vector_PartialConversionValueRange2TextRational.mf4";

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
            assertEquals("MDF4Lib", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_Vector_PartialConversionValueRange2TextRational.class);
    }

}
