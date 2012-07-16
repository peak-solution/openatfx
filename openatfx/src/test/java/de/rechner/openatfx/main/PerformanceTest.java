package de.rechner.openatfx.main;

import java.net.URL;

import org.asam.ods.AoSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.omg.CORBA.ORB;

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
        // aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
        // .newSession("FILENAME=" + new File("D:/PUBLIC/large.atfx"));

        // aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new
        // File(url.getFile()));
        // System.gc();
        // Print used memory
        // int mb = 1024 * 1024;
        // Runtime runtime = Runtime.getRuntime();
        // System.out.println("##### Heap utilization statistics [MB] #####"); // Print used memory
        // System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb); // Print free
        // memory
        // System.out.println("Free Memory:" + runtime.freeMemory() / mb); // Print total available memory
        // System.out.println("Total Memory:" + runtime.totalMemory() / mb); // Print Maximum available memory
        // System.out.println("Max Memory:" + runtime.maxMemory() / mb);
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
    // valNames.remove("values");
    // ie.getValueSeq(valNames.toArray(new String[0]));
    // }
    // }
    // } catch (AoException aoe) {
    // fail(aoe.reason);
    // }
    // }
    //
    // @Test
    // public void readAllValuesTree() {
    // try {
    // for (ApplicationElement ae : aoSession.getApplicationStructure().getTopLevelElements("*")) {
    // InstanceElementIterator iter = ae.getInstances("*");
    // for (InstanceElement ie : iter.nextN(iter.getCount())) {
    // readInstanceWithChildren(ie);
    // }
    // }
    // } catch (AoException aoe) {
    // fail(aoe.reason);
    // }
    // }
    //
    // private void readInstanceWithChildren(InstanceElement ie) throws AoException {
    // List<String> valNames = new ArrayList<String>();
    // valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
    // valNames.remove("values");
    // ie.getValueSeq(valNames.toArray(new String[0]));
    // InstanceElementIterator iter = ie.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
    // for (InstanceElement ieChild : iter.nextN(iter.getCount())) {
    // readInstanceWithChildren(ieChild);
    // }
    // }

}
