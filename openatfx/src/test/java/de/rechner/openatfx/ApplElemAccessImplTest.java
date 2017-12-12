package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameUnitId;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.JoinDef;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.ResultSetExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOpcode;
import org.asam.ods.SelOrder;
import org.asam.ods.SelValueExt;
import org.asam.ods.SetType;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import junit.framework.JUnit4TestAdapter;


/**
 * Test case for <code>de.rechner.openatfx.ApplElemAccessImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ApplElemAccessImplTest {

    private static AoSession aoSession;
    private static ApplElemAccess applElemAccess;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplElemAccessImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
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
        try {
            applElemAccess.updateInstances(null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testDeleteInstances() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeUnt = as.getElementByName("unt");

            T_LONGLONG aidUnt = aeUnt.getId();
            T_LONGLONG[] iids = new T_LONGLONG[] { ODSHelper.asODSLongLong(36), ODSHelper.asODSLongLong(42) };
            applElemAccess.deleteInstances(aidUnt, iids);

            assertEquals(5, aeUnt.listInstances("*").getCount());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetRelInst() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            T_LONGLONG aidPrj = as.getElementByName("prj").getId();
            T_LONGLONG aidTstser = as.getElementByName("tstser").getId();
            T_LONGLONG aidMea = as.getElementByName("mea").getId();

            // prj->tstser
            assertEquals(2, applElemAccess.getRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)),
                                                      "tstser_iid").length);
            // tstser->mea
            assertEquals(1, applElemAccess.getRelInst(new ElemId(aidTstser, ODSHelper.asODSLongLong(2)),
                                                      "mea_iid").length);
            // mea->dts
            assertEquals(3,
                         applElemAccess.getRelInst(new ElemId(aidMea, ODSHelper.asODSLongLong(22)), "dts_iid").length);
            // mea->setup_iid
            assertEquals(1, applElemAccess.getRelInst(new ElemId(aidMea, ODSHelper.asODSLongLong(22)),
                                                      "setup_iid").length);
            // mea->dsk_iid
            assertEquals(0,
                         applElemAccess.getRelInst(new ElemId(aidMea, ODSHelper.asODSLongLong(22)), "dsk_iid").length);
            // mea->audifahrzeug_iid
            assertEquals(1, applElemAccess.getRelInst(new ElemId(aidMea, ODSHelper.asODSLongLong(22)),
                                                      "audifahrzeug_iid").length);
            // mea->audifm_iid
            assertEquals(1, applElemAccess.getRelInst(new ElemId(aidMea, ODSHelper.asODSLongLong(22)),
                                                      "audifm_iid").length);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testSetRelInst() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            T_LONGLONG aidPrj = as.getElementByName("prj").getId();

            // prj->tstser
            assertEquals(2, applElemAccess.getRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)),
                                                      "tstser_iid").length);
            // remove
            applElemAccess.setRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)), "tstser_iid",
                                      new T_LONGLONG[] { ODSHelper.asODSLongLong(1) }, SetType.REMOVE);
            assertEquals(1, applElemAccess.getRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)),
                                                      "tstser_iid").length);

            // add again
            applElemAccess.setRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)), "tstser_iid",
                                      new T_LONGLONG[] { ODSHelper.asODSLongLong(1) }, SetType.INSERT);
            assertEquals(2, applElemAccess.getRelInst(new ElemId(aidPrj, ODSHelper.asODSLongLong(1)),
                                                      "tstser_iid").length);

        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInstances() {
        try {
            applElemAccess.getInstances(null, 0);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_BAD_PARAMETER, e.errCode);
        }
    }

    @Test
    public void testGetInstancesExt() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            T_LONGLONG aidMeq = as.getElementByName("meq").getId();
            // T_LONGLONG aidDts = as.getElementByName("dts").getId();
            // T_LONGLONG aidUnt = as.getElementByName("unt").getId();
            // T_LONGLONG aidDevice = as.getElementByName("device").getId();

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
            qse.anuSeq[0].attr = new AIDName(aidMeq, "iname"); // meq
            qse.anuSeq[0].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[0].aggregate = AggrFunc.NONE;
            qse.anuSeq[1] = new SelAIDNameUnitId();
            qse.anuSeq[1].attr = new AIDName(aidMeq, "aodt"); // meq
            qse.anuSeq[1].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[1].aggregate = AggrFunc.NONE;
            resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length); // no of aes
            assertEquals(ODSHelper.asJLong(aidMeq), ODSHelper.asJLong(resSetExt[0].firstElems[0].aid)); // aid
            assertEquals(2, resSetExt[0].firstElems[0].values.length); // no of attrs
            assertEquals(14, resSetExt[0].firstElems[0].values[0].value.flag.length); // no of rows
            assertEquals("LS.Right Side", resSetExt[0].firstElems[0].values[0].value.u.stringVal()[0]); // a value

            // 3. put in a condition
            qse.anuSeq = new SelAIDNameUnitId[2];
            qse.anuSeq[0] = new SelAIDNameUnitId();
            qse.anuSeq[0].attr = new AIDName(aidMeq, "iname"); // meq
            qse.anuSeq[0].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[0].aggregate = AggrFunc.NONE;
            qse.anuSeq[1] = new SelAIDNameUnitId();
            qse.anuSeq[1].attr = new AIDName(aidMeq, "aodt"); // meq
            qse.anuSeq[1].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[1].aggregate = AggrFunc.NONE;
            qse.condSeq = new SelItem[1];
            qse.condSeq[0] = new SelItem();
            SelValueExt selValue = new SelValueExt();
            selValue.attr = new AIDNameUnitId(new AIDName(aidMeq, "iname"), new T_LONGLONG());
            selValue.oper = SelOpcode.CI_LIKE;
            selValue.value = ODSHelper.string2tsValue(DataType.DT_STRING, "LS.*");
            qse.condSeq[0].value(selValue);
            resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length); // no of aes
            assertEquals(ODSHelper.asJLong(aidMeq), ODSHelper.asJLong(resSetExt[0].firstElems[0].aid)); // aid
            assertEquals(2, resSetExt[0].firstElems[0].values.length); // no of attrs
            assertEquals(4, resSetExt[0].firstElems[0].values[0].value.flag.length); // no of rows
            // the value of the aodt attribute
            assertEquals(DataType.DT_FLOAT.value(), resSetExt[0].firstElems[0].values[1].value.u.enumVal()[0]);

        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInstancesExtDT_STRING() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            T_LONGLONG aidMeq = as.getElementByName("meq").getId();

            QueryStructureExt qse = new QueryStructureExt();

            qse.anuSeq = new SelAIDNameUnitId[2];
            qse.anuSeq[0] = new SelAIDNameUnitId();
            qse.anuSeq[0].attr = new AIDName(aidMeq, "iname"); // meq
            qse.anuSeq[0].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[0].aggregate = AggrFunc.NONE;
            qse.anuSeq[1] = new SelAIDNameUnitId();
            qse.anuSeq[1].attr = new AIDName(aidMeq, "aodt"); // meq
            qse.anuSeq[1].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[1].aggregate = AggrFunc.NONE;

            SelValueExt selValueExt = new SelValueExt();
            selValueExt.attr = new AIDNameUnitId();
            selValueExt.attr.attr = new AIDName();
            selValueExt.attr.attr.aid = aidMeq;
            selValueExt.attr.attr.aaName = "iname";
            selValueExt.attr.unitId = new T_LONGLONG(0, 0);
            selValueExt.oper = SelOpcode.CI_EQ;
            selValueExt.value = new TS_Value();
            selValueExt.value.flag = (short) 15;
            selValueExt.value.u = new TS_Union();
            selValueExt.value.u.stringVal("LS.LEFT SIDE");
            qse.condSeq = new SelItem[1];
            qse.condSeq[0] = new SelItem();
            qse.condSeq[0].value(selValueExt);

            qse.joinSeq = new JoinDef[0];
            qse.groupBy = new AIDName[0];
            qse.orderBy = new SelOrder[0];

            ResultSetExt[] resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length);
            assertEquals(ODSHelper.asJLong(aidMeq), ODSHelper.asJLong(resSetExt[0].firstElems[0].aid)); // aid
            assertEquals(2, resSetExt[0].firstElems[0].values.length); // no of attrs
            assertEquals(2, resSetExt[0].firstElems[0].values[0].value.flag.length); // no of rows
            assertEquals("LS.Left Side", resSetExt[0].firstElems[0].values[0].value.u.stringVal()[0]); // a value
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetInstancesExtDS_LONGLONG() {
        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            T_LONGLONG aidMeq = as.getElementByName("meq").getId();

            QueryStructureExt qse = new QueryStructureExt();

            qse.anuSeq = new SelAIDNameUnitId[2];
            qse.anuSeq[0] = new SelAIDNameUnitId();
            qse.anuSeq[0].attr = new AIDName(aidMeq, "iname"); // meq
            qse.anuSeq[0].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[0].aggregate = AggrFunc.NONE;
            qse.anuSeq[1] = new SelAIDNameUnitId();
            qse.anuSeq[1].attr = new AIDName(aidMeq, "aodt"); // meq
            qse.anuSeq[1].unitId = ODSHelper.asODSLongLong(0);
            qse.anuSeq[1].aggregate = AggrFunc.NONE;

            SelValueExt selValueExt = new SelValueExt();
            selValueExt.attr = new AIDNameUnitId();
            selValueExt.attr.attr = new AIDName();
            selValueExt.attr.attr.aid = aidMeq;
            selValueExt.attr.attr.aaName = "meq_iid";
            selValueExt.attr.unitId = new T_LONGLONG(0, 0);
            selValueExt.oper = SelOpcode.INSET;
            selValueExt.value = new TS_Value();
            selValueExt.value.flag = (short) 15;
            selValueExt.value.u = new TS_Union();
            selValueExt.value.u.longlongSeq(new T_LONGLONG[] { new T_LONGLONG(0, 94), new T_LONGLONG(0, 60) });
            qse.condSeq = new SelItem[1];
            qse.condSeq[0] = new SelItem();
            qse.condSeq[0].value(selValueExt);

            qse.joinSeq = new JoinDef[0];
            qse.groupBy = new AIDName[0];
            qse.orderBy = new SelOrder[0];

            ResultSetExt[] resSetExt = applElemAccess.getInstancesExt(qse, 0);
            assertEquals(1, resSetExt[0].firstElems.length); // no of aes
            assertEquals(ODSHelper.asJLong(aidMeq), ODSHelper.asJLong(resSetExt[0].firstElems[0].aid)); // aid
            assertEquals(2, resSetExt[0].firstElems[0].values.length); // no of attrs
            assertEquals(2, resSetExt[0].firstElems[0].values[0].value.flag.length); // no of rows
            assertEquals("LS.Right Side", resSetExt[0].firstElems[0].values[0].value.u.stringVal()[0]); // a value
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testGetValueMatrix() {
        try {
            // ValueMatrix in AoSubMatrix
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeDts = applicationStructure.getElementByName("sm");
            InstanceElement ieDts = aeDts.getInstanceById(ODSHelper.asODSLongLong(33));
            ValueMatrix vm = applElemAccess.getValueMatrix(new ElemId(aeDts.getId(), ieDts.getId()));
            assertEquals(ValueMatrixMode.CALCULATED, vm.getMode());
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            // ValueMatrix in AoMeasurement
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeDts = applicationStructure.getElementByName("dts");
            InstanceElement ieDts = aeDts.getInstanceById(ODSHelper.asODSLongLong(32));
            applElemAccess.getValueMatrix(new ElemId(aeDts.getId(), ieDts.getId()));
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetValueMatrixInMode() {
        try {
            // ValueMatrix in AoSubMatrix
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeDts = applicationStructure.getElementByName("sm");
            InstanceElement ieDts = aeDts.getInstanceById(ODSHelper.asODSLongLong(33));
            ValueMatrix vm = applElemAccess.getValueMatrixInMode(new ElemId(aeDts.getId(), ieDts.getId()),
                                                                 ValueMatrixMode.STORAGE);
            assertEquals(ValueMatrixMode.STORAGE, vm.getMode());
        } catch (AoException e) {
            fail(e.reason);
        }

        try {
            // ValueMatrix in AoMeasurement
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            ApplicationElement aeDts = applicationStructure.getElementByName("dts");
            InstanceElement ieDts = aeDts.getInstanceById(ODSHelper.asODSLongLong(32));
            applElemAccess.getValueMatrixInMode(new ElemId(aeDts.getId(), ieDts.getId()), ValueMatrixMode.STORAGE);
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetAttributeRights() {
        try {
            applElemAccess.setAttributeRights(null, null, null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetElementRights() {
        try {
            applElemAccess.setElementRights(null, null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInstanceRights() {
        try {
            applElemAccess.setInstanceRights(null, null, null, 0, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetAttributeRights() {
        try {
            applElemAccess.getAttributeRights(null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetElementRights() {
        try {
            applElemAccess.getElementRights(null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInstanceRights() {
        try {
            applElemAccess.getInstanceRights(null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetElementInitialRights() {
        try {
            applElemAccess.setElementInitialRights(null, null, 0, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInstanceInitialRights() {
        try {
            applElemAccess.setInstanceInitialRights(null, null, null, 0, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testSetInitialRightReference() {
        try {
            applElemAccess.setInitialRightReference(null, null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInitialRightReference() {
        try {
            applElemAccess.getInitialRightReference(null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetElementInitialRights() {
        try {
            applElemAccess.getElementInitialRights(null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    @Test
    public void testGetInstanceInitialRights() {
        try {
            applElemAccess.getInstanceInitialRights(null, null);
            fail("AoException expected");
        } catch (AoException e) {
            assertEquals(ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ApplElemAccessImplTest.class);
    }

}
