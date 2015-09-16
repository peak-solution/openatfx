package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Column;
import org.asam.ods.InstanceElement;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrix;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test class for reading flags of measurement values.
 * 
 * @author Christian Rechner
 */
public class ValueMatrixReadFlagsTest {

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
    public void readFlagsFromGlobalFlag() {
        try {
            // read flags
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeSm = as.getElementsByBaseType("AoSubMatrix")[0];
            InstanceElement ieSM = aeSm.getInstanceByName("71.iad");
            SubMatrix sm = ieSM.upcastSubMatrix();
            ValueMatrix vm = sm.getValueMatrix();
            Column col = vm.getColumns("51900778_1.X")[0];
            TS_ValueSeq valueSeq = vm.getValueVector(col, 0, 0);

            // check length
            assertEquals(300004, valueSeq.flag.length);

            // check first flags
            assertEquals((short) 15, valueSeq.flag[0]);
            assertEquals((short) 15, valueSeq.flag[1]);
            assertEquals((short) 15, valueSeq.flag[2]);
            assertEquals((short) 15, valueSeq.flag[3]);

            // check last flags
            assertEquals((short) 15, valueSeq.flag[300000]);
            assertEquals((short) 15, valueSeq.flag[300001]);
            assertEquals((short) 15, valueSeq.flag[300002]);
            assertEquals((short) 15, valueSeq.flag[300003]);

        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readFlagsFromComponentFile() {
        try {
            // read flags
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeSm = as.getElementsByBaseType("AoSubMatrix")[0];
            InstanceElement ieSM = aeSm.getInstanceByName("71.iad");
            SubMatrix sm = ieSM.upcastSubMatrix();
            ValueMatrix vm = sm.getValueMatrix();
            Column col = vm.getColumns("51900778_1")[0];
            TS_ValueSeq valueSeq = vm.getValueVector(col, 0, 0);

            // check length
            assertEquals(300004, valueSeq.flag.length);

            // check first flags
            assertEquals((short) 15, valueSeq.flag[0]);
            assertEquals((short) 15, valueSeq.flag[1]);
            assertEquals((short) 0, valueSeq.flag[2]);
            assertEquals((short) 15, valueSeq.flag[3]);

            // check last flags
            assertEquals((short) 0, valueSeq.flag[300000]);
            assertEquals((short) 15, valueSeq.flag[300001]);
            assertEquals((short) 15, valueSeq.flag[300002]);
            assertEquals((short) 0, valueSeq.flag[300003]);

        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    // @Test
    // public void readFlagsFromInline() {
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

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ValueMatrixReadFlagsTest.class);
    }

}
