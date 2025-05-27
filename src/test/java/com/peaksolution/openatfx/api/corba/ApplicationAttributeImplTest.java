package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.RightsSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.ApplicationAttributeImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class ApplicationAttributeImplTest {

    private static AoSession aoSession;
    private static ApplicationAttribute aaId;
    private static ApplicationAttribute aaDB;
    private static ApplicationAttribute aaFactor;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationAttributeImpl.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement applicationElement = applicationStructure.getElementByName("unt");
        aaId = applicationElement.getAttributeByBaseName("id");
        aaDB = applicationElement.getAttributeByName("dB");
        aaFactor = applicationElement.getAttributeByName("factor");
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    void testGetApplicationElement() {
        try {
            assertEquals("unt", aaId.getApplicationElement().getName());
            assertEquals("unt", aaDB.getApplicationElement().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetName() {
        try {
            assertEquals("unt_iid", aaId.getName());
            assertEquals("dB", aaDB.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetName() {
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
    void testGetBaseAttribute() {
        try {
            assertEquals("id", aaId.getBaseAttribute().getName());
            assertEquals(null, aaDB.getBaseAttribute());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetBaseAttribute() {
        try {
            aaDB.setBaseAttribute(aaId.getBaseAttribute());
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testGetDataType() {
        try {
            assertEquals(DataType.DT_LONGLONG, aaId.getDataType());
            assertEquals(DataType.DT_SHORT, aaDB.getDataType());

            // changed base attribute
            ApplicationElement aeExtComp = aoSession.getApplicationStructure().getElementByName("ec");
            assertEquals(DataType.DT_LONG, aeExtComp.getAttributeByName("start_offset").getDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetDataType() {
        try {
            assertEquals(DataType.DT_SHORT.value(), aaDB.getDataType().value());
            aaDB.setDataType(DataType.DT_DOUBLE);
            assertEquals(DataType.DT_DOUBLE.value(), aaDB.getDataType().value());
            aaDB.setDataType(DataType.DT_SHORT);

            // new attribute
            ApplicationElement ae = aaDB.getApplicationElement();
            ApplicationAttribute aaNew = ae.createAttribute();
            aaNew.setName("testAttr");
            assertEquals(DataType.DT_UNKNOWN, aaNew.getDataType());
            aaNew.setDataType(DataType.DT_BLOB);
            assertEquals(DataType.DT_BLOB, aaNew.getDataType());
            ae.removeAttribute(aaNew);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetLength() {
        try {
            assertEquals(1, aaId.getLength());
            assertEquals(1, aaDB.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetLength() {
        try {
            aaId.setLength(1);
            assertEquals(1, aaId.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testIsUnique() {
        try {
            assertEquals(true, aaId.isUnique());
            assertEquals(false, aaDB.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetIsUnique() {
        // check setting unique on 'normal' attribute
        try {
            aaDB.setIsUnique(true);
            assertEquals(true, aaDB.isUnique());
            aaDB.setIsUnique(false);
            assertEquals(false, aaDB.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
        // check setting unique on unique 'base' attribute
        try {
            aaId.setIsUnique(true);
            assertEquals(true, aaId.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
        // reduce uniqueness on base attributes not allowed
        try {
            aaId.setIsUnique(false);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testIsObligatory() {
        try {
            assertEquals(true, aaId.isObligatory());
            assertEquals(false, aaDB.isObligatory());

            ApplicationStructure as = aoSession.getApplicationStructure();
            assertEquals(true, as.getElementByName("measurement_location").getAttributeByName("location_shape_id")
                                 .isObligatory());

            // false implicitly by base attribute, but overwritten with true in atfx
            assertEquals(true, as.getElementByName("tstser").getAttributeByName("mime_type").isObligatory());
            // implicit true by base attribute
            assertEquals(true, as.getElementByName("unt").getAttributeByBaseName("offset").isObligatory());
            // explicit without having base attribute
            assertEquals(true, as.getElementByName("env").getAttributeByName("version").isObligatory());

            // AoLocalColumn attrs
            ApplicationElement aeLc = as.getElementByName("lc");
            assertEquals(true, aeLc.getAttributeByBaseName("independent").isObligatory());
            assertEquals(true, aeLc.getAttributeByBaseName("sequence_representation").isObligatory());
            assertEquals(false, aeLc.getAttributeByBaseName("generation_parameters").isObligatory());

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetIsObligatory() {
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
    void testIsAutogenerated() {
        try {
            assertEquals(true, aaId.isAutogenerated());
            assertEquals(false, aaDB.isAutogenerated());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetIsAutogenerated() {
        try {
            aaId.setIsAutogenerated(true);
            assertEquals(true, aaId.isAutogenerated());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetEnumerationDefinition() {
        try {
            aaId.getEnumerationDefinition();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    void testSetEnumerationDefinition() {
        try {
            aaId.setEnumerationDefinition(null);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testHasUnit() {
        try {
            assertEquals(false, aaId.hasUnit());
            assertEquals(false, aaDB.hasUnit());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testWithUnit() {
        // nothing to do
    }

    @Test
    void testGetUnit() {
        try {
            assertEquals(0, ODSHelper.asJLong(aaId.getUnit()));
            assertEquals(0, ODSHelper.asJLong(aaDB.getUnit()));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetUnit() {
        try {
            aaDB.setUnit(ODSHelper.asODSLongLong(36));
            assertEquals(36, ODSHelper.asJLong(aaDB.getUnit()));
            aaDB.setUnit(ODSHelper.asODSLongLong(0));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetRights() {
        try {
            aaId.setRights(null, 0, RightsSet.SET_RIGHT);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    void testGetRights() {
        try {
            aaId.getRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    void testWithValueFlag() {
        try {
            assertEquals(false, aaId.hasValueFlag());
            assertEquals(false, aaDB.hasValueFlag());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testHasValueFlag() {
        try {
            aaDB.withValueFlag(true);
            assertEquals(true, aaDB.hasValueFlag());
            aaDB.withValueFlag(false);
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
