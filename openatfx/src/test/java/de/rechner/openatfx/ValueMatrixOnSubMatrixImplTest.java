package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.SubMatrix;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.SubMatrixImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ValueMatrixOnSubMatrixImplTest {

    private static AoSession aoSession;
    private static ValueMatrix vmStorage;
    private static ValueMatrix vmCalculated;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
        vmStorage = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        vmCalculated = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetMode() {
        try {
            assertEquals(ValueMatrixMode.STORAGE, vmStorage.getMode());
            assertEquals(ValueMatrixMode.CALCULATED, vmCalculated.getMode());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetColumnCount() {
        try {
            vmStorage.getColumnCount();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListColumns() {
        // try {
        // String[] cols = vmCalculated.listColumns("*");
        // assertEquals(3, cols.length);
        //
        // cols = vmStorage.listColumns("?ime");
        // assertEquals(1, cols.length);
        // } catch (AoException e) {
        // fail(e.reason);
        // }
    }

    @Test
    public void testGetColumns() {
        try {
            vmStorage.getColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ValueMatrixOnSubMatrixImplTest.class);
    }

}
