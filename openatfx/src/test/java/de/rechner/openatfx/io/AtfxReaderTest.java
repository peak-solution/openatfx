package de.rechner.openatfx.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @Test
    public void testCreateSessionForATFX() {
        try {
            URL url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/example.atfx");
            AtfxReader reader = new AtfxReader();
            IFileHandler fileHandler = new LocalFileHandler();
            String path = new File(url.getFile()).getAbsolutePath();
            AoSession aoSession = reader.createSessionForATFX(orb, fileHandler, path);
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }

    @Test
    public void testReadInstanceAttributesWithUnits() {
        try {
            URL url = AtfxReaderTest.class.getResource("/de/rechner/openatfx/example.atfx");
            AtfxReader reader = new AtfxReader();
            IFileHandler fileHandler = new LocalFileHandler();
            String path = new File(url.getFile()).getAbsolutePath();
            AoSession aoSession = reader.createSessionForATFX(orb, fileHandler, path);
            
            ApplicationElement projectElement = aoSession.getApplicationStructure().getElementByName("prj");
            InstanceElement projectInstance = projectElement.getInstanceByName("no_project");
            NameValueUnit[] nvus = projectInstance.getValueSeq(new String[] {"inst_attr_dt_float", "inst_attr_dt_double", "inst_attr_dt_long"});
            for (int i = 0; i < nvus.length; i++) {
                NameValueUnit value = nvus[i];
                switch(i) {
                    case 0:
                        assertEquals("inst_attr_dt_float", value.valName);
                        // checks the correct case, when the unit iid is defined at the instance attribute in atfx
                        assertEquals("Pa", value.unit);
                        break;
                    case 1:
                        assertEquals("inst_attr_dt_double", value.valName);
                        assertNull(value.unit);
                        break;
                    case 2:
                        assertEquals("inst_attr_dt_long", value.valName);
                        // checks the tolerated but actually incorrect definition of the unit's name for the instance
                        // attribute in atfx
                        assertEquals("s", value.unit);
                        break;
                }
            }
            
            aoSession.close();
        } catch (AoException e) {
            LOG.error(e.reason, e);
            fail(e.reason);
        }
    }
}
