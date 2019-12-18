package de.rechner.openatfx.main;

import static de.rechner.openatfx.util.ODSHelper.asODSLongLong;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ElemId;
import org.asam.ods.T_LONGLONG;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.InstanceElementImplTest;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Test class to test long term memory consumption.
 * 
 * @author Viktor Stoehr
 */
public class MissingUnit2MeaqTest {

    private static ORB orb;
    private static AoSession aoSession;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void readAllValues() {
        try {
            /**
             * 'unt' with id 64 does not have an inverse relation info  for the 'meq' instance with id 64.
             */
            ApplicationElement ae = aoSession.getApplicationStructure().getElementByName("meq");
            ApplicationRelation ar = ae.getRelationsByBaseName("unit")[0];
            T_LONGLONG[] unitIIDs = aoSession.getApplElemAccess().getRelInst(new ElemId(ae.getId(), asODSLongLong(66L)), ar.getRelationName());
            if (unitIIDs == null || unitIIDs.length != 1) {
                fail("failed to query related unit");
            } else if (ODSHelper.asJLong(unitIIDs[0]) != 64) {
                fail("returned unit iid is incorrect");
            }
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(MissingUnit2MeaqTest.class);
    }

}
