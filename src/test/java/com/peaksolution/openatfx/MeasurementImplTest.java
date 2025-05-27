package com.peaksolution.openatfx;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Measurement;
import org.asam.ods.ValueMatrixMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.corba.InstanceElementImplTest;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.MeasurementImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class MeasurementImplTest {

    private static AoSession aoSession;
    private static Measurement mea;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeDts = applicationStructure.getElementByName("dts");
        mea = aeDts.getInstanceById(ODSHelper.asODSLongLong(32)).upcastMeasurement();
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    void testGetValueMatrix() {
        try {
            mea.getValueMatrix();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testGetValueMatrixInMode() {
        try {
            mea.getValueMatrixInMode(ValueMatrixMode.STORAGE);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testCreateSMatLink() {
        try {
            mea.createSMatLink();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testGetSMatLinks() {
        try {
            mea.getSMatLinks();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testRemoveSMatLink() {
        try {
            mea.removeSMatLink(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }
}
