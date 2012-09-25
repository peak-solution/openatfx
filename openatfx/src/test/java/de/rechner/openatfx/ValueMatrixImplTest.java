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
import org.asam.ods.NameValue;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.SetType;
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
 * Test case for <code>de.rechner.openatfx.ValueMatrixImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ValueMatrixImplTest {

    private static AoSession aoSession;
    private static ValueMatrix vmSmStorage;
    private static ValueMatrix vmSmCalculated;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeSm = applicationStructure.getElementByName("sm");
        SubMatrix sm = aeSm.getInstanceById(ODSHelper.asODSLongLong(33)).upcastSubMatrix();
        vmSmStorage = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);
        vmSmCalculated = sm.getValueMatrixInMode(ValueMatrixMode.CALCULATED);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetMode() {
        try {
            ValueMatrixMode mode = vmSmStorage.getMode();
            assertEquals(ValueMatrixMode.STORAGE, mode);

            mode = vmSmCalculated.getMode();
            assertEquals(ValueMatrixMode.CALCULATED, mode);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetColumnCount() {
        try {
            vmSmStorage.getColumnCount();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testListColumns() {
        try {
            vmSmStorage.listColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetColumns() {
        try {
            vmSmStorage.getColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testListIndependentColumns() {
        try {
            vmSmStorage.listIndependentColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetIndependentColumns() {
        try {
            vmSmStorage.getIndependentColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetRowCount() {
        try {
            vmSmStorage.getRowCount();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueMeaPoint() {
        try {
            vmSmStorage.getValueMeaPoint(0);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValueVector() {
        try {
            vmSmStorage.getValueVector(null, 0, 0);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testRemoveValueMeaPoint() {
        try {
            vmSmStorage.removeValueMeaPoint(null, 0, 0);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testRemoveValueVector() {
        try {
            vmSmStorage.removeValueVector(null, 0, 0);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetValueMeaPoint() {
        try {
            vmSmStorage.setValueMeaPoint(SetType.APPEND, 0, new NameValue[0]);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetValueVector() {
        try {
            vmSmStorage.setValueVector(null, SetType.APPEND, 0, new TS_ValueSeq());
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetValue() {
        try {
            vmSmStorage.setValue(SetType.APPEND, 0, new NameValueSeqUnit[0]);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testAddColumn() {
        try {
            vmSmStorage.addColumn(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testListScalingColumns() {
        try {
            vmSmStorage.listScalingColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetScalingColumns() {
        try {
            vmSmStorage.getScalingColumns("*");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testListColumnsScaledBy() {
        try {
            vmSmStorage.listColumnsScaledBy(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetColumnsScaledBy() {
        try {
            vmSmStorage.getColumnsScaledBy(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testAddColumnScaledBy() {
        try {
            vmSmStorage.addColumnScaledBy(null, null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetValue() {
        try {
            vmSmStorage.getValue(new Column[0], 0, 0);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testDestroy() {}

}
