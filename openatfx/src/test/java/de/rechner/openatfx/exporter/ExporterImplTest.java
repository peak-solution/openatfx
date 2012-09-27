package de.rechner.openatfx.exporter;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ElemId;
import org.asam.ods.InstanceElement;
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

//    @Test
    public void testExport() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ExporterImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        try {
            File sourceFile = new File(url.getFile());
            AoSession sourceSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                      .newSession("FILENAME=" + sourceFile);
            File targetFile = File.createTempFile("test", "atfx");
            targetFile.deleteOnExit();
            IExporter exporter = new ExporterImpl();
            // meq
            ElemId elemId = new ElemId(ODSHelper.asODSLongLong(19), ODSHelper.asODSLongLong(32));
            exporter.export(sourceSession, new ElemId[] { elemId }, targetFile, new Properties());

            // try to open target session
            AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + targetFile);
        } catch (AoException e) {
            fail(e.reason);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testExport1() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        File source = new File("D:/PUBLIC/Crosstest_2012/Test_Data/MBBM-VAS/nvh-geometry/x-test_2012_nvh_geometry.atfx");
        File target = new File("D:/PUBLIC/Crosstest_2012/Test_Data/MBBM-VAS/nvh-geometry/export.atfx");
        try {
            AoSession sourceSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + source);
            IExporter exporter = new ExporterImpl();
            // meq
            ApplicationElement aeTest = sourceSession.getApplicationStructure().getElementsByBaseType("AoTest")[0];
            InstanceElement ieTest = aeTest.getInstances("*").nextOne();

            ElemId elemId = new ElemId(aeTest.getId(), ieTest.getId());
            exporter.export(sourceSession, new ElemId[] { elemId }, target, new Properties());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
