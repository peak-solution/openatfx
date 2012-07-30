package de.rechner.openatfx.exporter;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ElemId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ODSHelper;


public class ExporterImplTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testExport() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ExporterImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        try {
            File sourceFile = new File(url.getFile());
            // File sourceFile = new File(
            // "D:/PUBLIC/TestData/atfx/pak/201111111027_IN-PJ 967_ohne/SB_2.Gang_2011.11.11_10.52.53/transfer.atfx");
            AoSession sourceSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                      .newSession("FILENAME=" + sourceFile);
            // File targetFile = new File("D:/PUBLIC/export.atfx");
            File targetFile = File.createTempFile("test", "atfx");
            targetFile.deleteOnExit();
            IExporter exporter = new ExporterImpl();
            // meq
            ElemId elemId = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));
            exporter.export(sourceSession, new ElemId[] { elemId }, targetFile, new Properties());

            // T_LONGLONG aid = sourceSession.getApplicationStructure().getElementsByBaseType("AoTest")[0].getId();
            // T_LONGLONG iid = sourceSession.getApplicationStructure().getElementById(aid).getInstances("*").nextOne()
            // .getId();
            // ElemId elemId = new ElemId(aid, iid);
            // exporter.export(sourceSession, new ElemId[] { elemId }, targetFile, new Properties());

        } catch (AoException e) {
            fail(e.reason);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
