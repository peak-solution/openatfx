package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Column;
import org.asam.ods.DataType;
import org.asam.ods.SubMatrix;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.ColumnOnSubMatrixImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class ColumnImplTest {

    private static AoSession aoSession;
    private static Column colTimeCalculated;
    private static Column colExplicitStorage;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
        ValueMatrix vmStorage = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        colExplicitStorage = vmStorage.getColumns("LS.Left Side")[0];
        ValueMatrix vmCalculated = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
        colTimeCalculated = vmCalculated.getColumns("Time")[0];
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    void testGetSourceMQ() {
        try {
            assertEquals("Time", colTimeCalculated.getSourceMQ().getName());
            assertEquals("LS.Left Side", colExplicitStorage.getSourceMQ().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetName() {
        try {
            assertEquals("Time", colTimeCalculated.getName());
            assertEquals("LS.Left Side", colExplicitStorage.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetFormula() {
        try {
            assertEquals("", colTimeCalculated.getFormula());
            assertEquals("", colExplicitStorage.getFormula());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testIsIndependent() {
        try {
            assertEquals(true, colTimeCalculated.isIndependent());
            assertEquals(false, colExplicitStorage.isIndependent());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetDataType() {
        try {
            assertEquals(DataType.DT_DOUBLE, colTimeCalculated.getDataType());
            assertEquals(DataType.DT_FLOAT, colExplicitStorage.getDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetSequenceRepresentation() {
        try {
            assertEquals(0, colTimeCalculated.getSequenceRepresentation());
            assertEquals(7, colExplicitStorage.getSequenceRepresentation());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetGenerationParametersn() {
        try {
            assertEquals(0, colTimeCalculated.getGenerationParameters().doubleSeq().length);
            assertEquals(0, colExplicitStorage.getGenerationParameters().doubleSeq().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetRawDataType() {
        try {
            assertEquals(DataType.DT_DOUBLE, colTimeCalculated.getRawDataType());
            assertEquals(DataType.DT_FLOAT, colExplicitStorage.getRawDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetUnit() {
        try {
            assertEquals("s", colTimeCalculated.getUnit());
            assertEquals("Pa", colExplicitStorage.getUnit());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testDestroy() {
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
    }
}
