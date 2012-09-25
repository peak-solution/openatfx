package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.ApplicationRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationRelationImplTest {

    private static AoSession aoSession;
    private static ApplicationRelation baseRel;
    private static ApplicationRelation infoRel;
    private static ApplicationRelation m2nRel;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationAttributeImpl.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));

        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement aeGeo = as.getElementByName("geometry");
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeDts = as.getElementByName("dts");
        ApplicationElement aePas = as.getElementByName("pas");
        baseRel = as.getRelations(aeMea, aeDts)[0];
        infoRel = as.getRelations(aeGeo, aeDts)[0];
        m2nRel = as.getRelations(aeDts, aePas)[0];
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetBaseRelation() {
        try {
            assertEquals("children", baseRel.getBaseRelation().getRelationName());
            assertEquals(null, infoRel.getBaseRelation());
            assertEquals(null, m2nRel.getBaseRelation());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetElem1() {
        try {
            assertEquals("mea", baseRel.getElem1().getName());
            assertEquals("geometry", infoRel.getElem1().getName());
            assertEquals("dts", m2nRel.getElem1().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetElem2() {
        try {
            assertEquals("dts", baseRel.getElem2().getName());
            assertEquals("dts", infoRel.getElem2().getName());
            assertEquals("pas", m2nRel.getElem2().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationName() {
        try {
            assertEquals("dts_iid", baseRel.getRelationName());
            assertEquals("measurements_on_geometry", infoRel.getRelationName());
            assertEquals("pas_iid", m2nRel.getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInverseRelationName() {
        try {
            assertEquals("mea_iid", baseRel.getInverseRelationName());
            assertEquals("geometry", infoRel.getInverseRelationName());
            assertEquals("dts_iid", m2nRel.getInverseRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationRange() {
        try {
            RelationRange baseRelRange = baseRel.getRelationRange();
            assertEquals(0, baseRelRange.min);
            assertEquals(-1, baseRelRange.max);

            RelationRange infoRelRange = infoRel.getRelationRange();
            assertEquals(0, infoRelRange.min);
            assertEquals(-1, infoRelRange.max);

            RelationRange m2nRelRange = m2nRel.getRelationRange();
            assertEquals(0, m2nRelRange.min);
            assertEquals(-1, m2nRelRange.max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInverseRelationRange() {
        try {
            RelationRange baseRelRange = baseRel.getInverseRelationRange();
            assertEquals(1, baseRelRange.min);
            assertEquals(1, baseRelRange.max);

            RelationRange infoRelRange = infoRel.getInverseRelationRange();
            assertEquals(0, infoRelRange.min);
            assertEquals(1, infoRelRange.max);

            RelationRange m2nRelRange = m2nRel.getInverseRelationRange();
            assertEquals(0, m2nRelRange.min);
            assertEquals(-1, m2nRelRange.max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationship() {
        try {
            assertEquals(Relationship.CHILD, baseRel.getRelationship());
            assertEquals("INFO_FROM", ODSHelper.relationship2string(infoRel.getRelationship()));
            assertEquals(Relationship.INFO_REL, m2nRel.getRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInverseRelationship() {
        try {
            assertEquals(Relationship.FATHER, baseRel.getInverseRelationship());
            assertEquals(Relationship.INFO_TO, infoRel.getInverseRelationship());
            assertEquals(Relationship.INFO_REL, m2nRel.getInverseRelationship());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationType() {
        try {
            assertEquals(RelationType.FATHER_CHILD, baseRel.getRelationType());
            assertEquals(RelationType.INFO, infoRel.getRelationType());
            assertEquals(RelationType.INFO, m2nRel.getRelationType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetBaseRelation() {}

    @Test
    public void testSetElem1() {}

    @Test
    public void testSetElem2() {}

    @Test
    public void testSetRelationName() {}

    @Test
    public void testSetInverseRelationName() {}

    @Test
    public void testSetRelationRange() {}

    @Test
    public void testSetInverseRelationRange() {}

    @Test
    public void testSetRelationType() {}

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplicationRelationImplTest.class);
    }

}
