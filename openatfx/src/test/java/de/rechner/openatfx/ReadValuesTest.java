package de.rechner.openatfx;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test cases for reading external component values.
 * 
 * @author Christian Rechner
 */
public class ReadValuesTest {

    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ReadValuesTest.class.getResource("/de/rechner/openatfx/external_with_flags.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testReadValues() {
        try {
            ApplicationElement ae = aoSession.getApplicationStructure().getElementsByBaseType("AoLocalColumn")[0];
//            for (int x = 0; x < 1000; x++) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ieLc = iter.nextOne();
                    ieLc.getValueByBaseName("values");
                }
//            }
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ReadValuesTest.class);
    }

}
