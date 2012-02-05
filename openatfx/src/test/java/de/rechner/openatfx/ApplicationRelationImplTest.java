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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.ApplicationRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationRelationImplTest {

    private static AoSession aoSession;
    private static ApplicationRelation baseRel;
    private static ApplicationRelation infoRel;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationAttributeImpl.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));

        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement aeDsk = as.getElementByName("dsk");
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeDts = as.getElementByName("dts");
        baseRel = as.getRelations(aeMea, aeDts)[0];
        infoRel = as.getRelations(aeDsk, aeMea)[0];
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
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetElem1() {
        try {
            assertEquals("mea", baseRel.getElem1().getName());
            assertEquals("dsk", infoRel.getElem1().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetElem2() {
        try {
            assertEquals("dts", baseRel.getElem2().getName());
            assertEquals("mea", infoRel.getElem2().getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationName() {
        try {
            assertEquals("dts_iid", baseRel.getRelationName());
            assertEquals("mea_iid", infoRel.getRelationName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInverseRelationName() {
        try {
            assertEquals("mea_iid", baseRel.getInverseRelationName());
            assertEquals("dsk_iid", infoRel.getInverseRelationName());
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
    public void testGetRelationship() {}

    @Test
    public void testGetInverseRelationship() {}

    @Test
    public void testGetRelationType() {}

    @Test
    public void testSetRelationType() {}

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplicationRelationImplTest.class);
    }

}
