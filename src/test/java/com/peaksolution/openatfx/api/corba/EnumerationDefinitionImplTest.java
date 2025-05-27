package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.EnumerationDefinition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;


/**
 * Test case for <code>com.peaksolution.openatfx.EnumerationDefinitionImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class EnumerationDefinitionImplTest {

    private static AoSession aoSession;
    private EnumerationDefinition enumerationDefinition;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = EnumerationDefinitionImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @BeforeEach
    public void setUp() throws Exception {
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        this.enumerationDefinition = applicationStructure.getEnumerationDefinition("coordinate_system_types");
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getIndex()}.
     */
    @Test
    void testGetIndex() {
        try {
            assertEquals(6, enumerationDefinition.getIndex());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getName()}.
     */
    @Test
    void testGetName() {
        try {
            assertEquals("coordinate_system_types", enumerationDefinition.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#setRelationName(java.lang.String)}
     * .
     */
    @Test
    void testSetName() {
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
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#listItemNames()}.
     */
    @Test
    void testListItemNames() {
        try {
            assertEquals(3, enumerationDefinition.listItemNames().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItem(java.lang.String)}
     * .
     */
    @Test
    void testGetItem() {
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
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItemName(int)}.
     */
    @Test
    void testGetItemName() {
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
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#addItem(java.lang.String)}
     * .
     */
    @Test
    void testAddItemString() {
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
     * {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#renameItem(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    void testRenameItem() {
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
}
