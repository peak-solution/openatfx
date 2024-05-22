package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.InstanceElementIteratorImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class InstanceElementIteratorImplTest {

    private static AoSession aoSession;
    private InstanceElementIterator instanceElementIterator;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = EnumerationDefinitionImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement ae = as.getElementByName("meq");
        this.instanceElementIterator = ae.getInstances("*");
    }

    @Test
    public void testGetCount() {
        try {
            assertEquals(16, this.instanceElementIterator.getCount());
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
}
