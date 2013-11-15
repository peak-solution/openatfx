package de.rechner.openatfx.io;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.AoSessionImplTest;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxWriterTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxWriterTest {

    private static AoSession aoSession;
    ORB orb;

    @Before
    public void setUpBefore() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @After
    public void tearDown() throws Exception {
        aoSession.close();
    }

    @Test
    public void testWriteXML() {
        try {
            URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
            aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                        .newSession("FILENAME=" + new File(url.getFile()));
            aoSession.startTransaction();
            // aoSession.commitTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testWriteXML2() {
        try {
            URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/test.atfx");
            aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                        .newSession("FILENAME=" + new File(url.getFile()));

            aoSession.startTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
