package de.rechner.openatfx.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

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

    private static final int NO_OF_TESTS = 100000;

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
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
                            ie.getAsamPath();
                            ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    }

}
