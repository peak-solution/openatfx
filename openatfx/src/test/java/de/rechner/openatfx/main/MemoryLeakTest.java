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

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    @Test
    public void testOneHundredThousandSessions() {
        for (int i = 0; i < 10000; i++) {
            try {
                URL url = AoFactoryImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
                AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
                for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
                    InstanceElementIterator iter = ae.getInstances("*");
                    for (InstanceElement ie : iter.nextN(iter.getCount())) {
                        ie.getAsamPath();
                        ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                    }
                    iter.destroy();
                }

                aoSession.close();
            } catch (AoException e) {
                fail(e.reason);
            }
        }
    }
}
