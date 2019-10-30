package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

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
import junit.framework.JUnit4TestAdapter;


public class ReadValuesFromExampleAllTypesTest {

    private static ORB orb;
    private static AoSession aoSession;

    private static ValueMatrix vmCalculatedExample;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/asam600/Example_AllTypes.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("Submatrix");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(94)).upcastSubMatrix();
        // vmStorageExample = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        vmCalculatedExample = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetValueVectorBoolean() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqBoolean")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.booleanVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.booleanVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.booleanVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorByte() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqByte")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.byteVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.byteVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.byteVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorBytestr() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqBytestr")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.bytestrVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.bytestrVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.bytestrVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorComplex() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqComplex")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.complexVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.complexVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.complexVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorDate() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqDate")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.dateVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.dateVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.dateVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorDcomplex() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqDcomplex")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.dcomplexVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.dcomplexVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.dcomplexVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorDouble() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqDouble")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.doubleVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.doubleVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.doubleVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorFloat() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqFloat")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.floatVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.floatVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.floatVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorLong() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqLong")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.longVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.longVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.longVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorLongLong() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqLonglong")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.longlongVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.longlongVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.longlongVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorShort() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqShort")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.shortVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.shortVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.shortVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueVectorString() {
        try {
            Column col = vmCalculatedExample.getColumns("MyMqString")[0];
            // whole array
            TS_ValueSeq seq = vmCalculatedExample.getValueVector(col, 0, 0);
            assertEquals(5, seq.flag.length);
            assertEquals(5, seq.u.stringVal().length);

            // part array
            seq = vmCalculatedExample.getValueVector(col, 1, 3);
            assertEquals(3, seq.flag.length);
            assertEquals(3, seq.u.stringVal().length);

            // part array with count overflow
            seq = vmCalculatedExample.getValueVector(col, 1, 100);
            assertEquals(4, seq.flag.length);
            assertEquals(4, seq.u.stringVal().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ReadValuesFromExampleAllTypesTest.class);
    }

}
