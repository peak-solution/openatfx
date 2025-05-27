package com.peaksolution.openatfx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>com.peaksolution.openatfx.AoServiceFactory</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class AoServiceFactoryTest {

    private static ORB orb;

    @BeforeAll
    public static void setUpBeforeClass() {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    void testNewAoFactory() {
        try {
            AoFactory aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
            assertNotNull(aoFactory);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testNewAoSession() {
        try {
            URL url = AoSessionImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
            AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, new File(url.getFile()));
            assertNotNull(aoSession);
            aoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testNewEmptyAoSession() {
        try {
            AoSession aoSession = AoServiceFactory.getInstance()
                                                  .newEmptyAoSession(orb, File.createTempFile("tmp", "atfx"), "asam30");
            assertNotNull(aoSession);
            aoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
