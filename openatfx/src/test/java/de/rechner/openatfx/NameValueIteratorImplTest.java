package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.NameValueIteratorHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import de.rechner.openatfx.util.ODSHelper;
import junit.framework.JUnit4TestAdapter;


/**
 * Test case for <code>de.rechner.openatfx.NameValueIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
public class NameValueIteratorImplTest {

    private static POA poa;
    private NameValueIterator nameValueIterator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        poa.the_POAManager().activate();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        poa.destroy(false, false);
    }

    @Before
    public void setUp() throws Exception {
        NameValueIteratorImpl impl = new NameValueIteratorImpl(poa, new NameValue[] {
                ODSHelper.createStringNV("name1", "value1"), ODSHelper.createStringNV("name2", "value2"),
                ODSHelper.createStringNV("name3", "value3"), ODSHelper.createStringNV("name4", "value4"),
                ODSHelper.createStringNV("name5", "value5") });
        this.nameValueIterator = NameValueIteratorHelper.narrow(poa.servant_to_reference(impl));
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameValueIteratorImpl#getCount()}.
     */
    @Test
    public void testGetCount() {
        try {
            assertEquals(5, nameValueIterator.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameValueIteratorImpl#nextN(int)}.
     */
    @Test
    public void testNextN() {
        try {
            assertEquals(0, nameValueIterator.nextN(-1).length);
            assertEquals(2, nameValueIterator.nextN(2).length);
            assertEquals(3, nameValueIterator.nextN(20).length);
            assertEquals(0, nameValueIterator.nextN(20).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameValueIteratorImpl#nextOne()}.
     */
    @Test
    public void testNextOne() {
        try {
            assertEquals("name1", nameValueIterator.nextOne().valName);
            assertEquals("name2", nameValueIterator.nextOne().valName);
            assertEquals("name3", nameValueIterator.nextOne().valName);
            assertEquals("name4", nameValueIterator.nextOne().valName);
            assertEquals("name5", nameValueIterator.nextOne().valName);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            nameValueIterator.nextOne();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameValueIteratorImpl#reset()}.
     */
    @Test
    public void testReset() {
        try {
            assertEquals("name1", nameValueIterator.nextOne().valName);
            assertEquals("name2", nameValueIterator.nextOne().valName);
            nameValueIterator.reset();
            assertEquals("name1", nameValueIterator.nextOne().valName);
            assertEquals("name2", nameValueIterator.nextOne().valName);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameValueIteratorImpl#destroy()}.
     */
    @Test
    public void testDestroy() {
        try {
            this.nameValueIterator.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(NameValueIteratorImplTest.class);
    }

}
