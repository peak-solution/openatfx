package de.rechner.openatfx.io;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rechner.openatfx.IFileHandler;
import de.rechner.openatfx.LocalFileHandler;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxReaderTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxReaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(AtfxReaderTest.class);

    private static ORB orb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    public void testCreateSessionForATFX() {
        try {
            URL url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/example.atfx");
            AtfxReader reader = AtfxReader.getInstance();
            IFileHandler fileHandler = new LocalFileHandler();
            String path = new File(url.getFile()).getAbsolutePath();
            AoSession aoSession = reader.createSessionForATFX(orb, fileHandler, path);
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }

}
