package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ElemId;
import org.asam.ods.InstanceElement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.exporter.ExporterImpl;
import de.rechner.openatfx.exporter.ExporterImplTest;
import de.rechner.openatfx.exporter.IExporter;
import de.rechner.openatfx.util.ODSHelper;


public class WriteExtCompValuesTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testSetValueExtComp() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ExporterImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        try {
            File sourceFile = new File(url.getFile());
            AoSession sourceSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                      .newSession("FILENAME=" + sourceFile);
            // File targetFile = File.createTempFile("test", ".atfx");
            File targetFile = new File("D:/PUBLIC/test.atfx");
            // targetFile.deleteOnExit();
            IExporter exporter = new ExporterImpl();
            // meq
            ElemId elemId = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));
            exporter.export(sourceSession, new ElemId[] { elemId }, targetFile, new Properties());

            // open target session
            AoSession targetSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                      .newSession("FILENAME=" + targetFile);
            targetSession.setContextString("write_mode", "file");
            targetSession.startTransaction();

            ApplicationStructure as = targetSession.getApplicationStructure();
            ApplicationElement aeMea = as.getElementByName("dts");
            ApplicationElement aeMeq = as.getElementByName("meq");
            ApplicationElement aeSm = as.getElementByName("sm");
            ApplicationElement aeLc = as.getElementByName("lc");
            ApplicationRelation relMeqMea = as.getRelations(aeMeq, aeMea)[0];
            ApplicationRelation relLcSm = as.getRelations(aeLc, aeSm)[0];
            ApplicationRelation relLcMeq = as.getRelations(aeLc, aeMeq)[0];
            InstanceElement ieMea = aeMea.getInstanceByName("Detector;rms A fast - Zusammenfassung");
            InstanceElement ieSm = aeSm.getInstanceByName("Detector;rms A fast(Zusammenfassung)");

            // DT_SHORT
            InstanceElement ieMeq = aeMeq.createInstance("meq_DT_SHORT");
            ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 2));
            ieMeq.createRelation(relMeqMea, ieMea);
            InstanceElement ieLc = aeLc.createInstance("lc_DT_SHORT");
            short[] shortValues = new short[167];
            Arrays.fill(shortValues, (short) 42);
            ieLc.setValue(ODSHelper.createShortSeqNVU("values", shortValues));
            ieLc.createRelation(relLcSm, ieSm);
            ieLc.createRelation(relLcMeq, ieMeq);

            // DT_FLOAT
            ieMeq = aeMeq.createInstance("meq_DT_FLOAT");
            ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 3));
            ieMeq.createRelation(relMeqMea, ieMea);
            ieLc = aeLc.createInstance("lc_DT_FLOAT");
            float[] floatValues = new float[167];
            Arrays.fill(floatValues, 42.42f);
            ieLc.setValue(ODSHelper.createFloatSeqNVU("values", floatValues));
            ieLc.createRelation(relLcSm, ieSm);
            ieLc.createRelation(relLcMeq, ieMeq);

            // DT_BOOLEAN
            // ieMeq = aeMeq.createInstance("meq_DT_BOOLEAN");
            // ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 4));
            // ieMeq.createRelation(relMeqMea, ieMea);
            // ieLc = aeLc.createInstance("lc_DT_BOOLEAN");
            // boolean[] booleanValues = new boolean[167];
            // Arrays.fill(booleanValues, false);
            // ieLc.setValue(ODSHelper.createBooleanSeqNVU("values", booleanValues));
            // ieLc.createRelation(relLcSm, ieSm);
            // ieLc.createRelation(relLcMeq, ieMeq);

            // DT_BYTE
            // ieMeq = aeMeq.createInstance("meq_DT_BYTE");
            // ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 5));
            // ieMeq.createRelation(relMeqMea, ieMea);
            // ieLc = aeLc.createInstance("lc_DT_BYTE");
            // byte[] byteValues = new byte[167];
            // Arrays.fill(byteValues, (byte) 42);
            // ieLc.setValue(ODSHelper.createByteSeqNVU("values", byteValues));
            // ieLc.createRelation(relLcSm, ieSm);
            // ieLc.createRelation(relLcMeq, ieMeq);

            // DT_LONG
            ieMeq = aeMeq.createInstance("meq_DT_LONG");
            ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 6));
            ieMeq.createRelation(relMeqMea, ieMea);
            ieLc = aeLc.createInstance("lc_DT_LONG");
            int[] longValues = new int[167];
            Arrays.fill(longValues, 42);
            longValues[0] = Integer.MIN_VALUE;
            longValues[166] = Integer.MAX_VALUE;
            ieLc.setValue(ODSHelper.createLongSeqNVU("values", longValues));
            ieLc.createRelation(relLcSm, ieSm);
            ieLc.createRelation(relLcMeq, ieMeq);
            assertEquals(true, Arrays.equals(longValues, ieLc.getValue("values").value.u.longSeq()));

            // DT_DOUBLE
            ieMeq = aeMeq.createInstance("meq_DT_DOUBLE");
            ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 7));
            ieMeq.createRelation(relMeqMea, ieMea);
            ieLc = aeLc.createInstance("lc_DT_DOUBLE");
            double[] doubleValues = new double[167];
            Arrays.fill(doubleValues, 42.42d);
            doubleValues[0] = Double.MIN_VALUE;
            doubleValues[166] = Double.MAX_VALUE;
            ieLc.setValue(ODSHelper.createDoubleSeqNVU("values", doubleValues));
            ieLc.createRelation(relLcSm, ieSm);
            ieLc.createRelation(relLcMeq, ieMeq);
            assertEquals(true, Arrays.equals(doubleValues, ieLc.getValue("values").value.u.doubleSeq()));

            // DT_LONGLONG
            ieMeq = aeMeq.createInstance("meq_DT_LONGLONG");
            ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 8));
            ieMeq.createRelation(relMeqMea, ieMea);
            ieLc = aeLc.createInstance("lc_DT_LONGLONG");
            long[] longlongValues = new long[167];
            Arrays.fill(longlongValues, 42);
            longlongValues[0] = Long.MIN_VALUE;
            longlongValues[166] = Long.MAX_VALUE;
            ieLc.setValue(ODSHelper.createLongLongSeqNVU("values", longlongValues));
            ieLc.createRelation(relLcSm, ieSm);
            ieLc.createRelation(relLcMeq, ieMeq);
            assertEquals(true, Arrays.equals(longlongValues,
                                             ODSHelper.asJLong(ieLc.getValue("values").value.u.longlongSeq())));

            // DT_DATE
            // ieMeq = aeMeq.createInstance("meq_DT_DATE");
            // ieMeq.setValue(ODSHelper.createEnumNVU("aodt", 10));
            // ieMeq.createRelation(relMeqMea, ieMea);
            // ieLc = aeLc.createInstance("lc_DT_DATE");
            // String[] dateValues = new String[167];
            // Arrays.fill(dateValues, "19790520035657");
            // ieLc.setValue(ODSHelper.createDateSeqNVU("values", dateValues));
            // ieLc.createRelation(relLcSm, ieSm);
            // ieLc.createRelation(relLcMeq, ieMeq);
            // assertEquals(true, Arrays.equals(dateValues, ieLc.getValue("values").value.u.dateSeq()));

            targetSession.commitTransaction();
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
