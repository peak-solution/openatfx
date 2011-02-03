package de.rechner.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.BaseRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BaseRelationImplTest {

    private static BaseRelation baseRelation;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, "asam30");
        BaseElement elem1 = baseStructure.getElementByType("AoMeasurement");
        BaseElement elem2 = baseStructure.getElementByType("AoMeasurementQuantity");
        baseRelation = baseStructure.getRelation(elem1, elem2);
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getElem1()}.
     */
    @Test
    public void testGetElem1() {
        try {
            assertEquals("AoMeasurement", baseRelation.getElem1().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getElem2()}.
     */
    @Test
    public void testGetElem2() {
        try {
            assertEquals("AoMeasurementQuantity", baseRelation.getElem2().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getRelationName()}.
     */
    @Test
    public void testGetRelationName() {
        try {
            assertEquals("measurement_quantities", baseRelation.getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getInverseRelationName()}.
     */
    @Test
    public void testGetInverseRelationName() {
        try {
            assertEquals("measurement", baseRelation.getInverseRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getRelationRange()}.
     */
    @Test
    public void testGetRelationRange() {
        try {
            assertEquals((short) 0, baseRelation.getRelationRange().min);
            assertEquals((short) -1, baseRelation.getRelationRange().max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getInverseRelationRange()}.
     */
    @Test
    public void testGetInverseRelationRange() {
        try {
            assertEquals((short) 1, baseRelation.getInverseRelationRange().min);
            assertEquals((short) 1, baseRelation.getInverseRelationRange().max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getRelationship()}.
     */
    @Test
    public void testGetRelationship() {
        try {
            assertEquals(Relationship.CHILD, baseRelation.getRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getInverseRelationship()}.
     */
    @Test
    public void testGetInverseRelationship() {
        try {
            assertEquals(Relationship.FATHER, baseRelation.getInverseRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseRelationImpl#getRelationType()}.
     */
    @Test
    public void testGetRelationType() {
        try {
            assertEquals(RelationType.FATHER_CHILD, baseRelation.getRelationType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseRelationImplTest.class);
    }

}
