package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.AoFactoryImpl</code>.
 * 
 * @author Christian Rechner
 */
public class AoFactoryImplTest {

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoFactoryImpl#getInterfaceVersion()}.
     */
    @Test
    public void testGetInterfaceVersion() {
        try {
            assertEquals("V5.3.0", aoFactory.getInterfaceVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoFactoryImpl#getType()}.
     */
    @Test
    public void testGetType() {
        try {
            assertEquals("XATF-ASCII", aoFactory.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoFactoryImpl#getName()}.
     */
    @Test
    public void testGetName() {
        try {
            assertEquals("XATF-ASCII", aoFactory.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoFactoryImpl#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        try {
            assertEquals("ATFX file driver for ASAM OO-API", aoFactory.getDescription());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.AoFactoryImpl#newSession(java.lang.String)}.
     */
    @Test
    public void testNewSession() {
        try {
            URL url = AoFactoryImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
            assertEquals("asam29", aoSession.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(AoFactoryImplTest.class);
    }

}
