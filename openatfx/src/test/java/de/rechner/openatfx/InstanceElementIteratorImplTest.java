package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.InstanceElementIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
public class InstanceElementIteratorImplTest {

    private static AoSession aoSession;
    private InstanceElementIterator instanceElementIterator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = EnumerationDefinitionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Before
    public void setUp() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement ae = as.getElementByName("meq");
        this.instanceElementIterator = ae.getInstances("*");
    }

    @Test
    public void testGetCount() {
        try {
            assertEquals(14, this.instanceElementIterator.getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testNextOne() {
        try {
            InstanceElement ie = this.instanceElementIterator.nextOne();
            assertEquals("LS.Right Side", ie.getName());

            ie = this.instanceElementIterator.nextOne();
            assertEquals("Time", ie.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testNextN() {
        try {
            InstanceElement[] ies = this.instanceElementIterator.nextN(3);
            assertEquals(3, ies.length);
            assertEquals("LS.Right Side", ies[0].getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testReset() {
        try {
            InstanceElement ie = this.instanceElementIterator.nextOne();
            assertEquals("LS.Right Side", ie.getName());

            this.instanceElementIterator.reset();

            ie = this.instanceElementIterator.nextOne();
            assertEquals("LS.Right Side", ie.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testDestroy() {
        try {
            this.instanceElementIterator.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            this.instanceElementIterator.getCount();
            fail("Throwable expected");
        } catch (Throwable e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(InstanceElementIteratorImplTest.class);
    }

}
