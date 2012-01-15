package de.rechner.openatfx.main;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
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

    @Test
    public void readAllValues() {
        try {
            for (ApplicationElement ae : aoSession.getApplicationStructure().getElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (InstanceElement ie : iter.nextN(iter.getCount())) {
                    List<String> valNames = new ArrayList<String>();
                    valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
                    valNames.remove("values");
                    ie.getValueSeq(valNames.toArray(new String[0]));
                }
            }
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void readAllValuesTree() {
        try {
            for (ApplicationElement ae : aoSession.getApplicationStructure().getTopLevelElements("*")) {
                InstanceElementIterator iter = ae.getInstances("*");
                for (InstanceElement ie : iter.nextN(iter.getCount())) {
                    readInstanceWithChildren(ie);
                }
            }
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    private void readInstanceWithChildren(InstanceElement ie) throws AoException {
        List<String> valNames = new ArrayList<String>();
        valNames.addAll(Arrays.asList(ie.listAttributes("*", AttrType.ALL)));
        valNames.remove("values");
        ie.getValueSeq(valNames.toArray(new String[0]));
        InstanceElementIterator iter = ie.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
        for (InstanceElement ieChild : iter.nextN(iter.getCount())) {
            readInstanceWithChildren(ieChild);
        }
    }

}
