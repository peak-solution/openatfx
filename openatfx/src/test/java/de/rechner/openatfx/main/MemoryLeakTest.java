package de.rechner.openatfx.main;

import static org.junit.Assert.fail;

import java.io.File;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
import org.junit.BeforeClass;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;


/**
 * Test class to test long term memory consumption.
 * 
 * @author Christian Rechner
 */
public class MemoryLeakTest {

    private static final int NO_OF_TESTS = 100;

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    // @Test
    public void testMemoryConsumption() throws Exception {
        File file = new File("D:/PUBLIC/test.atfx");

        System.out.println("Used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024)
                + " kB");
        System.out.println(java.lang.Thread.activeCount());
        AoSession aoSession = aoFactory.newSession("FILENAME=" + file.getAbsolutePath());

        System.out.println("Used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024)
                + " kB");
        System.out.println(java.lang.Thread.activeCount());

        aoSession.close();

        System.gc();

        Thread.sleep(500);

        System.out.println("Used: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024)
                + " kB");
    }

    // @Test
    public void testOneHundredThousandSessions() {
        for (int i = 0; i < NO_OF_TESTS; i++) {
            AoSession[] sessions = new AoSession[5];

            // open sessions
            for (int x = 0; x < 1; x++) {
                try {
                    File file = new File("D:/PUBLIC/test.atfx");

                    sessions[x] = aoFactory.newSession("FILENAME=" + file.getAbsolutePath());

                    long start = System.currentTimeMillis();
                    for (ApplicationElement ae : sessions[x].getApplicationStructure().getElements("*")) {
                        InstanceElementIterator iter = ae.getInstances("*");
                        for (InstanceElement ie : iter.nextN(iter.getCount())) {
                            iterateChilds(ie);
                        }
                        iter.destroy();
                    }
                    System.out.println("Iterated " + (System.currentTimeMillis() - start));

                } catch (AoException e) {
                    fail(e.reason);
                }
            }

            // close sessions
            for (int x = 0; x < 1; x++) {
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

    private void iterateChilds(InstanceElement ie) throws AoException {
        InstanceElementIterator iter = ie.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
        for (int i = 0; i < iter.getCount(); i++) {
            iterateChilds(iter.nextOne());
        }
        iter.destroy();
    }

}
