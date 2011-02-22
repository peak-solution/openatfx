package de.rechner.openatfx.io;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testWriteXML() {
        try {
            aoSession.startTransaction();
//            aoSession.commitTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
