package com.peaksolution.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
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
 * Test case for <code>com.peaksolution.openatfx.basestructure.BaseStructureImpl</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class BaseStructureImplTest {

    private static ORB orb;
    private static BaseStructure baseStructure;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        BaseModel baseModel = BaseModelFactory.getInstance().getBaseModel("asam31");
        OpenAtfxAPI api = new OpenAtfxAPIImplementation(baseModel);
        baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getVersion()}.
     */
    @Test
    void testGetVersion() {
        try {
            assertEquals("asam31", baseStructure.getVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#listTopLevelElements(java.lang.String)}.
     */
    @Test
    void testListTopLevelElements() {
        try {
            assertEquals(16, baseStructure.listTopLevelElements("*").length);
            assertEquals(3, baseStructure.listTopLevelElements("AoUnit*").length);
            assertEquals(3, baseStructure.listTopLevelElements("AOuNIT*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getTopLevelElements(java.lang.String)}
     * .
     */
    @Test
    void testGetTopLevelElements() {
        try {
            assertEquals(0, baseStructure.getTopLevelElements("unknown").length);
            assertEquals(16, baseStructure.getTopLevelElements("*").length);
            assertEquals(3, baseStructure.getTopLevelElements("AoUnit*").length);
            assertEquals(2, baseStructure.getTopLevelElements("*tES?").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#listElements(java.lang.String)}.
     */
    @Test
    void testListElements() {
        try {
            assertEquals(29, baseStructure.listElements("*").length);
            assertEquals(4, baseStructure.listElements("AoUnit*").length);
            // test case insensitivity of base element lookup
            assertEquals(1, baseStructure.listElements("aoquantityg*").length);
            assertEquals(9, baseStructure.listElements("*TEST*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getElements(java.lang.String)}.
     */
    @Test
    void testGetElements() {
        try {
            assertEquals(29, baseStructure.getElements("*").length);
            assertEquals(4, baseStructure.getElements("AoUnit*").length);
            assertEquals(2, baseStructure.getElements("a?param*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getElementByType(java.lang.String)}.
     */
    @Test
    void testGetElementByType() {
        try {
            assertEquals("AoMeasurement", baseStructure.getElementByType("AoMeasurement").getType());
            assertEquals(false, baseStructure.getElementByType("AoMeasurement").isTopLevel());
            assertEquals(true, baseStructure.getElementByType("AoTest").isTopLevel());
            // test case insensitivity of base element lookup methods
            assertEquals("AoUnit", baseStructure.getElementByType("AOUNIT").getType());
            assertEquals("AoQuantity", baseStructure.getElementByType("aoquantity").getType());
            assertEquals("AoParameter", baseStructure.getElementByType("AoparAMetER").getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getRelation(org.asam.ods.BaseElement, org.asam.ods.BaseElement)}
     * .
     */
    @Test
    void testGetRelation() {
        try {
            BaseElement be1 = baseStructure.getElementByType("aoMeasurement");
            BaseElement be2 = baseStructure.getElementByType("AOMeasurementQUANTITY");
            assertEquals("measurement_quantities", baseStructure.getRelation(be1, be2).getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link com.peaksolution.openatfx.basestructure.BaseStructureImpl#getRelations(org.asam.ods.BaseElement, org.asam.ods.BaseElement)}
     * .
     */
    @Test
    void testGetRelations() {
        try {
            BaseElement be1 = baseStructure.getElementByType("aoMeasurement");
            BaseElement be2 = baseStructure.getElementByType("AOMeasurementQUANTITY");
            assertEquals(1, baseStructure.getRelations(be1, be2).length);
            assertEquals("measurement_quantities", baseStructure.getRelations(be1, be2)[0].getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
