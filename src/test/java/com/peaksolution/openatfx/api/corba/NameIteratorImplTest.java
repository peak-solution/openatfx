package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import com.peaksolution.openatfx.GlassfishCorbaExtension;


/**
 * Test case for <code>com.peaksolution.openatfx.NameIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class NameIteratorImplTest {

    private static POA poa;
    private NameIterator nameIterator;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        poa.the_POAManager().activate();
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        poa.destroy(false, false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        NameIteratorImpl impl = new NameIteratorImpl(poa, new String[] { "name1", "name2", "name3", "name4", "name5" });
        this.nameIterator = NameIteratorHelper.narrow(poa.servant_to_reference(impl));
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameIteratorImpl#getCount()}.
     */
    @Test
    void testGetCount() {
        try {
            assertEquals(5, nameIterator.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameIteratorImpl#nextN(int)}.
     */
    @Test
    void testNextN() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameIteratorImpl#nextOne()}.
     */
    @Test
    void testNextOne() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameIteratorImpl#reset()}.
     */
    @Test
    void testReset() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameIteratorImpl#destroy()}.
     */
    @Test
    void testDestroy() {
        try {
            this.nameIterator.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
