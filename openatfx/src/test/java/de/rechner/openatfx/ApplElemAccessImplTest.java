package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AIDNameValueUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ElemId;
import org.asam.ods.ElemResultSet;
import org.asam.ods.JoinDef;
import org.asam.ods.JoinType;
import org.asam.ods.QueryStructure;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelOperator;
import org.asam.ods.SelOrder;
import org.asam.ods.SelValue;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;


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
        try {
            // prj->tstser
            assertEquals(1, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(10),
                                                                 ODSHelper.asODSLongLong(1)), "tstser_iid").length);
            // tstser->mea
            assertEquals(1, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(11),
                                                                 ODSHelper.asODSLongLong(2)), "mea_iid").length);
            // mea->dts
            assertEquals(3, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(17),
                                                                 ODSHelper.asODSLongLong(22)), "dts_iid").length);
            // mea->setup_iid
            assertEquals(1, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(17),
                                                                 ODSHelper.asODSLongLong(22)), "setup_iid").length);
            // mea->dsk_iid
            assertEquals(0, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(17),
                                                                 ODSHelper.asODSLongLong(22)), "dsk_iid").length);
            // mea->audifahrzeug_iid
            assertEquals(1,
                         applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(17), ODSHelper.asODSLongLong(22)),
                                                   "audifahrzeug_iid").length);
            // mea->audifm_iid
            assertEquals(1, applElemAccess.getRelInst(new ElemId(ODSHelper.asODSLongLong(17),
                                                                 ODSHelper.asODSLongLong(22)), "audifm_iid").length);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetRelInst() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetInstances() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            // Create the query structure for the method getInstances
            QueryStructure aoq;
            // The application attribute.
            // ApplicationAttribute aaObj;
            // Result of the request
            ElemResultSet elemRes[];
            // Query on the application element AoTest
            ApplicationElement ae[] = as.getElementsByBaseType("AoMeasurementQuantity");
            if (ae.length > 0) {
                ApplicationElement aeObj = ae[0];
                aoq = new QueryStructure();
                ApplicationAttribute aaObj = aeObj.getAttributeByBaseName("id");
                aoq.anuSeq = new AIDNameUnitId[2];
                aoq.anuSeq[0] = new AIDNameUnitId();
                aoq.anuSeq[0].attr = new AIDName();
                // set Id of element
                aoq.anuSeq[0].attr.aid = aeObj.getId();
                // Set Name of attribute
                aoq.anuSeq[0].attr.aaName = aaObj.getName();
                // Build the query, two select values.
                aoq.condSeq = new SelValue[2];
                // First select on the Id
                aaObj = aeObj.getAttributeByBaseName("id");
                aoq.condSeq[0] = new SelValue();
                aoq.condSeq[0].attr = new AIDNameValueUnitId();
                aoq.condSeq[0].attr.attr = new AIDName();
                aoq.condSeq[0].attr.attr.aid = aeObj.getId();
                aoq.condSeq[0].attr.attr.aaName = aaObj.getName();
                aoq.condSeq[0].value = new TS_Value();
                aoq.condSeq[0].value.u = new TS_Union();
                T_LONGLONG iid = new T_LONGLONG();
                iid.high = 0;
                iid.low = 10;
                aoq.condSeq[0].value.u.longlongVal(iid);
                aoq.condSeq[0].oper = SelOpcode.LT;
                // Second select on the Name
                aaObj = aeObj.getAttributeByBaseName("name");
                aoq.anuSeq[1] = new AIDNameUnitId();
                aoq.anuSeq[1].attr = new AIDName();
                // set Id of element
                aoq.anuSeq[1].attr.aid = aeObj.getId();
                // Set name of attribute
                aoq.anuSeq[1].attr.aaName = aaObj.getName();
                // Build the query.
                aoq.condSeq[1] = new SelValue();
                aoq.condSeq[1].attr = new AIDNameValueUnitId();
                aoq.condSeq[1].attr.attr = new AIDName();
                aoq.condSeq[1].attr.attr.aid = aeObj.getId();
                aoq.condSeq[1].attr.attr.aaName = aaObj.getName();
                aoq.condSeq[1].value = new TS_Value();
                aoq.condSeq[1].value.u = new TS_Union();
                aoq.condSeq[1].value.u.stringVal("ZYK*");
                aoq.condSeq[1].oper = SelOpcode.EQ;
                // Set the operator.
                aoq.operSeq = new SelOperator[1];
                aoq.operSeq[0] = SelOperator.OR;
                // Set the reference selection
                ae = as.getElementsByBaseType("AoSubTest");
                ApplicationElement aeSubObj = null;
                if (ae.length > 0) {
                    aeSubObj = ae[0];
                    aoq.relInst = new ElemId();
                    aoq.relInst.aid = aeSubObj.getId();
                    aoq.relInst.iid = new T_LONGLONG();
                    aoq.relInst.iid.low = 1;
                }
                elemRes = applElemAccess.getInstances(aoq, 100);
            }
        } catch (AoException aoe) {
            fail(aoe.reason);
        }
    }

    @Test
    public void testGetInstancesExt() {
        try {
            // 1: empty query
            QueryStructureExt qse = new QueryStructureExt();
            qse.anuSeq = new SelAIDNameUnitId[0];
            qse.joinSeq = new JoinDef[0];
            qse.condSeq = new SelItem[0];
            qse.groupBy = new AIDName[0];
            qse.orderBy = new SelOrder[0];
            ResultSetExt[] resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(0, resSetExt[0].firstElems.length);

            // 2. only 'SELECT' with same application element
            qse.anuSeq = new SelAIDNameUnitId[2];
            qse.anuSeq[0] = new SelAIDNameUnitId();
            qse.anuSeq[0].attr = new AIDName(ODSHelper.asODSLongLong(21), "iname"); // meq
            qse.anuSeq[0].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[0].aggregate = AggrFunc.NONE;
            qse.anuSeq[1] = new SelAIDNameUnitId();
            qse.anuSeq[1].attr = new AIDName(ODSHelper.asODSLongLong(21), "aodt"); // meq
            qse.anuSeq[1].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[1].aggregate = AggrFunc.NONE;
            resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length); // no of aes
            assertEquals(21, ODSHelper.asJLong(resSetExt[0].firstElems[0].aid)); // aid
            assertEquals(2, resSetExt[0].firstElems[0].values.length); // no of attrs
            assertEquals(14, resSetExt[0].firstElems[0].values[0].value.flag.length); // no of rows
            assertEquals("LS.Right Side", resSetExt[0].firstElems[0].values[0].value.u.stringVal()[0]); // a value

            // 3. add a join to parent and to children with default join
            qse.joinSeq = new JoinDef[3];
            qse.joinSeq[0] = new JoinDef();
            qse.joinSeq[0].fromAID = ODSHelper.asODSLongLong(21); // dts
            qse.joinSeq[0].toAID = ODSHelper.asODSLongLong(19); // meq
            qse.joinSeq[0].refName = "dts_iid";
            qse.joinSeq[0].joiningType = JoinType.JTDEFAULT;
            qse.joinSeq[1] = new JoinDef();
            qse.joinSeq[1].fromAID = ODSHelper.asODSLongLong(21); // meq
            qse.joinSeq[1].toAID = ODSHelper.asODSLongLong(8); // unt
            qse.joinSeq[1].refName = "unt_iid";
            qse.joinSeq[1].joiningType = JoinType.JTDEFAULT;
            qse.joinSeq[2] = new JoinDef();
            qse.joinSeq[2].fromAID = ODSHelper.asODSLongLong(21); // meq
            qse.joinSeq[2].toAID = ODSHelper.asODSLongLong(13); // device
            qse.joinSeq[2].refName = "device_iid";
            qse.joinSeq[2].joiningType = JoinType.JTDEFAULT;
            resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length); // no of aes

        } catch (AoException e) {
            fail(e.reason);
        }
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
