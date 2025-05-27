package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIterator;
import org.asam.ods.NameValueIteratorHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.NameValueIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class NameValueIteratorImplTest {

    private static POA poa;
    private NameValueIterator nameValueIterator;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        poa.the_POAManager().activate();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        poa.destroy(false, false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        NameValueIteratorImpl impl = new NameValueIteratorImpl(poa, new NameValue[] {
                ODSHelper.createStringNV("name1", "value1"), ODSHelper.createStringNV("name2", "value2"),
                ODSHelper.createStringNV("name3", "value3"), ODSHelper.createStringNV("name4", "value4"),
                ODSHelper.createStringNV("name5", "value5") });
        this.nameValueIterator = NameValueIteratorHelper.narrow(poa.servant_to_reference(impl));
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameValueIteratorImpl#getCount()}.
     */
    @Test
    void testGetCount() {
        try {
            assertEquals(5, nameValueIterator.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameValueIteratorImpl#nextN(int)}.
     */
    @Test
    void testNextN() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameValueIteratorImpl#nextOne()}.
     */
    @Test
    void testNextOne() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameValueIteratorImpl#reset()}.
     */
    @Test
    void testReset() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.NameValueIteratorImpl#destroy()}.
     */
    @Test
    void testDestroy() {
        try {
            this.nameValueIterator.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
