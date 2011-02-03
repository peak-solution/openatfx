package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ElemId;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.ApplElemAccessImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplElemAccessImplTest {

    private static AoSession aoSession;
    private static ApplElemAccess applElemAccess;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
        applElemAccess = aoSession.getApplElemAccess();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Test
    public void testInsertInstances() {
        try {
            // 2 instances of dsk without relations
            T_LONGLONG aid = aoSession.getApplicationStructure().getElementByName("dsk").getId();
            AIDNameValueSeqUnitId[] aidSeq = new AIDNameValueSeqUnitId[2];
            aidSeq[0] = new AIDNameValueSeqUnitId();
            aidSeq[0].attr = new AIDName(aid, "iname");
            aidSeq[0].values = new TS_ValueSeq();
            aidSeq[0].values.flag = new short[] { 15, 15 };
            aidSeq[0].values.u = new TS_UnionSeq();
            aidSeq[0].values.u.stringVal(new String[] { "name1", "name2" });
            aidSeq[1] = new AIDNameValueSeqUnitId();
            aidSeq[1].attr = new AIDName(aid, "created");
            aidSeq[1].values = new TS_ValueSeq();
            aidSeq[1].values.flag = new short[] { 15, 15 };
            aidSeq[1].values.u = new TS_UnionSeq();
            aidSeq[1].values.u.dateVal(new String[] { "20100101", "20111111" });
            ElemId[] elemIds = applElemAccess.insertInstances(aidSeq);
            assertEquals(2, elemIds.length);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testUpdateInstances() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeleteInstances() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetRelInst() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetRelInst() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInstances() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInstancesExt() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetValueMatrixInMode() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetValueMatrix() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetAttributeRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetElementRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetInstanceRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAttributeRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetElementRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInstanceRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetElementInitialRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetInstanceInitialRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetInitialRightReference() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInitialRightReference() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetElementInitialRights() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInstanceInitialRights() {
        fail("Not yet implemented");
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplElemAccessImplTest.class);
    }

}
