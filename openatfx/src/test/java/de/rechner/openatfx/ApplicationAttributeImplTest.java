package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.ApplicationAttributeImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationAttributeImplTest {

    private static AoSession aoSession;
    private static ApplicationAttribute aaId;
    private static ApplicationAttribute aaDB;
    private static ApplicationAttribute aaFactor;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationAttributeImpl.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement applicationElement = applicationStructure.getElementByName("unt");
        aaId = applicationElement.getAttributeByBaseName("id");
        aaDB = applicationElement.getAttributeByName("dB");
        aaFactor = applicationElement.getAttributeByName("factor");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetApplicationElement() {
        try {
            assertEquals("unt", aaId.getApplicationElement().getName());
            assertEquals("unt", aaDB.getApplicationElement().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetName() {
        try {
            assertEquals("unt_iid", aaId.getName());
            assertEquals("dB", aaDB.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetName() {
        try {
            ApplicationElement aeUnit = aaDB.getApplicationElement();
            ApplicationAttribute aaName = aeUnit.getAttributeByName("iname");

            assertEquals("iname", aaName.getName());
            assertEquals("1/min", aeUnit.getInstanceByName("1/min").getValue("iname").value.u.stringVal());

            aaName.setName("new_name");
            assertEquals("new_name", aaName.getName());
            assertEquals("1/min", aeUnit.getInstanceByName("1/min").getValue("new_name").value.u.stringVal());

            aaName.setName("iname");
        } catch (AoException e) {
            fail(e.reason);
        }
        // empty attribute name
        try {
            aaDB.setName("");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_BAD_PARAMETER, e.errCode);
        }
        // attribute name length > 30
        try {
            aaDB.setName("012345678901234567890123456789x");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_BAD_PARAMETER, e.errCode);
        }
        // duplicate attribute name
        try {
            aaDB.setName("unt_iid");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_DUPLICATE_NAME, e.errCode);
        }
    }

    @Test
    public void testGetBaseAttribute() {
        try {
            assertEquals("id", aaId.getBaseAttribute().getName());
            assertEquals(null, aaDB.getBaseAttribute());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetBaseAttribute() {
        try {
            aaDB.setBaseAttribute(aaId.getBaseAttribute());
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetDataType() {
        try {
            assertEquals(DataType.DT_LONGLONG.value(), aaId.getDataType().value());
            assertEquals(DataType.DT_SHORT.value(), aaDB.getDataType().value());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetDataType() {
        try {
            assertEquals(DataType.DT_SHORT.value(), aaDB.getDataType().value());
            aaDB.setDataType(DataType.DT_DOUBLE);
            assertEquals(DataType.DT_DOUBLE.value(), aaDB.getDataType().value());
            aaDB.setDataType(DataType.DT_SHORT);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aaId.setDataType(DataType.DT_BOOLEAN);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetLength() {
        try {
            assertEquals(1, aaId.getLength());
            assertEquals(1, aaDB.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetLength() {
        try {
            aaId.setLength(1);
            assertEquals(1, aaId.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testIsUnique() {
        try {
            assertEquals(true, aaId.isUnique());
            assertEquals(false, aaDB.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetIsUnique() {
        try {
            aaId.setIsUnique(true);
            assertEquals(true, aaId.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testIsObligatory() {
        try {
            assertEquals(true, aaId.isObligatory());
            assertEquals(false, aaDB.isObligatory());

            ApplicationStructure as = aoSession.getApplicationStructure();
            assertEquals(true, as.getElementByName("measurement_location").getAttributeByName("location_shape_id")
                                 .isObligatory());

            // implicit by base attribute
            assertEquals(true, as.getElementByName("tstser").getAttributeByName("mime_type").isObligatory());
            // explicit with having base attribute
            assertEquals(true, as.getElementByName("unt").getAttributeByBaseName("offset").isObligatory());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetIsObligatory() {
        try {
            aaId.setIsObligatory(false);
            fail("AoException expected");
            aaDB.setIsObligatory(false);
            assertEquals(false, aaDB.isObligatory());
            assertEquals(true, aaFactor.isObligatory());
        } catch (AoException e) {
        }
    }

    @Test
    public void testIsAutogenerated() {
        try {
            assertEquals(true, aaId.isAutogenerated());
            assertEquals(false, aaDB.isAutogenerated());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetIsAutogenerated() {
        try {
            aaId.setIsAutogenerated(true);
            assertEquals(true, aaId.isAutogenerated());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetEnumerationDefinition() {
        try {
            aaId.getEnumerationDefinition();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testSetEnumerationDefinition() {
        try {
            aaId.setEnumerationDefinition(null);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testHasUnit() {
        try {
            assertEquals(false, aaId.hasUnit());
            assertEquals(false, aaDB.hasUnit());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testWithUnit() {
        // nothing to do
    }

    @Test
    public void testGetUnit() {
        try {
            assertEquals(0, ODSHelper.asJLong(aaId.getUnit()));
            assertEquals(0, ODSHelper.asJLong(aaDB.getUnit()));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetUnit() {
        try {
            aaDB.setUnit(ODSHelper.asODSLongLong(1));
            assertEquals(1, ODSHelper.asJLong(aaDB.getUnit()));
            aaDB.setUnit(ODSHelper.asODSLongLong(0));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetRights() {
        try {
            aaId.setRights(null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetRights() {
        try {
            aaId.getRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testWithValueFlag() {
        try {
            assertEquals(false, aaId.hasValueFlag());
            assertEquals(false, aaDB.hasValueFlag());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testHasValueFlag() {
        try {
            aaDB.withValueFlag(true);
            assertEquals(true, aaDB.hasValueFlag());
            aaDB.withValueFlag(false);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplicationAttributeImplTest.class);
    }

}
