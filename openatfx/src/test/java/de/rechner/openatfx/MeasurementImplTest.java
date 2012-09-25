package de.rechner.openatfx;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Measurement;
import org.asam.ods.ValueMatrixMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.MeasurementImpl</code>.
 * 
 * @author Christian Rechner
 */
public class MeasurementImplTest {

    private static AoSession aoSession;
    private static Measurement mea;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeDts = applicationStructure.getElementByName("dts");
        mea = aeDts.getInstanceById(ODSHelper.asODSLongLong(32)).upcastMeasurement();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetValueMatrix() {
        try {
            mea.getValueMatrix();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueMatrixInMode() {
        try {
            mea.getValueMatrixInMode(ValueMatrixMode.STORAGE);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testCreateSMatLink() {
        try {
            mea.createSMatLink();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetSMatLinks() {
        try {
            mea.getSMatLinks();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testRemoveSMatLink() {
        try {
            mea.removeSMatLink(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(MeasurementImplTest.class);
    }

}
