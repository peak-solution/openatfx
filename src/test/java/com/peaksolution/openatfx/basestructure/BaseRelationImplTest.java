package com.peaksolution.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
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
 * Test case for <code>com.peaksolution.openatfx.basestructure.BaseRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class BaseRelationImplTest {

    private static BaseRelation baseRelation;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseModel baseModel = BaseModelFactory.getInstance().getBaseModel("asam30");
        OpenAtfxAPI api = new OpenAtfxAPIImplementation(baseModel);
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
        BaseElement elem1 = baseStructure.getElementByType("AoMeasurement");
        BaseElement elem2 = baseStructure.getElementByType("AoMeasurementQuantity");
        baseRelation = baseStructure.getRelation(elem1, elem2);
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getElem1()}.
     */
    @Test
    void testGetElem1() {
        try {
            assertEquals("AoMeasurement", baseRelation.getElem1().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getElem2()}.
     */
    @Test
    void testGetElem2() {
        try {
            assertEquals("AoMeasurementQuantity", baseRelation.getElem2().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getRelationName()}.
     */
    @Test
    void testGetRelationName() {
        try {
            assertEquals("measurement_quantities", baseRelation.getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getInverseRelationName()}.
     */
    @Test
    void testGetInverseRelationName() {
        try {
            assertEquals("measurement", baseRelation.getInverseRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getRelationRange()}.
     */
    @Test
    void testGetRelationRange() {
        try {
            assertEquals((short) 0, baseRelation.getRelationRange().min);
            assertEquals((short) -1, baseRelation.getRelationRange().max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getInverseRelationRange()}.
     */
    @Test
    void testGetInverseRelationRange() {
        try {
            assertEquals((short) 1, baseRelation.getInverseRelationRange().min);
            assertEquals((short) 1, baseRelation.getInverseRelationRange().max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getRelationship()}.
     */
    @Test
    void testGetRelationship() {
        try {
            assertEquals(Relationship.CHILD, baseRelation.getRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getInverseRelationship()}.
     */
    @Test
    void testGetInverseRelationship() {
        try {
            assertEquals(Relationship.FATHER, baseRelation.getInverseRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseRelationImpl#getRelationType()}.
     */
    @Test
    void testGetRelationType() {
        try {
            assertEquals(RelationType.FATHER_CHILD, baseRelation.getRelationType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
