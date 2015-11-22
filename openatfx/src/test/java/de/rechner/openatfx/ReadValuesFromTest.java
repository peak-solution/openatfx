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
public class ReadValuesFromTest {

    private static AoSession aoSession;
    private static ValueMatrix vmStorageExample;
    private static ValueMatrix vmCalculatedExample;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/test.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("Submatrix");
        SubMatrix sm = aeSm.getInstanceByName("Submatrix1").upcastSubMatrix();
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
            assertEquals(5, vmStorageExample.getColumnCount());
            assertEquals(5, vmCalculatedExample.getColumnCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRowCount() {
        try {
            assertEquals(10, vmStorageExample.getRowCount());
            assertEquals(10, vmCalculatedExample.getRowCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetColumns() {
        try {
            Column[] cols = vmCalculatedExample.getColumns("*");
            assertEquals(5, cols.length);

            cols = vmStorageExample.getColumns("t");
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

            cols = vmStorageExample.getIndependentColumns("?");
            assertEquals(1, cols.length);

            cols = vmStorageExample.getIndependentColumns("Left Side");
            assertEquals(0, cols.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorImplicitLinear() {
        try {

            // impicit_linear, numeric datatype, ValueMatrixMode=CALCULATED
            Column[] cols = vmCalculatedExample.getColumns("implicit_linear");
            // whole array
            TS_ValueSeq valuesSeq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 3, 5, 7, 9, 11, 13, 15, 17, 19 }, valuesSeq.u.floatVal(), 0);
            // part array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 7, 9, 11, 13 }, valuesSeq.u.floatVal(), 0);

            // impicit_linear, numeric datatype, ValueMatrixMode=STORAGE
            cols = vmStorageExample.getColumns("implicit_linear");
            // whole array
            valuesSeq = vmStorageExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 3, 5, 7, 9, 11, 13, 15, 17, 19 }, valuesSeq.u.floatVal(), 0);
            // part array
            valuesSeq = vmStorageExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 7, 9, 11, 13 }, valuesSeq.u.floatVal(), 0);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorImplicitConstant() {
        try {

            //
            // implicit_constant, numeric datatype
            //

            // ValueMatrixMode=CALCULATED
            Column[] cols = vmCalculatedExample.getColumns("implicit_constant");
            // whole array
            TS_ValueSeq valuesSeq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, valuesSeq.u.floatVal(), 0);
            // part array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 1, 1, 1 }, valuesSeq.u.floatVal(), 0);

            // ValueMatrixMode=STORAGE
            cols = vmStorageExample.getColumns("implicit_constant");
            // whole array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, valuesSeq.u.floatVal(), 0);
            // part array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new float[] { 1, 1, 1, 1 }, valuesSeq.u.floatVal(), 0);

            //
            // implicit_constant, string datatype
            //

            // ValueMatrixMode=CALCULATED
            cols = vmCalculatedExample.getColumns("implicit_constant_string");
            // whole array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new String[] { "const", "const", "const", "const", "const", "const", "const", "const",
                    "const", "const" }, valuesSeq.u.stringVal());
            // part array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new String[] { "const", "const", "const", "const" }, valuesSeq.u.stringVal());

            // ValueMatrixMode=STORAGE
            cols = vmStorageExample.getColumns("implicit_constant_string");
            // whole array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 0, 0);
            assertArrayEquals(new short[] { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new String[] { "const", "const", "const", "const", "const", "const", "const", "const",
                    "const", "const" }, valuesSeq.u.stringVal());
            // part array
            valuesSeq = vmCalculatedExample.getValueVector(cols[0], 3, 4);
            assertArrayEquals(new short[] { 15, 15, 15, 15 }, valuesSeq.flag);
            assertArrayEquals(new String[] { "const", "const", "const", "const" }, valuesSeq.u.stringVal());

        } catch (AoException e) {
            e.printStackTrace();
            fail(e.reason);
        }
    }

    @Test
    public void testDestroy() {
        ValueMatrix vm = null;
        try {
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeSm = applicationStructure.getElementByName("Submatrix");
            SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(1)).upcastSubMatrix();
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
        return new JUnit4TestAdapter(ReadValuesFromTest.class);
    }

}
