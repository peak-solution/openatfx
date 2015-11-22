package de.rechner.openatfx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Column;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for reading values from a ValueMatrix.
 * 
 * @author Christian Rechner
 */
public class ReadValuesFromExampleTest {

    private static AoSession aoSession;
    private static ValueMatrix vmStorageExample;
    private static ValueMatrix vmCalculatedExample;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
        vmStorageExample = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        vmCalculatedExample = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetMode() {
        try {
            assertEquals(ValueMatrixMode.STORAGE, vmStorageExample.getMode());
            assertEquals(ValueMatrixMode.CALCULATED, vmCalculatedExample.getMode());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetColumnCount() {
        try {
            assertEquals(3, vmStorageExample.getColumnCount());
            assertEquals(3, vmCalculatedExample.getColumnCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRowCount() {
        try {
            assertEquals(167, vmStorageExample.getRowCount());
            assertEquals(167, vmCalculatedExample.getRowCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListColumns() {
        try {
            String[] cols = vmCalculatedExample.listColumns("*");
            assertEquals(3, cols.length);
            assertArrayEquals(new String[] { "LS.Right Side", "Time", "LS.Left Side" }, cols);

            cols = vmStorageExample.listColumns("?ime");
            assertEquals(1, cols.length);
            assertArrayEquals(new String[] { "Time" }, cols);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListIndependentColumns() {
        try {
            String[] cols = vmCalculatedExample.listIndependentColumns("*");
            assertEquals(1, cols.length);
            assertArrayEquals(new String[] { "Time" }, cols);

            cols = vmStorageExample.listIndependentColumns("?ime");
            assertEquals(1, cols.length);
            assertArrayEquals(new String[] { "Time" }, cols);

            cols = vmStorageExample.listIndependentColumns("Left Side");
            assertEquals(0, cols.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetColumns() {
        try {
            Column[] cols = vmCalculatedExample.getColumns("*");
            assertEquals(3, cols.length);

            cols = vmStorageExample.getColumns("?ime");
            assertEquals(1, cols.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetIndependentColumns() {
        try {
            Column[] cols = vmCalculatedExample.getIndependentColumns("*");
            assertEquals(1, cols.length);

            cols = vmStorageExample.getIndependentColumns("?ime");
            assertEquals(1, cols.length);

            cols = vmStorageExample.getIndependentColumns("Left Side");
            assertEquals(0, cols.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVector() {
        try {
            Column[] cols = vmCalculatedExample.getColumns("*");
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertEquals(167, seq.flag.length);
            assertEquals(167, seq.u.floatVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(cols[0], 100, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.floatVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(cols[0], 100, 100);
            assertEquals(67, seq.flag.length);
            assertEquals(67, seq.u.floatVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testDestroy() {
        ValueMatrix vm = null;
        try {
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeSm = applicationStructure.getElementByName("sm");
            SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
            vm = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
            vm.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            vm.getMode();
            fail("Exception expected");
        } catch (Throwable e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ReadValuesFromExampleTest.class);
    }

}
