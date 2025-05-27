package com.peaksolution.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.NameValue;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.OpenAtfxConstants;


/**
 * Test case for <code>com.peaksolution.openatfx.AoFactoryImpl</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class AoFactoryImplTest {

    private static AoFactory aoFactory;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        try {
            aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);
        } catch (AoException ex) {
            throw new RuntimeException("Encountered error getting AoFactory " + ex.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#getInterfaceVersion()}.
     */
    @Test
    void testGetInterfaceVersion() {
        try {
            assertEquals(OpenAtfxConstants.DEF_ODS_API_VERSION, aoFactory.getInterfaceVersion());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#getType()}.
     */
    @Test
    void testGetType() {
        try {
            assertEquals("XATF-ASCII", aoFactory.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#getName()}.
     */
    @Test
    void testGetName() {
        try {
            assertEquals("XATF-ASCII", aoFactory.getName());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#getDescription()}.
     */
    @Test
    void testGetDescription() {
        try {
            assertEquals("ATFX file driver for ASAM OO-API", aoFactory.getDescription());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#newSession(java.lang.String)}.
     */
    @Test
    void testNewSession() {
        try {
            URL url = AoFactoryImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
            AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
            assertEquals("asam31", aoSession.getType());
            aoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.AoFactoryImpl#newSessionNameValue(org.asam.ods.NameValue[])}
     * .
     */
    @Test
    void testNewSessionNameValue() {
        try {
            URL url = AoFactoryImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
            NameValue[] auth = new NameValue[1];
            auth[0] = new NameValue();
            auth[0].valName = "FILENAME";
            auth[0].value = new TS_Value();
            auth[0].value.flag = (short) 15;
            auth[0].value.u = new TS_Union();
            auth[0].value.u.stringVal(url.getPath());
            AoSession aoSession = aoFactory.newSessionNameValue(auth);
            assertEquals("asam31", aoSession.getType());
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
