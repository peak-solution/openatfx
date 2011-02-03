package de.rechner.openatfx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.AoServiceFactory</code>.
 * 
 * @author Christian Rechner
 */
public class AoServiceFactoryTest {

    private static ORB orb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    public void testNewAoFactory() {
        try {
            AoFactory aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
            assertNotNull(aoFactory);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testNewAoSession() {
        try {
            URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
            AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, new File(url.getFile()));
            assertNotNull(aoSession);
            aoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testNewEmptyAoSession() {
        try {
            AoSession aoSession = AoServiceFactory.getInstance().newEmptyAoSession(orb,
                                                                                   File.createTempFile("tmp", "atfx"),
                                                                                   "asam30");
            assertNotNull(aoSession);
            aoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(AoServiceFactoryTest.class);
    }

}
