package de.rechner.openatfx.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoFactoryImplTest;
import de.rechner.openatfx.AoServiceFactory;


/**
 * Test class to test long term memory consumption.
 * 
 * @author Christian Rechner
 */
public class MemoryLeakTest {

    private static final int NO_OF_TESTS = 0;

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    @Test
    public void testFatSessions() {
        File file = new File("D:/PUBLIC/tmp/20120105_IBN/au9147_rse_is4_inbetriebnahmetest_201112_e10.erg.atfx");
        try {
            AoSession s = aoFactory.newSession("FILENAME=" + file.getAbsolutePath());
            ApplicationElement ae = s.getApplicationStructure().getElementsByBaseType("AoLocalColumn")[0];
            InstanceElementIterator iter = ae.getInstances("*");
            for (InstanceElement ie : iter.nextN(iter.getCount())) {

            }

            s.close();
        } catch (AoException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testOneHundredThousandSessions() {
        for (int i = 0; i < NO_OF_TESTS; i++) {
            AoSession[] sessions = new AoSession[5];

            // open sessions
            for (int x = 0; x < 5; x++) {
                try {
                    URL url = AoFactoryImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
                    sessions[x] = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
                    for (ApplicationElement ae : sessions[x].getApplicationStructure().getElements("*")) {
                        InstanceElementIterator iter = ae.getInstances("*");
                        for (InstanceElement ie : iter.nextN(iter.getCount())) {
                            List<String> valNames = new ArrayList<String>();
                            valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
                            valNames.remove("values");
                            ie.getValueSeq(valNames.toArray(new String[0]));
                            ie.getAsamPath();
                        }
                        iter.destroy();
                    }
                } catch (AoException e) {
                    fail(e.reason);
                }
            }

            // close sessions
            for (int x = 0; x < 5; x++) {
                try {
                    sessions[x].close();
                } catch (AoException e) {
                    fail(e.reason);
                }
            }

            // wait
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

}
