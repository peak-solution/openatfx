package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.Blob;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.LockMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.AoSessionImpl</code>.
 * 
 * @author Christian Rechner
 */
public class AoSessionImplTest {

    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getName()}.
     */
    @Test
    public void testGetName() {
        try {
            assertEquals("example_atfx.xml", aoSession.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        try {
            assertEquals("ATFX File", aoSession.getDescription());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getType()}.
     */
    @Test
    public void testGetType() {
        try {
            assertEquals("asam29", aoSession.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getBaseStructure()}.
     */
    @Test
    public void testGetBaseStructure() {
        try {
            assertEquals("asam29", aoSession.getBaseStructure().getVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getApplicationStructure()}.
     */
    @Test
    public void testGetApplicationStructure() {
        try {
            aoSession.getApplicationStructure();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getApplElemAccess()}.
     */
    @Test
    public void testGetApplElemAccess() {
        try {
            assertNotNull(aoSession.getApplElemAccess());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#createQueryEvaluator()}.
     */
    @Test
    public void testCreateQueryEvaluator() {
        try {
            aoSession.createQueryEvaluator();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#listContext(java.lang.String)}.
     */
    @Test
    public void testListContext() {
        try {
            assertEquals(21, aoSession.listContext("*").getCount());
            assertEquals(3, aoSession.listContext("WILD*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getContext(java.lang.String)}.
     */
    @Test
    public void testGetContext() {
        try {
            assertEquals(21, aoSession.getContext("*").getCount());
            assertEquals(3, aoSession.getContext("WILD*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getContextByName(java.lang.String)}.
     */
    @Test
    public void testGetContextByName() {
        try {
            assertEquals("WILDCARD_ONE", aoSession.getContextByName("WILDCARD_ONE").valName);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aoSession.getContextByName("not existing context");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#setContext(org.asam.ods.NameValue)}.
     */
    @Test
    public void testSetContext() {
        try {
            aoSession.setContext(ODSHelper.createStringNV("NEW_CONTEXT", "test"));
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(22, aoSession.listContext("*").getCount());
            aoSession.removeContext("NEW_CONTEXT");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aoSession.setContext(ODSHelper.createStringNV("FILENAME", "test"));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.AoSessionImpl#setContextString(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testSetContextString() {
        try {
            aoSession.setContextString("NEW_CONTEXT", "test");
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(22, aoSession.listContext("*").getCount());
            aoSession.removeContext("NEW_CONTEXT");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aoSession.setContextString("FILENAME", "test");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#removeContext(java.lang.String)}.
     */
    @Test
    public void testRemoveContext() {
        try {
            aoSession.setContextString("NEW_CONTEXT", "test");
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(22, aoSession.listContext("*").getCount());
            aoSession.removeContext("NEW_CONTEXT");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aoSession.setContextString("FILENAME", "test");
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#createBlob()}.
     */
    @Test
    public void testCreateBlob() {
        try {
            Blob blob = aoSession.createBlob();
            blob.setHeader("header");
            blob.append(new byte[] { 1, 2, 3, 4, 5 });
            assertEquals("header", blob.getHeader());
            assertEquals(5, blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.AoSessionImpl#setCurrentInitialRights(org.asam.ods.InitialRight[], boolean)}
     * .
     */
    @Test
    public void testSetCurrentInitialRights() {
        try {
            aoSession.setCurrentInitialRights(new InitialRight[0], true);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#startTransaction()}.
     */
    @Test
    public void testStartTransaction() {}

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#commitTransaction()}.
     */
    @Test
    public void testCommitTransaction() {}

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#abortTransaction()}.
     */
    @Test
    public void testAbortTransaction() {}

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#flush()}.
     */
    @Test
    public void testFlush() {}

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#createCoSession()}.
     */
    @Test
    public void testCreateCoSession() {
        try {
            aoSession.createCoSession();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getLockMode()}.
     */
    @Test
    public void testGetLockMode() {
        try {
            aoSession.getLockMode();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#setLockMode(short)}.
     */
    @Test
    public void testSetLockMode() {
        try {
            aoSession.setLockMode(LockMode.LOCK_CHILDREN);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getUser()}.
     */
    @Test
    public void testGetUser() {
        try {
            aoSession.getUser();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for
     * {@link de.rechner.openatfx.util.atfx.AoSessionImpl#setPassword(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testSetPassword() {
        try {
            aoSession.setPassword("aa", "aas", "asda");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getApplicationStructureValue()}.
     */
    @Test
    public void testGetApplicationStructureValue() {
        try {
            ApplicationStructureValue asv = aoSession.getApplicationStructureValue();
            assertEquals(33, asv.applElems.length);
            assertEquals(114, asv.applRels.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getEnumerationAttributes()}.
     */
    @Test
    public void testGetEnumerationAttributes() {
        try {
            assertEquals(10, aoSession.getEnumerationAttributes().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#getEnumerationStructure()}.
     */
    @Test
    public void testGetEnumerationStructure() {
        try {
            EnumerationStructure[] es = aoSession.getEnumerationStructure();
            assertEquals(7, es.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoSessionImpl#close()}.
     */
    @Test
    public void testClose() {}

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(AoSessionImplTest.class);
    }

}
