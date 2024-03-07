package de.rechner.openatfx.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.asam.ods.AIDName;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseElement;
import org.asam.ods.ElemId;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.AoSessionImplTest;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxWriterTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxWriterTest {

    @TempDir
    File exportFolder;

    private AoSession aoSession;
    ORB orb;

    @BeforeEach
    public void setUpBefore() throws Exception {
        orb = ORB.init(new String[0], System.getProperties());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (aoSession != null) {
            aoSession.close();
            aoSession = null;
        }
    }

    @Test
    public void testWriteXML() {
        try {
            URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/example.atfx");
            aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                        .newSession("FILENAME=" + new File(url.getFile()));
            aoSession.startTransaction();
            // aoSession.commitTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testWriteXML2() {
        try {
            URL url = AoSessionImplTest.class.getResource("/de/rechner/openatfx/test.atfx");
            aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                        .newSession("FILENAME=" + new File(url.getFile()));

            aoSession.startTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testWriteInstanceAttributesToXML() throws IOException {
        try {
            File atfxFile = new File(exportFolder, "unit_test.atfx");
            aoSession = AoServiceFactory.getInstance().newEmptyAoSession(orb, atfxFile, "asam35");
            ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
            BaseElement be = applicationStructure.getSession().getBaseStructure().getElementByType("AoTest");
            ApplicationElement aePrj = applicationStructure.createElement(be);
            aePrj.setName("prj");

            be = applicationStructure.getSession().getBaseStructure().getElementByType("AoPhysicalDimension");
            ApplicationElement aeDim = applicationStructure.createElement(be);
            aeDim.setName("dim");

            be = applicationStructure.getSession().getBaseStructure().getElementByType("AoUnit");
            ApplicationElement aeUnit = applicationStructure.createElement(be);
            aeUnit.setName("unt");

            aoSession.startTransaction();
            // insert test unit
            AIDNameValueSeqUnitId[] unitAnvsuis = new AIDNameValueSeqUnitId[1];
            // unit name
            TS_UnionSeq untNameUnion = new TS_UnionSeq();
            untNameUnion.stringVal(new String[] { "s" });
            TS_ValueSeq unitNameValue = new TS_ValueSeq(untNameUnion, new short[] { 15 });
            unitAnvsuis[0] = new AIDNameValueSeqUnitId(new AIDName(aeUnit.getId(),
                                                                   aeUnit.getAttributeByBaseName("name").getName()),
                                                       new T_LONGLONG(0, 0), unitNameValue);
            ElemId[] unitIds = aoSession.getApplElemAccess().insertInstances(unitAnvsuis);

            // insert project instance with instance attrs
            AIDNameValueSeqUnitId[] prjAnvsuis = new AIDNameValueSeqUnitId[3];
            // project name
            TS_UnionSeq prjNameUnion = new TS_UnionSeq();
            prjNameUnion.stringVal(new String[] { "testProject" });
            TS_ValueSeq prjNameValue = new TS_ValueSeq(prjNameUnion, new short[] { 15 });
            prjAnvsuis[0] = new AIDNameValueSeqUnitId(new AIDName(aePrj.getId(),
                                                                  aePrj.getAttributeByBaseName("name").getName()),
                                                      new T_LONGLONG(0, 0), prjNameValue);
            // project instance attr string
            TS_UnionSeq stringUnion = new TS_UnionSeq();
            stringUnion.stringVal(new String[] { "testValue1" });
            TS_ValueSeq stringValue = new TS_ValueSeq(stringUnion, new short[] { 15 });
            prjAnvsuis[1] = new AIDNameValueSeqUnitId(new AIDName(aePrj.getId(), "inst_attr_string"),
                                                      new T_LONGLONG(0, 0), stringValue);
            // project instance attr float with unit
            TS_UnionSeq floatUnion = new TS_UnionSeq();
            floatUnion.floatVal(new float[] { 2.3456f });
            TS_ValueSeq floatValue = new TS_ValueSeq(floatUnion, new short[] { 15 });
            prjAnvsuis[2] = new AIDNameValueSeqUnitId(new AIDName(aePrj.getId(), "inst_attr_float"), unitIds[0].iid,
                                                      floatValue);
            aoSession.getApplElemAccess().insertInstances(prjAnvsuis);

            aoSession.commitTransaction();

            applicationStructure = aoSession.getApplicationStructure();
            aePrj = applicationStructure.getElementByName("prj");
            InstanceElement prjInstance = aePrj.getInstanceByName("testProject");
            NameValueUnit[] values = prjInstance.getValueSeq(new String[] { "inst_attr_string", "inst_attr_float" });
            assertEquals("", values[0].unit);
            assertEquals("s", values[1].unit);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
