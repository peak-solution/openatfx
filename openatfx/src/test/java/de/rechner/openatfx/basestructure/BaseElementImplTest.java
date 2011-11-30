package de.rechner.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.BaseElementImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BaseElementImplTest {

    private static BaseElement baseElement;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, "asam30");
        baseElement = baseStructure.getElementByType("AoMeasurement");
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseElementImpl#getType()}.
     */
    @Test
    public void testGetType() {
        try {
            assertEquals("AoMeasurement", baseElement.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseElementImpl#isTopLevel()}.
     */
    @Test
    public void testIsTopLevel() {
        try {
            assertEquals(false, baseElement.isTopLevel());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseElementImpl#listAttributes(java.lang.String)}.
     */
    @Test
    public void testListAttributes() {
        try {
            assertEquals(10, baseElement.listAttributes("*").length);
            assertEquals(2, baseElement.listAttributes("v*").length);
            assertEquals(2, baseElement.listAttributes("V*").length);
            assertEquals(1, baseElement.listAttributes("NaM?").length);
            assertEquals(0, baseElement.listAttributes("NaasdM?").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseElementImpl#getAttributes(java.lang.String)}.
     */
    @Test
    public void testGetAttributes() {
        try {
            assertEquals(10, baseElement.getAttributes("*").length);
            assertEquals(2, baseElement.getAttributes("v*").length);
            assertEquals(2, baseElement.getAttributes("V*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseElementImpl#getAllRelations()}.
     */
    @Test
    public void testGetAllRelations() {
        try {
            assertEquals(11, baseElement.getAllRelations().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseElementImpl#listRelatedElementsByRelationship(org.asam.ods.Relationship)}
     * .
     */
    @Test
    public void testListRelatedElementsByRelationship() {
        try {
            assertEquals(11, baseElement.listRelatedElementsByRelationship(Relationship.ALL_REL).length);
            assertEquals(2, baseElement.listRelatedElementsByRelationship(Relationship.CHILD).length);
            assertEquals(2, baseElement.listRelatedElementsByRelationship(Relationship.FATHER).length);
            assertEquals(0, baseElement.listRelatedElementsByRelationship(Relationship.INFO_FROM).length);
            assertEquals(7, baseElement.listRelatedElementsByRelationship(Relationship.INFO_TO).length);
            assertEquals(7, baseElement.listRelatedElementsByRelationship(Relationship.INFO_REL).length);
            assertEquals(0, baseElement.listRelatedElementsByRelationship(Relationship.SUBTYPE).length);
            assertEquals(0, baseElement.listRelatedElementsByRelationship(Relationship.SUPERTYPE).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseElementImpl#getRelatedElementsByRelationship(org.asam.ods.Relationship)}
     * .
     */
    @Test
    public void testGetRelatedElementsByRelationship() {
        try {
            assertEquals(11, baseElement.getRelatedElementsByRelationship(Relationship.ALL_REL).length);
            assertEquals(2, baseElement.getRelatedElementsByRelationship(Relationship.CHILD).length);
            assertEquals(2, baseElement.getRelatedElementsByRelationship(Relationship.FATHER).length);
            assertEquals(0, baseElement.getRelatedElementsByRelationship(Relationship.INFO_FROM).length);
            assertEquals(7, baseElement.getRelatedElementsByRelationship(Relationship.INFO_TO).length);
            assertEquals(7, baseElement.getRelatedElementsByRelationship(Relationship.INFO_REL).length);
            assertEquals(0, baseElement.getRelatedElementsByRelationship(Relationship.SUBTYPE).length);
            assertEquals(0, baseElement.getRelatedElementsByRelationship(Relationship.SUPERTYPE).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseElementImpl#getRelationsByType(org.asam.ods.RelationType)}.
     */
    @Test
    public void testGetRelationsByType() {
        try {
            assertEquals(4, baseElement.getRelationsByType(RelationType.FATHER_CHILD).length);
            assertEquals(7, baseElement.getRelationsByType(RelationType.INFO).length);
            assertEquals(0, baseElement.getRelationsByType(RelationType.INHERITANCE).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseElementImplTest.class);
    }

}
