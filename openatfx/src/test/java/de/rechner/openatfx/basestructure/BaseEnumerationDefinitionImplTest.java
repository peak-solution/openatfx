package de.rechner.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.asam.ods.EnumerationDefinition;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.EnumerationDefinitionImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BaseEnumerationDefinitionImplTest {

    private static EnumerationDefinition enumerationDefinition;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, "asam30");
        BaseElement baseElement = baseStructure.getElementByType("AoExternalComponent");
        BaseAttribute baseAttribute = baseElement.getAttributes("value_type")[0];
        enumerationDefinition = baseAttribute.getEnumerationDefinition();
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getIndex()}.
     */
    @Test
    public void testGetIndex() {
        try {
            assertEquals(3, enumerationDefinition.getIndex());
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
            assertEquals("typespec_enum", enumerationDefinition.getName());
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
            fail("Should not be possible");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#listItemNames()}.
     */
    @Test
    public void testListItemNames() {
        try {
            assertEquals(25, enumerationDefinition.listItemNames().length);
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
            assertEquals(0, enumerationDefinition.getItem("dt_boolean"));
            assertEquals(10, enumerationDefinition.getItem("ieeefloat4_beo"));
            assertEquals(24, enumerationDefinition.getItem("dt_ulong_beo"));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.getItem("non_existent_item");
            fail("Should not be possible");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItemName(int)}.
     */
    @Test
    public void testGetItemName() {
        try {
            assertEquals("dt_boolean", enumerationDefinition.getItemName(0));
            assertEquals("ieeefloat4_beo", enumerationDefinition.getItemName(10));
            assertEquals("dt_ulong_beo", enumerationDefinition.getItemName(24));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            enumerationDefinition.getItemName(999);
            fail("Should not be possible");
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
            fail("Should not be possible");
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
            enumerationDefinition.renameItem("test", "testNew");
            fail("Should not be possible");
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BaseEnumerationDefinitionImplTest.class);
    }

}
