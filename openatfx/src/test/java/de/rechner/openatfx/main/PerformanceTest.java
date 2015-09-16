package de.rechner.openatfx.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.InstanceElementImplTest;


/**
 * Test class to test long term memory consumption.
 * 
 * @author Christian Rechner
 */
public class PerformanceTest {

    private static ORB orb;
    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/external_with_flags.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void readAllValues() {
        try {
            for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (InstanceElement ie : iter.nextN(iter.getCount())) {
                    List<String> valNames = new ArrayList<String>();
                    valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
                    ie.getValueSeq(valNames.toArray(new String[0]));
                }
                iter.destroy();
            }
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    // @Test
    // public void readAllValues() {
    // try {
    // File dir = new File("C:/Users/FGAAW8T/Desktop/dala_export");
    // for (File file : dir.listFiles()) {
    // if (!file.getName().endsWith("atfx")) {
    // continue;
    // }
    //
    // AoSession session = AoServiceFactory.getInstance().newAoSession(orb, file);
    // for (ApplicationElement ae : session.getApplicationStructure().getElements("*")) {
    // InstanceElementIterator iter = ae.getInstances("*");
    // for (InstanceElement ie : iter.nextN(iter.getCount())) {
    // List<String> valNames = new ArrayList<String>();
    // valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
    // ie.getValueSeq(valNames.toArray(new String[0]));
    // }
    // iter.destroy();
    // }
    // session.close();
    // }
    // } catch (AoException aoe) {
    // fail(aoe.reason);
    // }
    // }
    //

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(PerformanceTest.class);
    }

}
