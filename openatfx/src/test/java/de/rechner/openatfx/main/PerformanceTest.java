package de.rechner.openatfx.main;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.InstanceElementImplTest;


/**
 * Test class to test long term memory consumption.
 * 
 * @author Christian Rechner
 */
public class PerformanceTest {

    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    // @Test
    // public void readAllValues() {
    // try {
    // for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
    // InstanceElementIterator iter = ae.getInstances("*");
    // for (InstanceElement ie : iter.nextN(iter.getCount())) {
    // List<String> valNames = new ArrayList<String>();
    // valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
    // ie.getValueSeq(valNames.toArray(new String[0]));
    // }
    // iter.destroy();
    // }
    // } catch (AoException aoe) {
    // fail(aoe.reason);
    // }
    // }

}
