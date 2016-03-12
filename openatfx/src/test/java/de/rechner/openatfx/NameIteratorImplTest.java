package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;


/**
 * Test case for <code>de.rechner.openatfx.NameIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
public class NameIteratorImplTest {

    private static POA poa;
    private NameIterator nameIterator;

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
        NameIteratorImpl impl = new NameIteratorImpl(poa, new String[] { "name1", "name2", "name3", "name4", "name5" });
        this.nameIterator = NameIteratorHelper.narrow(poa.servant_to_reference(impl));
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameIteratorImpl#getCount()}.
     */
    @Test
    public void testGetCount() {
        try {
            assertEquals(5, nameIterator.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameIteratorImpl#nextN(int)}.
     */
    @Test
    public void testNextN() {
        try {
            assertEquals(0, nameIterator.nextN(-1).length);
            assertEquals(2, nameIterator.nextN(2).length);
            assertEquals(3, nameIterator.nextN(20).length);
            assertEquals(0, nameIterator.nextN(20).length);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameIteratorImpl#nextOne()}.
     */
    @Test
    public void testNextOne() {
        try {
            assertEquals("name1", nameIterator.nextOne());
            assertEquals("name2", nameIterator.nextOne());
            assertEquals("name3", nameIterator.nextOne());
            assertEquals("name4", nameIterator.nextOne());
            assertEquals("name5", nameIterator.nextOne());
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            nameIterator.nextOne();
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameIteratorImpl#reset()}.
     */
    @Test
    public void testReset() {
        try {
            assertEquals("name1", nameIterator.nextOne());
            assertEquals("name2", nameIterator.nextOne());
            nameIterator.reset();
            assertEquals("name1", nameIterator.nextOne());
            assertEquals("name2", nameIterator.nextOne());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.NameIteratorImpl#destroy()}.
     */
    @Test
    public void testDestroy() {
        try {
            this.nameIterator.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(NameIteratorImplTest.class);
    }

}
