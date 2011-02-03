package de.rechner.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.BaseStructureImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BaseStructureImplTest {

    private static BaseStructure baseStructure;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, "asam30");
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseStructureImpl#getVersion()}.
     */
    @Test
    public void testGetVersion() {
        try {
            assertEquals("asam30", baseStructure.getVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseStructureImpl#listTopLevelElements(java.lang.String)}.
     */
    @Test
    public void testListTopLevelElements() {
        try {
            assertEquals(16, baseStructure.listTopLevelElements("*").length);
            assertEquals(3, baseStructure.listTopLevelElements("AoUnit*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseStructureImpl#getTopLevelElements(java.lang.String)}
     * .
     */
    @Test
    public void testGetTopLevelElements() {
        try {
            assertEquals(16, baseStructure.getTopLevelElements("*").length);
            assertEquals(3, baseStructure.getTopLevelElements("AoUnit*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseStructureImpl#listElements(java.lang.String)}.
     */
    @Test
    public void testListElements() {
        try {
            assertEquals(28, baseStructure.listElements("*").length);
            assertEquals(4, baseStructure.listElements("AoUnit*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseStructureImpl#getElements(java.lang.String)}.
     */
    @Test
    public void testGetElements() {
        try {
            assertEquals(28, baseStructure.getElements("*").length);
            assertEquals(4, baseStructure.getElements("AoUnit*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseStructureImpl#getElementByType(java.lang.String)}.
     */
    @Test
    public void testGetElementByType() {
        try {
            assertEquals("AoMeasurement", baseStructure.getElementByType("AoMeasurement").getType());
            assertEquals(false, baseStructure.getElementByType("AoMeasurement").isTopLevel());
            assertEquals(true, baseStructure.getElementByType("AoTest").isTopLevel());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseStructureImpl#getRelation(org.asam.ods.BaseElement, org.asam.ods.BaseElement)}
     * .
     */
    @Test
    public void testGetRelation() {
        try {
            BaseElement be1 = baseStructure.getElementByType("AoMeasurement");
            BaseElement be2 = baseStructure.getElementByType("AoMeasurementQuantity");
            assertEquals("measurement_quantities", baseStructure.getRelation(be1, be2).getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseStructureImplTest.class);
    }

}
