package de.rechner.openatfx.exporter;

import static org.junit.Assert.fail;

import java.io.File;
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
            AoSession sourceSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                      .newSession("FILENAME=" + new File(url.getFile()));
            File targetFile = new File("D:/PUBLIC/export.atfx");
            IExporter exporter = new ExporterImpl();
            exporter.export(sourceSession,
                            new ElemId[] { new ElemId(ODSHelper.asODSLongLong(21), ODSHelper.asODSLongLong(60)) },
                            targetFile, new Properties());

        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
