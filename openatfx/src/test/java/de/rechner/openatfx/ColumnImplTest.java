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
import org.asam.ods.Column;
import org.asam.ods.DataType;
import org.asam.ods.SubMatrix;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.ColumnOnSubMatrixImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ColumnImplTest {

    private static AoSession aoSession;
    private static Column colTimeCalculated;
    private static Column colExplicitStorage;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
        ValueMatrix vmStorage = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        colExplicitStorage = vmStorage.getColumns("LS.Left Side")[0];
        ValueMatrix vmCalculated = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
        colTimeCalculated = vmCalculated.getColumns("Time")[0];
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetSourceMQ() {
        try {
            assertEquals("Time", colTimeCalculated.getSourceMQ().getName());
            assertEquals("LS.Left Side", colExplicitStorage.getSourceMQ().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetName() {
        try {
            assertEquals("Time", colTimeCalculated.getName());
            assertEquals("LS.Left Side", colExplicitStorage.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetFormula() {
        try {
            assertEquals("", colTimeCalculated.getFormula());
            assertEquals("", colExplicitStorage.getFormula());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testIsIndependent() {
        try {
            assertEquals(true, colTimeCalculated.isIndependent());
            assertEquals(false, colExplicitStorage.isIndependent());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetDataType() {
        try {
            assertEquals(DataType.DT_DOUBLE, colTimeCalculated.getDataType());
            assertEquals(DataType.DT_FLOAT, colExplicitStorage.getDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetSequenceRepresentation() {
        try {
            assertEquals(0, colTimeCalculated.getSequenceRepresentation());
            assertEquals(7, colExplicitStorage.getSequenceRepresentation());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetGenerationParametersn() {
        try {
            assertEquals(0, colTimeCalculated.getGenerationParameters().doubleSeq().length);
            assertEquals(0, colExplicitStorage.getGenerationParameters().doubleSeq().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRawDataType() {
        try {
            assertEquals(DataType.DT_DOUBLE, colTimeCalculated.getRawDataType());
            assertEquals(DataType.DT_FLOAT, colExplicitStorage.getRawDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetUnit() {
        try {
            assertEquals("s", colTimeCalculated.getUnit());
            assertEquals("Pa", colExplicitStorage.getUnit());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testDestroy() {
        Column col = null;
        try {
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeSm = applicationStructure.getElementByName("sm");
            SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
            ValueMatrix vmStorage = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
            col = vmStorage.getColumns("Time")[0];
            col.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            col.getDataType();
            fail("Exception expected");
        } catch (Throwable e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ColumnImplTest.class);
    }

}
