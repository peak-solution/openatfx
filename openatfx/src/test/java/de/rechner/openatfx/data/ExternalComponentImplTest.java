package de.rechner.openatfx.data;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.EnumerationDefinitionImplTest;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.data.ExternalComponentImpl</code>.
 * 
 * @author Christian Rechner
 */
public class ExternalComponentImplTest {

    private static AoSession aoSession;
    private ExternalComponentImpl extComp;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = EnumerationDefinitionImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Before
    public void setUp() throws Exception {
        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement ae = as.getElementByName("ec");
        InstanceElement ieExtComp = ae.getInstanceById(ODSHelper.asODSLongLong(2));
        this.extComp = new ExternalComponentImpl(ieExtComp);
    }

    @After
    public void tearDown() throws Exception {
        this.extComp.close();
    }

    @Test
    public void testGetValue() {}

    @Test
    public void testGetValueSeq() {
    }

    @Test
    public void testClose() {}

}
