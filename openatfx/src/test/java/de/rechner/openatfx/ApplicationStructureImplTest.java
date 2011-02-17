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
import org.asam.ods.BaseElement;
import org.asam.ods.ElemId;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.ApplicationStructureImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplicationStructureImplTest {

    private static AoSession aoSession;
    private static ApplicationStructure applicationStructure;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationStructureImpl.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        applicationStructure = aoSession.getApplicationStructure();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getSession()}.
     */
    @Test
    public void testGetSession() {
        try {
            applicationStructure.getSession();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#listEnumerations()}.
     */
    @Test
    public void testListEnumerations() {
        try {
            assertEquals(7, applicationStructure.listEnumerations().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getEnumerationDefinition(java.lang.String)}.
     */
    @Test
    public void testGetEnumerationDefinition() {
        try {
            assertEquals("datatype_enum", applicationStructure.getEnumerationDefinition("datatype_enum").getName());
            assertEquals("axistype", applicationStructure.getEnumerationDefinition("axistype").getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#createEnumerationDefinition(java.lang.String)}.
     */
    @Test
    public void testCreateEnumerationDefinition() {
        try {
            EnumerationDefinition newEnumDef = applicationStructure.createEnumerationDefinition("new_enum");
            assertEquals("new_enum", newEnumDef.getName());
            applicationStructure.removeEnumerationDefinition("new_enum");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            applicationStructure.createEnumerationDefinition("");
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            applicationStructure.createEnumerationDefinition("datatype_enum");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#removeEnumerationDefinition(java.lang.String)}.
     */
    @Test
    public void testRemoveEnumerationDefinition() {
        try {
            applicationStructure.createEnumerationDefinition("new_enum");
            assertEquals("new_enum", applicationStructure.getEnumerationDefinition("new_enum").getName());
            assertEquals(8, applicationStructure.listEnumerations().length);
            applicationStructure.removeEnumerationDefinition("new_enum");
            assertEquals(7, applicationStructure.listEnumerations().length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            applicationStructure.removeEnumerationDefinition("xxx");
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            applicationStructure.removeEnumerationDefinition("datatype_enum");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#createElement(org.asam.ods.BaseElement)}.
     */
    @Test
    public void testCreateElement() {
        try {
            assertEquals(33, applicationStructure.listElements("*").length);
            BaseElement be = applicationStructure.getSession().getBaseStructure().getElementByType("AoMeasurement");
            ApplicationElement ae = applicationStructure.createElement(be);
            ae.setName("new_application_element");
            assertEquals(34, applicationStructure.listElements("*").length);

            // remove created element
            applicationStructure.removeElement(ae);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#listElements(java.lang.String)}.
     */
    @Test
    public void testListElements() {
        try {
            assertEquals(33, applicationStructure.listElements("*").length);
            assertEquals(9, applicationStructure.listElements("audi*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getElements(java.lang.String)}.
     */
    @Test
    public void testGetElements() {
        try {
            assertEquals(33, applicationStructure.getElements("*").length);
            assertEquals(9, applicationStructure.getElements("audi*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#listElementsByBaseType(java.lang.String)}.
     */
    @Test
    public void testListElementsByBaseType() {
        try {
            assertEquals(5, applicationStructure.listElementsByBaseType("AoAny").length);
            assertEquals(5, applicationStructure.getElementsByBaseType("aoany").length);
            assertEquals(4, applicationStructure.listElementsByBaseType("Ao*Test").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getElementsByBaseType(java.lang.String)}.
     */
    @Test
    public void testGetElementsByBaseType() {
        try {
            assertEquals(5, applicationStructure.getElementsByBaseType("AoAny").length);
            assertEquals(5, applicationStructure.getElementsByBaseType("aoany").length);
            assertEquals(4, applicationStructure.getElementsByBaseType("Ao*Test").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#listTopLevelElements(java.lang.String)}.
     */
    @Test
    public void testListTopLevelElements() {
        try {
            assertEquals(16, applicationStructure.listTopLevelElements("*").length);
            assertEquals(16, applicationStructure.listTopLevelElements("Ao*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getTopLevelElements(java.lang.String)} .
     */
    @Test
    public void testGetTopLevelElements() {
        try {
            assertEquals(16, applicationStructure.getTopLevelElements("*").length);
            assertEquals(16, applicationStructure.getTopLevelElements("Ao*").length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getElementById(org.asam.ods.T_LONGLONG)}.
     */
    @Test
    public void testGetElementById() {
        try {
            assertEquals("messzyklus", applicationStructure.getElementById(ODSHelper.asODSLongLong(5)).getName());
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            applicationStructure.getElementById(ODSHelper.asODSLongLong(999));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getElementByName(java.lang.String)}
     * .
     */
    @Test
    public void testGetElementByName() {
        try {
            assertEquals("dts", applicationStructure.getElementByName("dts").getName());
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            applicationStructure.getElementByName("not_existing_ae");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#removeElement(org.asam.ods.ApplicationElement)}.
     */
    @Test
    public void testRemoveElement() {
        try {
            // create element
            BaseElement be = aoSession.getBaseStructure().getElementByType("AoAny");
            ApplicationElement applElem = applicationStructure.createElement(be);
            applElem.setName("newElement");
            // create relation
            ApplicationElement elem2 = applicationStructure.getElementByName("dts");
            ApplicationRelation rel = applicationStructure.createRelation();
            rel.setElem1(applElem);
            rel.setElem2(elem2);
            rel.setRelationName("rel");
            rel.setInverseRelationName("inv_rel");
            ApplicationRelation invRel = applicationStructure.createRelation();
            invRel.setElem1(elem2);
            invRel.setElem2(applElem);
            invRel.setRelationName("inv_rel");
            invRel.setInverseRelationName("rel");

            assertEquals(34, applicationStructure.getElements("*").length);
            assertEquals(2, applicationStructure.getElementByName("newElement").getAttributes("*").length);
            assertEquals(1, applicationStructure.getElementByName("newElement").getAllRelations().length);
            assertEquals(8, applicationStructure.getElementByName("dts").getAllRelations().length);

            // remove element
            applicationStructure.removeElement(applElem);
            assertEquals(33, applicationStructure.getElements("*").length);
            assertEquals(7, applicationStructure.getElementByName("dts").getAllRelations().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#createRelation()}.
     */
    @Test
    public void testCreateRelation() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getRelations(org.asam.ods.ApplicationElement, org.asam.ods.ApplicationElement)}
     * .
     */
    @Test
    public void testGetRelations() {
        try {
            ApplicationElement elem1 = applicationStructure.getElementsByBaseType("AoTest")[0];
            ApplicationElement elem2 = applicationStructure.getElementsByBaseType("AoSubTest")[0];
            ApplicationRelation[] rels = applicationStructure.getRelations(elem1, elem2);
            assertEquals(1, rels.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#removeRelation(org.asam.ods.ApplicationRelation)}.
     */
    @Test
    public void testRemoveRelation() {
        try {
            ApplicationElement elem1 = applicationStructure.getElementsByBaseType("AoTest")[0];
            ApplicationElement elem2 = applicationStructure.getElementsByBaseType("AoSubTest")[0];
            assertEquals(1, applicationStructure.getRelations(elem1, elem2).length);
            assertEquals(1, applicationStructure.getRelations(elem2, elem1).length);

            ApplicationRelation rel = applicationStructure.createRelation();
            rel.setElem1(elem1);
            rel.setElem2(elem2);
            rel.setRelationName("rel");
            rel.setInverseRelationName("inv_rel");
            ApplicationRelation invRel = applicationStructure.createRelation();
            invRel.setElem1(elem2);
            invRel.setElem2(elem1);
            invRel.setRelationName("inv_rel");
            invRel.setInverseRelationName("rel");

            assertEquals(3, elem1.getAllRelations().length);
            assertEquals(3, elem2.getAllRelations().length);
            assertEquals(2, applicationStructure.getRelations(elem1, elem2).length);
            assertEquals(2, applicationStructure.getRelations(elem2, elem1).length);

            applicationStructure.removeRelation(rel);

            assertEquals(2, elem1.getAllRelations().length);
            assertEquals(2, elem2.getAllRelations().length);
            assertEquals(1, applicationStructure.getRelations(elem1, elem2).length);
            assertEquals(1, applicationStructure.getRelations(elem2, elem1).length);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getInstancesById(org.asam.ods.ElemId[])}.
     */
    @Test
    public void testGetInstancesById() {
        try {
            ElemId[] ieIds = new ElemId[3];
            ieIds[0] = new ElemId(ODSHelper.asODSLongLong(25), ODSHelper.asODSLongLong(14));
            ieIds[1] = new ElemId(ODSHelper.asODSLongLong(6), ODSHelper.asODSLongLong(35));
            ieIds[2] = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));

            InstanceElement[] ies = applicationStructure.getInstancesById(ieIds);
            assertEquals("test.2345", ies[0].getName());
            assertEquals("Pa for soundpressure", ies[1].getName());
            assertEquals("Detector;rms A fast - Zusammenfassung", ies[2].getName());
        } catch (AoException e) {
            fail(e.reason);
        }
        // invalid aid
        try {
            ElemId[] ieIds = new ElemId[3];
            ieIds[0] = new ElemId(ODSHelper.asODSLongLong(25), ODSHelper.asODSLongLong(14));
            ieIds[1] = new ElemId(ODSHelper.asODSLongLong(999), ODSHelper.asODSLongLong(35));
            ieIds[2] = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));
            applicationStructure.getInstancesById(ieIds);
            fail("AoException expected");
        } catch (AoException e) {
        }
        // invalid iid
        try {
            ElemId[] ieIds = new ElemId[3];
            ieIds[0] = new ElemId(ODSHelper.asODSLongLong(25), ODSHelper.asODSLongLong(14));
            ieIds[1] = new ElemId(ODSHelper.asODSLongLong(6), ODSHelper.asODSLongLong(95648));
            ieIds[2] = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));
            applicationStructure.getInstancesById(ieIds);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#getInstanceByAsamPath(java.lang.String)}.
     */
    @Test
    public void testGetInstanceByAsamPath() {
        try {
            // a single ASAM path
            String asamPath = "/[prj]no_project/[tstser]Test_Vorbeifahrt/[mea]Run_middEng_FINAL_RES";
            InstanceElement ie = applicationStructure.getInstanceByAsamPath(asamPath);
            assertEquals("mea", ie.getApplicationElement().getName());
            assertEquals("Run_middEng_FINAL_RES", ie.getName());

            // walk through all existing instances
            for (ApplicationElement ae : applicationStructure.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    ie = iter.nextOne();
                    // exclude parameterset
                    if (!ie.getApplicationElement().getName().equals("pas")
                            && !ie.getApplicationElement().getName().equals("par")) {
                        InstanceElement foundIe = applicationStructure.getInstanceByAsamPath(ie.getAsamPath());
                        assertEquals(0, ODSHelper.asJLong(ie.compare(foundIe)));
                    }
                }
                ie.destroy();
            }

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#createInstanceRelations(org.asam.ods.ApplicationRelation, org.asam.ods.InstanceElement[], org.asam.ods.InstanceElement[])}
     * .
     */
    @Test
    public void testCreateInstanceRelations() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.ApplicationStructureImpl#check()}.
     */
    @Test
    public void testCheck() {
        try {
            applicationStructure.check();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplicationStructureImplTest.class);
    }

}
