package com.peaksolution.openatfx.main;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.asam.ods.Column;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.ValueMatrix;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;


/**
 * Test class to check the official ASAM examples.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class ASAMExamplesTest {

    private static AoFactory aoFactory;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
    }

    @Test
    void readExampleAllTypes() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_AllTypes.atfx");
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
    void readExampleBus() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_Bus.atfx");
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

            { // read all valuematrix values.
                ApplicationElement ae = as.getElementsByBaseType("AoSubmatrix")[0];
                InstanceElementIterator iter = ae.getInstances("*");
                for (int i = 0; i < iter.getCount(); i++) {
                    InstanceElement ie = iter.nextOne();
                    ValueMatrix vm = ie.upcastSubMatrix().getValueMatrix();
                    for (Column column : vm.getColumns("*")) {
                        vm.getValueVector(column, 0, 0);
                    }
                }
            }

            aoSession.close();
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    void readExampleBusWithIndex() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_BusWithIndex.atfx");
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
    void readExampleGeometry() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_Geometry.atfx");
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
    void readExampleSimple() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_Simple.atfx");
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
    void readExampleWorkflow() {
        try {
            URL url = ASAMExamplesTest.class.getResource("/com/peaksolution/openatfx/asam600/Example_Workflow.atfx");
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
}
