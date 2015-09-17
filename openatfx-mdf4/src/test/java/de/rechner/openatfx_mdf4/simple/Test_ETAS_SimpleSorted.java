package de.rechner.openatfx_mdf4.simple;

import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx_mdf4.MDF4Reader;


/**
 * Test case for reading the example MDF4-file <code>ETAS_SimpleSorted.mf4</code>.
 * 
 * @author Christian Rechner
 */
public class Test_ETAS_SimpleSorted {

    private static final String mdfFile = "de/rechner/openatfx_mdf4/simple/ETAS_SimpleSorted.mf4";

    private static ORB orb;
    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
        MDF4Reader reader = new MDF4Reader();
        aoSession = reader.getAoSessionForMDF4(orb, path);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
        if (orb != null) {
            orb.destroy();
        }
    }

    @Test
    public void testReadIDBlock() {
        // try {
        // } catch (AoException e) {
        // fail(e.reason);
        // }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(Test_ETAS_SimpleSorted.class);
    }

}
