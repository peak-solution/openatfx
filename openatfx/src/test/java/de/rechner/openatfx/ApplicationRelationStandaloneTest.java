package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseStructure;
import org.asam.ods.RelationRange;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.ApplicationRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationRelationStandaloneTest {

    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoSession = AoServiceFactory.getInstance().newEmptyAoSession(orb, File.createTempFile("xxx", "tmp"), "asam30");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testCreateRelation() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            BaseStructure bs = as.getSession().getBaseStructure();
            ApplicationElement elem1 = as.createElement(bs.getElementByType("aounit"));
            elem1.setName("unt");
            ApplicationElement elem2 = as.createElement(bs.getElementByType("aoquantity"));
            elem2.setName("qua");

            // create relation
            ApplicationRelation rel = as.createRelation();
            assertEquals(null, rel.getBaseRelation());
            assertEquals(null, rel.getElem1());
            assertEquals(null, rel.getElem2());
            assertEquals(0, elem1.getAllRelations().length);
            assertEquals(0, elem2.getAllRelations().length);

            // set base relation
            rel.setBaseRelation(bs.getRelation(bs.getElementByType("aounit"), bs.getElementByType("aoquantity")));
            assertEquals("quantities", rel.getBaseRelation().getRelationName());

            // set elem1 and elem2
            rel.setElem1(elem1);
            assertEquals(1, elem1.getAllRelations().length);
            assertEquals(0, elem2.getAllRelations().length);
            assertEquals("unt", rel.getElem1().getName());

            rel.setElem2(elem2);
            assertEquals(1, elem1.getAllRelations().length);
            assertEquals(1, elem2.getAllRelations().length);
            assertEquals("unt", rel.getElem1().getName());
            assertEquals("qua", rel.getElem2().getName());

            // inverse relation
            ApplicationRelation invRel = elem2.getAllRelations()[0];
            assertEquals("qua", invRel.getElem1().getName());
            assertEquals("unt", invRel.getElem2().getName());
            assertEquals("default_unit", invRel.getBaseRelation().getRelationName());

            // relation name and inverse relation name
            rel.setRelationName("relname");
            rel.setInverseRelationName("invrelname");
            assertEquals("relname", rel.getRelationName());
            assertEquals("invrelname", rel.getInverseRelationName());
            assertEquals("invrelname", invRel.getRelationName());
            assertEquals("relname", invRel.getInverseRelationName());

            // relation range
            assertEquals(0, rel.getRelationRange().min);
            assertEquals(-1, rel.getRelationRange().max);
            assertEquals(0, rel.getInverseRelationRange().min);
            assertEquals(1, rel.getInverseRelationRange().max);
            assertEquals(0, invRel.getRelationRange().min);
            assertEquals(1, invRel.getRelationRange().max);
            assertEquals(0, invRel.getInverseRelationRange().min);
            assertEquals(-1, invRel.getInverseRelationRange().max);

            // change relation range
            rel.setRelationRange(new RelationRange((short) 1, (short) 1));
            assertEquals(1, rel.getRelationRange().min);
            assertEquals(1, rel.getRelationRange().max);
            assertEquals(0, rel.getInverseRelationRange().min);
            assertEquals(1, rel.getInverseRelationRange().max);
            assertEquals(0, invRel.getRelationRange().min);
            assertEquals(1, invRel.getRelationRange().max);
            assertEquals(1, invRel.getInverseRelationRange().min);
            assertEquals(1, invRel.getInverseRelationRange().max);

        } catch (AoException e) {
            fail(e.reason);
        }

    }

}
