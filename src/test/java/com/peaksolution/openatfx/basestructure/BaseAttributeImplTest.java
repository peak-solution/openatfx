package com.peaksolution.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.api.BaseModel;
import com.peaksolution.openatfx.api.BaseModelFactory;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;


/**
 * Test case for <code>com.peaksolution.openatfx.basestructure.BaseAttributeImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class BaseAttributeImplTest {

    private static BaseAttribute baseAttributeId;
    private static BaseAttribute baseAttributeExtRef;
    private static BaseAttribute baseAttributeInterpolation;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseModel baseModel = BaseModelFactory.getInstance().getBaseModel("asam30");
        OpenAtfxAPI api = new OpenAtfxAPIImplementation(baseModel);
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
        BaseElement baseElement = baseStructure.getElementByType("AoMeasurementQuantity");
        baseAttributeId = baseElement.getAttributes("id")[0];
        baseAttributeExtRef = baseElement.getAttributes("external_references")[0];
        baseAttributeInterpolation = baseElement.getAttributes("interpolation")[0];
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#getName()}.
     */
    @Test
    void testGetName() {
        try {
            assertEquals("id", baseAttributeId.getName());
            assertEquals("external_references", baseAttributeExtRef.getName());
            assertEquals("interpolation", baseAttributeInterpolation.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#getDataType()}.
     */
    @Test
    void testGetDataType() {
        try {
            assertEquals(DataType.DT_LONGLONG, baseAttributeId.getDataType());
            assertEquals(DataType.DS_EXTERNALREFERENCE, baseAttributeExtRef.getDataType());
            assertEquals(DataType.DT_ENUM, baseAttributeInterpolation.getDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#isObligatory()}.
     */
    @Test
    void testIsObligatory() {
        try {
            assertEquals(true, baseAttributeId.isObligatory());
            assertEquals(false, baseAttributeExtRef.isObligatory());
            assertEquals(false, baseAttributeInterpolation.isObligatory());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#isUnique()}.
     */
    @Test
    void testIsUnique() {
        try {
            assertEquals(true, baseAttributeId.isUnique());
            assertEquals(false, baseAttributeExtRef.isUnique());
            assertEquals(false, baseAttributeInterpolation.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#getBaseElement()}.
     */
    @Test
    void testGetBaseElement() {
        try {
            assertEquals("AoMeasurementQuantity", baseAttributeId.getBaseElement().getType());
            assertEquals("AoMeasurementQuantity", baseAttributeExtRef.getBaseElement().getType());
            assertEquals("AoMeasurementQuantity", baseAttributeInterpolation.getBaseElement().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseAttributeImpl#getEnumerationDefinition()}.
     */
    @Test
    void testGetEnumerationDefinition() {
        try {
            assertEquals("interpolation_enum", baseAttributeInterpolation.getEnumerationDefinition().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
