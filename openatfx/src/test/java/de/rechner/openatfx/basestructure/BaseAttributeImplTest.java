package de.rechner.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.BaseAttributeImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BaseAttributeImplTest {

    private static BaseAttribute baseAttributeId;
    private static BaseAttribute baseAttributeExtRef;
    private static BaseAttribute baseAttributeInterpolation;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, "asam30");
        BaseElement baseElement = baseStructure.getElementByType("AoMeasurementQuantity");
        baseAttributeId = baseElement.getAttributes("id")[0];
        baseAttributeExtRef = baseElement.getAttributes("external_references")[0];
        baseAttributeInterpolation = baseElement.getAttributes("interpolation")[0];
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#getName()}.
     */
    @Test
    public void testGetName() {
        try {
            assertEquals("id", baseAttributeId.getName());
            assertEquals("external_references", baseAttributeExtRef.getName());
            assertEquals("interpolation", baseAttributeInterpolation.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#getDataType()}.
     */
    @Test
    public void testGetDataType() {
        try {
            assertEquals(DataType.DT_LONGLONG, baseAttributeId.getDataType());
            assertEquals(DataType.DS_EXTERNALREFERENCE, baseAttributeExtRef.getDataType());
            assertEquals(DataType.DT_ENUM, baseAttributeInterpolation.getDataType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#isObligatory()}.
     */
    @Test
    public void testIsObligatory() {
        try {
            assertEquals(true, baseAttributeId.isObligatory());
            assertEquals(false, baseAttributeExtRef.isObligatory());
            assertEquals(false, baseAttributeInterpolation.isObligatory());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#isUnique()}.
     */
    @Test
    public void testIsUnique() {
        try {
            assertEquals(true, baseAttributeId.isUnique());
            assertEquals(false, baseAttributeExtRef.isUnique());
            assertEquals(false, baseAttributeInterpolation.isUnique());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#getBaseElement()}.
     */
    @Test
    public void testGetBaseElement() {
        try {
            assertEquals("AoMeasurementQuantity", baseAttributeId.getBaseElement().getType());
            assertEquals("AoMeasurementQuantity", baseAttributeExtRef.getBaseElement().getType());
            assertEquals("AoMeasurementQuantity", baseAttributeInterpolation.getBaseElement().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseAttributeImpl#getEnumerationDefinition()}.
     */
    @Test
    public void testGetEnumerationDefinition() {
        try {
            assertEquals("interpolation_enum", baseAttributeInterpolation.getEnumerationDefinition().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseAttributeImplTest.class);
    }

}
