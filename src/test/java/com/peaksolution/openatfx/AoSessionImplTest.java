package com.peaksolution.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.Blob;
import org.asam.ods.EnumerationStructure;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.LockMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.AoSessionImpl</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class AoSessionImplTest {

    private static AoFactory aoFactory;
    private static AoSession aoSession;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
        URL url = AoSessionImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getId()}.
     */
    @Test
    void testGetId() {

    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getName()}.
     */
    @Test
    void testGetName() {
        try {
            assertEquals("example.atfx", aoSession.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getDescription()}.
     */
    @Test
    void testGetDescription() {
        try {
            assertEquals("ATFX File", aoSession.getDescription());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getType()}.
     */
    @Test
    void testGetType() {
        try {
            assertEquals("asam31", aoSession.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getBaseStructure()}.
     */
    @Test
    void testGetBaseStructure() {
        try {
            assertEquals("asam31", aoSession.getBaseStructure().getVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getApplicationStructure()}.
     */
    @Test
    void testGetApplicationStructure() {
        try {
            aoSession.getApplicationStructure();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getApplElemAccess()}.
     */
    @Test
    void testGetApplElemAccess() {
        try {
            assertNotNull(aoSession.getApplElemAccess());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#createQueryEvaluator()}.
     */
    @Test
    void testCreateQueryEvaluator() {
        try {
            aoSession.createQueryEvaluator();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#listContext(java.lang.String)}.
     */
    @Test
    void testListContext() {
        try {
            assertEquals(24, aoSession.listContext("*").getCount());
            assertEquals(3, aoSession.listContext("WILD*").getCount());
            assertEquals("WILDCARD_ALL", aoSession.listContext("WILDCARD_ALL").nextOne());
            assertEquals("WILDCARD_ALL", aoSession.listContext("wildcard_ALL").nextOne());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getContext(java.lang.String)}.
     */
    @Test
    void testGetContext() {
        try {
            assertEquals(24, aoSession.getContext("*").getCount());
            assertEquals(3, aoSession.getContext("WILD*").getCount());
            assertEquals("*", aoSession.getContext("WILDCARD_ALL").nextOne().value.u.stringVal());
            assertEquals("*", aoSession.getContext("wildcard_ALL").nextOne().value.u.stringVal());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getContextByName(java.lang.String)}.
     */
    @Test
    void testGetContextByName() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#setContext(org.asam.ods.NameValue)}.
     */
    @Test
    void testSetContext() {
        try {
            aoSession.setContext(ODSHelper.createStringNV("NEW_CONTEXT", "test"));
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(25, aoSession.listContext("*").getCount());
            aoSession.removeContext("NEW_CONTEXT");
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            aoSession.setContext(ODSHelper.createStringNV(OpenAtfxConstants.CONTEXT_ODS_API_VERSION, "test"));
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for
     * {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#setContextString(java.lang.String, java.lang.String)}.
     */
    @Test
    void testSetContextString() {
        try {
            aoSession.setContextString("NEW_CONTEXT", "test");
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(25, aoSession.listContext("*").getCount());
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#removeContext(java.lang.String)}.
     */
    @Test
    void testRemoveContext() {
        try {
            aoSession.setContextString("NEW_CONTEXT", "test");
            assertEquals("test", aoSession.getContextByName("NEW_CONTEXT").value.u.stringVal());
            assertEquals(25, aoSession.listContext("*").getCount());
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#createBlob()}.
     */
    @Test
    void testCreateBlob() {
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
     * {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#setCurrentInitialRights(org.asam.ods.InitialRight[], boolean)}
     * .
     */
    @Test
    void testSetCurrentInitialRights() {
        try {
            aoSession.setCurrentInitialRights(new InitialRight[0], true);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#startTransaction()}.
     */
    @Test
    void testStartTransaction() {}

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#commitTransaction()}.
     */
    @Test
    void testCommitTransaction() {
        // try {
        // aoSession.commitTransaction();
        // } catch (AoException e) {
        // fail(e.reason);
        // }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#abortTransaction()}.
     */
    @Test
    void testAbortTransaction() {}

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#flush()}.
     */
    @Test
    void testFlush() {}

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#createCoSession()}.
     */
    @Test
    void testCreateCoSession() {
        try {
            aoSession.createCoSession();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getLockMode()}.
     */
    @Test
    void testGetLockMode() {
        try {
            aoSession.getLockMode();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#setLockMode(short)}.
     */
    @Test
    void testSetLockMode() {
        try {
            aoSession.setLockMode(LockMode.LOCK_CHILDREN);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getUser()}.
     */
    @Test
    void testGetUser() {
        try {
            aoSession.getUser();
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for
     * {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#setPassword(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    void testSetPassword() {
        try {
            aoSession.setPassword("aa", "aas", "asda");
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getApplicationStructureValue()}.
     * @throws AoException 
     */
    @Test
    void testGetApplicationStructureValue() throws AoException {
        try {
            ApplicationStructureValue asv = aoSession.getApplicationStructureValue();
            assertEquals(33, asv.applElems.length);
            assertEquals(114, asv.applRels.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getEnumerationAttributes()}.
     */
    @Test
    void testGetEnumerationAttributes() {
        try {
            assertEquals(10, aoSession.getEnumerationAttributes().length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#getEnumerationStructure()}.
     */
    @Test
    void testGetEnumerationStructure() {
        try {
            EnumerationStructure[] es = aoSession.getEnumerationStructure();
            assertEquals(8, es.length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoSessionImpl#close()}.
     */
    @Test
    void testClose() {}

}
