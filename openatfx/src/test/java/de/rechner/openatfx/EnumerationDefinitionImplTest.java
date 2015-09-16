package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.EnumerationDefinition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.EnumerationDefinitionImpl</code>.
 * 
 * @author Christian Rechner
 */
public class EnumerationDefinitionImplTest {

    private static AoSession aoSession;
    private EnumerationDefinition enumerationDefinition;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = EnumerationDefinitionImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Before
    public void setUp() throws Exception {
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        this.enumerationDefinition = applicationStructure.getEnumerationDefinition("coordinate_system_types");
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getIndex()}.
     */
    @Test
    public void testGetIndex() {
        try {
            assertEquals(6, enumerationDefinition.getIndex());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getName()}.
     */
    @Test
    public void testGetName() {
        try {
            assertEquals("coordinate_system_types", enumerationDefinition.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#setName(java.lang.String)}
     * .
     */
    @Test
    public void testSetName() {
        try {
            enumerationDefinition.setName("test");
            assertEquals("test", enumerationDefinition.getName());
            enumerationDefinition.setName("coordinate_system_types");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.setName("datatype_enum");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#listItemNames()}.
     */
    @Test
    public void testListItemNames() {
        try {
            assertEquals(3, enumerationDefinition.listItemNames().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItem(java.lang.String)}
     * .
     */
    @Test
    public void testGetItem() {
        try {
            assertEquals(0, enumerationDefinition.getItem("Cartesian"));
            assertEquals(1, enumerationDefinition.getItem("Polar"));
            assertEquals(2, enumerationDefinition.getItem("Cylindric"));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.getItem("non_existent_item");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItemName(int)}.
     */
    @Test
    public void testGetItemName() {
        try {
            assertEquals("Cartesian", enumerationDefinition.getItemName(0));
            assertEquals("Polar", enumerationDefinition.getItemName(1));
            assertEquals("Cylindric", enumerationDefinition.getItemName(2));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.getItemName(999);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#addItem(java.lang.String)}
     * .
     */
    @Test
    public void testAddItemString() {
        try {
            enumerationDefinition.addItem("test");
            assertEquals(4, enumerationDefinition.listItemNames().length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.addItem("Cartesian");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#renameItem(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testRenameItem() {
        try {
            enumerationDefinition.renameItem("Cylindric", "testNew");
            assertEquals(2, enumerationDefinition.getItem("testNew"));
            assertEquals("testNew", enumerationDefinition.getItemName(2));
            enumerationDefinition.renameItem("testNew", "Cylindric");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.renameItem("non_existing", "testNew");
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            enumerationDefinition.getItem("testNew");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(EnumerationDefinitionImplTest.class);
    }

}
