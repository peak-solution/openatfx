package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.BaseAttribute;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.ApplicationElementImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationElementImplTest {

    private static AoSession aoSession;
    private static ApplicationElement applicationElement;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationElementImpl.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        applicationElement = aoSession.getApplicationStructure().getElementByName("dts");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testGetApplicationStructure() {
        try {
            applicationElement.getApplicationStructure();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetBaseElement() {
        try {
            assertEquals("AoMeasurement", applicationElement.getBaseElement().getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetBaseElement() {
        try {
            applicationElement.setBaseElement(null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetId() {
        try {
            assertEquals(19, ODSHelper.asJLong(applicationElement.getId()));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetName() {
        try {
            assertEquals("dts", applicationElement.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetName() {
        try {
            assertEquals("dts", applicationElement.getName());
            applicationElement.setName("new_name");
            assertEquals("new_name", applicationElement.getApplicationStructure().getElementByName("new_name")
                                                       .getName());
            applicationElement.setName("dts");
            assertEquals("dts", applicationElement.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
        // empty attribute name
        try {
            applicationElement.setName("");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_BAD_PARAMETER, e.errCode);
        }
        // name length > 30
        try {
            applicationElement.setName("012345678901234567890123456789x");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_BAD_PARAMETER, e.errCode);
        }
        // duplicate application element name
        try {
            applicationElement.setName("mea");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_DUPLICATE_NAME, e.errCode);
        }
    }

    @Test
    public void testCreateAttribute() {
        try {
            assertEquals(6, applicationElement.listAttributes("*").length);
            ApplicationAttribute aa = applicationElement.createAttribute();
            aa.setName("new_attribute");
            assertEquals("new_attribute", aa.getName());
            assertEquals(7, applicationElement.listAttributes("*").length);
            applicationElement.removeAttribute(aa);
            assertEquals(6, applicationElement.listAttributes("*").length);
        } catch (AoException e) {
            fail(e.reason);
        }

        // test create two attributes without changing the name
        ApplicationAttribute aa1 = null;
        try {
            aa1 = applicationElement.createAttribute();
            applicationElement.createAttribute();
        } catch (AoException e) {
            try {
                applicationElement.removeAttribute(aa1);
            } catch (AoException e1) {
                fail(e.reason);
            }
        }
    }

    @Test
    public void testListAttributes() {
        try {
            assertEquals(6, applicationElement.listAttributes("*").length);
            assertEquals(2, applicationElement.listAttributes("meas*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetAttributes() {
        try {
            assertEquals(6, applicationElement.getAttributes("*").length);
            assertEquals(2, applicationElement.listAttributes("meas*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetAttributeByName() {
        try {
            assertEquals("dts_iid", applicationElement.getAttributeByName("dts_iid").getName());
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            applicationElement.getAttributeByName("non_existing_attribute");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetAttributeByBaseName() {
        try {
            assertEquals("dts_iid", applicationElement.getAttributeByBaseName("id").getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testRemoveAttribute() {
        try {
            assertEquals(6, applicationElement.listAttributes("*").length);

            BaseAttribute ba = applicationElement.getBaseElement().getAttributes("description")[0];
            ApplicationAttribute aa = applicationElement.createAttribute();
            aa.setName("new_attribute");
            aa.setBaseAttribute(ba);

            assertEquals("new_attribute", aa.getName());
            assertEquals(7, applicationElement.listAttributes("*").length);
            assertNotNull(applicationElement.getAttributeByBaseName("description"));

            applicationElement.removeAttribute(aa);
            assertEquals(6, applicationElement.listAttributes("*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
        // try to access removed base attribute
        try {
            applicationElement.getAttributeByBaseName("description");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_FOUND, e.errCode);
        }
    }

    @Test
    public void testGetAllRelations() {
        try {
            ApplicationElement ae = applicationElement.getApplicationStructure().getElementByName("prj");

            assertEquals(2, ae.getAllRelations().length);

            ApplicationRelation firstRel = ae.getAllRelations()[0];
            assertEquals("env_iid", firstRel.getRelationName());
            assertEquals("prj_iid", firstRel.getInverseRelationName());
            assertEquals("environment", firstRel.getBaseRelation().getRelationName());
            assertEquals("tests", firstRel.getBaseRelation().getInverseRelationName());
            assertEquals(RelationType.FATHER_CHILD, firstRel.getRelationType());
            assertEquals(0, firstRel.getRelationRange().min);
            assertEquals(1, firstRel.getRelationRange().max);
            assertEquals(0, firstRel.getInverseRelationRange().min);
            assertEquals(-1, firstRel.getInverseRelationRange().max);
            assertEquals(Relationship.FATHER, firstRel.getRelationship());
            assertEquals(Relationship.CHILD, firstRel.getInverseRelationship());

            ApplicationRelation secRel = ae.getAllRelations()[1];
            assertEquals("tstser_iid", secRel.getRelationName());
            assertEquals("prj_iid", secRel.getInverseRelationName());
            assertEquals("children", secRel.getBaseRelation().getRelationName());
            assertEquals("parent_test", secRel.getBaseRelation().getInverseRelationName());
            assertEquals(RelationType.FATHER_CHILD, secRel.getRelationType());
            assertEquals(0, secRel.getRelationRange().min);
            assertEquals(-1, secRel.getRelationRange().max);
            assertEquals(1, secRel.getInverseRelationRange().min);
            assertEquals(1, secRel.getInverseRelationRange().max);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListAllRelatedElements() {
        try {
            // dts
            assertEquals(7, applicationElement.listAllRelatedElements().length);

            // audifahrzeug
            ApplicationElement aeFzg = aoSession.getApplicationStructure().getElementByName("audifahrzeug");
            assertEquals(8, aeFzg.listAllRelatedElements().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetAllRelatedElements() {
        try {
            // dts
            assertEquals(7, applicationElement.getAllRelatedElements().length);

            // audifahrzeug
            ApplicationElement aeFzg = aoSession.getApplicationStructure().getElementByName("audifahrzeug");
            assertEquals(8, aeFzg.getAllRelatedElements().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationsByBaseName() {
        try {
            // dts
            assertEquals(1, applicationElement.getRelationsByBaseName("test").length);
            assertEquals(1, applicationElement.getRelationsByBaseName("measurement_quantities").length);
            assertEquals(1, applicationElement.getRelationsByBaseName("units_under_test").length);

            // audifahrzeug
            ApplicationElement aeFzg = aoSession.getApplicationStructure().getElementByName("audifahrzeug");
            assertEquals(5, aeFzg.getRelationsByBaseName("children").length);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListRelatedElementsByRelationship() {
        try {
            assertEquals(7, applicationElement.listRelatedElementsByRelationship(Relationship.ALL_REL).length);
            assertEquals(1, applicationElement.listRelatedElementsByRelationship(Relationship.FATHER).length);
            assertEquals(2, applicationElement.listRelatedElementsByRelationship(Relationship.CHILD).length);
            assertEquals(1, applicationElement.listRelatedElementsByRelationship(Relationship.INFO_REL).length);
            assertEquals(1, applicationElement.listRelatedElementsByRelationship(Relationship.INFO_FROM).length);
            assertEquals(2, applicationElement.listRelatedElementsByRelationship(Relationship.INFO_TO).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelatedElementsByRelationship() {
        try {
            assertEquals(7, applicationElement.getRelatedElementsByRelationship(Relationship.ALL_REL).length);
            assertEquals(1, applicationElement.getRelatedElementsByRelationship(Relationship.FATHER).length);
            assertEquals(2, applicationElement.getRelatedElementsByRelationship(Relationship.CHILD).length);
            assertEquals(1, applicationElement.getRelatedElementsByRelationship(Relationship.INFO_REL).length);
            assertEquals(1, applicationElement.getRelatedElementsByRelationship(Relationship.INFO_FROM).length);
            assertEquals(2, applicationElement.getRelatedElementsByRelationship(Relationship.INFO_TO).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelationsByType() {
        try {
            assertEquals(3, applicationElement.getRelationsByType(RelationType.FATHER_CHILD).length);
            assertEquals(4, applicationElement.getRelationsByType(RelationType.INFO).length);
            assertEquals(0, applicationElement.getRelationsByType(RelationType.INHERITANCE).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testCreateInstance() {
        try {
            InstanceElement ie = applicationElement.createInstance("new_instance");
            applicationElement.removeInstance(ie.getId(), false);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testListInstances() {
        try {
            assertEquals(3, applicationElement.listInstances("*").getCount());
            assertEquals(1, applicationElement.listInstances("Slow*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInstances() {
        try {
            assertEquals(3, applicationElement.getInstances("*").getCount());
            assertEquals(1, applicationElement.getInstances("Slow*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInstanceById() {
        try {
            InstanceElement ie = applicationElement.getInstanceById(ODSHelper.asODSLongLong(58));
            assertEquals("1/3 Octave - Zusammenfassung", ie.getName());
            assertEquals(58, ODSHelper.asJLong(ie.getId()));
        } catch (AoException e) {
            fail(e.reason);
        }
        // query non existing instance
        try {
            applicationElement.getInstanceById(ODSHelper.asODSLongLong(999));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testGetInstanceByName() {
        try {
            InstanceElement ie = applicationElement.getInstanceByName("1/3 Octave - Zusammenfassung");
            assertEquals(58, ODSHelper.asJLong(ie.getId()));
            ie = applicationElement.getInstanceByName("not existing instance");
            assertEquals(null, ie);
        } catch (AoException e) {
            fail(e.reason);
        }
        // check duplicate names
        try {
            ApplicationElement aeParamSet = applicationElement.getApplicationStructure().getElementByName("pas");
            aeParamSet.getInstanceByName("basic");
            fail("AoException expected");
        } catch (AoException e) {
        }

    }

    @Test
    public void testRemoveInstance() {
        try {
            assertEquals(3, applicationElement.getInstances("*").getCount());
            InstanceElement ie = applicationElement.createInstance("new_instance");
            assertEquals(4, applicationElement.getInstances("*").getCount());
            applicationElement.removeInstance(ie.getId(), true);
            assertEquals(3, applicationElement.getInstances("*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
        // non existing instance
        try {
            applicationElement.removeInstance(ODSHelper.asODSLongLong(999), false);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testCreateInstances() {
        try {
            applicationElement.createInstances(null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetRights() {
        try {
            applicationElement.setRights(null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetRights() {
        try {
            applicationElement.getRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInitialRights() {
        try {
            applicationElement.getInitialRights();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInitialRights() {
        try {
            applicationElement.setInitialRights(null, 0, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInitialRightRelation() {
        try {
            applicationElement.setInitialRightRelation(null, true);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInitialRightRelations() {
        try {
            applicationElement.getInitialRightRelations();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetSecurityLevel() {
        try {
            applicationElement.getSecurityLevel();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetSecurityLevel() {
        try {
            applicationElement.setSecurityLevel(0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplicationElementImplTest.class);
    }

}
