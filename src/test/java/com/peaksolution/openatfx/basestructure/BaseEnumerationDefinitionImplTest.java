package com.peaksolution.openatfx.basestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseStructure;
import org.asam.ods.EnumerationDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.api.BaseModel;
import com.peaksolution.openatfx.api.BaseModelFactory;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;


/**
 * Test case for <code>com.peaksolution.openatfx.basestructure.EnumerationDefinitionImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class BaseEnumerationDefinitionImplTest {

    private static EnumerationDefinition enumerationDefinition;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        BaseModel baseModel = BaseModelFactory.getInstance().getBaseModel("asam30");
        OpenAtfxAPI api = new OpenAtfxAPIImplementation(baseModel);
        BaseStructure baseStructure = BaseStructureFactory.getInstance().getBaseStructure(orb, api);
        BaseElement baseElement = baseStructure.getElementByType("AoExternalComponent");
        BaseAttribute baseAttribute = baseElement.getAttributes("value_type")[0];
        enumerationDefinition = baseAttribute.getEnumerationDefinition();
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getIndex()}.
     */
    @Test
    void testGetIndex() {
        try {
            assertEquals(3, enumerationDefinition.getIndex());
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
            assertEquals("typespec_enum", enumerationDefinition.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#setName(java.lang.String)}
     * .
     */
    @Test
    void testSetName() {
        try {
            enumerationDefinition.setName("test");
            fail("Should not be possible");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#listItemNames()}.
     */
    @Test
    void testListItemNames() {
        try {
            assertEquals(25, enumerationDefinition.listItemNames().length);
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
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#getItemName(int)}.
     */
    @Test
    void testGetItemName() {
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
     * Test method for {@link com.peaksolution.openatfx.basestructure.BaseEnumerationDefinitionImpl#addItem(java.lang.String)}
     * .
     */
    @Test
    void testAddItemString() {
        try {
            enumerationDefinition.addItem("test");
            fail("Should not be possible");
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
            enumerationDefinition.renameItem("test", "testNew");
            fail("Should not be possible");
        } catch (AoException e) {
        }
    }
}
