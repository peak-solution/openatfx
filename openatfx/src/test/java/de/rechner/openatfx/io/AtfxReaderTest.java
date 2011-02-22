package de.rechner.openatfx.io;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxReaderTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxReaderTest {

    private static final Log LOG = LogFactory.getLog(AtfxReaderTest.class);

    private static ORB orb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    public void testCreateSessionForATFX() {
        try {
            URL url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
            AtfxReader reader = AtfxReader.getInstance();
            reader.createSessionForATFX(orb, new File(url.getFile()));
            reader.createSessionForATFX(orb, new File(url.getFile()));
            reader.createSessionForATFX(orb, new File(url.getFile()));

            url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/mdm.xml");
            reader.createSessionForATFX(orb, new File(url.getFile()));

            // url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/header.xml");
            // reader.createSessionForATFX(orb, new File(url.getFile()));
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }
}
