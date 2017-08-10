package de.rechner.openatfx.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import junit.framework.JUnit4TestAdapter;


/**
 * Test class to check the official ASAM examples.
 * 
 * @author Christian Rechner
 */
public class ASAMExamplesTest {

    private static AoFactory aoFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    @Test
    public void readExampleAllTypes() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_AllTypes.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(14, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(30, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readExampleBus() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_Bus.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(18, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(48, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readExampleBusWithIndex() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_BusWithIndex.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(18, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(48, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readExampleGeometry() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_Geometry.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(21, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(82, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readExampleSimple() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_Simple.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(14, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(30, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readExampleWorkflow() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/de/rechner/openatfx/asam600/Example_Workflow.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));

            assertEquals(20, aoSession.getApplicationStructureValue().applElems.length);
            assertEquals(62, aoSession.getApplicationStructureValue().applRels.length);

            // read all instance values including AoLocalColumn.values
            ApplicationStructure as = aoSession.getApplicationStructure();
            for (ApplicationElement ae : as.getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ie.getValueSeq(ie.listAttributes("*", AttrType.ALL));
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ASAMExamplesTest.class);
    }

}
